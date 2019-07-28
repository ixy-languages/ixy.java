package de.tum.in.net.ixy.generator;

import de.tum.in.net.ixy.Device;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.generator.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.generator.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.generator.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.generator.BuildConfig.LOG_INFO;
import static de.tum.in.net.ixy.generator.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.generator.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.generator.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.generator.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.generator.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.generator.BuildConfig.PREFER_JNI_FULL;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

@Slf4j
@SuppressWarnings("ConstantConditions")
public final class Main {

	///////////////////////////////////////////// PROGRAM STATIC VARIABLES /////////////////////////////////////////////

	/** The list of positional arguments. */
	private static final @NotNull List<String> argumentsList = new ArrayList<>(1);

	/** The list of key-value arguments. */
	private static final @NotNull Map<String, String> argumentsKeyValue = new TreeMap<>();

	/** The default batch size to use. */
	private static final int DEFAULT_BATCH_SIZE = 64;

	/** The default buffer count to use. */
	private static final int DEFAULT_BUFFER_COUNT = 2048;

	//////////////////////////////////////////// GENERATOR STATIC VARIABLES ////////////////////////////////////////////

	/** The maximum value an unsigned short can have (low byte). */
	private static final int MAX_UNSIGNED_SHORT_LOW = 0x00FF;

	/** The maximum value an unsigned short can have (high byte). */
	private static final int MAX_UNSIGNED_SHORT_HIGH = 0xFF00;

	/** The maximum value an unsigned short can have. */
	private static final int MAX_UNSIGNED_SHORT = MAX_UNSIGNED_SHORT_HIGH | MAX_UNSIGNED_SHORT_LOW;

	/** The size of the whole packet data {@link #packetData}. */
	private static final int PACKET_SIZE = 60;

	//////////////////////////////////////////////// UDP PSEUDO-HEADER /////////////////////////////////////////////////

	/** The size of the UDP pseudo header source address field. */
	private static final int UDP_PH_SRC_SIZE = Integer.BYTES;

	/** The offset of the UDP pseudo header source address field. */
	private static final int UDP_PH_SRC_OFFSET = 0;

	/** The size of the UDP pseudo header destination address field. */
	private static final int UDP_PH_DEST_SIZE = Integer.BYTES;

	/** The offset of the UDP pseudo header destination address field. */
	private static final int UDP_PH_DEST_OFFSET = UDP_PH_SRC_OFFSET + UDP_PH_SRC_SIZE;

	/** The size of the UDP pseudo header reserved field. */
	private static final int UDP_PH_RSVD_SIZE = Byte.BYTES;

	/** The offset of the UDP pseudo header reserved field. */
	private static final int UDP_PH_RSVD_OFFSET = UDP_PH_DEST_OFFSET + UDP_PH_DEST_SIZE;

	/** The size of the UDP pseudo header IPv4 protocol field. */
	private static final int UDP_PH_PROTO_SIZE = Byte.BYTES;

	/** The offset of the UDP pseudo header IPv4 protocol field. */
	private static final int UDP_PH_PROTO_OFFSET = UDP_PH_RSVD_OFFSET + UDP_PH_RSVD_SIZE;

	/** The size of the UDP pseudo header UDP length field. */
	private static final int UDP_PH_LEN_SIZE = Short.BYTES;

	/** The offset of the UDP pseudo header UDP length field. */
	private static final int UDP_PH_LEN_OFFSET = UDP_PH_PROTO_OFFSET + UDP_PH_PROTO_SIZE;

	/** The size of the UDP pseudo header. */
	private static final int UDP_PH_SIZE = UDP_PH_SRC_SIZE + UDP_PH_DEST_SIZE + UDP_PH_RSVD_SIZE + UDP_PH_PROTO_SIZE + UDP_PH_LEN_SIZE;

	/////////////////////////////////////////////// PACKET DATA TEMPLATE ///////////////////////////////////////////////

	/** The size of the Ethernet header. */
	private static final int ETHERNET_HEADER_SIZE = 14;

	/** The size of the IP header. */
	private static final int IP_HEADER_SIZE = 20;

	/** The size of the UDP header. */
	private static final int UDP_HEADER_SIZE = 8;

