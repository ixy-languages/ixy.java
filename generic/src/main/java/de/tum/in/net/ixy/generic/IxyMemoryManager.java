package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Ixy's memory manager specification.
 * <p>
 * The specification is based on the <em>principle of least knowledge</em> or <em>Law of Demeter</em> (LoD).
 * This means that only the methods needed to build a packet forwarder and generator will be exposed.
 * Any driver-dependent methods must be implemented and exposed in the driver's package and module.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyMemoryManager {

	/**
	 * Provides a way to identify all the allocation techniques.
	 * <p>
	 * This is provided to not fall into the <em>boolean trap</em> and provide a cleaner API.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	enum AllocationType {

		/** Standard allocation method which imposes no constraints. */
		STANDARD,

		/** Hugepage-based memory allocation. */
		HUGE

	}

	/**
	 * Provides a way to identify all the allocation techniques.
	 * <p>
	 * This is provided to not fall into the <em>boolean trap</em> and provide a cleaner API.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	enum LayoutType {

		/** Standard memory layout method which imposes no constraints. */
		STANDARD,

		/** Layout with physical contiguity guarantee. */
		CONTIGUOUS

	}

	/**
	 * Returns the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 *
	 * @return The size of a memory address.
	 */
	@Contract(pure = true)
	int addressSize();

	/**
	 * Returns the size of a memory page.
	 *
	 * @return The size of a memory page.
	 */
	@Contract(pure = true)
	long pageSize();

	/**
	 * Returns the size of a huge memory page.
	 * <p>
	 * The result will be a multiple of the page size or, exceptionally, {@code -1} or {@code 0}, when the host system
	 * does not support it or an error occurred when obtaining it, respectively.
	 *
	 * @return The size of a huge memory page.
	 * @see #pageSize()
	 */
	@Contract(pure = true)
	long hugepageSize();

	/**
	 * Allocates raw bytes from the heap.
	 * <p>
	 * Several flags are provided to customise the behaviour of the allocation.
	 * When the parameter {@code allocationType} is set to {@link AllocationType#STANDARD}, normal memory allocation
	 * will take place, usually implemented with the C library function {@code malloc(size_t)}, and the parameter {@code
	 * layoutType} will be ignored.
	 * Conversely, when the parameter {@code allocationType} is set to {@link AllocationType#HUGE}, hugepage-based
	 * memory allocation will take place, usually implemented with platform-dependent system calls, and the parameter
	 * {@code layoutType} will be used to decide whether the operation will succeed or not.
	 * <p>
	 * Hugepage-based memory allocation usually returns hugepage-aligned memory addresses.
	 * Operative systems offer the possibility to lock memory pages, preventing the memory region from being swapped,
	 * which this method will use.
	 * If the operative system cannot guarantee the physical contiguity of the allocated huge memory pages, if the
	 * {@code bytes} to allocate is bigger than the size of a memory page, this method will fail and return the invalid
	 * memory address {@code 0}.
	 *
	 * @param bytes          The number of bytes.
	 * @param allocationType The memory allocation type.
	 * @param layoutType     The memory layout type.
	 * @return The base address of the allocated memory region.
	 */
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail; _, !null, !null -> _", pure = true)
	long allocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType);

	/**
	 * Allocates raw bytes from the heap.
	 * <p>
	 * Several flags are provided to customise the behaviour of the allocation in the same way as in {@link
	 * #allocate(long, AllocationType, LayoutType)}.
	 * <p>
	 * The main difference with {@link #allocate(long, AllocationType, LayoutType)} is that this method will return an
	 * instance of {@link IxyDmaMemory} which contains a copy of the physical address.
	 *
	 * @param bytes          The number of bytes.
	 * @param allocationType The memory allocation type.
	 * @param layoutType     The memory layout type.
	 * @return The {@link IxyDmaMemory} instance.
	 */
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail; _, !null, !null -> new", pure = true)
	@NotNull IxyDmaMemory dmaAllocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * Several flags are provided to customise the behaviour of the freeing.
	 * When the parameter {@code allocationType} is set to {@link AllocationType#STANDARD}, normal memory freeing will
	 * take place, usually implemented with the C library function {@code free(void *)}.
	 * Conversely, when the parameter {@code allocationType} is set to {@link AllocationType#HUGE}, hugepage-based
	 * memory freeing will take place, usually implemented with platform-dependent system calls.
	 * <p>
	 * The parameter {@code bytes} might be used in some platforms; consistency is important, as the behaviour is
	 * undefined if the size used with {@link #allocate(long, AllocationType, LayoutType)} and this method's do not
	 * match.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address        The base address of the memory region.
	 * @param bytes          The size of the memory region.
	 * @param allocationType The memory allocation type.
	 * @return Whether the operation succeeded.
	 */
	@Contract(value = "_, _, null -> fail", pure = true)
	boolean free(long address, long bytes, @NotNull AllocationType allocationType);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByte(long address);

	/**
	 * Reads a {@code byte} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByteVolatile(long address);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByte(long address, byte value);

	/**
	 * Writes a {@code byte} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByteVolatile(long address, byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShort(long address);

	/**
	 * Reads a {@code short} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShortVolatile(long address);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShort(long address, short value);

	/**
	 * Writes a {@code short} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShortVolatile(long address, short value);

	/**
	 * Reads an {@code int} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getInt(long address);

	/**
	 * Reads an {@code int} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getIntVolatile(long address);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to write.
	 */
	@Contract(pure = true)
	void putInt(long address, int value);

	/**
	 * Writes an {@code int} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code int} to write.
	 */
	@Contract(pure = true)
	void putIntVolatile(long address, int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to read from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLong(long address);

	/**
	 * Reads a {@code long} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to read from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLongVolatile(long address);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLong(long address, long value);

	/**
	 * Writes a {@code long} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The volatile memory address to write to.
	 * @param value   The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLongVolatile(long address, long value);

	/**
	 * Copies an arbitrary memory region into the JVM heap using a primitive byte array.
	 * <p>
	 * If the memory addresses are {@code 0}, the behaviour is undefined.
	 *
	 * @param src    The source memory address to copy from.
	 * @param bytes  The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	void get(long src, int bytes, @NotNull byte[] dest, int offset);

	/**
	 * Copies an arbitrary volatile memory region into the JVM heap using a primitive byte array.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src    The source memory address to copy from.
	 * @param bytes  The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	void getVolatile(long src, int bytes, @NotNull byte[] dest, int offset);

	/**
	 * Copies a primitive byte array from the JVM heap into an arbitrary memory region.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param bytes  The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	void put(long dest, int bytes, @NotNull byte[] src, int offset);

	/**
	 * Copies a primitive byte array from the JVM heap into an arbitrary volatile memory region.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param bytes  The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	void putVolatile(long dest, int bytes, @NotNull byte[] src, int offset);

	/**
	 * Copies an arbitrary memory region into another arbitrary memory region.
	 * <p>
	 * If the memory addresses are {@code 0}, the behaviour is undefined.
	 *
	 * @param src   The source memory address to copy from.
	 * @param bytes The number of bytes to copy.
	 * @param dest  The destination memory address to copy to.
	 */
	@Contract(pure = true)
	void copy(long src, int bytes, long dest);

	/**
	 * Copies an arbitrary volatile memory region into another arbitrary volatile memory region.
	 * <p>
	 * If the memory addresses are {@code 0}, the behaviour is undefined.
	 *
	 * @param src   The source memory address to copy from.
	 * @param bytes The number of bytes to copy.
	 * @param dest  The destination memory address to copy to.
	 */
	@Contract(pure = true)
	void copyVolatile(long src, int bytes, long dest);

	/**
	 * Translates a virtual memory address to its physical counterpart.
	 * <p>
	 * This method does not guarantee the validity of the returned physical memory address.
	 * Such guarantees must be given by the allocation method, as memory can be moved or swapped at any moment by the
	 * JVM.
	 *
	 * @param address The virtual memory address.
	 * @return The physical memory address.
	 */
	@Contract(pure = true)
	long virt2phys(long address);

}
