package de.tum.in.net.ixy.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import sun.misc.Unsafe; //NOPMD

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Implementation of a memory manager backed up by {@link Unsafe the unsafe object}.
 * <p>
 * The following operations will always throw an {@link UnsupportedOperationException} (<em>iff</em> {@link #isValid()}
 * returns {@code true}):
 * <ul>
 *     <li>Huge memory page size computation.</li>
 *     <li>Huge page allocation.</li>
 *     <li>Memory locking.</li>
 *     <li>DMA memory allocation.</li>
 *     <li>Virtual address translation.</li>
 *     <li>Memory mapping.</li>
 *     <li>Memory unmapping.</li>
 * </ul>
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({
		"ConstantConditions", "Duplicates",
		"PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize", "PMD.DontImportSun"
})
class UnsafeMemoryManager implements MemoryManager {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/**
	 * A cached instance of this class.
	 * -- GETTER --
	 * Returns a singleton instance.
	 *
	 * @return The singleton instance.
	 */
	@Getter
	@SuppressWarnings("JavaDoc")
	private static final UnsafeMemoryManager singleton = new UnsafeMemoryManager();

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** {@link Unsafe The unsafe object} used to access and manipulate the memory. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "unsafe", rank = 1)
	final @Nullable Unsafe unsafe;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Package-private constructor that sets the {@link #unsafe} field. */
	UnsafeMemoryManager() {
		Unsafe tmp = null;
		try {
			if (DEBUG >= LOG_TRACE) log.trace("Getting declared field 'theUnsafe' from 'sun.misc.Unsafe'.");
			val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			if (DEBUG >= LOG_TRACE) log.trace("Making declared field accessible.");
			theUnsafeField.setAccessible(true);
			try {
				if (DEBUG >= LOG_TRACE) log.trace("Getting the 'sun.misc.Unsafe' instance from the declared field.");
				tmp = (Unsafe) theUnsafeField.get(null);
			} catch (final IllegalArgumentException | IllegalAccessException e) {
				if (DEBUG >= LOG_ERROR) log.error("Error getting field value.", e);
			} finally {
				if (DEBUG >= LOG_ERROR) log.trace("Making declared field inaccessible.");
				theUnsafeField.setAccessible(false);
			}
		} catch (final NoSuchFieldException | SecurityException e) {
			if (DEBUG >= LOG_ERROR) log.error("Error getting declared field.", e);
		} finally {
			unsafe = tmp;
		}
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = " -> fail", pure = true)
	public long getHugepageSize() {
		if (!OPTIMIZED && unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
		throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_, _, _ -> fail", pure = true)
	public long mmap(final @NotNull File file, final boolean huge, final boolean lock) throws IOException {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (file == null) throw new IllegalArgumentException("The parameter 'file' MUST NOT be null.");
			if (!file.exists()) throw new IllegalArgumentException("The parameter 'file' MUST exist.");
			if (!file.canRead()) throw new IllegalArgumentException("The parameter 'file' MUST be readable.");
			if (!file.canWrite()) throw new IllegalArgumentException("The parameter 'file' MUST be writable.");
		}
		throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_, _, _, _ -> fail")
	public void munmap(final long address, final @NotNull File file, final boolean huge, final boolean lock)
			throws IOException {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (file == null) throw new IllegalArgumentException("The parameter 'file' MUST NOT be null.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
			if (!file.exists()) throw new IllegalArgumentException("The parameter 'file' MUST exist.");
			if (!file.canRead()) throw new IllegalArgumentException("The parameter 'file' MUST be readable.");
			if (!file.canWrite()) throw new IllegalArgumentException("The parameter 'file' MUST be writable.");
		}
		throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_ -> fail", pure = true)
	public long virt2phys(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_, _, _ -> fail", pure = true)
	public @NotNull DmaMemory dmaAllocate(final long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST BE positive.");
		}
		throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public boolean isValid() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking if the Unsafe object is available.");
		return unsafe != null;
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getAddressSize() {
		if (!OPTIMIZED && unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
		if (DEBUG >= LOG_TRACE) log.trace("Checking the address size.");
		return unsafe.addressSize();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getPageSize() {
		if (!OPTIMIZED && unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
		if (DEBUG >= LOG_TRACE) log.trace("Checking the page size.");
		return unsafe.pageSize();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(value = "_, true, _ -> fail; _, _, true -> fail", pure = true)
	public long allocate(final long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
		}
		if (huge) throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
		if (lock) throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
		if (DEBUG >= LOG_TRACE) log.trace("Allocating {} non-hugepage-based bytes.", bytes);
		return unsafe.allocateMemory(bytes);
	}

	/** {@inheritDoc} */
	@Override
	@Contract("_, _, true, _ -> fail; _, _, _, true -> fail")
	public void free(final long address, final long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
		}
		if (huge) throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
		if (lock) throw new UnsupportedOperationException("The Unsafe object does not implement this operation.");
		if (DEBUG >= LOG_TRACE) log.trace("Freeing {} non-hugepage-based bytes @ 0x{}.", bytes, leftPad(address));
		unsafe.freeMemory(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public byte getByte(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading byte @ 0x{}.", leftPad(address));
		return unsafe.getByte(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public short getShort(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading short @ 0x{}.", leftPad(address));
		return unsafe.getShort(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getInt(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading int @ 0x{}.", leftPad(address));
		return unsafe.getInt(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getLong(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading long @ 0x{}.", leftPad(address));
		return unsafe.getLong(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public byte getByteVolatile(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile byte @ 0x{}.", leftPad(address));
		return unsafe.getByteVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public short getShortVolatile(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile short @ 0x{}.", leftPad(address));
		return unsafe.getShortVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getIntVolatile(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile int @ 0x{}.", leftPad(address));
		return unsafe.getIntVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getLongVolatile(final long address) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile long @ 0x{}.", leftPad(address));
		return unsafe.getLongVolatile(null, address);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long address, final byte value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing byte 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putByte(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long address, final short value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing short 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putShort(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long address, final int value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing int 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putInt(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long address, final long value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing int 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putLong(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long address, final byte value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile byte 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putByteVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long address, final short value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile short 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putShortVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long address, final int value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile int 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putIntVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long address, final long value) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile long 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		unsafe.putLongVolatile(null, address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void get(final long src, int bytes, final @NotNull byte[] dest, final int offset) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (src == 0) throw new IllegalArgumentException("The parameter 'src' MUST NOT be 0.");
			if (bytes < 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
			if (dest == null) throw new NullPointerException("The parameter 'dest' MUST NOT be null.");
			if (offset < 0 || offset >= dest.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be inside [0, dest.length).");
			}
			val diff = dest.length - offset;
			if (diff < bytes) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to write more bytes than the buffer can hold. Adapting bytes.");
				}
				bytes = diff;
			}
			if (bytes == 0) return;
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Copying memory region @ 0x{} + {} ({} bytes).", leftPad(src), offset, bytes);
		}
		unsafe.copyMemory(null, src, dest, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, bytes);
	}

	/** {@inheritDoc} */
	@Override
	public void put(final long dest, int bytes, final @NotNull byte[] src, final int offset) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (dest == 0) throw new IllegalArgumentException("The parameter 'dest' MUST NOT be 0.");
			if (bytes < 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
			if (src == null) throw new NullPointerException("The parameter 'src' MUST NOT be null.");
			if (offset < 0 || offset >= src.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be inside [0, src.length).");
			}
			val diff = src.length - offset;
			if (diff < bytes) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to write more bytes than the buffer can hold. Adapting bytes.");
				}
				bytes = diff;
			}
			if (bytes == 0) return;
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Copying memory region @ 0x{} + {} ({} bytes).", leftPad(dest), offset, bytes);
		}
		unsafe.copyMemory(src, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, null, dest, bytes);
	}

}