	/**
	 * The default packet data to send which contains the Ethernet frame, the IP packet and the UDP datagram.
	 * <p>
	 * The hardcoded values are the following:
	 * <ul>
	 *     <li>Destination MAC address: {@code 01:02:03:04:05:06}</li>
	 *     <li>Source MAC address: {@code 11:12:13:14:15:16}</li>
	 *     <li>Ethernet frame type: IPv4 ({@code 0x8000})</li>
	 *     <li>IP version and header length: 4 ({@code 0x40}) and 5 ({@code 0x05}), respectively</li>
	 *     <li>IP length: 60 - Ethernet header size ({@code 0x002E})</li>
	 *     <li>Identification, flags & fragmentation: {@code 0x00000000}</li>
	 *     <li>TTL, protocol & checksum: 64 ({@code 0x40}), UDP ({@code 0x11}) and to be defined {@code 0x0000}</li>
	 *     <li>Source IP address: 10.0.0.1 ({@code 0x0A000001})</li>
	 *     <li>Destination IP address: 10.0.0.2 ({@code 0x0A000002})</li>
	 *     <li>Source port: 42 ({@code 0x002A})</li>
	 *     <li>Destination port: 1337 ({@code 0x0539})</li>
	 *     <li>UDP length: 60 - Ethernet header - IP header ({@code 0x001A})</li>
	 *     <li>UDP checksum: to be defined {@code 0x0000}</li>
	 *     <li>UDP payload: ixy ({@code 0x697879})</li>
	 * </ul>
	 */
	private static final @NotNull byte[] packetData = {
			// Ethernet frame header                                       (14 bytes)
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // Destination MAC address (6 bytes)
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // Source MAC address      (6 bytes)
			0x08, 0x00,                         // Ether type: IPv4        (2 bytes)

			// IPv4 header                                                                        (20 bytes)
			0x45, 0x00,                                  // Version (4) & IHL (5), & TOS          (2 bytes)   =|
			(PACKET_SIZE - ETHERNET_HEADER_SIZE) >> 8,   // IP length, high byte                  (1 byte)    =|
			(PACKET_SIZE - ETHERNET_HEADER_SIZE) & 0xFF, // IP length, low byte                   (1 byte)    =|
			0x00, 0x00, 0x00, 0x00,                      // Identification, flags & fragmentation (4 bytes)   =| IPv4 Checksum Fields
			0x40, 0x11, 0x00, 0x00,                      // TTL (64), protocol (UDP) & checksum   (4 bytes)   =|  -|
			0x0A, 0x00, 0x00, 0x01,                      // Source IP address (10.0.0.1)          (4 bytes)   =|  =|
			0x0A, 0x00, 0x00, 0x02,                      // Destination IP address (10.0.0.2)     (4 bytes)   =|  =|
//                                                                                                                 |
			// UDP header                                                                            (8 bytes)     | UDP Pseudo Header
			0x00, 0x2A,                                                   // Source port (42)        (2 bytes)     |
			0x05, 0x39,                                                   // Destination port (1337) (2 bytes)     |
			(PACKET_SIZE - ETHERNET_HEADER_SIZE - IP_HEADER_SIZE) >> 8,   // UDP length, high byte   (1 byte)     =|
			(PACKET_SIZE - ETHERNET_HEADER_SIZE - IP_HEADER_SIZE) & 0xFF, // UDP length, low byte    (1 byte)     =|
			0x00, 0x00,                                                   // UDP checksum            (2 bytes)

			// UDP payload            (3 bytes)
			0x69, 0x78, 0x79 // "ixy" (3 bytes)
	};

	///////////////////////////////////////////////////// ETHERNET /////////////////////////////////////////////////////

	/** The offset of the Ethernet header in the {@link #packetData}. */
	private static final int ETHERNET_HEADER_OFFSET = 0;

	/////////////////////////////////////////////////////// IPv4 ///////////////////////////////////////////////////////

	/** The offset of the IPv4 header in the {@link #packetData}. */
	private static final int IP_HEADER_OFFSET = ETHERNET_HEADER_OFFSET + ETHERNET_HEADER_SIZE;

	/** The offset of the IPv4 protocol field in the {@link #packetData}. */
	private static final int IP_HEADER_PROTO_OFFSET = IP_HEADER_OFFSET + 9;

	/** The size of the IPv4 protocol field in the {@link #packetData}. */
	private static final int IP_HEADER_PROTO_SIZE = Byte.BYTES;

