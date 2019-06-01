package de.tum.in.net.ixy.memory;

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
public class UnsafeMemoryManager implements IxyMemoryManager {

	/** The unsafe object that will do all the operations. */
	private transient Unsafe unsafe;

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

	/** Private constructor that throws an exception if the instance is already instantiated. */
	protected UnsafeMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (instance != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getInstance() instead.");
		}
		try {
			val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = (Unsafe) theUnsafeField.get(null);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
			log.error("Error getting Unsafe object", e);
		}
	}

	/** Checks if the {@link Unsafe} object is available. */
	private void checkUnsafe() {
		if (unsafe == null) {
			throw new IllegalStateException("The Unsafe object is not available");
		}
	}

	/** {@inheritDoc} */
	@Override
	public int addressSize() {
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		if (BuildConfig.DEBUG) log.debug("Computing address size using the Unsafe object");
		return unsafe.addressSize();
	}

	/** {@inheritDoc} */
	@Override
	public long pageSize() {
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		if (BuildConfig.DEBUG) log.debug("Computing page size using the Unsafe object");
		return unsafe.pageSize();
	}

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * The method will throw an {@link UnsupportedOperationException} because the {@link Unsafe} object does not provide
	 * an implementation for this operation.
	 * <p>
	 * This method is marked as deprecated to prevent accidental usage, however it won't be removed in future versions.
	 *
	 * @return The size of a huge memory page.
	 * @see #pageSize()
	 * @deprecated
	 */
	@Override
	@Deprecated
	public long hugepageSize() {
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using the Unsafe object");
		throw new UnsupportedOperationException("Unsafe does not provide an implementation for this operation");
	}

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The method can be customized to use huge memory pages or to fail if the physical contiguity cannot be guaranteed.
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
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (size > 0) {
				throw new IllegalArgumentException("Size must be an integer greater than 0");
			}
		}
		if (huge) {
			if (BuildConfig.DEBUG) {
				if (contiguous) {
					log.debug("Allocating {} contiguous huge-page-backed bytes using the Unsafe object", size);
				} else {
					log.debug("Allocating {} non-contiguous huge-page-backed bytes using the Unsafe object", size);
				}
			}
			throw new UnsupportedOperationException("Unsafe does not provide an implementation for this operation");
		}
		if (BuildConfig.DEBUG) {
			if (contiguous) {
				log.debug("Allocating {} contiguous bytes using the Unsafe object", size);
			} else {
				log.debug("Allocating {} non-contiguous bytes using the Unsafe object", size);
			}
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
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @param huge    Whether huge memory pages should be used.
	 * @return Whether the operation succeeded.
	 */
	@Override
	public boolean free(final long address, final long size, final boolean huge) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			} else if (size > 0) {
				throw new IllegalArgumentException("Size must be an integer greater than 0");
			}
		}
		if (huge) {
			if (BuildConfig.DEBUG) {
				val xaddress = Long.toHexString(address);
				log.debug("Freeing {} huge-page-backed bytes @ 0x{} using the Unsafe object", size, xaddress);
			}
			throw new UnsupportedOperationException("Unsafe does not provide an implementation for this operation");
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", size, xaddress);
		}
		if (BuildConfig.OPTIMIZED) {
			unsafe.freeMemory(address);
		} else {
			try {
				unsafe.freeMemory(address);
			} catch (final RuntimeException e) {
				if (BuildConfig.DEBUG) log.error("Could not free memory", e);
				return false;
			}
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading byte @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getByte(address);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile byte @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getByteVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long address, final byte value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting byte 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putByte(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long address, final byte value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile byte 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putByteVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading short @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getShort(address);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile short @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getShortVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long address, final short value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting short 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putShort(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long address, final short value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile short 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putShortVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading int @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getInt(address);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile int @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getIntVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long address, final int value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting int 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putInt(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long address, final int value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile int 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putIntVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading long @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getLong(address);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final long address) {
		if (!BuildConfig.OPTIMIZED) {
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
			checkUnsafe();
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile long @ 0x{} using the Unsafe object", xaddress);
		}
		return unsafe.getLongVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long address, final long value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting long 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putLong(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long address, final long value) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			}
		}
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile long 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putLongVolatile(null, address, value);
	}

}
