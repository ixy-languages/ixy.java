package de.tum.in.net.ixy.generic;

/**
 * Collection of methods a memory manager should expose.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyMemoryManager {

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 *
	 * @return The address size of the system.
	 */
	int addressSize();

	/**
	 * Computes the page size of the host system.
	 *
	 * @return The page size of the system.
	 */
	long pageSize();

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * The result will be a multiple of the page size or {@code 0} if the system does not support it.
	 *
	 * @return The size of a huge memory page.
	 * @see #pageSize()
	 */
	long hugepageSize();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method can be customized to use huge memory pages or to fail if the physical contiguity cannot be guaranteed.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory page should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @return The base address of the allocated memory region.
	 */
	long allocate(final long size, final boolean huge, final boolean contiguous);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * If the parameters do not match the ones used to allocate the region, the behaviour is undefined.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @param huge    Whether huge memory pages should be used.
	 * @return Whether the operation succeeded.
	 */
	boolean free(final long address, final long size, final boolean huge);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	byte getByte(final long address);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	byte getByteVolatile(final long address);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putByte(final long address, final byte value);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putByteVolatile(final long address, final byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	short getShort(final long address);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	short getShortVolatile(final long address);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putShort(final long address, final short value);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putShortVolatile(final long address, final short value);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	int getInt(final long address);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	int getIntVolatile(final long address);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putInt(final long address, final int value);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putIntVolatile(final long address, final int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	long getLong(final long address);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	long getLongVolatile(final long address);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putLong(final long address, final long value);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	void putLongVolatile(final long address, final long value);

}