	/** The offset of the IPv4 checksum field in the {@link #packetData}. */
	private static final int IP_CHECKSUM_OFFSET = IP_HEADER_PROTO_OFFSET + IP_HEADER_PROTO_SIZE;

	/** The size of the IPv4 checksum field in the {@link #packetData}. */
	private static final int IP_CHECKSUM_SIZE = Short.BYTES;

	/** The offset of the IPv4 source address int the {@link #packetData}. */
	private static final int IP_HEADER_SRC_OFFSET = IP_CHECKSUM_OFFSET + IP_CHECKSUM_SIZE;

	/** The size of the IPv4 source address in the {@link #packetData}. */
	private static final int IP_HEADER_SRC_SIZE = Integer.BYTES;

	/** The offset of the IPv4 destination address in the {@link #packetData}. */
	private static final int IP_HEADER_DEST_OFFSET = IP_HEADER_SRC_OFFSET + IP_HEADER_SRC_SIZE;

	/////////////////////////////////////////////////////// UDP ////////////////////////////////////////////////////////

	/** The offset of the UDP header in the {@link #packetData}. */
	private static final int UDP_HEADER_OFFSET = IP_HEADER_OFFSET + IP_HEADER_SIZE;

	/** The offset of the UDP length field in the {@link #packetData}. */
	private static final int UDP_HEADER_LEN_OFFSET = UDP_HEADER_OFFSET + 4;

	/** The size of the UDP length field in the {@link #packetData}. */
	private static final int UDP_HEADER_LEN_SIZE = Short.BYTES;

	/** The offset of the UDP checksum field in the {@link #packetData}. */
	private static final int UDP_HEADER_CHECKSUM_OFFSET = UDP_HEADER_LEN_OFFSET + UDP_HEADER_LEN_SIZE;

	/** The size of the UDP checksum field in the {@link #packetData}. */
	private static final int UDP_HEADER_CHECKSUM__SIZE = Short.BYTES;

	/** The offset of the UDP payload in the {@link #packetData}. */
	private static final int UDP_PAYLOAD_OFFSET = UDP_HEADER_OFFSET + UDP_HEADER_SIZE;

	/** The size of the UDP payload in the {@link #packetData}. */
	private static final int UDP_PAYLOAD_SIZE = packetData.length - UDP_PAYLOAD_OFFSET;

	/** The minimum number of batches processed between two prints. */
	private static final int BATCHES_PER_PRINT = 64_000;

	/** The minimum number of nanoseconds between two prints. */
	private static final int NANOS_PER_PRINT = 1_000_000_000;

	/** The memory manager. */
	private static final @NotNull MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	/** The memory pool. */
	private static Mempool mempool;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	// Set the name of the thread
	static {
		Thread.currentThread().setName("Ixy Generator");
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
		val argvDevice = argumentsList.isEmpty() ? "" : argumentsList.remove(0);

		// Access the given PCI device
		try {
			val nic = new IxgbeDevice(argvDevice, 1, 1);
			Runtime.getRuntime().addShutdownHook(new RestoreShutdownHook(nic));

			// Guess whether the device can be used for packet generation
			if (!nic.isMappable()) {
				System.err.println("Legacy device cannot be memory mapped.");
				return;
			} else if (!nic.isSupported()) {
				System.err.println("The selected device driver does not support this NIC.");
				return;
			}

			// Preparing instances to be used
			if (DEBUG >= LOG_INFO) log.info("Allocating resources.");
			var argvCapacity = 0;
			try {
				val argvCapacityStr = argumentsKeyValue.getOrDefault("--buffer-count", String.valueOf(DEFAULT_BUFFER_COUNT));
				argvCapacity = Integer.parseInt(argvCapacityStr);
			} catch (final NumberFormatException e) {
				argvCapacity = DEFAULT_BUFFER_COUNT;
			}
			if (DEBUG >= LOG_DEBUG) log.info(">>> Allocating memory.");
			val dma = mmanager.dmaAllocate((long) argvCapacity * 21, true, true);
			if (DEBUG >= LOG_DEBUG) log.info(">>> Allocating memory pool.");
			mempool = new Mempool(argvCapacity);
			mempool.allocate(PacketBufferWrapper.DATA_OFFSET + PACKET_SIZE, dma);

			// Write the correct data into the packets
			initPackets();

			// Call the packet generator routine
			generate(nic);

		} catch (final FileNotFoundException e) {
			System.err.println("The given device doest not exist.");
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("There was an error while reading the device.");
		}
	}

