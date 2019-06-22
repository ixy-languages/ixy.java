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

}
