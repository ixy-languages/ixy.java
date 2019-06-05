package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Implementation of memory manager backed by a native C library using JNI calls.
 * <p>
 * This implementation performs checks on the parameters based on the value of {@link BuildConfig#OPTIMIZED}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class JniMemoryManager implements IxyMemoryManager {

	//////////////////////////////////////////////////// EXCEPTIONS ////////////////////////////////////////////////////

	/** Cached exception thrown by the methods that need a virtual memory address and it is not correctly formatted. */
	private static final IllegalArgumentException ADDRESS = new IllegalArgumentException("Address must not be null");

	/** Cached exception thrown by the methods that need a size and it is not correctly formatted. */
	private static final IllegalArgumentException SIZE = new IllegalArgumentException("Size must be an integer greater than 0");

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/** Cached huge page size. */
	private static long HUGE_PAGE_SIZE;

	/**
	 * Cached instance to use as singleton.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	private static final JniMemoryManager instance = new JniMemoryManager();

	/**
	 * Common checks performed by {@link #get(long, int, byte[], int)} and {@link #getVolatile(long, int, byte[],
	 * int)}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	@SuppressWarnings("Duplicates")
	private static void getCheck(final long src, int size, final byte[] dest, final int offset) {
		if (src == 0) throw ADDRESS;
		else if (size <= 0) throw SIZE;
		else if (dest == null) throw new IllegalArgumentException("Buffer must not be null");
		else if (dest.length <= 0) throw new IllegalArgumentException("Buffer must have a capacity of at least 1");
		else if (offset < 0) throw new IllegalArgumentException("Offset must be greater than or equal to 0");
		else if (offset >= dest.length)
			throw new IllegalArgumentException("Offset cannot be greater than or equal to the buffer length");
	}

	/**
	 * Common checks performed by {@link #put(long, int, byte[], int)} and {@link #putVolatile(long, int, byte[],
	 * int)}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 */
	@SuppressWarnings("Duplicates")
	private static void putCheck(final long dest, int size, final byte[] src, final int offset) {
		if (dest == 0) throw ADDRESS;
		else if (size <= 0) throw SIZE;
		else if (src == null) throw new IllegalArgumentException("Buffer must not be null");
		else if (src.length <= 0) throw new IllegalArgumentException("Buffer must have a capacity of at least 1");
		else if (offset < 0) throw new IllegalArgumentException("Offset must be greater than or equal to 0");
		else if (offset >= src.length)
			throw new IllegalArgumentException("Offset cannot be greater than or equal to the buffer length");
	}

	/**
	 * Common checks performed by {@link #copy(long, int, long)} and {@link #copyVolatile(long, int, long)}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 */
	private static void copyCheck(final long src, final int size, final long dest) {
		if (src == 0) throw ADDRESS;
		else if (size <= 0) throw SIZE;
		else if (dest == 0) throw ADDRESS;
	}

	// Load the native library and cache the huge memory page size
	static {
		System.loadLibrary("ixy");
		HUGE_PAGE_SIZE = c_hugepage_size();
	}

	////////////////////////////////////////////////// NATIVE METHODS //////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with any other
	 * operative system will use a dummy implementation that will always return {@code 0}.
	 *
	 * @return The page size of the system.
	 */
	private static native int c_page_size();

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * This method is portable and should work in all operative systems. The result will always be a power of two.
	 *
	 * @return The address size of the system.
	 */
	private static native int c_address_size();

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * When there is an error, two different numbers can be returned, although it is not consistent across operative
	 * systems:
	 * <ul>
	 * <li>When there is an error or the mount point of the {@code hugetlbfs} is not found, a {@code -1} is returned
	 * in {@code Linux} and {@code Windows}.</li>
	 * <li>If the mount point was found but the smartHugepageSize size could not be computed, a {@code 0} is returned
	 * in {@code Linux}. This error code will never be returned under {@code Windows}.</li>
	 * </ul>
	 * <p>
	 * Although the name uses the nomenclature {@code smartHugepageSize}, other operative systems use the same
	 * technology but with different name, for example, {@code largepage} on {@code Windows}, {@code superpage} in
	 * {@code BSD} or {@code bigpage} in {@code RHEL}.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with another operative
	 * system will use a dummy implementation that will always return {@code -1}.
	 *
	 * @return The size of a huge memory page.
	 */
	private static native long c_hugepage_size();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The memory allocated by this method MUST be freed with {@link #c_free(long, long, boolean)}, as it won't be
	 * garbage-collected by the JVM.
	 * <p>
	 * All the checks that were once performed by this native method have been moved to the Java interface.
	 * <p>
	 * The implementation needs a the mount point of the {@code hugetlbfs}, but it will be used only under Linux. On
	 * Windows it uses {@code VirtualAlloc} to allocate a huge memory page.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory page should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @param mnt        The mount point of the {@code hugetlbfs}.
	 * @return The base address of the allocated memory region.
	 */
	private static native long c_allocate(final long size, final boolean huge, final boolean contiguous, final String mnt);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * All the checks that were once performed by this native method have been moved to the Java interface.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @return If the operation succeeded.
	 */
	private static native boolean c_free(final long address, final long size, final boolean huge);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code byte}.
	 */
	private static native byte c_get_byte(final long src);

	/**
	 * Reads a {@code byte} from an arbitrary volatile memory address.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code byte}.
	 */
	private static native byte c_get_byte_volatile(final long src);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code byte} to write.
	 */
	private static native void c_put_byte(final long dest, final byte value);

	/**
	 * Writes a {@code byte} to an arbitrary volatile memory address.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code byte} to write.
	 */
	private static native void c_put_byte_volatile(final long dest, final byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code short}.
	 */
	private static native short c_get_short(final long src);

	/**
	 * Reads a {@code short} from an arbitrary volatile memory address.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code short}.
	 */
	private static native short c_get_short_volatile(final long src);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code short} to write.
	 */
	private static native void c_put_short(final long dest, final short value);

	/**
	 * Writes a {@code short} to an arbitrary volatile memory address.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code short} to write.
	 */
	private static native void c_put_short_volatile(final long dest, final short value);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code int}.
	 */
	private static native int c_get_int(final long src);

	/**
	 * Reads a {@code int} from an arbitrary volatile memory address.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code int}.
	 */
	private static native int c_get_int_volatile(final long src);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code int} to write.
	 */
	private static native void c_put_int(final long dest, final int value);

	/**
	 * Writes an {@code int} to an arbitrary volatile memory address.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code int} to write.
	 */
	private static native void c_put_int_volatile(final long dest, final int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param src The memory address to read from.
	 * @return The read {@code long}.
	 */
	private static native long c_get_long(final long src);

	/**
	 * Reads a {@code long} from an arbitrary volatile memory address.
	 *
	 * @param src The volatile memory address to read from.
	 * @return The read {@code long}.
	 */
	private static native long c_get_long_volatile(final long src);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param dest  The memory address to write to.
	 * @param value The {@code long} to write.
	 */
	private static native void c_put_long(final long dest, final long value);

	/**
	 * Writes a {@code long} to an arbitrary volatile memory address.
	 *
	 * @param dest  The volatile memory address to write to.
	 * @param value The {@code long} to write.
	 */
	private static native void c_put_long_volatile(final long dest, final long value);

	/**
	 * Copies a memory region into a primitive byte array.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	private static native void c_get(final long src, final int size, final byte[] dest, final int offset);

	/**
	 * Copies a memory region into a primitive byte array.
	 *
	 * @param src    The source volatile memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 */
	private static native void c_get_volatile(final long src, final int size, final byte[] dest, final int offset);

	/**
	 * Copies a primitive byte array into a memory region.
	 *
	 * @param dest   The destination memory address to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source primitive array to copy from.
	 * @param offset The offset from which to start copying from.
	 */
	private static native void c_put(final long dest, final int size, final byte[] src, final int offset);

	/**
	 * Copies a primitive byte array into a memory region.
	 *
	 * @param dest   The destination volatile memory address to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source primitive array to copy from.
	 * @param offset The offset from which to start copying from.
	 */
	private static native void c_put_volatile(final long dest, final int size, final byte[] src, final int offset);

	/**
	 * Copies a memory region into another memory region.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 */
	private static native void c_copy(final long src, final int size, final long dest);

	/**
	 * Copies a memory region into another memory region using volatile memory addresses.
	 *
	 * @param src  The source volatile memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination volatile memory address to copy to.
	 */
	private static native void c_copy_volatile(final long src, final int size, final long dest);

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
	private static native long c_virt2phys(final long address);

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/**
	 * Once-callable private constructor.
	 * <p>
	 * This constructor will check if the member {@link #instance} is {@code null} or not. Because the member {@link
	 * #instance} is initialized with a new instance of this class, any further attempts to instantiate it will produce
	 * an {@link IllegalStateException} to be thrown.
	 */
	private JniMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (instance != null)
			throw new IllegalStateException("An instance cannot be created twice. Use getInstance() instead.");
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	public int addressSize() {
		if (BuildConfig.DEBUG) log.debug("Computing address size using C");
		return c_address_size();
	}

	/** {@inheritDoc} */
	@Override
	public long pageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing page size using C");
		return c_page_size();
	}

	/** {@inheritDoc} */
	@Override
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using C");
		return c_hugepage_size();
	}

	/** {@inheritDoc} */
	@Override
	public long allocate(final long size, final boolean huge, final boolean contiguous) {
		if (BuildConfig.DEBUG) {
			if (huge) {
				if (contiguous) log.debug("Allocating {} huge-page-backed contiguous bytes using C", size);
				else log.debug("Allocating {} huge-page-backed non-contiguous bytes using C", size);
			} else {
				if (contiguous) log.debug("Allocating {} contiguous bytes using C", size);
				else log.debug("Allocating {} non-contiguous bytes using C", size);
			}
		}
		if (!BuildConfig.OPTIMIZED && size < 0) throw SIZE;

		// Perform some checks when using huge memory pages
		if (huge) {
			// If no huge memory page file support has been detected, exit right away
			if (HUGE_PAGE_SIZE <= 0) return 0;

			// Round the size to a multiple of the page size
			val mask = (HUGE_PAGE_SIZE - 1);
			val round = (size & mask) == 0 ? size : (size + HUGE_PAGE_SIZE) & ~mask;

			// Skip if we cannot guarantee contiguity
			if (contiguous && round > HUGE_PAGE_SIZE) return 0;

			// Call the native method with the correct size
			return c_allocate(round, huge, contiguous, BuildConfig.HUGE_MNT);
		} else {
			return c_allocate(size, huge, contiguous, BuildConfig.HUGE_MNT);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean free(final long src, final long size, final boolean huge) {
		if (!BuildConfig.OPTIMIZED) {
			if (src == 0) throw ADDRESS;
			else if (size < 0) throw SIZE;
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			if (huge) log.debug("Freeing {} huge-page-backed bytes @ 0x{} using the Unsafe object", size, xaddress);
			else log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", size, xaddress);
		}

		// Perform some checks when using huge memory pages
		if (huge) {
			// If no huge memory page file support has been detected, exit right away
			if (HUGE_PAGE_SIZE <= 0) return false;

			// Round the size and address to a multiple of the page size
			val mask = (HUGE_PAGE_SIZE - 1);
			val roundAddress = (src & mask) == 0 ? src : src & ~mask;
			val roundSize = (size & mask) == 0 ? size : (size + HUGE_PAGE_SIZE) & ~mask;

			// Call the native method with the correct size
			return c_free(roundAddress, roundSize, huge);
		} else {
			return c_free(src, size, huge);
		}
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading byte @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_byte(src);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading volatile byte @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_byte_volatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long dest, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(dest);
			log.debug("Putting byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_byte(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long dest, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(dest);
			log.debug("Putting volatile byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_byte_volatile(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading short @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_short(src);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading volatile short @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_short_volatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long dest, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(dest);
			log.debug("Putting short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_short(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long dest, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(dest);
			log.debug("Putting volatile short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_short_volatile(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading int @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_int(src);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading volatile int @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_int_volatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long dest, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(dest);
			log.debug("Putting int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_int(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long dest, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(dest);
			log.debug("Putting volatile int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_int_volatile(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading long @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_long(src);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Reading volatile long @ 0x{} using C", xaddress);
		}
		if (!BuildConfig.OPTIMIZED && src == 0) throw ADDRESS;
		return c_get_long_volatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long dest, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(dest);
			log.debug("Putting long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_long(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long dest, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(dest);
			log.debug("Putting volatile long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		if (!BuildConfig.OPTIMIZED && dest == 0) throw ADDRESS;
		c_put_long_volatile(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("Duplicates")
	public void get(final long src, int size, final byte[] dest, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			getCheck(src, size, dest, offset);
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Copying memory data segment of {} bytes from 0x{} using the Unsafe object", size, xaddress);
		}
		c_get(src, size, dest, offset);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("Duplicates")
	public void getVolatile(final long src, int size, final byte[] dest, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			getCheck(src, size, dest, offset);
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(src);
			log.debug("Copying memory data segment of {} bytes from 0x{} using the Unsafe object", size, xaddress);
		}
		c_get_volatile(src, size, dest, offset);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("Duplicates")
	public void put(final long dest, int size, final byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			putCheck(dest, size, src, offset);
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(dest);
			log.debug("Copying buffer of {} bytes at offset 0x{} using the Unsafe object", size, xaddress);
		}
		c_put(dest, size, src, offset);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("Duplicates")
	public void putVolatile(final long dest, int size, final byte[] src, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			putCheck(dest, size, src, offset);
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(dest);
			log.debug("Copying buffer of {} bytes at offset 0x{} using the Unsafe object", size, xaddress);
		}
		c_put_volatile(dest, size, src, offset);
	}

	/** {@inheritDoc} */
	@Override
	public void copy(final long src, final int size, final long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using C", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) copyCheck(src, size, dest);
		c_copy(src, size, dest);
	}

	/** {@inheritDoc} */
	@Override
	public void copyVolatile(final long src, final int size, final long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using C", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) copyCheck(src, size, dest);
		c_copy_volatile(src, size, dest);
	}

	/** {@inheritDoc} */
	@Override
	public long virt2phys(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Translating virtual address 0x{} using C", xaddress);
		}
		return c_virt2phys(address);
	}

	/** {@inheritDoc} */
	@Override
	public IxyDmaMemory dmaAllocate(long size, boolean huge, boolean contiguous) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DualMemory using C");
		val virt = allocate(size, huge, contiguous);
		val phys = virt2phys(virt);
		return new DmaMemory(virt, phys);
	}

}
