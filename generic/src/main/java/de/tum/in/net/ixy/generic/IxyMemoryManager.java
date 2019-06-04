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
	 * @return The size of a memory address in the host system it is being executed.
	 */
	int addressSize();

	/**
	 * Computes the size of a memory page.
	 *
	 * @return The size of a memory page in the host system it is being executed.
	 */
	long pageSize();

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * The result will be a multiple of the page size or, exceptionally, {@code -1} or {@code 1}, when the host system
	 * does not support it or an error occurred, respectively.
	 *
	 * @return The size of a huge memory page in the host system it is being executed.
	 * @see #pageSize()
	 */
	long hugepageSize();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method can be customized to use huge memory pages by setting the parameter {@code huge} to {@code true}.
	 * In addition, if the parameter {@code contiguous} is set to {@code true}, the method allocation will not succeed
	 * if the operative system cannot guarantee that the physical memory will be contiguous.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory pages should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @return The base address of the allocated memory region.
	 */
	long allocate(final long size, final boolean huge, final boolean contiguous);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * If the parameters do not match the ones used to allocate the region, the memory might not be freed and the
	 * behaviour will be undefined.
	 *
	 * @param address The memory address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @param huge    Whether huge memory pages should be used.
	 * @return Whether the operation succeeded.
	 */
	boolean free(final long address, final long size, final boolean huge);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code byte}.
	 */
	byte getByte(final long address);

	/**
	 * Reads a {@code byte} from an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code byte}.
	 */
	byte getByteVolatile(final long address);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to write.
	 */
	void putByte(final long address, final byte value);

	/**
	 * Writes a {@code byte} to an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code byte} to write.
	 */
	void putByteVolatile(final long address, final byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code short}.
	 */
	short getShort(final long address);

	/**
	 * Reads a {@code short} from an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code short}.
	 */
	short getShortVolatile(final long address);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to write.
	 */
	void putShort(final long address, final short value);

	/**
	 * Writes a {@code short} to an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code short} to write.
	 */
	void putShortVolatile(final long address, final short value);

	/**
	 * Reads an {@code int} from an arbitrary memory address.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code int}.
	 */
	int getInt(final long address);

	/**
	 * Reads an {@code int} from an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code int}.
	 */
	int getIntVolatile(final long address);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to write.
	 */
	void putInt(final long address, final int value);

	/**
	 * Writes an {@code int} to an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code int} to write.
	 */
	void putIntVolatile(final long address, final int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code long}.
	 */
	long getLong(final long address);

	/**
	 * Reads a {@code long} from an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code long}.
	 */
	long getLongVolatile(final long address);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to write.
	 */
	void putLong(final long address, final long value);

	/**
	 * Writes a {@code long} to an arbitrary volatile memory address.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code long} to write.
	 */
	void putLongVolatile(final long address, final long value);

	/**
	 * Copies a memory region into a primitive byte array.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	void get(final long src, final int size, final byte[] dest, final int offset);

	/**
	 * Copies a memory region into a primitive byte array using volatile memory addresses.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	void getVolatile(final long src, final int size, final byte[] dest, final int offset);

	/**
	 * Copies a primitive byte array into a memory region.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 */
	void put(final long dest, final int size, final byte[] src, final int offset);

	/**
	 * Copies a primitive byte array into a memory region using volatile memory addresses.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 */
	void putVolatile(final long dest, final int size, final byte[] src, final int offset);

	/**
	 * Copies a memory region into another memory region.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 */
	void copy(final long src, final int size, final long dest);

	/**
	 * Copies a memory region into another memory region using volatile memory addresses.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 */
	void copyVolatile(final long src, final int size, final long dest, final int offset);

	/**
	 * Translates a virtual memory {@code address} to its equivalent physical counterpart.
	 * <p>
	 * There is no guarantees that the physical memory address will be valid even just after this method returns the
	 * value. The guarantee has to be made by the allocation method, by locking the memory pages that contain the
	 * allocated memory region and guaranteeing they will be contiguously ordered on the underlying hardware.
	 *
	 * @param address The memory address to translate.
	 * @return The physical memory address.
	 */
	long virt2phys(final long address);

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method can be customized to use huge memory pages or to fail if the physical contiguity cannot be
	 * guaranteed.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory page should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @return The {@link DmaMemory} instance with the virtual and physical addresses.
	 */
	DmaMemory dmaAllocate(final long size, final boolean huge, final boolean contiguous);

}
