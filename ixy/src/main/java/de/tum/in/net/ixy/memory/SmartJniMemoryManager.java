package de.tum.in.net.ixy.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.File;
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
 * Implementation of a memory manager backed up by {@code native} methods.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"ConstantConditions", "Duplicates", "PMD.BeanMembersShouldSerialize"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
public final class SmartJniMemoryManager extends JniMemoryManager {

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
	private static final MemoryManager singleton = new SmartJniMemoryManager();

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** A cached copy of the output of {@link #getPageSize()}. */
	private long pageSize;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Private constructor that does nothing. */
	private SmartJniMemoryManager() {
		if (DEBUG >= LOG_TRACE) log.trace("Created a smart JNI-backed memory manager.");
		pageSize = getPageSize();
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	public long getPageSize() {
		return pageSize = super.getPageSize();
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public long getHugepageSize() {
		// Trace message
		if (DEBUG >= LOG_TRACE) log.trace("Checking the huge page size.");

		// If no entry exists with the given parameters, then hugepages are definitely not supported
		if (!existsInMtab(MTAB_PATH, "hugetlbfs", DEFAULT_HUGEPAGE_PATH, "hugetlbfs")) return HUGE_PAGE_NOT_SUPPORTED;

		// Read the /proc/meminfo file to get the size of a hugepage
		if (DEBUG >= LOG_DEBUG) log.debug("Parsing file '{}'.", MEMINFO_PATH);
		try (val meminfo = bufferedReader(new File(MEMINFO_PATH))) {
			val pattern = Pattern.compile("Hugepagesize:[\\t\\s ]+(\\d+)[\\t\\s ]+([kMG]?B)");
			for (var line = meminfo.readLine(); line != null; line = meminfo.readLine()) {
				val matcher = pattern.matcher(line);
				if (matcher.find()) {
					return applyFactor(Long.parseLong(matcher.group(1)), matcher.group(2));
				}
			}
		} catch (final FileNotFoundException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be found.", MEMINFO_PATH, e);
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("The file '{}' cannot be read or closed.", MEMINFO_PATH, e);
		}
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	@Contract(pure = true)
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
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
