package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.utils.Native;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.DEFAULT_HUGEPAGE_PATH;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Implementation of a memory manager backed up by {@code native} methods.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"ConstantConditions", "Duplicates", "PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
public class JniMemoryManager implements MemoryManager {

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
	private static final JniMemoryManager singleton = new JniMemoryManager();

	////////////////////////////////////////////////// NATIVE METHODS //////////////////////////////////////////////////

	/**
	 * Returns whether the library is loaded and supported by the operative system.
	 *
	 * @return The support status.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native boolean c_is_valid();

	/**
	 * Returns the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 *
	 * @return The size of a memory address.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native int c_address_size();

	/**
	 * Returns the size of a memory page.
	 *
	 * @return The size of a memory page.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native int c_page_size();

	/**
	 * Returns the size of a huge memory page.
	 * <p>
	 * When the OS does not support huge memory pages, {@code -1} is returned.
	 * When there is a problem computing the huge memory page size, {@code 0} is returned.
	 *
	 * @return The size of a huge memory page.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_hugepage_size();

	/**
	 * Allocates raw bytes from the heap.
	 * <p>
	 * When the parameter {@code huge} is set to {@code true}, normal memory allocation will take place, usually
	 * implemented with the C library function {@code malloc(size_t)}, and the parameter {@code mnt} will be ignored.
	 *
	 * @param bytes The number of bytes.
	 * @param huge  Whether to enable huge memory pages.
	 * @param lock  Whether to enable memory locking.
	 * @param mnt   The {@code hugetlbfs} mount point.
	 * @return The memory region's base address.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_allocate(long bytes, boolean huge, boolean lock, @Nullable String mnt);

	/**
	 * Frees a previously allocated memory region.
	 * <p>
	 * When the parameter {@code huge} is set to {@code true}, normal memory freeing will take place, usually
	 * implemented with the C library function {@code free(void *)}, and the parameter {@code bytes} will be ignored.
	 *
	 * @param address The base address of the memory region.
	 * @param bytes   The size of the memory region.
	 * @param huge    Whether huge memory pages was used.
	 * @param lock    Whether memory locking was used.
	 * @return Whether the operation succeeded.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native boolean c_free(long address, long bytes, boolean huge, boolean lock);

	/**
	 * Maps a file to memory.
	 *
	 * @param fd   The file descriptor.
	 * @param size The size of the file.
	 * @param huge Whether to enable huge memory pages.
	 * @param lock Whether to enable memory locking.
	 * @return The virtual address of the mapped region.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_mmap(@NotNull FileDescriptor fd, long size, boolean huge, boolean lock);

	/**
	 * Destroys a memory mapping.
	 *
	 * @param address The virtual address of the mapped region.
	 * @param size    The size of the mapped region.
	 * @param huge    Whether huge memory pages were used.
	 * @param lock    Whether memory locking was used.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_munmap(long address, long size, boolean huge, boolean lock);

	/**
	 * Reads a {@code byte} from an arbitrary virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native byte c_get_byte(long src);

	/**
	 * Reads a {@code short} from an arbitrary virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native short c_get_short(long src);

	/**
	 * Reads a {@code int} from an arbitrary virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native int c_get_int(long src);

	/**
	 * Reads a {@code long} from an arbitrary virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_get_long(long src);

	/**
	 * Reads a {@code byte} from an arbitrary volatile virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native byte c_get_byte_volatile(long src);

	/**
	 * Reads a {@code short} from an arbitrary volatile virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native short c_get_short_volatile(long src);

	/**
	 * Reads a {@code int} from an arbitrary volatile virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native int c_get_int_volatile(long src);

	/**
	 * Reads a {@code long} from an arbitrary volatile virtual address.
	 *
	 * @param src The virtual address.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_get_long_volatile(long src);

	/**
	 * Writes a {@code byte} to an arbitrary virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code byte} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_byte(long dest, byte value);

	/**
	 * Writes a {@code short} to an arbitrary virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code short} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_short(long dest, short value);

	/**
	 * Writes an {@code int} to an arbitrary virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code int} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_int(long dest, int value);

	/**
	 * Writes a {@code long} to an arbitrary virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code long} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_long(long dest, long value);

	/**
	 * Writes a {@code byte} to an arbitrary volatile virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code byte} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_byte_volatile(long dest, byte value);

	/**
	 * Writes a {@code short} to an arbitrary volatile virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code short} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_short_volatile(long dest, short value);

	/**
	 * Writes an {@code int} to an arbitrary volatile virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code int} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_int_volatile(long dest, int value);

	/**
	 * Writes a {@code long} to an arbitrary volatile virtual address.
	 *
	 * @param dest  The virtual address.
	 * @param value The {@code long} to write.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put_long_volatile(long dest, long value);

	/**
	 * Reads the data from an arbitrary memory region into a {@code byte[]}.
	 *
	 * @param src    The virtual address.
	 * @param bytes  The number of bytes.
	 * @param dest   The data.
	 * @param offset The offset of {@code dest}.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_get(long src, int bytes, @NotNull byte[] dest, int offset);

	/**
	 * Writes the data from a {@code byte[]} into an arbitrary memory region.
	 *
	 * @param dest   The virtual address.
	 * @param bytes  The number of bytes.
	 * @param src    The data.
	 * @param offset The offset of {@code src}.
	 */
	@SuppressWarnings("checkstyle:MethodName")
	private static native void c_put(long dest, int bytes, @NotNull byte[] src, int offset);

