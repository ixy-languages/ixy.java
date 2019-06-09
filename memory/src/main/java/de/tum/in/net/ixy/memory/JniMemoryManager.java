package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.memory.Utility.check;

/**
 * A simple implementation of Ixy's memory manager specification using JNI.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"DuplicateStringLiteralInspection", "HardCodedStringLiteral", "ConstantConditions"})
public final class JniMemoryManager implements IxyMemoryManager {

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/** Cached huge page size. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "hugepage")
	@SuppressWarnings("NonConstantFieldWithUpperCaseName")
	private final long HUGE_PAGE_SIZE;

	/**
	 * Singleton instance.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Setter(AccessLevel.NONE)
	@SuppressWarnings("JavaDoc")
	@Getter(onMethod_ = {@NotNull, @Contract(value = "_ -> !null", pure = true)})
	private static final @NotNull IxyMemoryManager singleton = new JniMemoryManager();

	////////////////////////////////////////////////// NATIVE METHODS //////////////////////////////////////////////////

	/**
	 * Returns the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 * <p>
	 * This method is portable and should work in all operative systems.
	 *
	 * @return The size of a memory address.
	 */
	@Contract(pure = true)
	private static native int c_address_size();

	/**
	 * Returns the size of a memory page.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}.
	 * Calling this method with any other operative system will use a dummy implementation that will always return
	 * {@code 0}.
	 *
	 * @return The size of a memory page.
	 */
	@Contract(pure = true)
	private static native int c_page_size();

	/**
	 * Returns the size of a huge memory page.
	 * <p>
	 * Although the name uses the nomenclature {@code hugepage}, other operative systems use the same technology but
	 * with different name, for example, {@code largepage} on {@code Windows}, {@code superpage} in {@code BSD} or
	 * {@code bigpage} in {@code RHEL}.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}.
	 * Calling this method with another operative system will use a dummy implementation that will always return {@code
	 * -1}.
	 * <ul>
	 * <li>When there is an error or the mount point of the {@code hugetlbfs} is not found, a {@code -1} is returned
	 * in {@code Linux}. This code is returned in {@code Windows} when there is no support for huge memory pages.</li>
	 * <li>If the mount point was found but the size could not be computed, a {@code 0} is returned in {@code Linux}.
	 * This error code will never be returned under {@code Windows}.</li>
	 * </ul>
	 *
	 * @return The size of a huge memory page.
	 * @see #pageSize()
	 */
	@Contract(pure = true)
	private static native long c_hugepage_size();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * Several flags are provided to customise the behaviour of the allocation.
	 * When the parameter {@code huge} is set to {@code true}, normal memory allocation will take place, usually
	 * implemented with the C library function {@code malloc(size_t)}, and the parameter {@code mnt} will be ignored.
	 * Conversely, when the parameter {@code contiguous} is set to {@code true}, hugepage-based memory allocation will
	 * take place, usually implemented with platform-dependent system calls, and the parameter {@code mnt} will be used
	 * to allocate the huge memory page in {@code Linux}.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}.
	 * Calling this method with another operative system will use a dummy implementation that will always return {@code 0}.
	 *
	 * @param size The number of bytes.
	 * @param huge Whether huge pages should be used.
	 * @return The base address of the allocated memory region.
	 */
	@Contract(value = "_, true, null -> fail", pure = true)
	private static native long c_allocate(long size, boolean huge, String mnt);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * Several flags are provided to customise the behaviour of the freeing.
	 * When the parameter {@code huge} is set to {@code true}, normal memory freeing will take place, usually
	 * implemented with the C library function {@code free(void *)}.
	 * Conversely, when the parameter {@code huge} is set to {@link AllocationType#HUGE}, hugepage-based memory freeing
	 * will take place, usually implemented with platform-dependent system calls.
	 * <p>
	 * The parameter {@code size} might be used in some platforms; consistency is important, as the behaviour is
	 * undefined if the size used with {@link #allocate(long, AllocationType, LayoutType)} and this method's do not
	 * match.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param address The base address of the memory region.
	 * @param size    The size of the memory region.
	 * @param huge    Whether huge memory pages should be used.
	 * @return Whether the operation succeeded.
	 */
	@Contract(pure = true)
	@SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
	private static native boolean c_free(long address, long size, boolean huge);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	private static native byte c_get_byte(long src);

	/**
	 * Reads a {@code byte} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	private static native byte c_get_byte_volatile(long src);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code byte} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_byte(long dest, byte value);

	/**
	 * Writes a {@code byte} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code byte} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_byte_volatile(long dest, byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	private static native short c_get_short(long src);

	/**
	 * Reads a {@code short} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	private static native short c_get_short_volatile(long src);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code short} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_short(long dest, short value);

	/**
	 * Writes a {@code short} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code short} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_short_volatile(long dest, short value);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	private static native int c_get_int(long src);

	/**
	 * Reads a {@code int} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	private static native int c_get_int_volatile(long src);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code int} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_int(long dest, int value);

	/**
	 * Writes an {@code int} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code int} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_int_volatile(long dest, int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	private static native long c_get_long(long src);

	/**
	 * Reads a {@code long} from an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	private static native long c_get_long_volatile(long src);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code long} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_long(long dest, long value);

	/**
	 * Writes a {@code long} to an arbitrary volatile memory address.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code long} to write.
	 */
	@Contract(pure = true)
	private static native void c_put_long_volatile(long dest, long value);

	/**
	 * Copies a memory region into a primitive byte array.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	private static native void c_get(long src, int size, @NotNull byte[] dest, int offset);

	/**
	 * Copies a memory region into a primitive byte array.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src    The source volatile memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	private static native void c_get_volatile(long src, int size, @NotNull byte[] dest, int offset);

	/**
	 * Copies a primitive byte array into a memory region.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest   The destination memory address to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source primitive array to copy from.
	 * @param offset The offset from which to start copying from.
	 */
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	private static native void c_put(long dest, int size, @NotNull byte[] src, int offset);

	/**
	 * Copies a primitive byte array into a memory region.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param dest   The destination volatile memory address to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source primitive array to copy from.
	 * @param offset The offset from which to start copying from.
	 */
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	private static native void c_put_volatile(long dest, int size, @NotNull byte[] src, int offset);

	/**
	 * Copies a memory region into another memory region.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 */
	@Contract(pure = true)
	private static native void c_copy(long src, int size, long dest);

	/**
	 * Copies a memory region into another memory region using volatile memory addresses.
	 * <p>
	 * If the memory address is {@code 0}, the behaviour is undefined.
	 *
	 * @param src  The source volatile memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination volatile memory address to copy to.
	 */
	@Contract(pure = true)
	private static native void c_copy_volatile(long src, int size, long dest);

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
	private static native long c_virt2phys(long address);

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/**
	 * Once-callable private constructor.
	 * <p>
	 * This constructor will check if the member {@link #singleton} is {@code null} or not.
	 * Because the member {@link #singleton} is initialized with a new instance of this class, any further attempts to
	 * instantiate it will produce an {@link IllegalStateException} to be thrown.
	 */
	private JniMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating a JNI-backed memory manager");
		if (singleton != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getSingleton() instead.");
		}
		System.loadLibrary("ixy");
		HUGE_PAGE_SIZE = c_hugepage_size();
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int addressSize() {
		if (BuildConfig.DEBUG) log.debug("Computing address size using C");
		return c_address_size();
	}

	@Override
	@Contract(pure = true)
	public long pageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing page size using C");
		return c_page_size();
	}

	@Override
	@Contract(pure = true)
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using C");
		return c_hugepage_size();
	}

	@Override
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail", pure = true)
	public long allocate(long size, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			if (size <= 0) throw new InvalidSizeException("size");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
			if (layoutType == null) throw new InvalidNullParameterException("layoutType");
		}

		// Perform some checks when using huge memory pages
		val type = layoutType == LayoutType.CONTIGUOUS ? "contiguous" : "non-contiguous";
		if (allocationType == AllocationType.HUGE) {
			if (BuildConfig.DEBUG) {
				log.debug("Allocating {} {} hugepage-backed bytes using C", size, type);
			}

			// If no huge memory page file support has been detected, exit right away
			if (HUGE_PAGE_SIZE <= 0) return 0;

			// Round the size to a multiple of the page size
			val mask = (HUGE_PAGE_SIZE - 1);
			size = (size & mask) == 0 ? size : (size + HUGE_PAGE_SIZE) & ~mask;

			// Skip if we cannot guarantee contiguity
			if (layoutType == LayoutType.CONTIGUOUS && size > HUGE_PAGE_SIZE) return 0;
		} else if (BuildConfig.DEBUG) {
			log.debug("Allocating {} {} bytes using C", size, type);
		}

		// Allocate the memory
		return c_allocate(size, allocationType == AllocationType.HUGE, BuildConfig.HUGE_MNT);
	}

	@Override
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail; _, !null, !null -> new", pure = true)
	public @NotNull IxyDmaMemory dmaAllocate(long size, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DualMemory using C");
		val virt = allocate(size, allocationType, layoutType);
		val phys = virt2phys(virt);
		return DmaMemory.of(virt, phys);
	}

	@Override
	@Contract(value = "_, _, null -> fail", pure = true)
	public boolean free(long address, long size, @NotNull AllocationType allocationType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			if (address == 0) throw new InvalidMemoryAddressException("address");
			if (size <= 0) throw new InvalidSizeException("size");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
		}

		// Perform some checks when using huge memory pages
		val xaddress = Long.toHexString(address);
		val huge = allocationType == AllocationType.HUGE;
		if (huge) {
			if (BuildConfig.DEBUG) {
				log.debug("Freeing {} hugepage-backed bytes @ 0x{} using C", size, xaddress);
			}

			// If no huge memory page file support has been detected, exit right away
			if (HUGE_PAGE_SIZE <= 0) return false;

			// Round the size and address to a multiple of the page size
			val mask = (HUGE_PAGE_SIZE - 1);
			address = (address & mask) == 0 ? address : address & ~mask;
			size = (size & mask) == 0 ? size : (size + HUGE_PAGE_SIZE) & ~mask;
		} else if (BuildConfig.DEBUG) {
			log.debug("Freeing {} bytes @ 0x{} using C", size, xaddress);
		}

		// Perform some checks when using huge memory pages
		return c_free(address, size, huge);
	}

	@Override
	@Contract(pure = true)
	public byte getByte(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading byte @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_byte(address);
	}

	@Override
	@Contract(pure = true)
	public byte getByteVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile byte @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_byte_volatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putByte(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_byte(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putByteVolatile(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_byte_volatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public short getShort(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading short @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_short(address);
	}

	@Override
	@Contract(pure = true)
	public short getShortVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile short @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_short_volatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putShort(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_short(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putShortVolatile(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_short_volatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public int getInt(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading int @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_int(address);
	}

	@Override
	@Contract(pure = true)
	public int getIntVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile int @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_int_volatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putInt(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_int(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putIntVolatile(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_int_volatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public long getLong(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading long @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_long(address);
	}

	@Override
	@Contract(pure = true)
	public long getLongVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile long @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("src");
		return c_get_long_volatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putLong(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_long(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putLongVolatile(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && address == 0L) throw new InvalidMemoryAddressException("dest");
		c_put_long_volatile(address, value);
	}

	@Override
	@SuppressWarnings("Duplicates")
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	public void get(long src, int size, @NotNull byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (!check(src, size, dest, offset)) return;
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Copying memory data segment of {} bytes from 0x{} using C", size, xaddress);
		}
		c_get(src, size, dest, offset);
	}

	@Override
	@SuppressWarnings("Duplicates")
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	public void getVolatile(long src, int size, @NotNull byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (!check(src, size, dest, offset)) return;
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Copying memory data segment of {} bytes from 0x{} using C", size, xaddress);
		}
		c_get_volatile(src, size, dest, offset);
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	public void put(long dest, int size, @NotNull byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (!check(dest, size, src, offset)) return;
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(dest);
			log.debug("Copying buffer of {} bytes at offset 0x{} using the Unsafe object", size, xaddress);
		}
		c_put(dest, size, src, offset);
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	public void putVolatile(long dest, int size, @NotNull byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (!check(dest, size, src, offset)) return;
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(dest);
			log.debug("Copying buffer of {} bytes at offset 0x{} using C", size, xaddress);
		}
		c_put_volatile(dest, size, src, offset);
	}

	@Override
	@Contract(pure = true)
	public void copy(long src, int size, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using C", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED && !check(src, dest, size)) return;
		c_copy(src, size, dest);
	}

	@Override
	@Contract(pure = true)
	public void copyVolatile(long src, int size, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using C", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED && !check(src, dest, size)) return;
		c_copy_volatile(src, size, dest);
	}

	@Override
	@Contract(pure = true)
	public long virt2phys(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Translating virtual address 0x{} using C", xaddress);
		}
		return c_virt2phys(address);
	}

}
