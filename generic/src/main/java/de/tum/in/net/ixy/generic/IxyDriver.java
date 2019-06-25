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
	 * Returns a memory manager.
	 *
	 * @return The memory manager.
	 */
	@Contract(pure = true)
	@NotNull IxyMemoryManager getMemoryManager();

	/**
	 * Returns a memory pool instance.
	 *
	 * @param capacity The capacity of the memory pool.
	 * @return A memory pool.
	 */
	@Contract(value = "_ -> new", pure = true)
	@NotNull IxyMempool getMemoryPool(int capacity);

	/**
	 * Returns a stats instance that tracks the given device.
	 *
	 * @param device The device to track.
	 * @return The stats.
	 */
	@Contract(value = "!null -> new", pure = true)
	@NotNull IxyStats getStats(@NotNull IxyDevice device);

	/**
	 * Returns the implementation of an Ixy device.
	 *
	 * @param device        The device name.
	 * @param rxQueues      The number of read queues.
	 * @param txQueues      The number of write queues.
	 * @param memoryManager The memory manager.
	 * @return The device.
	 * @throws FileNotFoundException If the device cannot be found.
	 */
	@Contract(value = "!null, _, _, !null -> new", pure = true)
	@NotNull IxyDevice getDevice(@NotNull String device, int rxQueues, int txQueues, @NotNull IxyMemoryManager memoryManager) throws FileNotFoundException;

}
