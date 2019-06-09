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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple implementation of Ixy's memory manager specification using JNI.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"DuplicateStringLiteralInspection", "HardCodedStringLiteral"})
public final class SmartMemoryManager implements IxyMemoryManager {

	/////////////////////////////////////////////////// RETURN CODES ///////////////////////////////////////////////////

	/** The return code used when no huge memory page technology is supported by the CPU. */
	private static final int HUGE_PAGE_NOT_SUPPORTED = -1;

	//////////////////////////////////////////////////// CONSTANTS /////////////////////////////////////////////////////

	/** Space character. */
	private static final char CHAR_SPACE = ' ';

	/** Colon character. */
	private static final char CHAR_COLON = ':';

	/** Kilobyte factor. */
	private static final int KB = 1024;

	/** Megabyte factor. */
	private static final int MB = 1024 * KB;

	/** Gigabyte factor. */
	private static final int GB = 1024 * MB;

	//////////////////////////////////////////////////// FILE PATHS ////////////////////////////////////////////////////

	/** The path to the mounted (filesystem) table. */
	private static final Path MTAB_PATH = Paths.get(File.separator, "etc", "mtab");

	/** The path to the memory info file. */
	private static final Path MEMINFO_PATH = Paths.get(File.separator, "proc", "meminfo");

	/** The page map file. */
	private static final Path PAGEMAP_PATH = Paths.get(File.separator, "proc", "self", "pagemap");

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Singleton instance.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	@SuppressWarnings("JavaDoc")
	private static final IxyMemoryManager singleton = new SmartMemoryManager();

	/**
	 * Creates a buffered reader given a {@code file}.
	 *
	 * @param file The file to read.
	 * @return The buffered reader.
	 * @throws FileNotFoundException If the {@code file} does not exist.
	 */
	private static BufferedReader bufferedReader(File file) throws FileNotFoundException {
		val fileInputStream = new FileInputStream(file);
		val inputFilterStream = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
		return new BufferedReader(inputFilterStream);
	}

	///////////////////////////////////////////////////// MEMBERS //////////////////////////////////////////////////////

	/** The singleton instance of the Unsafe-based memory manager. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "unsafe", rank = 2)
	private final IxyMemoryManager unsafe = UnsafeMemoryManager.getSingleton();

	/** The singleton instance of the JNI-based memory manager. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "jni", rank = 1)
	private final IxyMemoryManager jni = JniMemoryManager.getSingleton();

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/** Private constructor that throws an exception if the instance is already instantiated. */
	private SmartMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating a smart memory manager");
		if (singleton != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getSingleton() instead.");
		}
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public int addressSize() {
		return BuildConfig.UNSAFE ? unsafe.addressSize() : jni.addressSize();
	}

	@Override
	public long pageSize() {
		return BuildConfig.UNSAFE ? unsafe.pageSize() : jni.pageSize();
	}

	@Override
	@SuppressWarnings("AccessOfSystemProperties")
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.trace("Smart hugepage size computation");

		// If we are on a non-Linux OS or forcing the C implementation, call the JNI method
		if (!BuildConfig.UNSAFE || !System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) {
			return jni.hugepageSize();
		}

		// Try parsing the file /etc/mtab
		if (BuildConfig.DEBUG) log.trace("Parsing file {}", MTAB_PATH);
		try (val mtab = bufferedReader(MTAB_PATH.toFile())) {
			var line = mtab.readLine();
			while (line != null) {
				var firstSpace = 0;
				var secondSpace = line.indexOf(CHAR_SPACE, firstSpace);
				var word = line.substring(firstSpace, secondSpace);
				if (Objects.equals(word, "hugetlbfs")) {
					firstSpace = secondSpace + 1;
					secondSpace = line.indexOf(CHAR_SPACE, firstSpace);
					word = line.substring(firstSpace, secondSpace);
					if (Objects.equals(word, BuildConfig.HUGE_MNT)) {
						firstSpace = secondSpace + 1;
						secondSpace = line.indexOf(CHAR_SPACE, firstSpace);
						word = line.substring(firstSpace, secondSpace);
						if (Objects.equals(word, "hugetlbfs")) break;
					}
				}
				line = mtab.readLine();
			}
			if (line == null) return HUGE_PAGE_NOT_SUPPORTED;
		} catch (FileNotFoundException e) {
			log.error("The {} cannot be found", MTAB_PATH, e);
			return HUGE_PAGE_NOT_SUPPORTED;
		} catch (IOException e) {
			log.error("The {} cannot be read or closed", MTAB_PATH, e);
			return HUGE_PAGE_NOT_SUPPORTED;
		}

