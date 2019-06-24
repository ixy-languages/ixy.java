package de.tum.in.net.ixy.generic;

import lombok.val;
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
	@Contract(pure = true)
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
	@Contract(value = "_, !null, !null -> new", pure = true)
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
	@Contract(pure = true)
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
	 * Writes a {@code byte} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndPutByte(long address, byte value) {
		val old = getByte(address);
		putByte(address, value);
		return old;
	}

	/**
	 * Writes a {@code byte} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndPutByteVolatile(long address, byte value) {
		val old = getByteVolatile(address);
		putByteVolatile(address, value);
		return old;
	}

	/**
	 * Adds a {@code byte} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 */
	@Contract(pure = true)
	default void addByte(long address, byte value) {
		val old = getByte(address);
		putByte(address, (byte) (old + value));
	}

	/**
	 * Adds a {@code byte} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 */
	@Contract(pure = true)
	default void addByteVolatile(long address, byte value) {
		val old = getByteVolatile(address);
		putByteVolatile(address, (byte) (old + value));
	}

	/**
	 * Adds a {@code byte} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndAddByte(long address, byte value) {
		val old = getByte(address);
		putByte(address, (byte) (old + value));
		return old;
	}

	/**
	 * Adds a {@code byte} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndAddByteVolatile(long address, byte value) {
		val old = getByteVolatile(address);
		putByteVolatile(address, (byte) (old + value));
		return old;
	}

	/**
	 * Adds a {@code byte} to an arbitrary memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default byte addAndGetByte(long address, byte value) {
		val old = getByte(address);
		val neu = (byte) (old + value);
		putByte(address, neu);
		return neu;
	}

	/**
	 * Adds a {@code byte} to an arbitrary volatile memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code byte} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default byte addAndGetByteVolatile(long address, byte value) {
		val old = getByteVolatile(address);
		val neu = (byte) (old + value);
		putByteVolatile(address, neu);
		return neu;
	}

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
	 * Writes a {@code short} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndPutShort(long address, short value) {
		val old = getShort(address);
		putShort(address, value);
		return old;
	}

	/**
	 * Writes a {@code short} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndPutShortVolatile(long address, short value) {
		val old = getShortVolatile(address);
		putShortVolatile(address, value);
		return old;
	}

	/**
	 * Adds a {@code short} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 */
	@Contract(pure = true)
	default void addShort(long address, short value) {
		val old = getShort(address);
		putShort(address, (short) (old + value));
	}

	/**
	 * Adds a {@code short} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 */
	@Contract(pure = true)
	default void addShortVolatile(long address, short value) {
		val old = getShortVolatile(address);
		putShortVolatile(address, (short) (old + value));
	}

	/**
	 * Adds a {@code short} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndAddShort(long address, short value) {
		val old = getShort(address);
		putShort(address, (short) (old + value));
		return old;
	}

	/**
	 * Adds a {@code short} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndAddShortVolatile(long address, short value) {
		val old = getShortVolatile(address);
		putShortVolatile(address, (short) (old + value));
		return old;
	}

	/**
	 * Adds a {@code short} to an arbitrary memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default short addAndGetShort(long address, short value) {
		val old = getShort(address);
		val neu = (short) (old + value);
		putShort(address, neu);
		return neu;
	}

	/**
	 * Adds a {@code short} to an arbitrary volatile memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code short} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default short addAndGetShortVolatile(long address, short value) {
		val old = getShortVolatile(address);
		val neu = (short) (old + value);
		putShortVolatile(address, neu);
		return neu;
	}

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
	 * Writes a {@code int} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndPutInt(long address, int value) {
		val old = getInt(address);
		putInt(address, value);
		return old;
	}

	/**
	 * Writes a {@code int} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndPutIntVolatile(long address, int value) {
		val old = getIntVolatile(address);
		putIntVolatile(address, value);
		return old;
	}

	/**
	 * Adds a {@code int} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 */
	@Contract(pure = true)
	default void addInt(long address, int value) {
		val old = getInt(address);
		putInt(address, (int) (old + value));
	}

	/**
	 * Adds a {@code int} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 */
	@Contract(pure = true)
	default void addIntVolatile(long address, int value) {
		val old = getIntVolatile(address);
		putIntVolatile(address, (int) (old + value));
	}

	/**
	 * Adds a {@code int} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndAddInt(long address, int value) {
		val old = getInt(address);
		putInt(address, (int) (old + value));
		return old;
	}

	/**
	 * Adds a {@code int} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndAddIntVolatile(long address, int value) {
		val old = getIntVolatile(address);
		putIntVolatile(address, (int) (old + value));
		return old;
	}

	/**
	 * Adds a {@code int} to an arbitrary memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default int addAndGetInt(long address, int value) {
		val old = getInt(address);
		val neu = old + value;
		putInt(address, neu);
		return neu;
	}

	/**
	 * Adds a {@code int} to an arbitrary volatile memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code int} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default int addAndGetIntVolatile(long address, int value) {
		val old = getIntVolatile(address);
		val neu = old + value;
		putIntVolatile(address, neu);
		return neu;
	}

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
	 * Writes a {@code long} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndPutLong(long address, long value) {
		val old = getLong(address);
		putLong(address, value);
		return old;
	}

	/**
	 * Writes a {@code long} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndPutLongVolatile(long address, long value) {
		val old = getLongVolatile(address);
		putLongVolatile(address, value);
		return old;
	}

	/**
	 * Adds a {@code long} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 */
	@Contract(pure = true)
	default void addLong(long address, long value) {
		val old = getLong(address);
		putLong(address, (long) (old + value));
	}

	/**
	 * Adds a {@code long} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 */
	@Contract(pure = true)
	default void addLongVolatile(long address, long value) {
		val old = getLongVolatile(address);
		putLongVolatile(address, (long) (old + value));
	}

	/**
	 * Adds a {@code long} to an arbitrary memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndAddLong(long address, long value) {
		val old = getLong(address);
		putLong(address, (long) (old + value));
		return old;
	}

	/**
	 * Adds a {@code long} to an arbitrary volatile memory address and returns the old value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndAddLongVolatile(long address, long value) {
		val old = getLongVolatile(address);
		putLongVolatile(address, (long) (old + value));
		return old;
	}

	/**
	 * Adds a {@code long} to an arbitrary memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default long addAndGetLong(long address, long value) {
		val old = getLong(address);
		val neu = old + value;
		putLong(address, neu);
		return neu;
	}

	/**
	 * Adds a {@code long} to an arbitrary volatile memory address and returns the new value.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The memory address to write to.
	 * @param value   The {@code long} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default long addAndGetLongVolatile(long address, long value) {
		val old = getLongVolatile(address);
		val neu = old + value;
		putLongVolatile(address, neu);
		return neu;
	}

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
	@Contract(mutates = "param3")
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
	@Contract(mutates = "param3")
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
	@Contract(pure = true)
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
	@Contract(pure = true)
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
	 * Translates an object to its virtual memory address.
	 *
	 * @param object The object.
	 * @return The virtual memory address.
	 */
	@Contract(pure = true)
	long obj2virt(@NotNull Object object);

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