	/**
	 * Translates a virtual address to a physical address.
	 *
	 * @param address The virtual address.
	 * @return The physical address.
	 */
	@Contract(pure = true)
	@SuppressWarnings("checkstyle:MethodName")
	private static native long c_virt2phys(long address);

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** A cached copy of the output of {@link #getHugepageSize()}. */
	private long hugepageSize;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Package-private constructor that sets the field {@link #hugepageSize}. */
	JniMemoryManager() {
		if (DEBUG >= LOG_TRACE) log.trace("Creating a JNI-backed memory manager.");
		Native.loadLibrary("ixy", "resources");
		try {
			c_is_valid();
		} catch (final UnsatisfiedLinkError e) {
			System.loadLibrary("ixy");
		} finally {
			hugepageSize = c_hugepage_size();
		}
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public boolean isValid() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking if the native library is loaded.");
		try {
			return c_is_valid();
		} catch (final UnsatisfiedLinkError e) {
			return false;
		}
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getAddressSize() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking the address size.");
		return c_address_size();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getPageSize() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking the page size.");
		return c_page_size();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getHugepageSize() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking the huge page size.");
		return hugepageSize = c_hugepage_size();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long allocate(long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED && bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");

		// Adapt the parameters if we are using huge memory pages
		if (huge) {
			// If no huge memory page file support has been detected, exit right away
			if (hugepageSize <= 0) {
				if (DEBUG >= LOG_TRACE) log.trace("Allocating {} hugepage-based bytes.", bytes);
				return 0;
			}

			// Round the size to a multiple of the page size
			if (DEBUG >= LOG_TRACE) {
				val bytesCopy = bytes;
				val mask = (hugepageSize - 1);
				bytes = (bytes & mask) == 0 ? bytes : (bytes + hugepageSize) & ~mask;
				log.trace("Allocating {} hugepage-based bytes (originally {} bytes).", bytes, bytesCopy);
			} else {
				val mask = (hugepageSize - 1);
				bytes = (bytes & mask) == 0 ? bytes : (bytes + hugepageSize) & ~mask;
			}
		} else if (DEBUG >= LOG_TRACE) {
			log.trace("Allocating {} bytes.", bytes);
		}

		// Call the C implementation
		return c_allocate(bytes, huge, lock, DEFAULT_HUGEPAGE_PATH);
	}

	/** {@inheritDoc} */
	@Override
	public void free(long address, long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
		}

		// Adapt the parameters if we are using hugepages
		if (huge) {
			// If no huge memory page file support has been detected, exit right away
			if (hugepageSize <= 0) {
				if (DEBUG >= LOG_TRACE) log.trace("Freeing {} hugepage-based bytes @ 0x{}.", bytes, leftPad(address));
				return;
			}

			// Round the size and address to a multiple of the page size
			if (DEBUG >= LOG_TRACE) {
				val xaddressCopy = leftPad(address);
				val bytesCopy = bytes;
				val mask = (hugepageSize - 1);
				address = (address & mask) == 0 ? address : address & ~mask;
				bytes = (bytes & mask) == 0 ? bytes : (bytes + hugepageSize) & ~mask;
				log.trace("Freeing {} hugepage-based bytes @ 0x{} (originally {} bytes @ 0x{}).", bytes,
						leftPad(address), bytesCopy, xaddressCopy);
			} else {
				val mask = (hugepageSize - 1);
				address = (address & mask) == 0 ? address : address & ~mask;
				bytes = (bytes & mask) == 0 ? bytes : (bytes + hugepageSize) & ~mask;
			}
		}

		// Call the C implementation
		c_free(address, bytes, huge, lock);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long mmap(final @NotNull File file, final boolean huge, final boolean lock) throws IOException {
		if (!OPTIMIZED) {
			if (file == null) throw new NullPointerException("The parameter 'file' MUST NOT be null.");
			if (!file.exists()) throw new FileNotFoundException("The parameter 'file' MUST exist.");
			if (!file.canRead()) throw new IllegalArgumentException("The parameter 'file' MUST be readable.");
			if (!file.canWrite()) throw new IllegalArgumentException("The parameter 'file' MUST be writable.");
		}

		// Trace message
		if (DEBUG >= LOG_TRACE) log.trace("Mapping file: {}", file.getAbsolutePath());

		// Create a RandomAccessFile that allows us to get the file descriptor and size
		val randomAccessFile = new RandomAccessFile(file, "rwd");
		val fd = randomAccessFile.getFD();

		// Call the C implementation
		return c_mmap(fd, randomAccessFile.length(), huge, lock);
	}

	/** {@inheritDoc} */
	@Override
	public void munmap(final long address, final @NotNull File file, final boolean huge, final boolean lock)
			throws IOException {
		if (!OPTIMIZED) {
			if (address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
			if (file == null) throw new NullPointerException("The parameter 'file' MUST NOT be null.");
			if (!file.exists()) throw new FileNotFoundException("The parameter 'file' MUST exist.");
			if (!file.canRead()) throw new IllegalArgumentException("The parameter 'file' MUST be readable.");
			if (!file.canWrite()) throw new IllegalArgumentException("The parameter 'file' MUST be writable.");
		}

		// Trace message
		if (DEBUG >= LOG_TRACE) log.trace("Destroying file mapping: {}", file.getAbsolutePath());

		// Call the C implementation
		c_munmap(address, file.length(), huge, lock);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public byte getByte(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading byte @ 0x{}.", leftPad(address));
		return c_get_byte(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public short getShort(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading short @ 0x{}.", leftPad(address));
		return c_get_short(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getInt(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading int @ 0x{}.", leftPad(address));
		return c_get_int(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getLong(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading long @ 0x{}.", leftPad(address));
		return c_get_long(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public byte getByteVolatile(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile byte @ 0x{}.", leftPad(address));
		return c_get_byte_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public short getShortVolatile(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile short @ 0x{}.", leftPad(address));
		return c_get_short_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public int getIntVolatile(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile int @ 0x{}.", leftPad(address));
		return c_get_int_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getLongVolatile(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Reading volatile long @ 0x{}.", leftPad(address));
		return c_get_long_volatile(address);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long address, final byte value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing byte 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_byte(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long address, final short value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing short 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_short(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long address, final int value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing int 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_int(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long address, final long value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing long 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_long(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long address, final byte value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile byte 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_byte_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long address, final short value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile short 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_short_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long address, final int value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile int 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_int_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long address, final long value) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Writing volatile long 0x{} @ 0x{}.", leftPad(value), leftPad(address));
		c_put_long_volatile(address, value);
	}

	/** {@inheritDoc} */
	@Override
	public void get(final long src, int bytes, final @NotNull byte[] dest, final int offset) {
		if (!OPTIMIZED) {
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
		c_get(src, bytes, dest, offset);
	}

	/** {@inheritDoc} */
	@Override
	public void put(final long dest, int bytes, final @NotNull byte[] src, final int offset) {
		if (!OPTIMIZED) {
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
		c_put(dest, bytes, src, offset);
	}

	@Override
	@Contract(pure = true)
	public long virt2phys(final long address) {
		if (!OPTIMIZED && address == 0) throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		if (DEBUG >= LOG_TRACE) log.trace("Translating virtual address 0x{} to physical address.", leftPad(address));
		return c_virt2phys(address);
	}

}
