package de.tum.in.net.ixy.generator;

import de.tum.in.net.ixy.generic.IxyDevice;
import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import de.tum.in.net.ixy.generic.IxyStats;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Ixy's implementation of a packet generator.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@Builder(builderClassName = "Builder")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class IxyGenerator implements Runnable {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The maximum value an unsigned short can have (low byte). */
	private static final int MAX_UNSIGNED_SHORT_LOW = 0x00FF;

	/** The maximum value an unsigned short can have (high byte). */
	private static final int MAX_UNSIGNED_SHORT_HIGH = 0xFF00;

	/** The maximum value an unsigned short can have. */
	private static final int MAX_UNSIGNED_SHORT = MAX_UNSIGNED_SHORT_HIGH | MAX_UNSIGNED_SHORT_LOW;

	/** The size of the whole packet data {@link #packetData}. */
	private static final int PACKET_SIZE = 60;

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
	private static final byte[] packetData = {
			// Ethernet frame header                                       (14 bytes)
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // Destination MAC address (6 bytes)
			0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // Source MAC address      (6 bytes)
			0x08, 0x00,                         // Ether type: IPv4        (2 bytes)

			// IPv4 header                                                                        (20 bytes)
			0x45, 0x00,                                  // Version (4) & IHL (5), & TOS          (2 bytes)
			(PACKET_SIZE - ETHERNET_HEADER_SIZE) >> 8,   // IP length, high byte                  (1 byte)
			(PACKET_SIZE - ETHERNET_HEADER_SIZE) & 0xFF, // IP length, low byte                   (1 byte)
			0x00, 0x00, 0x00, 0x00,                      // Identification, flags & fragmentation (4 bytes)
			0x40, 0x11, 0x00, 0x00,                      // TTL (64), protocol (UDP) & checksum   (4 bytes)
			0x0A, 0x00, 0x00, 0x01,                      // Source IP address (10.0.0.1)          (4 bytes)
			0x0A, 0x00, 0x00, 0x02,                      // Destination IP address (10.0.0.2)     (4 bytes)

			// UDP header                                                                            (8 bytes)
			0x00, 0x2A,                                                   // Source port (42)        (2 bytes)
			0x05, 0x39,                                                   // Destination port (1337) (2 bytes)
			(PACKET_SIZE - ETHERNET_HEADER_SIZE - IP_HEADER_SIZE) >> 8,   // UDP length, high byte   (1 byte)
			(PACKET_SIZE - ETHERNET_HEADER_SIZE - IP_HEADER_SIZE) & 0xFF, // UDP length, low byte    (1 byte)
			0x00, 0x00,                                                   // UDP checksum            (2 bytes)

			// UDP payload            (3 bytes)
			0x69, 0x78, 0x79 // "ixy" (3 bytes)
	};

	/** The minimum number of batches processed between two prints. */
	private static final int BATCHES_PER_PRINT = 0xFFF;

	/** The minimum number of nanoseconds between two prints. */
	private static final int NANOS_PER_PRINT = 100_000;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * The device the manipulates the target NIC.
	 * ----------------- SETTER -----------------
	 * Sets the NIC to send packets to.
	 *
	 * @param destination The NIC.
	 * @return This builder.
	 */
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@ToString.Include(name = "target", rank = 5)
	private final @NotNull IxyDevice destination;

	/**
	 * The memory pool that manages the packets.
	 * ----------------- SETTER -----------------
	 * Sets the memory pool that manages the packets.
	 *
	 * @param memoryPool The memory pool.
	 * @return This builder.
	 */
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@ToString.Include(name = "pool", rank = 4)
	private final @NotNull IxyMempool memoryPool;

	/**
	 * The batch size.
	 * ---- SETTER ----
	 * Sets the maximum batch size.
	 *
	 * @param batchSize The batch size.
	 * @return This builder.
	 */
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@ToString.Include(name = "batch_size", rank = 3)
	private final int batchSize;

	/**
	 * The stats instance used when the tracking starts.
	 * --------------------- SETTER ---------------------
	 * Sets the stats instance used at the beginning.
	 *
	 * @param statsStart The stats instance.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private final @NotNull IxyStats statsStart;

	/**
	 * The stats instance used when the tracking ends.
	 * --------------------- SETTER ---------------------
	 * Sets the stats instance used at the end.
	 *
	 * @param statsEnd The stats instance.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private final @NotNull IxyStats statsEnd;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a packet generator that sends packets uninterrumptedly.
	 *
	 * @param destination The first NIC.
	 * @param memoryPool  The memory pool.
	 * @param batchSize   The batch size.
	 */
	public IxyGenerator(@NotNull IxyDevice destination, @NotNull IxyMempool memoryPool, int batchSize, @NotNull IxyStats statsStart, @NotNull IxyStats statsEnd) {
		if (!BuildConfig.OPTIMIZED) {
			if (destination == null) throw new InvalidNullParameterException("destination");
			if (memoryPool == null) throw new InvalidNullParameterException("memoryPool");
			if (batchSize <= 0) throw new InvalidSizeException("batchSize");
			if (statsStart == null) throw new InvalidNullParameterException("statsStart");
			if (statsEnd == null) throw new InvalidNullParameterException("statsEnd");
		}
		this.destination = destination;
		this.memoryPool = memoryPool;
		this.batchSize = batchSize;
		this.statsStart = statsStart;
		this.statsEnd = statsEnd;
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	public void run() {
		if (BuildConfig.DEBUG) log.debug("Allocating resources.");
		initPackets();
		val buffers = new IxyPacketBuffer[batchSize];
		var sequence = 0;
		var counter = (short) 0;
		statsStart.reset();
		statsEnd.reset();
		var startTime = System.nanoTime();
		while (true) {
			val batch = memoryPool.get(buffers);
			for (var i = 0; i < batch; i += 1) {
				val buffer = buffers[i];
				buffer.putInt(PACKET_SIZE - 4, sequence++);
			}
			destination.txBusyWait(0, buffers, 0, batch);
			if(counter++ % BATCHES_PER_PRINT == 0) {
				val endTime = System.nanoTime();
				val nanos = endTime - startTime;
				if (nanos > NANOS_PER_PRINT) {
					destination.readStats(statsEnd);
					try {
						statsEnd.writeStats(System.out, statsStart, nanos);
					} catch (IOException e) {
						if (BuildConfig.DEBUG) log.error("Could not write the stats.", e);
					}
					statsStart.copy(statsEnd);
					counter = 0;
					startTime = endTime;
					break; // TODO: REMOVE THIS
				}
			}
		}
	}

	/** Uses the template packet data {@link #packetData} to populate the packets with the default data. */
	private void initPackets() {
		val copy = new IxyPacketBuffer[memoryPool.getCapacity()];
		var offset = 0;
		while (offset < copy.length) {
			val buffer = memoryPool.get();
			if (buffer == null) {
				if (BuildConfig.DEBUG) log.warn("There was an error while extracting a packet");
				break;
			} else {
				if (BuildConfig.DEBUG) log.debug("Writing packet data to {}th packet: {}", offset, buffer);
				copy[offset++] = buffer;
			}
		}
		for(val packet : copy){
			if (packet != null) memoryPool.free(packet);
		}
	}

	/**
	 * Computes the IPv4 header checksum based on the bytes of a packet.
	 *
	 * @param data   The data to compute the checksum of.
	 * @param offset The offset in which the IPv4 header starts.
	 * @return The IPv4 checksum.
	 */
	private static short checksumIp(@NotNull byte[] data, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (offset < 0) throw new InvalidOffsetException("offset");
			if (offset + IP_HEADER_SIZE > data.length) {
				throw new IllegalArgumentException("There is less than 20 bytes of data left");
			}
		}
		val len = data.length;
		if (BuildConfig.DEBUG) {
			log.debug("Computing IPv4 checksum of {} bytes", len);
			if(len % 2 != 0) log.warn("Odd-sized headers are not supported, the last byte will be ignored");
		}
		val buffer = ByteBuffer.wrap(data, offset, IP_HEADER_SIZE);
		var sum = 0;
		for (var i = 0; i < IP_HEADER_SIZE / Short.BYTES; i += 1) {
			if (i != 5) sum += buffer.getShort(i);
		}
		val carry = (sum >> Short.SIZE) & MAX_UNSIGNED_SHORT_LOW;
		var finalSum = (sum & MAX_UNSIGNED_SHORT) + carry;
		return (short) ~((short) finalSum & MAX_UNSIGNED_SHORT);
	}

}
