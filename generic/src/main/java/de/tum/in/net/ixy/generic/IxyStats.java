package de.tum.in.net.ixy.generic;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Ixy's stats specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public abstract class IxyStats {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** Factor used to convert from/to nanoseconds. */
	private static final double NANO_FACTOR = 1_000_000_000.0;

	/** Factor used to convert from/to mega. */
	private static final double MEGA_FACTOR = 1_000_000.0;

	///////////////////////////////////////////////// ABSTRACT METHODS /////////////////////////////////////////////////

	/**
	 * Returns the PCI device whose stats are being tracked.
	 *
	 * @return The PCI device.
	 */
	@Contract(pure = true)
	protected abstract @NotNull IxyPciDevice getDevice();

	/**
	 * Returns the number of read packets.
	 *
	 * @return The number of read packets.
	 */
	@Contract(pure = true)
	public abstract int getRxPackets();

	/**
	 * Sets the number of read packets.
	 *
	 * @param packets The number of read packets.
	 */
	public abstract void setRxPackets(int packets);

	/**
	 * Returns the number of written packets.
	 *
	 * @return The number of written packets.
	 */
	@Contract(pure = true)
	public abstract int getTxPackets();

	/**
	 * Sets the number of written packets.
	 *
	 * @param packets The number of written packets.
	 */
	public abstract void setTxPackets(int packets);

	/**
	 * Returns the number of read bytes.
	 *
	 * @return The number of read bytes.
	 */
	@Contract(pure = true)
	public abstract long getRxBytes();

	/**
	 * Sets the number of read bytes.
	 *
	 * @param bytes The number of read bytes.
	 */
	public abstract void setRxBytes(long bytes);

	/**
	 * The number of written bytes.
	 *
	 * @return The number of written bytes.
	 */
	@Contract(pure = true)
	public abstract long getTxBytes();

	/**
	 * Sets the number of written bytes.
	 *
	 * @param bytes The number of written bytes.
	 */
	public abstract void setTxBytes(long bytes);

	///////////////////////////////////////////////// CONCRETE METHODS /////////////////////////////////////////////////

	/** Clears the stats. */
	public void clear() {
		if (BuildConfig.DEBUG) log.debug("Clearing stats");
		setRxPackets(0);
		setTxPackets(0);
		setRxBytes(0);
		setTxBytes(0);
	}

	/** Prints the stats using the <em>Simple Logging Facade 4 Java</em>. */
	public void printStats() {
		val address = getDevice().getName();
		log.info("{} RX: {} packets | {} bytes", address, getRxPackets(), getRxBytes());
		log.info("{} TX: {} packets | {} bytes", address, getTxPackets(), getTxBytes());
	}

	/** Prints the connection stats by comparing them with an older instance. **/
	public void printStatsConnection(@NotNull IxyStats stats, long nanos){
		if (!BuildConfig.OPTIMIZED) {
			if (stats == null) throw new InvalidNullParameterException("stats");
			if (nanos <= 0) throw new InvalidSizeException("nanos");
		}
		val address = getDevice().getName();
		val seconds = nanos / NANO_FACTOR;
		val rxPackets = getRxPackets() - stats.getRxPackets();
		val rxMpps = rxPackets / seconds / MEGA_FACTOR;
		val rxBytes = (getRxBytes() - stats.getRxBytes());
		val rxThroughput = (rxBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val rxMbit = rxThroughput + rxMpps*20*Byte.SIZE;
		log.info("{} RX: {} Mpps | {} Mbit/s", address, rxMpps, rxMbit);
		val txPackets = getTxPackets() - stats.getTxPackets();
		val txMpps = txPackets / seconds / MEGA_FACTOR;
		val txBytes = (getTxBytes() - stats.getTxBytes());
		val txThroughput = (txBytes / MEGA_FACTOR / seconds) * Byte.SIZE;
		val txMbit = txThroughput + txMpps*20*Byte.SIZE;
		log.info("{} TX: {} Mpps | {} Mbit/s", address, txMpps, txMbit);
	}

}
