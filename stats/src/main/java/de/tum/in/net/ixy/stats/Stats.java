package de.tum.in.net.ixy.stats;

import de.tum.in.net.ixy.generic.IxyDevice;
import de.tum.in.net.ixy.generic.IxyStats;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Simple implementation of Ixy's stats specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@RequiredArgsConstructor(staticName = "of")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class Stats implements IxyStats {

	////////////////////////////////////////////////// STATIC MEMBERS //////////////////////////////////////////////////

	/** The line separator used when writing the statistics. */
	private static final @NotNull String ENDL = System.lineSeparator();

	/** Factor used to convert from/to nanoseconds. */
	private static final double NANO_FACTOR = 1_000_000_000.0;

	/** Factor used to convert from/to mega. */
	private static final double MEGA_FACTOR = 1_000_000.0;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Sums two integers while protecting from unsigned overflow.
	 *
	 * @param x The original value.
	 * @param y The number to sum.
	 * @return The sum or maximum unsigned value.
	 */
	private static int unsignedSum(int x, int y) {
		val sum = x + y;
		return Integer.compareUnsigned(x, sum) > 0 ? -1 : sum;
	}

	/**
	 * Sums two longs while protecting from unsigned overflow.
	 *
	 * @param x The original value.
	 * @param y The number to sum.
	 * @return The sum or maximum unsigned value.
	 */
	private static long unsignedSum(long x, long y) {
		val sum = x + y;
		return Long.compareUnsigned(x, sum) > 0 ? -1 : sum;
	}

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	@EqualsAndHashCode.Include
	@ToString.Include(name = "device", rank = 5)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private final @NotNull IxyDevice device;

	@EqualsAndHashCode.Include
	@ToString.Include(name = "rx_packets", rank = 4)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private int rxPackets;

	@EqualsAndHashCode.Include
	@ToString.Include(name = "tx_packets", rank = 3)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private int txPackets;

	@EqualsAndHashCode.Include
	@ToString.Include(name = "rx_bytes", rank = 2)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private long rxBytes;

	@EqualsAndHashCode.Include
	@ToString.Include(name = "tx_bytes", rank = 1)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private long txBytes;

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public void addRxPackets(int packets) {
		if (BuildConfig.DEBUG) log.debug("Adding {} to the RX packets counter.", packets);
		rxPackets = unsignedSum(rxPackets, packets);
	}

	@Override
	public void addTxPackets(int packets) {
		if (BuildConfig.DEBUG) log.debug("Adding {} to the TX packets counter.", packets);
		txPackets = unsignedSum(txPackets, packets);
	}

	@Override
	public void addRxBytes(long bytes) {
		if (BuildConfig.DEBUG) log.debug("Adding {} to the RX bytes counter.", bytes);
		rxBytes = unsignedSum(rxBytes, bytes);
	}

	@Override
	public void addTxBytes(long bytes) {
		if (BuildConfig.DEBUG) log.debug("Adding {} to the TX bytes counter.", bytes);
		txBytes = unsignedSum(txBytes, bytes);
	}

	@Override
	public void reset() {
		if (BuildConfig.DEBUG) log.debug("Resetting stats counters.");
		rxPackets = 0;
		txPackets = 0;
		rxBytes = 0;
		txBytes = 0;
	}

	@Override
	@Contract(pure = true)
	public void writeStats(@NotNull OutputStream out) throws IOException {
		if (BuildConfig.DEBUG) log.debug("Writing statistics to an output stream.");
		// Gather all the data that will be used for the output message
		val address = device.getName();
		val strRxPackets = Integer.toUnsignedString(rxPackets);
		val strTxPackets = Integer.toUnsignedString(txPackets);
		val strRxBytes = Long.toUnsignedString(rxBytes);
		val strTxBytes = Long.toUnsignedString(txBytes);
		// Construct the output message
		var msg = String.format("%1$s RX: %2$s packets | %3$s bytes", address, strRxPackets, strRxBytes);
		msg += ENDL;
		msg += String.format("%1$s TX: %2$s packets | %3$s bytes", address, strTxPackets, strTxBytes);
		msg += ENDL;
		// Write the bytes of the message
		out.write(msg.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	@Contract(pure = true)
	@SuppressWarnings("HardcodedFileSeparator")
	public void writeStats(@NotNull OutputStream out, @NotNull IxyStats stats, long delta) throws IOException {
		if (BuildConfig.DEBUG) log.debug("Writing statistics to an output stream.");
		if (!BuildConfig.OPTIMIZED && delta <= 0) throw new InvalidSizeException("nanos");
		// Gather all the data that will be used for the output message
		val address = device.getName();
		val seconds = delta / NANO_FACTOR;
		val diffRxPackets = rxPackets - stats.getRxPackets();
		val rxMpps = diffRxPackets / seconds / MEGA_FACTOR;
		val diffRxBytes = (rxBytes - stats.getRxBytes());
		val rxThroughput = (diffRxBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val rxMbit = rxThroughput + rxMpps * 20 * Byte.SIZE;
		val diffTxPackets = txPackets - stats.getTxPackets();
		val txMpps = diffTxPackets / seconds / MEGA_FACTOR;
		val diffTxBytes = (txBytes - stats.getTxBytes());
		val txThroughput = (diffTxBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val txMbit = txThroughput + txMpps * 20 * Byte.SIZE;
		// Construct the output message
		var msg = String.format("%1$s RX: %2$s Mpps | %3$s Mbit/s", address, rxMpps, rxMbit);
		msg += ENDL;
		msg += String.format("%1$s TX: %2$s Mpps | %3$s Mbit/s", address, txMpps, txMbit);
		msg += ENDL;
		// Write the bytes of the message
		out.write(msg.getBytes(StandardCharsets.UTF_8));
	}

}
