package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyMemoryManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Implementation of memory manager backed by a native library and JNI calls.
 * <p>
 * This implementation performs checks on the parameters based on the value of {@link BuildConfig#OPTIMIZED}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class JniMemoryManager implements IxyMemoryManager {

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

	/** Private constructor that throws an exception if the instance is already instantiated. */
	private JniMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (instance != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getInstance() instead.");
		}
	}

	// Load the native library
	static {
		System.loadLibrary("ixy");
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
	 *   <li>When there is an error or the mount point of the {@code hugetlbfs} is not found, a {@code -1} is returned
	 *       in {@code Linux} and {@code Windows}.</li>
	 *   <li>If the mount point was found but the smartHugepageSize size could not be computed, a {@code 0} is returned
	 *       in {@code Linux}. This error code will never be returned under {@code Windows}.</li>
	 * </ul>
	 * <p>
	 * Although the name uses the nomenclature {@code smartHugepageSize}, other operative systems use the same
	 * technology but with different name, for example, {@code largepage} on {@code Windows}, {@code superpage} in
	 * {@code BSD} or {@code bigpage} in {@code RHEL}.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with another
	 * operative system will use a dummy implementation that will always return {@code -1}.
	 *
	 * @return The size of a huge memory page.
	 */
	private static native long c_hugepage_size();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The memory allocated by this method MUST be freed with {@link #c_free(long, long, boolean)}, as it won't be
	 * garbage collected by the JVM.
	 * <p>
	 * The allocated memory region can be contiguous if requested, but since the implementation uses {@code hugepages}
	 * to do so, if the requested size is bigger than the bytes a huge page can store, the allocation will return the
	 * invalid address {@code 0}.
	 * <p>
	 * The implementation needs a the mount point of the {@code hugetlbfs}, but it will be used only under Linux.
	 * On Windows it uses {@code VirtualAlloc} to allocate a huge memory page.
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
	 * If the given address is not a multiple of the size of a memory page it won't fail because it will be converted to
	 * the base address of a huge memory page.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @return If the operation succeeded.
	 */
	private static native boolean c_free(final long address, final long size, final boolean huge);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native byte c_get_byte(final long address);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native byte c_get_byte_volatile(final long address);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_byte(final long address, final byte value);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_byte_volatile(final long address, final byte value);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native short c_get_short(final long address);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native short c_get_short_volatile(final long address);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_short(final long address, final short value);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_short_volatile(final long address, final short value);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native int c_get_int(final long address);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native int c_get_int_volatile(final long address);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_int(final long address, final int value);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_int_volatile(final long address, final int value);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native long c_get_long(final long address);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	private static native long c_get_long_volatile(final long address);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_long(final long address, final long value);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	private static native void c_put_long_volatile(final long address, final long value);

	/**
	 * Copies a memory region into a primitive byte array.
	 *
	 * @param address The source address to copy from.
	 * @param size    The number of bytes to copy.
	 * @param buffer  The primitive array to copy to.
	 */
	private static native long c_copy(final long address, final int size, final byte[] buffer);

	/**
	 * Copies a memory region into a primitive byte array.
	 *
	 * @param address The source address to copy from.
	 * @param size    The number of bytes to copy.
	 * @param buffer  The primitive array to copy to.
	 */
	private static native long c_copy_volatile(final long address, final int size, final byte[] buffer);

	/**
	 * Translates a virtual memory {@code address} to its equivalent physical counterpart.
	 *
	 * @param address The address to write to.
	 * @return The physical address.
	 */
	private static native long c_virt2phys(final long address);

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
		if (!BuildConfig.OPTIMIZED) {
			if (size < 0) {
				throw new IllegalArgumentException("Size must be an integer greater than 0");
			}
		}
		if (BuildConfig.DEBUG) {
			if (huge) {
				if (contiguous) {
					log.debug("Allocating {} huge-page-backed contiguous bytes using C", size);
				} else {
					log.debug("Allocating {} huge-page-backed non-contiguous bytes using C", size);
				}
			} else {
				if (contiguous) {
					log.debug("Allocating {} contiguous bytes using C", size);
				} else {
					log.debug("Allocating {} non-contiguous bytes using C", size);
				}
			}
		}
		return c_allocate(size, huge, contiguous, BuildConfig.HUGE_MNT);
	}

	/** {@inheritDoc} */
	@Override
	public boolean free(final long address, final long size, final boolean huge) {
		if (!BuildConfig.OPTIMIZED) {
			if (address == 0) {
				throw new IllegalArgumentException("Address must not be null");
			} else if (size < 0) {
				throw new IllegalArgumentException("Size must be an integer greater than 0");
			}
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			if (huge) {
				log.debug("Freeing {} huge-page-backed bytes @ 0x{} using the Unsafe object", size, xaddress);
			} else {
				log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", size, xaddress);
			}
		}
		return c_free(address, size, huge);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading byte @ 0x{} using C", xaddress);
		}
		return c_get_byte(address);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile byte @ 0x{} using C", xaddress);
		}
		return c_get_byte_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long address, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_byte(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long address, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile byte 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_byte_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading short @ 0x{} using C", xaddress);
		}
		return c_get_short(address);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile short @ 0x{} using C", xaddress);
		}
		return c_get_short_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long address, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_short(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long address, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile short 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_short_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading int @ 0x{} using C", xaddress);
		}
		return c_get_int(address);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile int @ 0x{} using C", xaddress);
		}
		return c_get_int_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long address, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_int(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long address, final int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile int 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_int_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading long @ 0x{} using C", xaddress);
		}
		return c_get_long(address);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Reading volatile long @ 0x{} using C", xaddress);
		}
		return c_get_long_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long address, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_long(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long address, final long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			log.debug("Putting volatile long 0x{} @ 0x{} using C", xvalue, xaddress);
		}
		c_put_long_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void copy(final long address, int size, final byte[] buffer) {
		if (!BuildConfig.OPTIMIZED) {
			if (size <= 0) {
				throw new IllegalArgumentException("Size must be greater than 0");
			} else if (buffer == null) {
				throw new IllegalArgumentException("Buffer must not be null");
			} else if (buffer.length <= 0) {
				throw new IllegalArgumentException("Buffer must have a capacity of at least 1");
			}
			size = Math.min(size, buffer.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Copying data segment of {} bytes at offset 0x{} using C", size, xaddress);
		}
		c_copy(address, size, buffer);
	}

	/** {@inheritDoc} */
	@Override
	public void copyVolatile(final long address, int size, final byte[] buffer) {
		if (!BuildConfig.OPTIMIZED) {
			if (size <= 0) {
				throw new IllegalArgumentException("Size must be greater than 0");
			} else if (buffer == null) {
				throw new IllegalArgumentException("Buffer must not be null");
			} else if (buffer.length <= 0) {
				throw new IllegalArgumentException("Buffer must have a capacity of at least 1");
			}
			size = Math.min(size, buffer.length);
		}
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Copying volatile data segment of {} bytes at offset 0x{} using C", size, xaddress);
		}
		c_copy_volatile(address, size, buffer);
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
	public DualMemory dmaAllocate(long size, boolean huge, boolean contiguous) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DualMemory using C");
		val virt = allocate(size, huge, contiguous);
		val phys = virt2phys(virt);
		return new DmaMemory(virt, phys);
	}

}
