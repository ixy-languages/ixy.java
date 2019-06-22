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

	/** Maximum length of an integer. */
	private static final int MAX_LENGTH_INTEGER = 10;

	/** Maximum length of a long. */
	private static final int MAX_LENGTH_LONG = 20;

	/** Maximum length of a double. */
	private static final int MAX_LENGTH_DOUBLE = 1079;

	/** The text used to print RX statistics. */
	private static final @NotNull String RX = " RX: ";

	/** The length of the text used to print RX statistics. */
	private static final int RX_LENGTH = RX.length();

	/** The text used to print TX statistics. */
	private static final @NotNull String TX = " TX: ";

	/** The length of the text used to print TX statistics. */
	private static final int TX_LENGTH = TX.length();

	/** The text used to print the number of packets. */
	private static final @NotNull String PACKETS = " packets | ";

	/** The length of the text used to print the number of packets. */
	private static final int PACKETS_LENGTH = PACKETS.length();

	/** The text used to print the mega packets per second. */
	private static final @NotNull String MPPS = " Mpps | ";

	/** The length of the text used to print the mega packets per second. */
	private static final int MPPS_LENGTH = MPPS.length();

	/** The text used to print the number of bytes. */
	private static final @NotNull String BYTES = " bytes";

	/** The length of the text used to print the number of bytes. */
	private static final int BYTES_LENGTH = BYTES.length();

	/** The text used to print the megabits per second. */
	private static final @NotNull String MBPS = " Mbit/s";

	/** The length of the text used to print the megabits per second. */
	private static final int MBPS_LENGTH = MBPS.length();

	/** The line separator used when writing the statistics. */
	private static final @NotNull String ENDL = System.lineSeparator();

	/** The size of the line separator used when writing the statistics. */
	private static final int ENDL_LENGTH = ENDL.length();

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
		if (!BuildConfig.OPTIMIZED && out == null) throw new InvalidNullParameterException("out");
		val address = device.getName();
		val strRxPackets = Integer.toUnsignedString(rxPackets);
		val strTxPackets = Integer.toUnsignedString(txPackets);
		val strRxBytes = Long.toUnsignedString(rxBytes);
		val strTxBytes = Long.toUnsignedString(txBytes);
		val lineMaxSize = address.length() + Math.max(RX_LENGTH, TX_LENGTH) + MAX_LENGTH_INTEGER + PACKETS_LENGTH + MAX_LENGTH_LONG + BYTES_LENGTH + ENDL_LENGTH;
		val builder = new StringBuilder(lineMaxSize * 2);
		builder.append(address).append(RX).append(strRxPackets).append(PACKETS).append(strRxBytes).append(BYTES).append(ENDL)
				.append(address).append(TX).append(strTxPackets).append(PACKETS).append(strTxBytes).append(BYTES).append(ENDL);
		out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	@Contract(pure = true)
	public void writeStats(@NotNull OutputStream out, @NotNull IxyStats stats, long nanos) throws IOException {
		if (!BuildConfig.OPTIMIZED) {
			if (out == null) throw new InvalidNullParameterException("out");
			if (stats == null) throw new InvalidNullParameterException("stats");
			if (nanos <= 0) throw new InvalidSizeException("nanos");
		}
		val address = device.getName();
		val seconds = nanos / NANO_FACTOR;
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
		var endl = System.lineSeparator();
		val lineMaxSize = address.length() + Math.max(RX_LENGTH, TX_LENGTH) + MAX_LENGTH_DOUBLE + MPPS_LENGTH + MAX_LENGTH_DOUBLE + MBPS_LENGTH + ENDL_LENGTH;
		val builder = new StringBuilder(lineMaxSize * 2);
		builder.append(address).append(RX).append(rxMpps).append(MPPS).append(rxMbit).append(MBPS).append(endl)
				.append(address).append(TX).append(txMpps).append(MPPS).append(txMbit).append(MBPS).append(endl);
		out.write(builder.toString().getBytes(StandardCharsets.UTF_8));
	}

}
