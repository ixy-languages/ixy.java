package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;

import sun.misc.Unsafe;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Implementation of memory manager backed by the {@link Unsafe} object.
 * <p>
 * This implementation performs checks on the parameters and catches exceptions based on the value of {@link
 * BuildConfig#OPTIMIZED}. Also, the {@link Unsafe} instance is cached when the class is first instantiated but an
 * exception is not thrown until a method is called, this way if this class is extended and only overridden methods are
 * called, no exception will be thrown, which is something good.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class UnsafeMemoryManager implements IxyMemoryManager {

	//////////////////////////////////////////////////// EXCEPTIONS ////////////////////////////////////////////////////

	/** Cached exception thrown by the methods that cannot be implemented using the {@link Unsafe} object. */
	private static final UnsupportedOperationException UNSUPPORTED = new UnsupportedOperationException("Unsafe does not provide an implementation for this operation");

	/** Cached exception thrown by the methods that need a virtual memory address and it is not correctly formatted. */
	private static final IllegalArgumentException ADDRESS = new IllegalArgumentException("Address must not be null");

	/** Cached exception thrown by the methods that need a size and it is not correctly formatted. */
	private static final IllegalArgumentException SIZE = new IllegalArgumentException("Size must be an integer greater than 0");

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Cached instance to use as singleton.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	private static final UnsafeMemoryManager instance = new UnsafeMemoryManager();

	/**
	 * Common checks performed by {@link #get(long, int, byte[], int)} and {@link #getVolatile(long, int, byte[], int)}.
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
		else if (offset >= dest.length) throw new IllegalArgumentException("Offset cannot be greater than or equal to the buffer length");
	}

	/**
	 * Common checks performed by {@link #put(long, int, byte[], int)} and {@link #putVolatile(long, int, byte[], int)}.
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
		else if (offset >= src.length) throw new IllegalArgumentException("Offset cannot be greater than or equal to the buffer length");
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

	///////////////////////////////////////////////////// MEMBERS //////////////////////////////////////////////////////

	/** The unsafe object that will do all the operations. */
	private transient Unsafe unsafe;

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/**
	 * Once-callable private constructor.
	 * <p>
	 * This constructor will check if the member {@link #instance} is {@code null} or not.
	 * Because the member {@link #instance} is initialized with a new instance of this class, any further attempts to
	 * instantiate it will produce an {@link IllegalStateException} to be thrown.
	 */
	private UnsafeMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (instance != null) throw new IllegalStateException("An instance cannot be created twice. Use getInstance() instead.");
		try {
			val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = (Unsafe) theUnsafeField.get(null);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
			log.error("Error getting Unsafe object", e);
		}
	}

	/**
	 * Throws an {@link IllegalStateException} if the {@link #unsafe} member is null.
	 * <p>
	 * This method is only called in conditional blocks using the {@link BuildConfig#OPTIMIZED} member, so when the
	 * library is build for production mode this method is never called as a way of speeding up the whole program.
	 */
	private void checkUnsafe() {
		if (unsafe == null) throw new IllegalStateException("The Unsafe object is not available");
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage, however it won't be removed in future versions.
	 *
	 * @return The size of a huge memory page in the host system it is being executed.
	 * @see #pageSize()
	 * @deprecated
	 */
	@Override
	@Deprecated
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		throw UNSUPPORTED;
	}

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method can be customized to use huge memory pages or to fail if the physical contiguity cannot be
	 * guaranteed.
	 * <p>
	 * This method will throw an {@link UnsupportedOperationException} when allocating using huge memory pages because
	 * the {@link Unsafe} object does not provide an implementation for this operation.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory page should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @return The base address of the allocated memory region.
	 */
	@Override
	public long allocate(final long size, final boolean huge, final boolean contiguous) {
		if (huge) {
			if (BuildConfig.DEBUG) {
				if (contiguous) log.debug("Allocating {} contiguous huge-page-backed bytes using the Unsafe object", size);
				else log.debug("Allocating {} non-contiguous huge-page-backed bytes using the Unsafe object", size);
			}
			throw UNSUPPORTED;
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (size < 0) throw SIZE;
		}
		if (BuildConfig.DEBUG) {
			if (contiguous) log.debug("Allocating {} contiguous bytes using the Unsafe object", size);
			else log.debug("Allocating {} non-contiguous bytes using the Unsafe object", size);
		}
		if (BuildConfig.OPTIMIZED) {
			return unsafe.allocateMemory(size);
		} else {
			try {
				return unsafe.allocateMemory(size);
			} catch (final RuntimeException | OutOfMemoryError e) {
				if (BuildConfig.DEBUG) log.error("Could not allocate memory", e);
				return 0;
			}
		}
	}

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * If the parameters do not match the ones used to allocate the region, the behaviour is undefined.
	 * <p>
	 * This method will throw an {@link UnsupportedOperationException} when allocating using huge memory pages because
	 * the {@link Unsafe} object does not provide an implementation for this operation.
	 *
	 * @param src  The address of the previously allocated region.
	 * @param size The size of the allocated region.
	 * @param huge Whether huge memory pages should be used.
	 * @return Whether the operation succeeded.
	 */
	@Override
	public boolean free(final long src, final long size, final boolean huge) {
		if (huge) {
			if (BuildConfig.DEBUG) {
				val xsrc = Long.toHexString(src);
				log.debug("Freeing {} huge-page-backed bytes @ 0x{} using the Unsafe object", size, xsrc);
			}
			throw UNSUPPORTED;
		}
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", size, xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
			else if (size < 0) throw SIZE;
		}
		if (BuildConfig.OPTIMIZED) {
			unsafe.freeMemory(src);
		} else {
			try {
				unsafe.freeMemory(src);
			} catch (final RuntimeException e) {
				if (BuildConfig.DEBUG) log.error("Could not free memory", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * Copies a memory region into a primitive byte array using volatile memory addresses.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage and it won't be removed in future versions.
	 *
	 * @param src    The source memory address to copy from.
	 * @param size   The number of bytes to copy.
	 * @param dest   The destination primitive array to copy to.
	 * @param offset The offset from which to start copying to.
	 * @deprecated
	 */
	@Override
	@Deprecated
	public void getVolatile(final long src, int size, final byte[] dest, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			getCheck(src, size, dest, offset);
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile memory region ({} B) @ 0x{} with offset {} using the Unsafe object", size, xsrc, xoffset);
		}
		throw UNSUPPORTED;
	}

	/**
	 * Copies a primitive byte array into a memory region using volatile memory addresses.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage and it won't be removed in future versions.
	 *
	 * @param dest   The destination primitive array to copy to.
	 * @param size   The number of bytes to copy.
	 * @param src    The source memory address to copy from.
	 * @param offset The offset from which to start copying to.
	 * @deprecated
	 */
	@Override
	@Deprecated
	public void putVolatile(final long dest, int size, final byte[] src, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			putCheck(dest, size, src, offset);
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing volatile buffer ({} B) @ 0x{} with offset {} using the Unsafe object", size, xdest, xoffset);
		}
		throw UNSUPPORTED;
	}

	/**
	 * Copies a memory region into another memory region using volatile memory addresses.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage and it won't be removed in future versions.
	 *
	 * @param src  The source memory address to copy from.
	 * @param size The number of bytes to copy.
	 * @param dest The destination memory address to copy to.
	 * @deprecated
	 */
	@Override
	@Deprecated
	public void copyVolatile(final long src, final int size, final long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying volatile memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			copyCheck(src, size, dest);
		}
		throw UNSUPPORTED;
	}

	/**
	 * Translates a virtual memory {@code address} to its equivalent physical counterpart.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage and it won't be removed in future versions.
	 *
	 * @param address The memory address to translate.
	 * @return The physical memory address.
	 * @deprecated
	 */
	@Override
	@Deprecated
	public long virt2phys(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Translating virtual address 0x{} using the Unsafe object", xaddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
		}
		throw UNSUPPORTED;
	}

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for the method {@link #virt2phys(long)}.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage, however it won't be removed in future versions.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param huge       Whether huge memory pages should used.
	 * @param contiguous Whether the memory region should be physically contiguous.
	 * @return The {@link DmaMemory} instance with the virtual and physical addresses.
	 * @deprecated
	 */
	@Override
	@Deprecated
	public IxyDmaMemory dmaAllocate(long size, boolean huge, boolean contiguous) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DmaMemory using the Unsafe object");
		val virt = allocate(size, huge, contiguous);
		val phys = virt2phys(virt);
		return new DmaMemory(virt, phys);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	public int addressSize() {
		if (BuildConfig.DEBUG) log.debug("Computing address size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.addressSize();
	}

	/** {@inheritDoc} */
	@Override
	public long pageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.pageSize();
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getByte(src);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getByteVolatile(null, src);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long dest, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(dest);
			log.debug("Putting byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putByte(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long dest, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(dest);
			log.debug("Putting volatile byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putByteVolatile(null, dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getShort(src);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getShortVolatile(null, src);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long dest, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(dest);
			log.debug("Putting short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putShort(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long dest, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(dest);
			log.debug("Putting volatile short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putShortVolatile(null, dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getInt(src);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getIntVolatile(null, src);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long dest, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(dest);
			log.debug("Putting int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putInt(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long dest, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(dest);
			log.debug("Putting volatile int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putIntVolatile(null, dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getLong(src);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final long src) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (src == 0) throw ADDRESS;
		}
		return unsafe.getLongVolatile(null, src);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long dest, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(dest);
			log.debug("Putting long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putLong(dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long dest, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(dest);
			log.debug("Writing volatile long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (dest == 0) throw ADDRESS;
		}
		unsafe.putLongVolatile(null, dest, value);
	}

	/** {@inheritDoc} */
	@Override
	public void get(final long src, int size, final byte[] dest, final int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			getCheck(src, size, dest, offset);
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading memory region ({} B) @ 0x{} with offset {} using the Unsafe object", size, xsrc, xoffset);
		}
		unsafe.copyMemory(null, src, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, size);
	}

	/** {@inheritDoc} */
	@Override
	public void put(final long dest, int size, final byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			putCheck(dest, size, src, offset);
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing buffer ({} B) @ 0x{} with offset {} using the Unsafe object", size, xdest, xoffset);
		}
		unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, dest, size);
	}

	/** {@inheritDoc} */
	@Override
	public void copy(final long src, final int size, final long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			copyCheck(src, size, dest);
		}
		unsafe.copyMemory(src, dest, size);
	}

}
