package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.nio.MappedByteBuffer;

/**
 * Ixy's PCI device specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyPciDevice extends Closeable {

	/**
	 * Returns the name.
	 *
	 * @return The name.
	 */
	@NotNull String getName();

	/**
	 * Returns the driver.
	 *
	 * @return The driver.
	 */
	@NotNull String getDriver();

	/**
	 * Returns the vendor identifier.
	 *
	 * @return The vendor identifier.
	 */
	@Contract(pure = true)
	int getVendorId();

	/**
	 * Returns the device identifier.
	 *
	 * @return The device identifier.
	 */
	@Contract(pure = true)
	short getDeviceId();

	/**
	 * Returns the class identifier.
	 *
	 * @return The class identifier.
	 */
	@Contract(pure = true)
	byte getClassId();

	/**
	 * Checks whether the <em>direct memory access</em> is enabled or not.
	 *
	 * @return The DMA status.
	 */
	@Contract(pure = true)
	boolean isDmaEnabled();

	/** Enables <em>direct memory access</em>. */
	@Contract(pure = true)
	void enableDma();

	/** Disables <em>direct memory access</em>. */
	@Contract(pure = true)
	void disableDma();

	/** Binds the driver. */
	void bind();

	/** Unbinds the driver. */
	void unbind();

	/**
	 * Checks whether the device memory can be mapped or not.
	 *
	 * @return The mappability.
	 */
	@Contract(pure = true)
	boolean isMappable();

	/**
	 * Returns the memory space mapped to a byte buffer.
	 *
	 * @return The mapped memory.
	 */
	@Contract(value = " -> new", pure = true)
	@NotNull MappedByteBuffer map();

}
