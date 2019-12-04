package de.tum.in.net.ixy.memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;

/**
 * A common interface for an object that is able to directly manipulate the memory.
 *
 * @author Esau García Sánchez-Torija
 */
public interface MemoryManager {

	/////////////////////////////////////////////////// RETURN CODES ///////////////////////////////////////////////////

	/** The return code used when no huge memory page technology is supported by the CPU. */
	int HUGE_PAGE_NOT_SUPPORTED = -1;

	////////////////////////////////////////////////// MISCELLANEOUS ///////////////////////////////////////////////////

	/**
	 * Returns whether the memory manager instance can be used or not.
	 *
	 * @return The support status.
	 */
	@Contract(pure = true)
	boolean isValid();

	//////////////////////////////////////////////// SYSTEM PROPERTIES /////////////////////////////////////////////////

	/**
	 * Returns the address size in bytes.
	 *
	 * @return The address size.
	 */
	@Contract(pure = true)
	int getAddressSize();

	/**
	 * Returns the memory page size in bytes.
	 *
	 * @return The memory page size.
	 */
	@Contract(pure = true)
	long getPageSize();

	/**
	 * Returns the huge memory page size.
	 * <p>
	 * If the hugepages are not supported, {@code -1} will be returned.
	 * If hugepages are supported but there was an error while computing the size, {@code 0} will be returned.
	 *
	 * @return The size of a huge memory page.
	 */
	@Contract(pure = true)
	long getHugepageSize();

	/////////////////////////////////////////////// ALLOCATORS & FREERS ////////////////////////////////////////////////

	/**
	 * Allocates memory outside of the GC heap.
	 *
	 * @param bytes The number of bytes.
	 * @param huge  Whether to enable huge memory pages.
	 * @param lock  Whether to enable memory locking.
	 * @return The base address of the allocated memory region.
	 */
	@Contract(pure = true)
	@SuppressWarnings("BooleanParameter")
	long allocate(long bytes, boolean huge, boolean lock);

	/**
	 * Frees a memory region.
	 *
	 * @param address The memory region's base address.
	 * @param bytes   The number of bytes.
	 * @param huge    Whether to enable huge memory pages.
	 * @param lock    Whether to enable memory locking.
	 */
	@SuppressWarnings("BooleanParameter")
	void free(long address, long bytes, boolean huge, boolean lock);

	/////////////////////////////////////////////// MAPPERS & UNMAPPERS ////////////////////////////////////////////////

	/**
	 * Maps a file to memory.
	 *
	 * @param file The file.
	 * @param huge Whether to enable huge memory pages.
	 * @param lock Whether to enable memory locking.
	 * @return The base address of the mapped memory region.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IOException           If an I/O error occurs.
	 */
	@Contract(pure = true)
	@SuppressWarnings("BooleanParameter")
	long mmap(@NotNull File file, boolean huge, boolean lock) throws FileNotFoundException, IOException;

	/**
	 * Destroys a memory mapping created with {@link #mmap(File, boolean, boolean)}.
	 *
	 * @param address The memory region's base address.
	 * @param file    The file.
	 * @param huge    Whether huge memory pages was enabled.
	 * @param lock    Whether memory locking was enabled.
	 * @throws FileNotFoundException If the file does not exist.
	 * @throws IOException If an I/O error occurs.
	 */
	@SuppressWarnings("BooleanParameter")
	void munmap(long address, @NotNull File file, boolean huge, boolean lock) throws FileNotFoundException, IOException;

	///////////////////////////////////////////////////// GETTERS //////////////////////////////////////////////////////

	/**
	 * Reads a {@code byte} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	byte getByte(long address);

	/**
	 * Reads a {@code short} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	short getShort(long address);

	/**
	 * Reads an {@code int} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	int getInt(long address);

	/**
	 * Reads a {@code long} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	long getLong(long address);

	///////////////////////////////////////////////// VOLATILE GETTERS /////////////////////////////////////////////////

	/**
	 * Reads a {@code byte} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	byte getByteVolatile(long address);

	/**
	 * Reads a {@code short} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	short getShortVolatile(long address);

	/**
	 * Reads an {@code int} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	int getIntVolatile(long address);

	/**
	 * Reads a {@code long} from an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @return The stored value.
	 */
	@Contract(pure = true)
	long getLongVolatile(long address);

	///////////////////////////////////////////////////// PUTTERS //////////////////////////////////////////////////////

	/**
	 * Writes a {@code byte} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putByte(long address, byte value);

	/**
	 * Writes a {@code short} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putShort(long address, short value);

	/**
	 * Writes an {@code int} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putInt(long address, int value);

	/**
	 * Writes a {@code long} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putLong(long address, long value);

	///////////////////////////////////////////////// VOLATILE PUTTERS /////////////////////////////////////////////////

	/**
	 * Writes a {@code byte} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putByteVolatile(long address, byte value);

	/**
	 * Writes a {@code short} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putShortVolatile(long address, short value);

	/**
	 * Writes an {@code int} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putIntVolatile(long address, int value);

	/**
	 * Writes a {@code long} to an arbitrary virtual address.
	 *
	 * @param address The virtual address.
	 * @param value   The value to store.
	 */
	void putLongVolatile(long address, long value);

	////////////////////////////////////////////////// REGION PUTTERS //////////////////////////////////////////////////

	/**
	 * Reads the data from an arbitrary memory region into a {@code byte[]}.
	 *
	 * @param dest   The virtual address.
	 * @param bytes  The number of bytes.
	 * @param src    The data.
	 * @param offset The offset of {@code dest}.
	 */
	@Contract(mutates = "param3")
	void get(long src, int bytes, @NotNull byte[] dest, int offset);

	/**
	 * Writes the data from a {@code byte[]} into an arbitrary memory region.
	 *
	 * @param dest   The virtual address.
	 * @param bytes  The number of bytes.
	 * @param src    The data.
	 * @param offset The offset of {@code src}.
	 */
	void put(long dest, int bytes, @NotNull byte[] src, int offset);

	/////////////////////////////////////////////// ADDRESS TRANSLATORS ////////////////////////////////////////////////

	/**
	 * Translates a virtual address to a physical address.
	 *
	 * @param address The virtual address.
	 * @return The physical address.
	 */
	@Contract(pure = true)
	long virt2phys(long address);

	///////////////////////////////////////////////// DEFAULT METHODS //////////////////////////////////////////////////

	/**
	 * Allocates memory, translates the address to its physical counterpart and returns a {@link DmaMemory} instance.
	 *
	 * @param bytes The number of bytes.
	 * @param huge  Whether to enable huge memory pages.
	 * @param lock  Whether to enable memory locking.
	 * @return The DMA memory.
	 */
	@Contract(pure = true)
	@SuppressWarnings("BooleanParameter")
	default @NotNull DmaMemory dmaAllocate(final long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED && bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
		val virtual = allocate(bytes, huge, lock);
		if (virtual != 0) putByte(virtual, getByte(virtual));
		val physical = virt2phys(virtual);
		return new DmaMemory(virtual, physical);
	}

}
