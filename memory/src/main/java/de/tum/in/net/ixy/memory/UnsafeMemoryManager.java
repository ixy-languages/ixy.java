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
import sun.misc.Unsafe;

import static de.tum.in.net.ixy.memory.Utility.check;

/**
 * A simple implementation of Ixy's memory manager specification using the {@link Unsafe} object.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"DuplicateStringLiteralInspection", "HardCodedStringLiteral"})
public final class UnsafeMemoryManager implements IxyMemoryManager {

	/**
	 * Exception thrown when an operation cis not supported by the {@link Unsafe} object.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	private static final class UnsupportedUnsafeOperationException extends UnsupportedOperationException {

		/** Serial used for serialization purposes. */
		private static final long serialVersionUID = 2827402454070821412L;

		/** Builds the error message. */
		UnsupportedUnsafeOperationException() {
			super("The Unsafe object does not provide an implementation for this operation");
		}

	}

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Singleton instance.
	 * ----------- GETTER -----------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	@SuppressWarnings("JavaDoc")
	private static final IxyMemoryManager singleton = new UnsafeMemoryManager();

	///////////////////////////////////////////////////// MEMBERS //////////////////////////////////////////////////////

	/** The unsafe object that will do all the operations. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "unsafe")
	@SuppressWarnings("UseOfSunClasses")
	private Unsafe unsafe;

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/**
	 * Once-callable private constructor.
	 * <p>
	 * This constructor will check if the member property {@link #singleton} is {@code null} or not.
	 * Because the member property {@link #singleton} is initialized with a new instance of this class, any further
	 * attempts to instantiate it will produce an {@link IllegalStateException} to be thrown.
	 */
	private UnsafeMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (singleton != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getSingleton() instead.");
		}
		try {
			val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = (Unsafe) theUnsafeField.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.error("Error getting Unsafe object", e);
		}
	}

	/**
	 * Throws an {@link IllegalStateException} if the member property {@link #unsafe} is {@code null}.
	 * <p>
	 * This method is only called in conditional blocks using the {@link BuildConfig#OPTIMIZED} member, so when the
	 * library is build for production mode this method is never called as a way of speeding up the whole program.
	 */
	@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
	private void checkUnsafe() {
		if (unsafe == null) throw new IllegalStateException("The Unsafe object is not available");
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public int addressSize() {
		if (BuildConfig.DEBUG) log.debug("Computing address size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.addressSize();
	}

	@Override
	public long pageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.pageSize();
	}

	@Override
	public long allocate(long size, AllocationType allocationType, LayoutType layoutType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (size <= 0L) throw new InvalidSizeException("size");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
			if (layoutType == null) throw new InvalidNullParameterException("layoutType");
		}

		// Hugepage-based allocation is not supported
		val type = layoutType == LayoutType.CONTIGUOUS ? "contiguous" : "non-contiguous";
		if (allocationType == AllocationType.HUGE) {
			if (BuildConfig.DEBUG) {
				log.debug("Allocating {} {} hugepage-backed bytes using the Unsafe object", size, type);
			}
			throw new UnsupportedUnsafeOperationException();
		}

		// Allocate the memory
		if (BuildConfig.DEBUG) log.debug("Allocating {} {} bytes using the Unsafe object", size, type);
		if (BuildConfig.OPTIMIZED) {
			return unsafe.allocateMemory(size);
		} else {
			try {
				return unsafe.allocateMemory(size);
			} catch (RuntimeException | OutOfMemoryError e) {
				if (BuildConfig.DEBUG) log.error("Could not allocate memory", e);
				return 0L;
			}
		}
	}

	@Override
	public boolean free(long address, long size, AllocationType allocationType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
			if (size <= 0L) throw new InvalidSizeException("size");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
		}

		// Hugepage-based freeing is not supported
		val xsrc = Long.toHexString(address);
		if (allocationType == AllocationType.HUGE) {
			if (BuildConfig.DEBUG) log.debug("Freeing {} {} hugepage-backed bytes using the Unsafe object", size, xsrc);
			throw new UnsupportedUnsafeOperationException();
		}

		// Free the memory
		if (BuildConfig.DEBUG) log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", size, xsrc);
		if (BuildConfig.OPTIMIZED) {
			unsafe.freeMemory(address);
		} else {
			try {
				unsafe.freeMemory(address);
			} catch (RuntimeException e) {
				if (BuildConfig.DEBUG) log.error("Could not free memory", e);
				return false;
			}
		}
		return true;
	}

	@Override
	public byte getByte(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getByte(address);
	}

	@Override
	public byte getByteVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getByteVolatile(null, address);
	}

	@Override
	public void putByte(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putByte(address, value);
	}

	@Override
	public void putByteVolatile(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putByteVolatile(null, address, value);
	}

	@Override
	public short getShort(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getShort(address);
	}

	@Override
	public short getShortVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getShortVolatile(null, address);
	}

	@Override
	public void putShort(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putShort(address, value);
	}

	@Override
	public void putShortVolatile(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putShortVolatile(null, address, value);
	}

	@Override
	public int getInt(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getInt(address);
	}

	@Override
	public int getIntVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getIntVolatile(null, address);
	}

	@Override
	public void putInt(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putInt(address, value);
	}

	@Override
	public void putIntVolatile(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putIntVolatile(null, address, value);
	}

	@Override
	public long getLong(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getLong(address);
	}

	@Override
	public long getLongVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		return unsafe.getLongVolatile(null, address);
	}

	@Override
	public void putLong(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putLong(address, value);
	}

	@Override
	public void putLongVolatile(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Writing volatile long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException();
		}
		unsafe.putLongVolatile(null, address, value);
	}

	@Override
	public void get(long src, int size, byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(src, size, dest, offset)) return;
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading memory region ({} B) @ 0x{} with offset {} using the Unsafe object", size, xsrc, xoffset);
		}
		unsafe.copyMemory(null, src, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, size);
	}

	@Override
	public void getVolatile(long src, int size, byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(src, size, dest, offset)) return;
			size = Math.min(size, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile memory region ({} B) @ 0x{} with offset {} using the Unsafe object", size, xsrc, xoffset);
		}
		while (size-- > 0) {
			dest[offset++] = unsafe.getByteVolatile(null, src++);
		}
	}

	@Override
	public void put(long dest, int size, byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(dest, size, src, offset)) return;
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing buffer ({} B) @ 0x{} with offset {} using the Unsafe object", size, xdest, xoffset);
		}
		unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, dest, size);
	}

	@Override
	public void putVolatile(long dest, int size, byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(dest, size, src, offset)) return;
			size = Math.min(size, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing volatile buffer ({} B) @ 0x{} with offset {} using the Unsafe object", size, xdest, xoffset);
		}
		while (size-- > 0) {
			unsafe.putByteVolatile(null, dest++, src[offset++]);
		}
	}

	@Override
	public void copy(long src, int size, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(src, dest, size)) return;
		}
		unsafe.copyMemory(src, dest, size);
	}

	@Override
	public void copyVolatile(long src, int size, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying volatile memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", size, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (!check(src, dest, size)) return;
		}

		// Change how the loop operates based on the address order
		if (src < dest) {
			src += size - 1L;
			dest += size - 1L;
			while (size-- > 0) {
				val value = unsafe.getByteVolatile(null, src--);
				unsafe.putByteVolatile(null, dest--, value);
			}
		} else {
			while (size-- > 0) {
				val value = unsafe.getByteVolatile(null, src++);
				unsafe.putByteVolatile(null, dest++, value);
			}
		}
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	/** @deprecated  */
	@Override
	@Deprecated
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		throw new UnsupportedUnsafeOperationException();
	}

	/** @deprecated  */
	@Override
	@Deprecated
	public IxyDmaMemory dmaAllocate(long size, AllocationType allocationType, LayoutType layoutType) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DmaMemory using the Unsafe object");
		val virt = allocate(size, allocationType, layoutType);
		val phys = virt2phys(virt);
		return DmaMemory.of(virt, phys);
	}

	/** @deprecated  */
	@Override
	@Deprecated
	public long virt2phys(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Translating virtual address 0x{} using the Unsafe object", xaddress);
		}
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		throw new UnsupportedUnsafeOperationException();
	}

}
