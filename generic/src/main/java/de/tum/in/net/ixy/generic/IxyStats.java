package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Ixy's stats specification.
 * <p>
 * All counters shall be treated as unsigned numbers.
 * Allows Ixy devices to save certain statistics in order to compute the packet processing capabilities.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyStats {

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
	@Contract(pure = true)
	void writeStats(@NotNull OutputStream out) throws IOException;

	/**
	 * Prints the connection stats to the given output stream.
	 * <p>
	 * In order to compute the connection stats, a copy of the instance from a previous time is needed, along with the
	 * delta of time between the two instances last updates.
	 *
	 * @param stats The initial stats.
	 * @param delta The delta of time in nanoseconds.
	 */
	@Contract(pure = true)
	void writeStats(@NotNull OutputStream out, @NotNull IxyStats stats, long delta) throws IOException;

}
