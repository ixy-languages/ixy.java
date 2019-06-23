package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

import java.io.FileNotFoundException;

/**
 * Ixy's driver specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDriver extends ExtensionPoint {

	/**
	 * Returns the name of the driver.
	 *
	 * @return The name.
	 */
	@Contract(value = " -> new", pure = true)
	@NotNull String getName();

	/**
	 * Returns a memory pool instance.
	 *
	 * @return A memory pool.
	 */
	@Contract(value = " -> new", pure = true)
	@NotNull IxyMempool getMemoryPool();

	/**
	 * Returns the implementation of an Ixy device.
	 *
	 * @param device   The device name.
	 * @param rxQueues The number of read queues.
	 * @param txQueues The number of write queues.
	 * @return The device.
	 * @throws FileNotFoundException If the device cannot be found.
	 */
	@Contract(value = "!null, _, _ -> new", pure = true)
	@NotNull IxyDevice getDevice(@NotNull String device, int rxQueues, int txQueues) throws FileNotFoundException;

}