		// Try parsing the file /proc/meminfo
		if (BuildConfig.DEBUG) log.trace("Parsing file {}", MEMINFO_PATH);
		try (val meminfo = bufferedReader(MEMINFO_PATH.toFile())) {
			for (var line = meminfo.readLine(); line != null; line = meminfo.readLine()) {
				var firstSpace = 0;
				var secondSpace = line.indexOf(CHAR_COLON, firstSpace);
				var word = line.substring(firstSpace, secondSpace);
				if (Objects.equals(word, "Hugepagesize")) {
					line = line.substring(secondSpace + 1).trim();
					secondSpace = line.indexOf(CHAR_SPACE);
					var bytes = Long.parseLong(line.substring(0, secondSpace));
					val units = line.substring(secondSpace + 1).trim();
					switch (units) {
						case "GB":
							bytes *= GB;
							break;
						case "MB":
							bytes *= MB;
							break;
						case "kB":
							bytes *= KB;
							break;
						case "B":
							break;
						default:
							return 0;
					}
					return bytes;
				}
			}
		} catch (FileNotFoundException e) {
			log.error("The {} cannot be found", MEMINFO_PATH, e);
		} catch (IOException e) {
			log.error("The {} cannot be read or closed", MEMINFO_PATH, e);
		}
		return 0;
	}

	@Override
	public long allocate(long size, AllocationType allocationType, LayoutType layoutType) {
		if (allocationType == AllocationType.HUGE) {
			return jni.allocate(size, AllocationType.HUGE, layoutType);
		} else {
			return BuildConfig.UNSAFE
					? unsafe.allocate(size, allocationType, layoutType)
					: jni.allocate(size, allocationType, layoutType);
		}
	}

	@Override
	public IxyDmaMemory dmaAllocate(long size, AllocationType allocationType, LayoutType layoutType) {
		return jni.dmaAllocate(size, allocationType, layoutType);
	}

	@Override
	public boolean free(long address, long size, AllocationType allocationType) {
		if (allocationType == AllocationType.HUGE) {
			return jni.free(address, size, AllocationType.HUGE);
		} else {
			return BuildConfig.UNSAFE
					? unsafe.free(address, size, allocationType)
					: jni.free(address, size, allocationType);
		}
	}

	@Override
	public byte getByte(long address) {
		return BuildConfig.UNSAFE ? unsafe.getByte(address) : jni.getByte(address);
	}

	@Override
	public byte getByteVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getByteVolatile(address) : jni.getByteVolatile(address);
	}

	@Override
	public void putByte(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByte(address, value);
		} else {
			jni.putByte(address, value);
		}
	}

	@Override
	public void putByteVolatile(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByteVolatile(address, value);
		} else {
			jni.putByteVolatile(address, value);
		}
	}

	@Override
	public short getShort(long address) {
		return BuildConfig.UNSAFE ? unsafe.getShort(address) : jni.getShort(address);
	}

	@Override
	public short getShortVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getShortVolatile(address) : jni.getShortVolatile(address);
	}

	@Override
	public void putShort(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShort(address, value);
		} else {
			jni.putShort(address, value);
		}
	}

	@Override
	public void putShortVolatile(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShortVolatile(address, value);
		} else {
			jni.putShortVolatile(address, value);
		}
	}

	@Override
	public int getInt(long address) {
		return BuildConfig.UNSAFE ? unsafe.getInt(address) : jni.getInt(address);
	}

	@Override
	public int getIntVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getIntVolatile(address) : jni.getIntVolatile(address);
	}

	@Override
	public void putInt(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putInt(address, value);
		} else {
			jni.putInt(address, value);
		}
	}

	@Override
	public void putIntVolatile(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putIntVolatile(address, value);
		} else {
			jni.putIntVolatile(address, value);
		}
	}

	@Override
	public long getLong(long address) {
		return BuildConfig.UNSAFE ? unsafe.getLong(address) : jni.getLong(address);
	}

	@Override
	public long getLongVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getLongVolatile(address) : jni.getLongVolatile(address);
	}

	@Override
	public void putLong(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLong(address, value);
		} else {
			jni.putLong(address, value);
		}
	}

	@Override
	public void putLongVolatile(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLongVolatile(address, value);
		} else {
			jni.putLongVolatile(address, value);
		}
	}

	@Override
	public void get(long src, int size, byte[] dest, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.get(src, size, dest, offset);
		} else {
			jni.get(src, size, dest, offset);
		}
	}

	@Override
	public void getVolatile(long src, int size, byte[] dest, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.getVolatile(src, size, dest, offset);
		} else {
			jni.getVolatile(src, size, dest, offset);
		}
	}

	@Override
	public void put(long dest, int size, byte[] src, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.put(dest, size, src, offset);
		} else {
			jni.put(dest, size, src, offset);
		}
	}

	@Override
	public void putVolatile(long dest, int size, byte[] src, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.putVolatile(dest, size, src, offset);
		} else {
			jni.putVolatile(dest, size, src, offset);
		}
	}

	@Override
	public void copy(long src, int size, long dest) {
		if (BuildConfig.UNSAFE) {
			unsafe.copy(src, size, dest);
		} else {
			jni.copy(src, size, dest);
		}
	}

	@Override
	public void copyVolatile(long src, int size, long dest) {
		if (BuildConfig.UNSAFE) {
			unsafe.copyVolatile(src, size, dest);
		} else {
			jni.copyVolatile(src, size, dest);
		}
	}

	@Override
	@SuppressWarnings("AccessOfSystemProperties")
	public long virt2phys(long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory translation of address 0x{}", Long.toHexString(address));

		// If we are on a non-Linux OS this won't work
		if (!System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) return 0;

		// Cache other values
		val pgsz = pageSize();
		val adsz = addressSize();

		// Compute the offset, the base address and the page number
		val mask = pgsz - 1;
		val offset = address & mask;
		val base = address - offset;
		val page = base / pgsz * adsz;

		// Try parsing the file /proc/self/pagemap
		if (BuildConfig.DEBUG) log.trace("Parsing file {}", PAGEMAP_PATH);
		var phys = 0L;
		try (
				val pagemap = new RandomAccessFile(PAGEMAP_PATH.toFile(), "r");
				val channel = pagemap.getChannel().position(page)
		) {
			val buffer = ByteBuffer.allocate(adsz).order(ByteOrder.nativeOrder());
			channel.read(buffer);
			switch (adsz) {
				case Byte.BYTES:
					phys = buffer.flip().get();
					break;
				case Short.BYTES:
					phys = buffer.flip().getShort();
					break;
				case Integer.BYTES:
					phys = buffer.flip().getInt();
					break;
				case Long.BYTES:
					phys = buffer.flip().getLong();
					break;
				default:
					return 0;
			}
			phys *= pgsz;
			phys += offset;
		} catch (FileNotFoundException e) {
			log.error("The {} cannot be found", PAGEMAP_PATH, e);
		} catch (IOException e) {
			log.error("The {} cannot be read, closed or we read past its size", PAGEMAP_PATH, e);
		}
		return phys;
	}

}
