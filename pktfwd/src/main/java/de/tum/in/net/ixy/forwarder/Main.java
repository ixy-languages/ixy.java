package de.tum.in.net.ixy.forwarder;

import de.tum.in.net.ixy.Stats;
import de.tum.in.net.ixy.ixgbe.IxgbeDevice;
import de.tum.in.net.ixy.memory.JniMemoryManager;
import de.tum.in.net.ixy.memory.MemoryManager;
import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.PacketBufferWrapper;
import de.tum.in.net.ixy.memory.SmartJniMemoryManager;
import de.tum.in.net.ixy.memory.SmartUnsafeMemoryManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.tum.in.net.ixy.utils.Threads;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.forwarder.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_INFO;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.forwarder.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.forwarder.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.forwarder.BuildConfig.PREFER_JNI_FULL;

@Slf4j
@SuppressWarnings({"ConstantConditions", "UseOfSystemOutOrSystemErr", "CallToSystemGC"})
public final class Main {

	///////////////////////////////////////////// PROGRAM STATIC VARIABLES /////////////////////////////////////////////

	/** The list of positional arguments. */
	private static final @NotNull List<String> argumentsList = new ArrayList<>(1);

	/** The list of key-value arguments. */
	private static final @NotNull Map<String, String> argumentsKeyValue = new TreeMap<>();

	/** The default batch size to use. */
	private static final int DEFAULT_BATCH_SIZE = 64;

	/////////////////////////////////////////////// PACKET DATA TEMPLATE ///////////////////////////////////////////////

	/** The minimum number of batches processed between two prints. */
	private static final int ITERATIONS_PER_NANOTIME = 0xFFFFFF;

	/** The minimum number of nanoseconds between two prints. */
	private static final int NANOS_PER_PRINT = 1_000_000_000;

	/** The memory manager. */
	@SuppressWarnings("NestedConditionalExpression")
	private static final @NotNull MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	// Set the name of the thread
	static {
		Thread.currentThread().setName("Ixy Forwarder");
	}

	/**
	 * Entry point of the application.
	 *
	 * @param argv The command line arguments.
	 */
	public static void main(final @NotNull String[] argv) {
		// Check that the memory manager works
		if (!mmanager.isValid()) {
			if (DEBUG >= LOG_WARN) {
				log.warn("The memory manager is not valid. Either the Unsafe object is not available or the JNI library not linked.");
			}
			return;
		} else if (DEBUG >= LOG_INFO) {
			log.info("The memory manager is valid.");
		}

		// Parse the parameters and read the required ones
		parseArguments(argv);
		val argvDevice1 = argumentsList.isEmpty() ? "" : argumentsList.remove(0);
		val argvDevice2 = argumentsList.isEmpty() ? "" : argumentsList.remove(0);

		// Access the given PCI device
		try {
			val nic1 = new IxgbeDevice(argvDevice1, 1, 1);
			val nic2 = new IxgbeDevice(argvDevice2, 1, 1);
			Runtime.getRuntime().addShutdownHook(new RestoreShutdownHook(nic1, nic2));

			// Guess whether the device can be used for packet generation
			if (!nic1.isMappable() || !nic2.isMappable()) {
				System.err.println("Legacy device cannot be memory mapped.");
				return;
			} else if (!nic1.isSupported() || !nic2.isSupported()) {
				System.err.println("The selected device driver does not support this NIC.");
				return;
			}

			// Call the packet forwarder routine
			forward(nic1, nic2);

		} catch (final FileNotFoundException e) {
			System.err.println("The given device doest not exist.");
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("There was an error while reading the device.");
		}
	}