	/** Blocking function that generates an infinite stream of packets. */
	private static void generate(final @NotNull Device nic) {
		try {
			if (nic.isBound()) {
				if (DEBUG >= LOG_INFO) log.info("Removing drivers from the NIC.");
				nic.unbind();
			}
			if (!nic.isDmaEnabled()) {
				if (DEBUG >= LOG_INFO) log.info("Enabling DMA on the NIC.");
				nic.enableDma();
			}
		} catch (IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("Error while manipulating the NIC.", e);
		}

		if (DEBUG >= LOG_INFO) log.info("Allocating data structures for this driver.");
		nic.configure();

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
		val stats = new Stats();
		val buffers = new PacketBufferWrapper[argvBatchSize];
		var sequence = 0;
		var counter = (short) 0;

		var startTime = System.nanoTime();
		while (true) {
			// Read some data
			val batch = mempool.pollFirst(buffers);
			if (batch == 0) {
				if (DEBUG >= LOG_WARN) log.warn("No more packets buffers available.");
				break;
			}

			// Modify the data
			if (DEBUG >= LOG_DEBUG) log.debug("Updating {} packets.", batch);
			for (var i = 0; i < batch; i += 1) {
				val buffer = buffers[i];
				buffer.putInt(PACKET_SIZE - 4, ++sequence);
			}

			// Send the data
			nic.txBusyWait(0, buffers, 0, batch);

			// Log if necessary
			if (counter++ % BATCHES_PER_PRINT == 0) {
				val endTime = System.nanoTime();
				val nanos = endTime - startTime;
				if (nanos > NANOS_PER_PRINT) {
					nic.readStats(stats);
					try {
						stats.writeStats(System.out, nic.name, nanos);
						System.out.println();
						System.out.flush();
					} catch (IOException e) {
						if (DEBUG >= LOG_ERROR) log.error("Could not write the stats.", e);
					}
					stats.swap();
					counter = 0;
					startTime = endTime;
				}
			}
		}
	}

