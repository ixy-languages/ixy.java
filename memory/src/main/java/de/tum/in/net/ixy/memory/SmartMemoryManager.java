package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
 * Simple implementation of Ixy's memory manager specification using JNI.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
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
	private static final @NotNull Path MTAB_PATH = Paths.get(File.separator, "etc", "mtab");

	/** The path to the memory info file. */
	private static final @NotNull Path MEMINFO_PATH = Paths.get(File.separator, "proc", "meminfo");

	/** The page map file. */
	private static final @NotNull Path PAGEMAP_PATH = Paths.get(File.separator, "proc", "self", "pagemap");

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Singleton instance.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Setter(AccessLevel.NONE)
	@SuppressWarnings("JavaDoc")
	@Getter(onMethod_ = {@Contract(pure = true)})
	@SuppressFBWarnings("SI_INSTANCE_BEFORE_FINALS_ASSIGNED")
	private static final @NotNull IxyMemoryManager singleton = new SmartMemoryManager();

	/**
	 * Creates a buffered reader given a {@code file}.
	 *
	 * @param file The file to read.
	 * @return The buffered reader.
	 * @throws FileNotFoundException If the {@code file} does not exist.
	 */
	@Contract(value = "null -> fail; !null -> new", pure = true)
	private static @NotNull BufferedReader bufferedReader(@NotNull File file) throws FileNotFoundException {
		val fileInputStream = new FileInputStream(file);
		val inputFilterStream = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
		return new BufferedReader(inputFilterStream);
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The singleton instance of the Unsafe-based memory manager. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "unsafe", rank = 2)
	private final @NotNull IxyMemoryManager unsafe = UnsafeMemoryManager.getSingleton();

	/** The singleton instance of the JNI-based memory manager. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "jni", rank = 1)
	private final @NotNull IxyMemoryManager jni = JniMemoryManager.getSingleton();

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Private constructor that throws an exception if the instance is already instantiated. */
	@SuppressWarnings("ConstantConditions")
	@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
	private SmartMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating a smart memory manager");
		if (singleton != null) {
			throw new IllegalStateException("An instance cannot be created twice. Use getSingleton() instead.");
		}
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int addressSize() {
		return BuildConfig.UNSAFE ? unsafe.addressSize() : jni.addressSize();
	}

	@Override
	@Contract(pure = true)
	public long pageSize() {
		return BuildConfig.UNSAFE ? unsafe.pageSize() : jni.pageSize();
	}

	@Override
	@Contract(pure = true)
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
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail", pure = true)
	public long allocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		if (allocationType == AllocationType.HUGE) {
			return jni.allocate(bytes, AllocationType.HUGE, layoutType);
		} else {
			return BuildConfig.UNSAFE
					? unsafe.allocate(bytes, allocationType, layoutType)
					: jni.allocate(bytes, allocationType, layoutType);
		}
	}

	@Override
	@Contract(value = "_, null, _ -> fail; _, _, null -> fail; _, !null, !null -> new", pure = true)
	public @NotNull IxyDmaMemory dmaAllocate(long bytes, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		return jni.dmaAllocate(bytes, allocationType, layoutType);
	}

	@Override
	@Contract(value = "_, _, null -> fail", pure = true)
	public boolean free(long address, long bytes, @NotNull AllocationType allocationType) {
		if (allocationType == AllocationType.HUGE) {
			return jni.free(address, bytes, AllocationType.HUGE);
		} else {
			return BuildConfig.UNSAFE
					? unsafe.free(address, bytes, allocationType)
					: jni.free(address, bytes, allocationType);
		}
	}

	@Override
	@Contract(pure = true)
	public byte getByte(long address) {
		return BuildConfig.UNSAFE ? unsafe.getByte(address) : jni.getByte(address);
	}

	@Override
	@Contract(pure = true)
	public byte getByteVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getByteVolatile(address) : jni.getByteVolatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putByte(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByte(address, value);
		} else {
			jni.putByte(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void putByteVolatile(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByteVolatile(address, value);
		} else {
			jni.putByteVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public byte getAndPutByte(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutByte(address, value) : jni.getAndPutByte(address, value);
	}

	@Override
	@Contract(pure = true)
	public byte getAndPutByteVolatile(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutByteVolatile(address, value) : jni.getAndPutByteVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public void addByte(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addByte(address, value);
		} else {
			jni.addByte(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void addByteVolatile(long address, byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addByteVolatile(address, value);
		} else {
			jni.addByteVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public byte getAndAddByte(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddByte(address, value) : jni.getAndAddByte(address, value);
	}

	@Override
	@Contract(pure = true)
	public byte getAndAddByteVolatile(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddByteVolatile(address, value) : jni.getAndAddByteVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public byte addAndGetByte(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetByte(address, value) : jni.addAndGetByte(address, value);
	}

	@Override
	@Contract(pure = true)
	public byte addAndGetByteVolatile(long address, byte value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetByteVolatile(address, value) : jni.addAndGetByteVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public short getShort(long address) {
		return BuildConfig.UNSAFE ? unsafe.getShort(address) : jni.getShort(address);
	}

	@Override
	@Contract(pure = true)
	public short getShortVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getShortVolatile(address) : jni.getShortVolatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putShort(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShort(address, value);
		} else {
			jni.putShort(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void putShortVolatile(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShortVolatile(address, value);
		} else {
			jni.putShortVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public short getAndPutShort(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutShort(address, value) : jni.getAndPutShort(address, value);
	}

	@Override
	@Contract(pure = true)
	public short getAndPutShortVolatile(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutShortVolatile(address, value) : jni.getAndPutShortVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public void addShort(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addShort(address, value);
		} else {
			jni.addShort(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void addShortVolatile(long address, short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addShortVolatile(address, value);
		} else {
			jni.addShortVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public short getAndAddShort(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddShort(address, value) : jni.getAndAddShort(address, value);
	}

	@Override
	@Contract(pure = true)
	public short getAndAddShortVolatile(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddShortVolatile(address, value) : jni.getAndAddShortVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public short addAndGetShort(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetShort(address, value) : jni.addAndGetShort(address, value);
	}

	@Override
	@Contract(pure = true)
	public short addAndGetShortVolatile(long address, short value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetShortVolatile(address, value) : jni.addAndGetShortVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public int getInt(long address) {
		return BuildConfig.UNSAFE ? unsafe.getInt(address) : jni.getInt(address);
	}

	@Override
	@Contract(pure = true)
	public int getIntVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getIntVolatile(address) : jni.getIntVolatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putInt(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putInt(address, value);
		} else {
			jni.putInt(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void putIntVolatile(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putIntVolatile(address, value);
		} else {
			jni.putIntVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public int getAndPutInt(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutInt(address, value) : jni.getAndPutInt(address, value);
	}

	@Override
	@Contract(pure = true)
	public int getAndPutIntVolatile(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutIntVolatile(address, value) : jni.getAndPutIntVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public void addInt(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addInt(address, value);
		} else {
			jni.addInt(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void addIntVolatile(long address, int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addIntVolatile(address, value);
		} else {
			jni.addIntVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public int getAndAddInt(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddInt(address, value) : jni.getAndAddInt(address, value);
	}

	@Override
	@Contract(pure = true)
	public int getAndAddIntVolatile(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddIntVolatile(address, value) : jni.getAndAddIntVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public int addAndGetInt(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetInt(address, value) : jni.addAndGetInt(address, value);
	}

	@Override
	@Contract(pure = true)
	public int addAndGetIntVolatile(long address, int value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetIntVolatile(address, value) : jni.addAndGetIntVolatile(address, value);
	}
	
	@Override
	@Contract(pure = true)
	public long getLong(long address) {
		return BuildConfig.UNSAFE ? unsafe.getLong(address) : jni.getLong(address);
	}

	@Override
	@Contract(pure = true)
	public long getLongVolatile(long address) {
		return BuildConfig.UNSAFE ? unsafe.getLongVolatile(address) : jni.getLongVolatile(address);
	}

	@Override
	@Contract(pure = true)
	public void putLong(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLong(address, value);
		} else {
			jni.putLong(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void putLongVolatile(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLongVolatile(address, value);
		} else {
			jni.putLongVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public long getAndPutLong(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutLong(address, value) : jni.getAndPutLong(address, value);
	}

	@Override
	@Contract(pure = true)
	public long getAndPutLongVolatile(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.getAndPutLongVolatile(address, value) : jni.getAndPutLongVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public void addLong(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addLong(address, value);
		} else {
			jni.addLong(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public void addLongVolatile(long address, long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.addLongVolatile(address, value);
		} else {
			jni.addLongVolatile(address, value);
		}
	}

	@Override
	@Contract(pure = true)
	public long getAndAddLong(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddLong(address, value) : jni.getAndAddLong(address, value);
	}

	@Override
	@Contract(pure = true)
	public long getAndAddLongVolatile(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.getAndAddLongVolatile(address, value) : jni.getAndAddLongVolatile(address, value);
	}

	@Override
	@Contract(pure = true)
	public long addAndGetLong(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetLong(address, value) : jni.addAndGetLong(address, value);
	}

	@Override
	@Contract(pure = true)
	public long addAndGetLongVolatile(long address, long value) {
		return BuildConfig.UNSAFE ? unsafe.addAndGetLongVolatile(address, value) : jni.addAndGetLongVolatile(address, value);
	}

	@Override
	@Contract(mutates = "param3")
	public void get(long src, int bytes, @NotNull byte[] dest, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.get(src, bytes, dest, offset);
		} else {
			jni.get(src, bytes, dest, offset);
		}
	}

	@Override
	@Contract(mutates = "param3")
	public void getVolatile(long src, int bytes, @NotNull byte[] dest, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.getVolatile(src, bytes, dest, offset);
		} else {
			jni.getVolatile(src, bytes, dest, offset);
		}
	}

	@Override
	@Contract(pure = true)
	public void put(long dest, int bytes, @NotNull byte[] src, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.put(dest, bytes, src, offset);
		} else {
			jni.put(dest, bytes, src, offset);
		}
	}

	@Override
	@Contract(pure = true)
	public void putVolatile(long dest, int bytes, @NotNull byte[] src, int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.putVolatile(dest, bytes, src, offset);
		} else {
			jni.putVolatile(dest, bytes, src, offset);
		}
	}

	@Override
	@Contract(pure = true)
	public void copy(long src, int bytes, long dest) {
		if (BuildConfig.UNSAFE) {
			unsafe.copy(src, bytes, dest);
		} else {
			jni.copy(src, bytes, dest);
		}
	}

	@Override
	@Contract(pure = true)
	public void copyVolatile(long src, int bytes, long dest) {
		if (BuildConfig.UNSAFE) {
			unsafe.copyVolatile(src, bytes, dest);
		} else {
			jni.copyVolatile(src, bytes, dest);
		}
	}

	@Override
	public long obj2virt(@NotNull Object object) {
		return unsafe.obj2virt(object);
	}

	@Override
	@Contract(pure = true)
	@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
	@SuppressWarnings({"AccessOfSystemProperties", "PMD.DataflowAnomalyAnalysis"})
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