	/** Blocking function that generates an infinite stream of packets. */
	private static void forward(final @NotNull IxgbeDevice nic1, final @NotNull IxgbeDevice nic2) {
		try {
			if (nic1.isBound()) {
				if (DEBUG >= LOG_INFO) log.info("Removing drivers from the first NIC.");
				nic1.unbind();
			}
			if (nic2.isBound()) {
				if (DEBUG >= LOG_INFO) log.info("Removing drivers from the second NIC.");
				nic2.unbind();
			}
			if (!nic1.isDmaEnabled()) {
				if (DEBUG >= LOG_INFO) log.info("Enabling DMA on the first NIC.");
				nic1.enableDma();
			}
			if (!nic2.isDmaEnabled()) {
				if (DEBUG >= LOG_INFO) log.info("Enabling DMA on the second NIC.");
				nic2.enableDma();
			}
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("Error while manipulating the NIC.", e);
		}

		if (DEBUG >= LOG_INFO) log.info("Allocating data structures for this driver.");
		nic1.configure();
		nic2.configure();

		if (DEBUG >= LOG_DEBUG) log.debug("Creating counters.");
		var argvBatchSize = 0;
		try {
			val argvBatchSizeStr = argumentsKeyValue.getOrDefault("--batch-size", String.valueOf(DEFAULT_BATCH_SIZE));
			argvBatchSize = Integer.parseInt(argvBatchSizeStr);
		} catch (NumberFormatException e) {
			argvBatchSize = DEFAULT_BATCH_SIZE;
		}

		if (DEBUG >= LOG_DEBUG) log.debug("Forcing GC pause to release memory before starting.");
		System.gc();

		// Objects to be used inside the loop
		val stats1 = new Stats();
		val stats2 = new Stats();
		val buffers = new PacketBufferWrapper[argvBatchSize];
		var counter = (short) 0;

		var startTime = System.nanoTime();
		while (true) {
			forward(nic1, 0, nic2, 0, buffers);
			forward(nic2, 0, nic1, 0, buffers);

			// Log if necessary
			if (counter++ % ITERATIONS_PER_NANOTIME == 0) {
				val endTime = System.nanoTime();
				val nanos = endTime - startTime;
				if (nanos > NANOS_PER_PRINT) {
					nic1.readStats(stats1);
					nic2.readStats(stats2);
					try {
						stats1.writeStats(System.out, nic1.name, nanos);
						System.out.println();
						stats2.writeStats(System.out, nic2.name, nanos);
						System.out.println(System.lineSeparator());
					} catch (final IOException e) {
						if (DEBUG >= LOG_ERROR) log.error("Could not write the stats.", e);
					}
					stats1.swap();
					stats2.swap();
					counter = 0;
					startTime = endTime;
				}
			}
		}
	}

	private static void forward(final @NotNull IxgbeDevice rxDev,
								final int rxQueue,
								final @NotNull IxgbeDevice txDev,
								final int txQueue,
								final @NotNull PacketBufferWrapper[] buffers) {
		// Read packets from the source
		val rxCount = rxDev.rxBatch(rxQueue, buffers, 0, buffers.length);

		// If we received something, get the memory pool of the first packet of the batch forward as many packets as
		// possible and drop the unsent packets
		if (rxCount > 0) {
			for (var i = 0; i < rxCount; i++) {
				buffers[i].putInt(0, 1);
			}
			val mempool = Mempool.find(buffers[0]);
			val txCount = txDev.txBatch(txQueue, buffers, 0, rxCount);
			for (var i = txCount; i < rxCount; i += 1) {
				mempool.push(buffers[i]);
				buffers[i] = null;
			}
		}
	}

	/**
	 * Parses a list of arguments and stores them in {@link #argumentsList} and {@link #argumentsKeyValue}.
	 *
	 * @param argv The list of arguments.
	 */
	private static void parseArguments(final @NotNull String[] argv) {
		if (DEBUG >= LOG_DEBUG) log.debug("Parsing parameters:");
		for (var i = 0; i < argv.length; i += 1) {
			val param = argv[i].trim();
			if (param.isEmpty()) {
				if (DEBUG >= LOG_WARN) log.warn(">>> Skipping empty parameter #{}.", i);
			} else if (param.charAt(0) != '-') {
				if (DEBUG >= LOG_DEBUG) log.debug(">>> Found positional argument '{}'.", param);
				argumentsList.add(param);
			} else if (i + 1 < argv.length && argv[i + 1].charAt(0) != '-' || param.contains("=")) {
				val parts = param.split("=");
				if (parts.length > 1) {
					val value = param.replaceFirst(Pattern.quote(parts[0]), "");
					if (DEBUG >= LOG_DEBUG) log.debug(">>> Found key value argument '{}' => '{}'.", parts[0], value);
					argumentsKeyValue.put(parts[0], value);
				} else {
					val value = argv[++i];
					if (DEBUG >= LOG_DEBUG) log.debug(">>> Found key value argument '{}' => '{}'.", param, value);
					argumentsKeyValue.put(param, value);
				}
			} else {
				if (DEBUG >= LOG_DEBUG) log.debug(">>> Found option argument '{}'.", param);
			}
		}
	}

}
