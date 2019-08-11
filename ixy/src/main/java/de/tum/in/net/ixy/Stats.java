package de.tum.in.net.ixy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;

/**
 * A simple statistics tracker to compute the delta values.
 * <p>
 * Even though Java does not support unsigned numeric values, the methods {@link #addRxPackets(long)}, {@link
 * #addTxPackets(int)}, {@link #addRxBytes(long)} and {@link #addTxBytes(long)} will treat the stored value and the
 * delta value as unsigned.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@NoArgsConstructor
@SuppressWarnings("ConstantConditions")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class Stats {

	////////////////////////////////////////////////// STATIC MEMBERS //////////////////////////////////////////////////

	/** Factor used to convert from/to mega. */
	private static final double FACTOR_MEGA = 1_000_000.0;

	/** Factor used to convert from/to giga. */
	private static final double FACTOR_GIGA = 1_000_000_000.0;

	/** The preamble, SFD, IFG and other fields that need to be taken into account when computing the throughput. */
	private static final int EXTRA_BYTES = 20;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Performs the addition of two longs treating them as unsigned.
	 *
	 * @param x An addend.
	 * @param y An addend.
	 * @return The sum.
	 */
	@SuppressWarnings("MagicNumber")
	private static long unsignedSum(final long x, final long y) {
		val sum = x + y;
		return Long.compareUnsigned(x, sum) > 0 ? 0xFFFFFFFFFFFFFFFFL : sum;
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The counters index. */
	@ToString.Include(name = "index", rank = 5)
	private int index;

	/** The RX packet counters. */
	@ToString.Include(name = "rx_packets", rank = 4)
	private final @NotNull long[] rxPackets = new long[2];

	/** The TX packet counters. */
	@ToString.Include(name = "tx_packets", rank = 3)
	private final @NotNull long[] txPackets = new long[2];

	/** The RX bytes counters. */
	@ToString.Include(name = "rx_bytes", rank = 2)
	private final @NotNull long[] rxBytes = new long[2];

	/** The TX bytes counters. */
	@ToString.Include(name = "tx_bytes", rank = 1)
	private final @NotNull long[] txBytes = new long[2];

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Updates the number of read packets.
	 *
	 * @param delta The delta value.
	 */
	public void addRxPackets(final long delta) {
		if (DEBUG >= LOG_TRACE) log.trace("Adding {} to the RX packets counter.", delta);
		rxPackets[index] = unsignedSum(rxPackets[index], delta);
	}

	/**
	 * Updates the number of written packets.
	 *
	 * @param delta The delta value.
	 */
	public void addTxPackets(final int delta) {
		if (DEBUG >= LOG_TRACE) log.trace("Adding {} to the TX packets counter.", delta);
		txPackets[index] = unsignedSum(txPackets[index], delta);
	}

	/**
	 * Updates the number of read bytes.
	 *
	 * @param delta The delta value.
	 */
	public void addRxBytes(final long delta) {
		if (DEBUG >= LOG_TRACE) log.trace("Adding {} to the RX bytes counter.", delta);
		rxBytes[index] = unsignedSum(rxBytes[index], delta);
	}

	/**
	 * Updates the number of written bytes.
	 *
	 * @param delta The delta value.
	 */
	public void addTxBytes(final long delta) {
		if (DEBUG >= LOG_TRACE) log.trace("Adding {} to the TX bytes counter.", delta);
		txBytes[index] = unsignedSum(txBytes[index], delta);
	}

	/** Uses an alternate counter to keep a copy of the old counters. */
	public void swap() {
		val newIndex = index == 0 ? 1 : 0;
		rxPackets[newIndex] = rxPackets[index];
		txPackets[newIndex] = txPackets[index];
		rxBytes[newIndex] = rxBytes[index];
		txBytes[newIndex] = txBytes[index];
		index = newIndex;
	}

	/**
	 * Writes the counter statistics and the throughput to an output stream.
	 *
	 * @param out    The output stream.
	 * @param device The device address.
	 * @param delta  The delta time in nanoseconds.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	@SuppressWarnings("HardcodedFileSeparator")
	public void writeStats(final @NotNull OutputStream out, final @NotNull String device, final long delta)
			throws IOException {
		if (!OPTIMIZED && delta <= 0) {
			throw new IllegalArgumentException("The parameter 'delta' MUST BE positive.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing statistics to an output stream.");

		// Compute the index of the previous record and transform the time to SI
		val other = (index + 1) % 2;
		val seconds = delta / FACTOR_GIGA;

		// Get the string representation of the packets
		val rxPacketsStr = Long.toUnsignedString(rxPackets[index]);
		val txPacketsStr = Long.toUnsignedString(txPackets[index]);
		val rxBytesStr = Long.toUnsignedString(rxBytes[index]);
		val txBytesStr = Long.toUnsignedString(txBytes[index]);

		// Compute the delta values for every counter
		val diffRxPackets = rxPackets[index] - rxPackets[other];
		val diffTxPackets = txPackets[index] - txPackets[other];
		val diffRxBytes = (rxBytes[index] - rxBytes[other]);
		val diffTxBytes = (txBytes[index] - txBytes[other]);

		// Compute the millions of packets per second and the transfer rate
		val rxMpps = diffRxPackets / seconds / FACTOR_MEGA;
		val txMpps = diffTxPackets / seconds / FACTOR_MEGA;
		val rxThroughput = (diffRxBytes / FACTOR_MEGA / seconds) * Byte.SIZE;
		val txThroughput = (diffTxBytes / FACTOR_MEGA / seconds) * Byte.SIZE;
		val rxMbit = rxThroughput + rxMpps * EXTRA_BYTES * Byte.SIZE;
		val txMbit = txThroughput + txMpps * EXTRA_BYTES * Byte.SIZE;

		// Construct the output message (no need for StringBuilder; the compiler will optimize this concatenation)
		val msg = device + " RX: " + rxPacketsStr + " packets | " + rxBytesStr + " bytes"
				+ System.lineSeparator()
				+ device + " TX: " + txPacketsStr + " packets | " + txBytesStr + " bytes"
				+ System.lineSeparator()
				+ device + " RX: " + rxMpps + " Mpps | " + rxMbit + " Mbit/s"
				+ System.lineSeparator()
				+ device + " TX: " + txMpps + " Mpps | " + txMbit + " Mbit/s"
				+ System.lineSeparator();

		// Write the bytes of the message
		out.write(msg.getBytes(StandardCharsets.UTF_8));
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/**
	 * Compares the statistics counters without taking into account the index in which they are stored.
	 *
	 * @param stats The other object to compare with.
	 * @return Whether the objects are equal or not.
	 */
	@Override
	@SuppressWarnings({"NonFinalFieldReferenceInEquals", "OverlyComplexBooleanExpression"})
	public boolean equals(final Object stats) {
		if (stats == this) {
			return true;
		} else if (stats instanceof Stats) {
			val other = (Stats) stats;
			val newIndex = index == 0 ? 1 : 0;
			val newOtherIndex = other.index == 0 ? 1 : 0;
			return rxPackets[index] == other.rxPackets[other.index]
					&& txPackets[index] == other.txPackets[other.index]
					&& rxBytes[index] == other.rxBytes[other.index]
					&& txBytes[index] == other.txBytes[other.index]
					&& rxPackets[newIndex] == other.rxPackets[newOtherIndex]
					&& txPackets[newIndex] == other.txPackets[newOtherIndex]
					&& rxBytes[newIndex] == other.rxBytes[newOtherIndex]
					&& txBytes[newIndex] == other.txBytes[newOtherIndex];
		}
		return false;
	}

	/**
	 * Produces a hash code of the instance without taking into account they order of the stored counters.
	 *
	 * @return The hash code.
	 */
	@Override
	@SuppressWarnings("NonFinalFieldReferencedInHashCode")
	public int hashCode() {
		val prime = 59;
		var result = 1;

		// Hash code for current counters
		result *= prime;
		result += rxPackets[index];
		result *= prime;
		result += txPackets[index];
		result *= prime;
		result += rxBytes[index];
		result *= prime;
		result += txBytes[index];

		// Hash code for other counters
		val newIndex = index == 0 ? 1 : 0;
		result *= prime;
		result += rxPackets[newIndex];
		result *= prime;
		result += txPackets[newIndex];
		result *= prime;
		result += rxBytes[newIndex];
		result *= prime;
		result += txBytes[newIndex];

		// Return the hash code
		return result;
	}

}