	/** Uses the template packet data {@link #packetData} to populate the packets with the default data. */
	private static void initPackets() {
		if (DEBUG >= LOG_INFO) log.info("Configuring packets.");

		if (DEBUG >= LOG_DEBUG) log.debug("Computing IPv4 checksum.");
		val checksumIp = checksum(packetData, IP_HEADER_OFFSET, IP_HEADER_SIZE);

		if (DEBUG >= LOG_DEBUG) log.debug("Computing UDP checksum.");
		val wrap = ByteBuffer.wrap(packetData);
		val udpChecksumSize = UDP_PH_SIZE + UDP_HEADER_SIZE + (packetData.length - UDP_PAYLOAD_OFFSET);
		val udpChecksumData = ByteBuffer.allocate(udpChecksumSize + udpChecksumSize % 2);
		// Pseudo header data
		udpChecksumData.putInt(UDP_PH_SRC_OFFSET, wrap.getInt(IP_HEADER_SRC_OFFSET));
		udpChecksumData.putInt(UDP_PH_DEST_OFFSET, wrap.getInt(IP_HEADER_DEST_OFFSET));
		udpChecksumData.put(UDP_PH_RSVD_OFFSET, (byte) 0);
//		udpChecksumData.put(UDP_PH_PROTO_OFFSET, wrap.get(IP_HEADER_PROTO_OFFSET));
		udpChecksumData.putShort(UDP_PH_LEN_OFFSET, wrap.get(UDP_HEADER_LEN_OFFSET));
		// UDP header
		udpChecksumData.position(UDP_PH_SIZE).put(wrap.limit(UDP_HEADER_OFFSET + UDP_HEADER_SIZE).position(UDP_HEADER_OFFSET));
		// UDP payload
		udpChecksumData.position(UDP_PH_SIZE + UDP_HEADER_SIZE).put(wrap.limit(UDP_PAYLOAD_OFFSET + UDP_PAYLOAD_SIZE).position(UDP_PAYLOAD_OFFSET));
		// Compute the checksum
		val checksumUdp = checksum(udpChecksumData.array(), 0, udpChecksumData.capacity());

		if (DEBUG >= LOG_DEBUG) log.debug("Writing IPv4 checksum: 0x{}.", leftPad(checksumIp));
		wrap.putShort(IP_CHECKSUM_OFFSET, checksumIp);

		if (DEBUG >= LOG_DEBUG) log.debug("Writing UDP checksum: 0x{}.", leftPad(checksumUdp));
		wrap.putShort(UDP_HEADER_CHECKSUM_OFFSET, checksumUdp);

		if (DEBUG >= LOG_INFO) {
			log.debug("Ethernet header : {}.", toHexString(packetData, ETHERNET_HEADER_OFFSET, ETHERNET_HEADER_SIZE));
			log.debug("IPv4 header     : {}.", toHexString(packetData, IP_HEADER_OFFSET, IP_HEADER_SIZE));
			log.debug("UDP header      : {}.", toHexString(packetData, UDP_HEADER_OFFSET, UDP_HEADER_SIZE));
			log.debug("UDP payload     : {}.", toHexString(packetData, UDP_PAYLOAD_OFFSET, UDP_PAYLOAD_SIZE));
		}

		if (DEBUG >= LOG_DEBUG) {
			var counter = 0;
			for (val packetBufferWrapper : mempool) {
				if (packetBufferWrapper == null) {
					log.error("A packet buffer wrapper was 'null' and that MUST NOT happen.");
					System.exit(0);
				}

				log.debug(">>> Writing packet data to packet #{}: {}", counter++, packetBufferWrapper);

				if (DEBUG >= LOG_TRACE) log.trace("Setting packet size.");
				packetBufferWrapper.setSize(packetData.length);

				if (DEBUG >= LOG_TRACE) log.trace("Write data.");
				packetBufferWrapper.put(0, packetData.length, packetData);
			}
		} else {
			for (val packetBufferWrapper : mempool) {
				if (packetBufferWrapper == null) {
					if (DEBUG >= LOG_ERROR) log.error("A packet buffer wrapper was 'null' and that MUST NOT happen.");
					System.exit(0);
				}
				packetBufferWrapper.setSize(packetData.length);
				packetBufferWrapper.put(0, packetData.length, packetData);
			}
		}
	}

	/**
	 * Computes the IPv4 header checksum based on the bytes of a packet.
	 *
	 * @param data   The data to compute the checksum of.
	 * @param offset The offset in which the IPv4 header starts.
	 * @return The IPv4 checksum.
	 */
	private static short checksum(final @NotNull byte[] data, final int offset, int length) {
		if (!OPTIMIZED) {
			if (offset < 0) throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be positive.");
			if (offset + length > data.length) {
				throw new IllegalArgumentException("There are not as many bytes to read as expected.");
			}
		}

		// Trace message
		if (DEBUG >= LOG_WARN) {
			if (DEBUG >= LOG_DEBUG) log.trace("Computing checksum of {} bytes.", length);
			if (length % 2 != 0) {
				log.warn("Odd-sized headers are not supported, the last byte will be ignored.");
				length -= 1;
			}
		}

		// Create a short-based view of the array using the native byte order
		val buffer = ByteBuffer.wrap(data, offset, length).order(ByteOrder.nativeOrder()).asShortBuffer();

		// Compute the sum of all shorts
		var sum = 0;
		for (int i = 0; i < length / 2; i += 1) {
			sum += buffer.get(i);
		}

		// Fix the carried bits
		val carry = (sum >> Short.SIZE) & MAX_UNSIGNED_SHORT_LOW;
		var finalSum = (sum & MAX_UNSIGNED_SHORT) + carry;
		return (short) ~((short) finalSum & MAX_UNSIGNED_SHORT);
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
				if (parts.length > 1) { ;
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

	/**
	 * Returns the hex string representation of a {@code byte} array.
	 *
	 * @param data   The array of data.
	 * @param offset The offset.
	 * @param length The length.
	 * @return The hex string representation.
	 */
	private static @NotNull String toHexString(final @NotNull byte[] data, final int offset, final int length) {
		return IntStream.range(offset, offset + length)
				.parallel()
				.mapToObj(i -> leftPad(data[i]))
				.map(s -> "0x" + s)
				.collect(Collectors.joining(" "));
	}

}
