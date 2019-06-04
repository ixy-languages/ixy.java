package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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
public final class SmartMemoryManager implements IxyMemoryManager {

	/** The return code used when no huge memory page technology is supported by the CPU. */
	public static final int HUGE_PAGE_NOT_SUPPORTED = -1;

	/**
	 * Cached instance to use as singleton.
	 * -------------- GETTER --------------
	 * Returns a singleton instance.
	 *
	 * @return A singleton instance.
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	private static final SmartMemoryManager instance = new SmartMemoryManager();

	/** The singleton instance of the JNI-based memory manager. */
	private transient UnsafeMemoryManager unsafe = UnsafeMemoryManager.getInstance();

	/** The singleton instance of the JNI-based memory manager. */
	private transient JniMemoryManager jni = JniMemoryManager.getInstance();

	/** Private constructor that throws an exception if the instance is already instantiated. */
	private SmartMemoryManager() {
		if (BuildConfig.DEBUG) log.debug("Creating a smart memory manager");
		if (instance != null) throw new IllegalStateException("An instance cannot be created twice. Use getInstance() instead.");
	}

	/** {@inheritDoc} */
	@Override
	public int addressSize() {
		return BuildConfig.UNSAFE ? unsafe.addressSize() : jni.addressSize();
	}

	/** {@inheritDoc} */
	@Override
	public long pageSize() {
		return BuildConfig.UNSAFE ? unsafe.pageSize() : jni.pageSize();
	}

