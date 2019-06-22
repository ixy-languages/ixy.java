package de.tum.in.net.ixy.generic;

import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Ixy's stats specification.
 * <p>
 * All counters shall be treated as unsigned numbers.
 * Allows Ixy devices to save certain statistics in order to compute the packet processing capabilities.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyStats {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** Logger used by the default implementations. */
	@NotNull Logger log = LoggerFactory.getLogger(IxyStats.class);

	/** Maximum length of an integer. */
	int MAX_LENGTH_INTEGER = 10;

	/** Maximum length of a long. */
	int MAX_LENGTH_LONG = 20;

	/** Maximum length of a double. */
	int MAX_LENGTH_DOUBLE = 1079;

	/** The text used to print RX statistics. */
	@NotNull String RX = " RX: ";

	/** The length of the text used to print RX statistics. */
	int RX_LENGTH = RX.length();

	/** The text used to print TX statistics. */
	@NotNull String TX = " TX: ";

	/** The length of the text used to print TX statistics. */
	int TX_LENGTH = TX.length();

	/** The text used to print the number of packets. */
	@NotNull String PACKETS = " packets | ";

	/** The length of the text used to print the number of packets. */
	int PACKETS_LENGTH = PACKETS.length();

	/** The text used to print the mega packets per second. */
	@NotNull String MPPS = " Mpps | ";

	/** The length of the text used to print the mega packets per second. */
	int MPPS_LENGTH = MPPS.length();

	/** The text used to print the number of bytes. */
	@NotNull String BYTES = " bytes";

	/** The length of the text used to print the number of bytes. */
	int BYTES_LENGTH = BYTES.length();

	/** The text used to print the megabits per second. */
	@NotNull String MBPS = " Mbit/s";

	/** The length of the text used to print the megabits per second. */
	int MBPS_LENGTH = MBPS.length();

	/** The line separator used when writing the statistics. */
	@NotNull String ENDL = System.lineSeparator();

	/** The size of the line separator used when writing the statistics. */
	int ENDL_LENGTH = ENDL.length();

	/** Factor used to convert from/to nanoseconds. */
	double NANO_FACTOR = 1_000_000_000.0;

	/** Factor used to convert from/to mega. */
	double MEGA_FACTOR = 1_000_000.0;

	///////////////////////////////////////////////// USELESS METHODS //////////////////////////////////////////////////

	/**
	 * Returns the NIC whose stats are being tracked.
	 * <p>
	 * This method exists so that default implementations can access the device.
	 *
	 * @return The NIC.
	 */
	@Contract(pure = true)
	@NotNull IxyDevice getDevice();

	///////////////////////////////////////////////// USELESS METHODS //////////////////////////////////////////////////

	/**
	 * Returns the number of read packets.
	 *
	 * @return The number of read packets.
	 */
	@Contract(pure = true)
	int getRxPackets();

	/**
	 * Sets the number of read packets.
	 *
	 * @param packets The number of read packets.
	 */
	void addRxPackets(int packets);

	/**
	 * Returns the number of written packets.
	 *
	 * @return The number of written packets.
	 */
	@Contract(pure = true)
	int getTxPackets();

	/**
	 * Sets the number of written packets.
	 *
	 * @param packets The number of written packets.
	 */
	void addTxPackets(int packets);

	/**
	 * Returns the number of read bytes.
	 *
	 * @return The number of read bytes.
	 */
	@Contract(pure = true)
	long getRxBytes();

	/**
	 * Sets the number of read bytes.
	 *
	 * @param bytes The number of read bytes.
	 */
	void addRxBytes(long bytes);

	/**
	 * The number of written bytes.
	 *
	 * @return The number of written bytes.
	 */
	@Contract(pure = true)
	long getTxBytes();

	/**
	 * Sets the number of written bytes.
	 *
	 * @param bytes The number of written bytes.
	 */
	void addTxBytes(long bytes);

	///////////////////////////////////////////////// CONCRETE METHODS /////////////////////////////////////////////////

	/** Resets the stats. */
	void reset();

	/**
	 * Prints the stats to the given output stream.
	 *
	 * @param out The output stream.
	 */
	default void writeStats(@NotNull OutputStream out) {
		if (BuildConfig.DEBUG) log.debug("Writing statistics to an output stream.");
		if (!BuildConfig.OPTIMIZED && out == null) throw new InvalidNullParameterException("out");
		val address = getDevice().getName();
		val lineMaxSize = address.length() + Math.max(RX_LENGTH, TX_LENGTH) + MAX_LENGTH_INTEGER + PACKETS_LENGTH + MAX_LENGTH_LONG + BYTES_LENGTH + ENDL_LENGTH;
		val builder = new StringBuilder(lineMaxSize * 2);
		builder.append(address).append(RX).append(getRxBytes()).append(PACKETS).append(getRxBytes()).append(BYTES).append(ENDL)
				.append(address).append(TX).append(getTxBytes()).append(PACKETS).append(getTxBytes()).append(BYTES).append(ENDL);
		try {
			out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.error("Could not write stats to the output stream.", e);
		}
	}

	/**
	 * Prints the connection stats to the given output stream.
	 * <p>
	 * In order to compute the connection stats, a copy of the instance from a previous time is needed, along with the
	 * delta of time between the two instances last updates.
	 *
	 * @param stats The initial stats.
	 * @param nanos The delta of time in nanoseconds.
	 */
	default void printStatsConnection(@NotNull OutputStream out, @NotNull IxyStats stats, long nanos) {
		if (!BuildConfig.OPTIMIZED) {
			if (out == null) throw new InvalidNullParameterException("out");
			if (stats == null) throw new InvalidNullParameterException("stats");
			if (nanos <= 0) throw new InvalidSizeException("nanos");
		}
		val address = getDevice().getName();
		val seconds = nanos / NANO_FACTOR;
		val rxPackets = getRxPackets() - stats.getRxPackets();
		val rxMpps = rxPackets / seconds / MEGA_FACTOR;
		val rxBytes = (getRxBytes() - stats.getRxBytes());
		val rxThroughput = (rxBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val rxMbit = rxThroughput + rxMpps * 20 * Byte.SIZE;
		val txPackets = getTxPackets() - stats.getTxPackets();
		val txMpps = txPackets / seconds / MEGA_FACTOR;
		val txBytes = (getTxBytes() - stats.getTxBytes());
		val txThroughput = (txBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val txMbit = txThroughput + txMpps * 20 * Byte.SIZE;
		var endl = System.lineSeparator();
		val lineMaxSize = address.length() + Math.max(RX_LENGTH, TX_LENGTH) + MAX_LENGTH_DOUBLE + MPPS_LENGTH + MAX_LENGTH_DOUBLE + MBPS_LENGTH + ENDL_LENGTH;
		val builder = new StringBuilder(lineMaxSize * 2);
		builder.append(address).append(RX).append(rxMpps).append(MPPS).append(rxMbit).append(MBPS).append(endl)
				.append(address).append(TX).append(txMpps).append(MPPS).append(txMbit).append(MBPS).append(endl);
		try {
			out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.error("Could not write stats to the output stream.", e);
		}
	}

}
