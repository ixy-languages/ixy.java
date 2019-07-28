package de.tum.in.net.ixy.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.TreeSet;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.BuildConfig.LOG_INFO;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;

/**
 * Native utils to load native libraries that are packed inside the JAR itself.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@NoArgsConstructor
@SuppressWarnings("ConstantConditions")
public final class Native {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The set of loaded libraries. */
	private static final @NotNull Set<Path> loaded = new TreeSet<>();

	/** Temporary directory which will contain the DLLs/SOs. */
	private static @Nullable File tempdir = null;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Loads a native library from inside the JAR.
	 *
	 * @param name The name of the library without any prefix or OS-dependent extension.
	 * @param path The path inside the JAR to the library.
	 */
	@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
	@SuppressWarnings({"PMD.AvoidCatchingNPE", "PMD.DataflowAnomalyAnalysis"})
	public static void loadLibrary(final @NotNull String name, @NotNull String path) {
		// Load the library using the classic method
		if (DEBUG >= LOG_INFO) log.info("Loading native library '{}'.", name);

		// Construct the full path
		if (!path.startsWith(File.separator)) path = File.separator + path;
		val filename = System.mapLibraryName(name);
		val fullpath = Paths.get(path, filename);
		if (DEBUG >= LOG_TRACE) log.trace("Computed library path: '{}'.", fullpath);

		// Stop if the library has already been loaded
		if (loaded.contains(fullpath)) return;

		// Prepare temporary file
		if (tempdir == null) {
			try {
				tempdir = createTempDirectory("ixy-");
				tempdir.deleteOnExit();
			} catch (final IOException e) {
				if (DEBUG >= LOG_ERROR) log.error("Could NOT create temporal directory.", e);
				return;
			}
		}

		// Build the path to the file inside the temporal directory
		val temp = Paths.get(tempdir.getAbsolutePath(), filename);

		// Copy the file from inside the JAR
		if (DEBUG >= LOG_TRACE) log.trace("Extracting resource from JAR: '{}' => '{}'.", fullpath, temp);
		try (val is = Native.class.getResourceAsStream(fullpath.toString())) {
			Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException e) {
			if (DEBUG >= LOG_ERROR) log.error("Could NOT copy to temporal directory.", e);
			try {
				Files.deleteIfExists(temp);
			} catch (final IOException e2) {
				if (DEBUG >= LOG_ERROR) log.error("Could NOT delete temporal directory.", e2);
			}
			return;
		} catch (final NullPointerException e) {
			if (DEBUG >= LOG_ERROR) log.error("File does NOT exist inside JAR.", e);
			try {
				Files.deleteIfExists(temp);
			} catch (final IOException e2) {
				if (DEBUG >= LOG_ERROR) log.error("Could NOT delete temporal directory.", e2);
			}
			return;
		}

		// Load the library using the absolute path
		try {
			System.load(temp.toString());
			loaded.add(fullpath);
		} finally {
			if (isPosixCompliant()) {
				try {
					Files.deleteIfExists(temp);
				} catch (final IOException e) {
					if (DEBUG >= LOG_ERROR) log.error("Could NOT delete temporal directory.", e);
				}
			} else {
				temp.toFile().deleteOnExit();
			}
		}
	}

	/**
	 * Returns whether the file system is POSIX compliant.
	 *
	 * @return The file system POSIX compatibility.
	 */
	@Contract(pure = true)
	private static boolean isPosixCompliant() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking POSIX compliance.");
		try {
			return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
		} catch (final FileSystemNotFoundException | ProviderNotFoundException | SecurityException e) {
			return false;
		}
	}

	/**
	 * Creates a temporal directory.
	 *
	 * @param prefix The prefix of the directory, which needs to be 3 bytes long at least.
	 * @return The file pointing to the temporal directory.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	private static @NotNull File createTempDirectory(@NotNull String prefix) throws IOException {
		if (DEBUG >= LOG_TRACE) log.trace("Creating temporal directory with prefix '{}'.", prefix);
		val tempdir = System.getProperty("java.io.tmpdir");
		val dir = new File(tempdir, prefix + System.nanoTime());
		if (!dir.mkdir()) throw new IOException("Failed to create temp directory '" + dir.getAbsolutePath() + "'.");
		return dir;
	}

}
