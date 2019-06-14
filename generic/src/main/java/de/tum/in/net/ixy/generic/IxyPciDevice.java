package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Optional;

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
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	short getVendorId() throws IOException;

	/**
	 * Returns the device identifier.
	 *
	 * @return The device identifier.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	short getDeviceId() throws IOException;

	/**
	 * Returns the class identifier.
	 *
	 * @return The class identifier.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	byte getClassId() throws IOException;

	/**
	 * Checks whether the <em>direct memory access</em> is enabled or not.
	 *
	 * @return The DMA status.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	boolean isDmaEnabled() throws IOException;

	/**
	 * Enables <em>direct memory access</em>.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	void enableDma() throws IOException;

	/**
	 * Disables <em>direct memory access</em>.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	void disableDma() throws IOException;

	/**
	 * Binds the driver.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	void bind() throws IOException;

	/**
	 * Unbinds the driver.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	void unbind() throws IOException;

	/**
	 * Checks whether the device memory can be mapped or not.
	 *
	 * @return The mappability.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	boolean isMappable() throws IOException;

	/**
	 * Returns the memory space mapped to a byte buffer.
	 *
	 * @return The mapped memory.
	 */
	@Contract(value = " -> new", pure = true)
	@NotNull Optional<MappedByteBuffer> map();

}
