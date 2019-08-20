package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.utils.Native;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.DEFAULT_HUGEPAGE_PATH;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

import static java.io.File.separator;

/**
 * Implementation of a <em>smart</em> memory manager backed up by {@link sun.misc.Unsafe the unsafe object}.
 * <p>
 * The following operations will always throw an {@link UnsupportedOperationException} (<em>iff</em> {@link #isValid()}
 * returns {@code true}):
 * <ul>
 *     <li>Huge page allocation.</li>
 *     <li>Memory locking.</li>
 *     <li>Memory mapping.</li>
 *     <li>Memory unmapping.</li>
 * </ul>
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
@SuppressWarnings({"ConstantConditions", "Duplicates", "PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
public final class SmartUnsafeMemoryManager extends UnsafeMemoryManager {

	//////////////////////////////////////////////////// FILE PATHS ////////////////////////////////////////////////////

	/** The path to the mounted (filesystem) table. */
	private static final @NotNull String MTAB_PATH = separator + String.join(separator, "etc", "mtab");

	/** The path to the memory info file. */
	private static final @NotNull String MEMINFO_PATH = separator + String.join(separator, "proc", "meminfo");

	/** The page map file. */
	private static final @NotNull String PAGEMAP_PATH = separator + String.join(separator, "proc", "self", "pagemap");

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The factor of 2^10 used for {K,M,G,T}iB units. */
	private static final int K_FACTOR = 1024;

	/**
	 * A cached instance of this class.
	 * -- GETTER --
	 * Returns a singleton instance.
	 *
	 * @return The singleton instance.
	 */
	@Getter
	@SuppressWarnings("JavaDoc")
	private static final MemoryManager singleton = new SmartUnsafeMemoryManager();

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** A cached copy of the output of {@link #getPageSize()}. */
	private long pageSize;

	/** A cached copy of the output of {@link #getHugepageSize()}. */
	private long hugepageSize;

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

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Private constructor that sets the fields {@link #pageSize} and {@link #hugepageSize}. */
	private SmartUnsafeMemoryManager() {
		log.trace("Created a smart Unsafe-based memory manager.");
		pageSize = super.getPageSize();
		hugepageSize = getHugepageSize();
		Native.loadLibrary("ixy", "resources");
		if (!isValid()) {
			try {
				System.loadLibrary("ixy");
			} catch (final UnsatisfiedLinkError e) {
//				e.printStackTrace();
			}
		}
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public boolean isValid() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking if the native library is loaded.");
		try {
			return super.isValid() && c_is_valid();
		} catch (final UnsatisfiedLinkError e) {
			return false;
		}
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getPageSize() {
		return pageSize = super.getPageSize();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(value = " -> fail", pure = true)
	@SuppressWarnings({"deprecation", "PMD.DataflowAnomalyAnalysis"})
	public long getHugepageSize() {
		if (!OPTIMIZED && unsafe == null) throw new NullPointerException("The Unsafe object is not available.");

		// Trace message
		if (DEBUG >= LOG_TRACE) log.trace("Checking the huge page size.");

		// If no entry exists with the given parameters, then hugepages are definitely not supported
		if (!existsInMtab(MTAB_PATH, "hugetlbfs", DEFAULT_HUGEPAGE_PATH, "hugetlbfs")) {
			return hugepageSize = HUGE_PAGE_NOT_SUPPORTED;
		}

		// Read the /proc/meminfo file to get the size of a hugepage
		if (DEBUG >= LOG_DEBUG) log.debug("Parsing file '{}'.", MEMINFO_PATH);
		try (val meminfo = bufferedReader(new File(MEMINFO_PATH))) {
			val pattern = Pattern.compile("Hugepagesize:[\\t\\s ]+(\\d+)[\\t\\s ]+([kMG]?B)");
			for (var line = meminfo.readLine(); line != null; line = meminfo.readLine()) {
				val matcher = pattern.matcher(line);
				if (matcher.find()) {
					return hugepageSize = applyFactor(Long.parseLong(matcher.group(1)), matcher.group(2));
				}
			}
		} catch (final FileNotFoundException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be found.", MEMINFO_PATH, e);
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be read or closed.", MEMINFO_PATH, e);
		}
		return hugepageSize = 0;
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	@SuppressWarnings("BooleanParameter")
	public long allocate(long bytes, boolean huge, boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
		}

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
		} else {
			if (DEBUG >= LOG_TRACE) {
				log.trace("Allocating {} bytes.", bytes);
			}
			if (!lock) return unsafe.allocateMemory(bytes);
		}

		// Call the C implementation
		try {
			return c_allocate(bytes, huge, lock, DEFAULT_HUGEPAGE_PATH);
		} catch (final UnsatisfiedLinkError e) {
			return 0;
		}
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("BooleanParameter")
	public void free(long address, long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
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
		} else {
			unsafe.freeMemory(address);
			return;
		}

		// Call the C implementation
		c_free(address, bytes, huge, lock);
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	@SuppressWarnings({"deprecation", "BooleanParameter", "IOResourceOpenedButNotSafelyClosed", "resource"})
	public long mmap(final @NotNull File file, final boolean huge, final boolean lock)
			throws FileNotFoundException, IOException {
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
	@SuppressWarnings({"deprecation", "BooleanParameter", "PMD.DataflowAnomalyAnalysis"})
	public void munmap(final long address, final @NotNull File file, final boolean huge, final boolean lock)
			throws FileNotFoundException, IOException {
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
	@SuppressWarnings("deprecation")
	@SuppressFBWarnings("")
	public long virt2phys(final long address) {
		if (!OPTIMIZED && address == 0) {
			throw new IllegalArgumentException("The parameter 'address' MUST NOT be 0.");
		}

		// If we are on a non-Linux OS this won't work
		if (!System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) {
			if (DEBUG >= LOG_WARN) log.warn("Cannot translate virtual addresses in a non-Linux OS.");
			return 0;
		}

		// Trace message
		if (DEBUG >= LOG_TRACE) log.trace("Translating virtual address 0x{} to physical address.", leftPad(address));
		if (DEBUG >= LOG_DEBUG) log.debug("Parsing file '{}'.", PAGEMAP_PATH);

		try (
				val pagemap = new RandomAccessFile(PAGEMAP_PATH, "r");
				val channel = pagemap.getChannel().position(Long.divideUnsigned(address, pageSize) * Long.BYTES)
		) {
			val buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.nativeOrder());
			channel.read(buffer);
			val mask = pageSize - 1;
			val offset = address & mask;
			val phys = buffer.flip().getLong();
			return phys * pageSize + offset;
		} catch (final FileNotFoundException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be found.", PAGEMAP_PATH, e);
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) {
				log.error("The file '{}' cannot be read, closed or we read past its size.", PAGEMAP_PATH, e);
			}
		}
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	@Contract(value = "_, _, _ -> fail", pure = true)
	@SuppressWarnings({"deprecation", "BooleanParameter"})
	public @NotNull DmaMemory dmaAllocate(final long bytes, final boolean huge, final boolean lock) {
		if (!OPTIMIZED) {
			if (unsafe == null) throw new NullPointerException("The Unsafe object is not available.");
			if (bytes <= 0) throw new IllegalArgumentException("The parameter 'bytes' MUST BE positive.");
		}
		val virtual = allocate(bytes, huge, lock);
		if (virtual != 0) putByte(virtual, getByte(virtual));
		val physical = virt2phys(virtual);
		return new DmaMemory(virtual, physical);
	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Checks an entry exists in a mount table file using the the given column values.
	 * <p>
	 * This method assumes the mount table follows the same format as Linux' {@code /etc/mtab} file.
	 *
	 * @param path The mount table path.
	 * @param fs   The file system type.
	 * @param mnt  The mount path.
	 * @param type The mount type.
	 * @return Whether the entry exists.
	 */
	@Contract(pure = true)
	@SuppressWarnings({"HardcodedFileSeparator", "SameParameterValue", "PMD.DataflowAnomalyAnalysis"})
	private static boolean existsInMtab(final @NotNull String path,
										final @NotNull String fs,
										final @NotNull String mnt,
										final @NotNull String type) {
		if (DEBUG >= LOG_DEBUG) log.debug("Parsing file '{}'.", MTAB_PATH);
		try (val mtab = bufferedReader(new File(path))) {
			val qfs = Pattern.quote(fs);
			val qmnt = Pattern.quote(mnt);
			val qtype = Pattern.quote(type);
			val pattern = Pattern.compile(String.format("^%s[\\t\\s ]+%s[\\t\\s ]+%s[\\t\\s ]+.*$", qfs, qmnt, qtype));
			for (var line = mtab.readLine(); line != null; line = mtab.readLine()) {
				if (pattern.matcher(line).matches()) return true;
			}
		} catch (final FileNotFoundException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be found.", MTAB_PATH, e);
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be read or closed.", MTAB_PATH, e);
		}
		return false;
	}

	/**
	 * Creates a {@link BufferedReader buffered reader} using {@code file} as input.
	 *
	 * @param file The file.
	 * @return A buffered reader.
	 * @throws FileNotFoundException If the parameter {@code file} does not exist.
	 */
	@Contract(pure = true)
	private static @NotNull BufferedReader bufferedReader(final @NotNull File file) throws FileNotFoundException {
		val fis = new FileInputStream(file);
		val ifs = new InputStreamReader(fis, StandardCharsets.UTF_8);
		return new BufferedReader(ifs);
	}

	/**
	 * Applies a factor to a number based on the {@code unit}.
	 * <p>
	 * It only accepts the units {@code GB}, {@code MB}, {@code kB} and {@code B}.
	 *
	 * @param x    The magnitude.
	 * @param unit The units.
	 * @return The magnitude multiplied by the unit.
	 */
	@Contract(pure = true)
	@SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
	@SuppressWarnings("PMD.MissingBreakInSwitch")
	private static long applyFactor(@Range(from = 0, to = Long.MAX_VALUE) long x, final @NotNull String unit) {
		switch (unit) {
			case "GB":
				x *= K_FACTOR; // fall through
			// fall through
			case "MB":
				x *= K_FACTOR; // fall through
			// fall through
			case "kB":
				x *= K_FACTOR; // fall through
				break;
			default:
				x *= 0;
		}
		return x;
	}

}
