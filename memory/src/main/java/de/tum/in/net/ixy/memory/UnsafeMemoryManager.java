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
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import static de.tum.in.net.ixy.memory.Utility.check;

/**
 * A simple implementation of Ixy's memory manager specification using the {@link Unsafe} object.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("ConstantConditions")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class UnsafeMemoryManager implements IxyMemoryManager {

	//////////////////////////////////////////////////// EXCEPTIONS ////////////////////////////////////////////////////

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
	@Setter(AccessLevel.NONE)
	@SuppressWarnings("JavaDoc")
	@Getter(onMethod_ = {@Contract(value = "_ -> !null", pure = true)})
	private static final @NotNull IxyMemoryManager singleton = new UnsafeMemoryManager();

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The unsafe object that will do all the operations. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "unsafe")
	@SuppressWarnings("UseOfSunClasses")
	private @Nullable Unsafe unsafe;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Once-callable private constructor.
	 * <p>
	 * This constructor will check if the member property {@link #singleton} is {@code null} or not.
	 * Because the member property {@link #singleton} is initialized with a new instance of this class, any further
	 * attempts to instantiate it will produce an {@link IllegalStateException} to be thrown.
	 */
	private UnsafeMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating an Unsafe-backed memory manager");
		if (singleton == null) {
			try {
				val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
				theUnsafeField.setAccessible(true);
				unsafe = (Unsafe) theUnsafeField.get(null);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				log.error("Error getting Unsafe object", e);
			}
		} else {
			throw new IllegalStateException("An instance cannot be created twice. Use getSingleton() instead.");
		}
	}

	/**
	 * Throws an {@link IllegalStateException} if the member property {@link #unsafe} is {@code null}.
	 * <p>
	 * This method is only called in conditional blocks using the {@link BuildConfig#OPTIMIZED} member, so when the
	 * library is build for production mode this method is never called as a way of speeding up the whole program.
	 */
	@Contract(pure = true)
	private void checkUnsafe() {
		if (unsafe == null) {
			throw new IllegalStateException("The Unsafe object is not available");
		}
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int addressSize() {
		if (BuildConfig.DEBUG) log.debug("Computing address size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.addressSize();
	}

	@Override
	@Contract(pure = true)
	public long pageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		return unsafe.pageSize();
	}

	@Override
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail", pure = true)
	public long allocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (bytes <= 0L) throw new InvalidSizeException("bytes");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
			if (layoutType == null) throw new InvalidNullParameterException("layoutType");
		}
		// Hugepage-based allocation is not supported
		val type = layoutType == LayoutType.CONTIGUOUS ? "contiguous" : "non-contiguous";
		if (allocationType == AllocationType.HUGE) {
			if (BuildConfig.DEBUG) {
				log.debug("Allocating {} {} hugepage-backed bytes using the Unsafe object", bytes, type);
			}
			throw new UnsupportedUnsafeOperationException();
		}
		// Allocate the memory
		if (BuildConfig.DEBUG) log.debug("Allocating {} {} bytes using the Unsafe object", bytes, type);
		if (BuildConfig.OPTIMIZED) {
			return unsafe.allocateMemory(bytes);
		} else {
			try {
				return unsafe.allocateMemory(bytes);
			} catch (RuntimeException | OutOfMemoryError e) {
				if (BuildConfig.DEBUG) log.error("Could not allocate memory", e);
				return 0L;
			}
		}
	}

	@Override
	@Contract(value = "_, _, null -> fail", pure = true)
	public boolean free(long address, long bytes, @NotNull AllocationType allocationType) {
		// Stop if anything is wrong
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
			if (bytes <= 0L) throw new InvalidSizeException("size");
			if (allocationType == null) throw new InvalidNullParameterException("allocationType");
		}
		// Hugepage-based freeing is not supported
		val xsrc = Long.toHexString(address);
		if (allocationType == AllocationType.HUGE) {
			if (BuildConfig.DEBUG) log.debug("Freeing {} {} hugepage-backed bytes using the Unsafe object", bytes, xsrc);
			throw new UnsupportedUnsafeOperationException();
		}
		// Free the memory
		if (BuildConfig.DEBUG) log.debug("Freeing {} bytes @ 0x{} using the Unsafe object", bytes, xsrc);
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
	@Contract(pure = true)
	public byte getByte(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getByte(address);
	}

	@Override
	@Contract(pure = true)
	public byte getByteVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile byte @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getByteVolatile(null, address);
	}

	@Override
	@Contract(pure = true)
	public void putByte(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putByte(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putByteVolatile(long address, byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile byte 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putByteVolatile(null, address, value);
	}

	@Override
	@Contract(pure = true)
	public short getShort(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getShort(address);
	}

	@Override
	@Contract(pure = true)
	public short getShortVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile short @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getShortVolatile(null, address);
	}

	@Override
	@Contract(pure = true)
	public void putShort(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putShort(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putShortVolatile(long address, short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile short 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putShortVolatile(null, address, value);
	}

	@Override
	@Contract(pure = true)
	public int getInt(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getInt(address);
	}

	@Override
	@Contract(pure = true)
	public int getIntVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile int @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getIntVolatile(null, address);
	}

	@Override
	@Contract(pure = true)
	public void putInt(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putInt(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putIntVolatile(long address, int value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting volatile int 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putIntVolatile(null, address, value);
	}

	@Override
	@Contract(pure = true)
	public long getLong(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getLong(address);
	}

	@Override
	@Contract(pure = true)
	public long getLongVolatile(long address) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(address);
			log.debug("Reading volatile long @ 0x{} using the Unsafe object", xsrc);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		return unsafe.getLongVolatile(null, address);
	}

	@Override
	@Contract(pure = true)
	public void putLong(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Putting long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putLong(address, value);
	}

	@Override
	@Contract(pure = true)
	public void putLongVolatile(long address, long value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xdest = Long.toHexString(address);
			log.debug("Writing volatile long 0x{} @ 0x{} using the Unsafe object", xvalue, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (address == 0L) throw new InvalidMemoryAddressException("address");
		}
		unsafe.putLongVolatile(null, address, value);
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	public void get(long src, int bytes, @NotNull byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(src, bytes, dest, offset)) return;
			bytes = Math.min(bytes, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading memory region ({} B) @ 0x{} with offset {} using the Unsafe object", bytes, xsrc, xoffset);
		}
		unsafe.copyMemory(null, src, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, bytes);
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", mutates = "param3")
	public void getVolatile(long src, int bytes, @NotNull byte[] dest, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(src, bytes, dest, offset)) return;
			bytes = Math.min(bytes, dest.length - offset);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xsrc = Long.toHexString(src);
			log.debug("Reading volatile memory region ({} B) @ 0x{} with offset {} using the Unsafe object", bytes, xsrc, xoffset);
		}
		while (bytes-- > 0) {
			dest[offset++] = unsafe.getByteVolatile(null, src++);
		}
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	public void put(long dest, int bytes, @NotNull byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(dest, bytes, src, offset)) return;
			bytes = Math.min(bytes, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing buffer ({} B) @ 0x{} with offset {} using the Unsafe object", bytes, xdest, xoffset);
		}
		unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, dest, bytes);
	}

	@Override
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	public void putVolatile(long dest, int bytes, @NotNull byte[] src, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(dest, bytes, src, offset)) return;
			bytes = Math.min(bytes, src.length);
		}
		if (BuildConfig.DEBUG) {
			val xoffset = Long.toHexString(offset);
			val xdest = Long.toHexString(dest);
			log.debug("Writing volatile buffer ({} B) @ 0x{} with offset {} using the Unsafe object", bytes, xdest, xoffset);
		}
		while (bytes-- > 0) {
			unsafe.putByteVolatile(null, dest++, src[offset++]);
		}
	}

	@Override
	@Contract(pure = true)
	public void copy(long src, int bytes, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", bytes, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(src, dest, bytes)) return;
		}
		unsafe.copyMemory(src, dest, bytes);
	}

	@Override
	@Contract(pure = true)
	public void copyVolatile(long src, int bytes, long dest) {
		if (BuildConfig.DEBUG) {
			val xsrc = Long.toHexString(src);
			val xdest = Long.toHexString(dest);
			log.debug("Copying volatile memory region ({} B) @ 0x{} to 0x{} using the Unsafe object", bytes, xsrc, xdest);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkUnsafe();
			if (check(src, dest, bytes)) return;
		}

		// Change how the loop operates based on the address order
		if (src < dest) {
			src += bytes - 1L;
			dest += bytes - 1L;
			while (bytes-- > 0) {
				val value = unsafe.getByteVolatile(null, src--);
				unsafe.putByteVolatile(null, dest--, value);
			}
		} else {
			while (bytes-- > 0) {
				val value = unsafe.getByteVolatile(null, src++);
				unsafe.putByteVolatile(null, dest++, value);
			}
		}
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	/** @deprecated  */
	@Override
	@Deprecated
	@Contract(value = " -> fail", pure = true)
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.debug("Computing huge page size using the Unsafe object");
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		throw new UnsupportedUnsafeOperationException();
	}

	/** @deprecated  */
	@Override
	@Deprecated
	@Contract(value = "_, _, _ -> fail", pure = true)
	public @NotNull IxyDmaMemory dmaAllocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		if (!BuildConfig.DEBUG) log.debug("Allocating DmaMemory using the Unsafe object");
		val virt = allocate(bytes, allocationType, layoutType);
		val phys = virt2phys(virt);
		return DmaMemory.of(virt, phys);
	}

	/** @deprecated  */
	@Override
	@Deprecated
	@Contract(value = "_ -> fail", pure = true)
	public long virt2phys(long address) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.debug("Translating virtual address 0x{} using the Unsafe object", xaddress);
		}
		if (!BuildConfig.OPTIMIZED) checkUnsafe();
		throw new UnsupportedUnsafeOperationException();
	}

}