	/** {@inheritDoc} */
	@Override
	public long hugepageSize() {
		if (BuildConfig.DEBUG) log.trace("Smart huge memory page size computation");

		// If we are on a non-Linux OS or forcing the C implementation, call the JNI method
		if (!BuildConfig.UNSAFE || !System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) {
			return jni.hugepageSize();
		}

		// Try parsing the file /etc/mtab
		if (BuildConfig.DEBUG) log.trace("Parsing file /etc/mtab");
		try (val mtab = new BufferedReader(new InputStreamReader(new FileInputStream("/etc/mtab"), StandardCharsets.UTF_8))) {
			var line = mtab.readLine();
			for (; line != null; line = mtab.readLine()) {
				var _space = 0;
				var space = line.indexOf(' ', _space);
				var word = line.substring(_space, space);
				if (!word.equals("hugetlbfs")) continue; // Not the file system we want
				_space = space + 1;
				space = line.indexOf(' ', _space);
				word = line.substring(_space, space);
				if (!word.equals(BuildConfig.HUGE_MNT)) continue; // Not the mount point we want
				_space = space + 1;
				space = line.indexOf(' ', _space);
				word = line.substring(_space, space);
				if (word.equals("hugetlbfs")) break;
			}
			if (line == null) return HUGE_PAGE_NOT_SUPPORTED;
		} catch (final IOException e) {
			log.error("The /etc/mtab cannot be found, read or closed", e);
			return HUGE_PAGE_NOT_SUPPORTED;
		}

		// Try parsing the file /proc/meminfo
		if (BuildConfig.DEBUG) log.trace("Parsing file /proc/meminfo");
		try (val meminfo = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo"), StandardCharsets.UTF_8))) {
			for (var line = meminfo.readLine(); line != null; line = meminfo.readLine()) {
				var _space = 0;
				var space = line.indexOf(':', _space);
				var word = line.substring(_space, space);
				if (word.equals("Hugepagesize")) {
					line = line.substring(space + 1).trim();
					space = line.indexOf(' ');
					var bytes = Long.parseLong(line.substring(0, space));
					val units = line.substring(space + 1).trim();
					switch (units) {
						case "GB":
							bytes *= 1024*1024*1024;
							break;
						case "MB":
							bytes *= 1024*1024;
							break;
						case "kB":
							bytes *= 1024;
							break;
						case "B":
							break;
						default:
							return 0;
					}
					return bytes;
				}
			}
		} catch (final IOException e) {
			log.error("The /proc/meminfo cannot be found, read or closed", e);
		}
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public long allocate(final long size, final boolean huge, final boolean contiguous) {
		if (huge) {
			return jni.allocate(size, huge, contiguous);
		} else {
			return BuildConfig.UNSAFE ? unsafe.allocate(size, huge, contiguous) : jni.allocate(size, huge, contiguous);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean free(final long src, final long size, final boolean huge) {
		if (huge) {
			return jni.free(src, size, huge);
		} else {
			return BuildConfig.UNSAFE ? unsafe.free(src, size, huge) : jni.free(src, size, huge);
		}
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getByte(src) : jni.getByte(src);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getByteVolatile(src) : jni.getByteVolatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final long dest, final byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByte(dest, value);
		} else {
			jni.putByte(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final long dest, final byte value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putByteVolatile(dest, value);
		} else {
			jni.putByteVolatile(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getShort(src) : jni.getShort(src);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getShortVolatile(src) : jni.getShortVolatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final long dest, final short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShort(dest, value);
		} else {
			jni.putShort(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final long dest, final short value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putShortVolatile(dest, value);
		} else {
			jni.putShortVolatile(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getInt(src) : jni.getInt(src);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getIntVolatile(src) : jni.getIntVolatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final long dest, final int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putInt(dest, value);
		} else {
			jni.putInt(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final long dest, final int value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putIntVolatile(dest, value);
		} else {
			jni.putIntVolatile(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getLong(src) : jni.getLong(src);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final long src) {
		return BuildConfig.UNSAFE ? unsafe.getLongVolatile(src) : jni.getLongVolatile(src);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final long dest, final long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLong(dest, value);
		} else {
			jni.putLong(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final long dest, final long value) {
		if (BuildConfig.UNSAFE) {
			unsafe.putLongVolatile(dest, value);
		} else {
			jni.putLongVolatile(dest, value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void get(final long src, final int size, final byte[] dest, final int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.get(src, size, dest, offset);
		} else {
			jni.get(src, size, dest, offset);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void getVolatile(final long src, final int size, final byte[] dest, final int offset) {
		jni.getVolatile(src, size, dest, offset);
	}

	/** {@inheritDoc} */
	@Override
	public void put(final long dest, final int size, final byte[] src, final int offset) {
		if (BuildConfig.UNSAFE) {
			unsafe.put(dest, size, src, offset);
		} else {
			jni.put(dest, size, src, offset);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void putVolatile(final long dest, final int size, final byte[] src, final int offset) {
		jni.putVolatile(dest, size, src, offset);
	}

	/** {@inheritDoc} */
	@Override
	public void copy(final long src, final int size, final long dest) {
		if (BuildConfig.UNSAFE) {
			unsafe.copy(src, size, dest);
		} else {
			jni.copy(src, size, dest);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void copyVolatile(final long src, final int size, final long dest) {
		jni.copyVolatile(src, size, dest);
	}

	/** {@inheritDoc} */
	@Override
	public long virt2phys(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory translation of address 0x{}", Long.toHexString(address));

		// If we are on a non-Linux OS this won't work
		if (!System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) return 0;

		// Cache other values
		val pgsz = pageSize();
		val adsz = addressSize();

		// Compute the offset, the base address and the page number
		val mask   = pgsz - 1;
		val offset = address & mask;
		val base   = address - offset;
		val page   = base / pgsz * adsz;

		// Try parsing the file /proc/self/pagemap
		if (BuildConfig.DEBUG) log.trace("Parsing file /proc/self/pagemap");
		var phys = 0L;
		try (val pagemap = new RandomAccessFile("/proc/self/pagemap", "r")) {
			val buffer = ByteBuffer.allocate(adsz).order(ByteOrder.nativeOrder());
			pagemap.getChannel().position(page).read(buffer);
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
		} catch (final IOException e) {
			log.error("The /proc/self/pagemap cannot be found, read, closed or we read past its size", e);
		}
		return phys;
	}

	/** {@inheritDoc} */
	@Override
	public IxyDmaMemory dmaAllocate(long size, boolean huge, boolean contiguous) {
		return jni.dmaAllocate(size, huge, contiguous);
	}

}
