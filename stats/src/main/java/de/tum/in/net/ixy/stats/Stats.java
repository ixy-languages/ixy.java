package de.tum.in.net.ixy.stats;

import de.tum.in.net.ixy.generic.BuildConfig;
import de.tum.in.net.ixy.generic.InvalidSizeException;
import de.tum.in.net.ixy.generic.IxyPciDevice;
import de.tum.in.net.ixy.generic.IxyStats;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
public final class Stats extends IxyStats {

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	@EqualsAndHashCode.Include
	@ToString.Include(name = "device", rank = 5)
	@Getter(onMethod_ = {@Contract(pure = true)})
	private final @NotNull IxyPciDevice device;

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
	public void setRxPackets(int packets) {
		if (!BuildConfig.OPTIMIZED && packets < 0) throw new InvalidSizeException("packets");
		rxPackets = packets;
	}

	@Override
	public void setTxPackets(int packets) {
		if (!BuildConfig.OPTIMIZED && packets < 0) throw new InvalidSizeException("packets");
		txPackets = packets;
	}

	@Override
	public void setRxBytes(long bytes) {
		if (!BuildConfig.OPTIMIZED && bytes < 0) throw new InvalidSizeException("bytes");
		rxBytes = bytes;
	}

	@Override
	public void setTxBytes(long bytes) {
		if (!BuildConfig.OPTIMIZED && bytes < 0) throw new InvalidSizeException("bytes");
		txBytes = bytes;
	}

}
