package de.tum.in.net.ixy.memory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import sun.misc.Unsafe;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Collection of static methods used to manipulate the memory.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.DontImportSun")
public final class Memory {

	/** A cached instance of the {@link Unsafe} object. */
	private static Unsafe unsafe;

	/**
	 * The page size of the host system.
	 * --------------------------- GETTER ---------------------------
	 * Returns page size of the host system.
	 * <p>
	 * This value is cached when the class is statically initialized.
	 *
	 * @return The page size.
	 */
	@Getter
	private static int pageSize;

	/**
	 * The address size of the host system.
	 * --------------------------- GETTER ---------------------------
	 * Returns the address size of the host system.
	 * <p>
	 * This value is cached when the class is statically initialized.
	 *
	 * @return The address size.
	 */
	@Getter
	private static int addressSize;

	/** The number of bits used as offset inside a single huge memory page. */
	private static int HUGE_PAGE_BITS = 21;

	/** The size of a huge memory page. */
	private static long HUGE_PAGE_SIZE = (1 << HUGE_PAGE_BITS);

	// Cache the variables statically
	static {
		// Load the native library
		System.loadLibrary("ixy");

		// Load the Unsafe object
		try {
			val theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.error("Error getting Unsafe object", e);
		}

		// Cache the values using the configured method
		if (BuildConfig.UNSAFE && unsafe != null) {
			pageSize = unsafe.pageSize();
			addressSize = unsafe.addressSize();
		} else {
			pageSize = c_page_size();
			addressSize = c_address_size();
		}

		// Cache other values
		val size = smartHugepageSize();
		if (size > 0) {
			HUGE_PAGE_SIZE = size;
			HUGE_PAGE_BITS = Long.numberOfTrailingZeros(size);
		}
	}

	/**
	 * Private constructor that always throws an exception.
	 *
	 * @throws UnsupportedOperationException Always.
	 * @deprecated
	 */
	@Deprecated
	private Memory() {
		if (BuildConfig.DEBUG) log.trace("Creating MemoryUtils instance");
		throw new UnsupportedOperationException("This class only contains static methods, instantiation not allowed");
	}

	/**
	 * Getter of the member {@link #HUGE_PAGE_SIZE}.
	 *
	 * @return The value of the member {@link #HUGE_PAGE_SIZE}.
	 */
	public static long getHugepagesize() {
		return HUGE_PAGE_SIZE;
	}

	////////////////////////////////////////////////// NATIVE METHODS //////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with any other
	 * operative system will use a dummy implementation that will always return {@code 0}.
	 *
	 * @return The page size of the system.
	 */
	public static native int c_page_size();

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * This method is portable and should work in all operative systems. The result will always be a power of two.
	 *
	 * @return The address size of the system.
	 */
	public static native int c_address_size();

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * When there is an error, two different numbers can be returned, although it is not consistent across operative
	 * systems:
	 * <ul>
	 *   <li>When there is an error or the mount point of the {@code hugetlbfs} is not found, a {@code -1} is returned
	 *       in {@code Linux} and {@code Windows}.</li>
	 *   <li>If the mount point was found but the smartHugepageSize size could not be computed, a {@code 0} is returned in
	 *       {@code Linux}. This error code will never be returned under {@code Windows}.</li>
	 * </ul>
	 * <p>
	 * Although the name uses the nomenclature {@code smartHugepageSize}, other operative systems use the same technology but
	 * with different name, for example, {@code largepage} on {@code Windows}, {@code superpage} in {@code BSD} or
	 * {@code bigpage} in {@code RHEL}.
	 * <p>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with another
	 * operative system will use a dummy implementation that will always return {@code -1}.
	 *
	 * @return The size of a huge memory page.
	 */
	public static native long c_hugepage_size();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The memory allocated by this method MUST be deallocated with {@link #c_deallocate(long, long)}, {@link
	 * #u_deallocate(long, long)} or {@link #smartDeallocate(long, long)}, as it won't be garbage collected by the Java
	 * Virtual Machine.
	 * <p>
	 * The allocated memory region can be contiguous if requested, but since the implementation uses {@code hugepages}
	 * to do so, if the requested size is bigger than the bytes a smartHugepageSize can smartAllocate, the allocation will return
	 * the invalid address {@code 0}.
	 * <p>
	 * The implementation assumes that a {@code hugetlbfs} is mounted at {@link BuildConfig#HUGE_MNT} on Linux.
	 * On Windows it uses {@code VirtualAlloc} to smartAllocate a big memory page.
	 *
	 * @param size       The number of bytes to smartAllocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 */
	public static native long c_allocate(final long size, final boolean contiguous);

	/**
	 * Deallocates a previously allocated memory region.
	 * <p>
	 * If the given address is not a multiple of the smartPageSize it won't fail because it will be converted to the base
	 * address of a huge memory page.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @return If the operation succeeded.
	 */
	public static native boolean c_deallocate(final long address, final long size);

	/**
	 * Reads a {@code byte} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static native byte c_getByte(final long address);

	/**
	 * Reads a {@code short} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static native short c_getShort(final long address);

	/**
	 * Reads a {@code int} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static native int c_getInt(final long address);

	/**
	 * Reads a {@code long} from an arbitrary memory address.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static native long c_getLong(final long address);

	/**
	 * Writes a {@code byte} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static native void c_putByte(final long address, final byte value);

	/**
	 * Writes a {@code short} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static native void c_putShort(final long address, final short value);

	/**
	 * Writes an {@code int} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static native void c_putInt(final long address, final int value);

	/**
	 * Writes a {@code long} to an arbitrary memory address.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static native void c_putLong(final long address, final long value);

	////////////////////////////////////////////////// UNSAFE METHODS //////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 *
	 * @return The page size of the system.
	 */
	public static int u_page_size() {
		if (BuildConfig.DEBUG) log.trace("Computing page size using the Unsafe object");
		return unsafe.pageSize();
	}

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 *
	 * @return The address size of the system.
	 */
	public static int u_address_size() {
		if (BuildConfig.DEBUG) log.trace("Computing virtual address size using the Unsafe object");
		return unsafe.addressSize();
	}

	/**
	 * The {@link Unsafe} object does not have any method to check the size of a huge memory page.
	 *
	 * @return The size of a huge memory page.
	 * @throws UnsupportedOperationException Always.
	 */
	public static long u_hugepage_size() throws UnsupportedOperationException {
		if (BuildConfig.DEBUG) log.trace("Computing huge page size using the Unsafe object");
		throw new UnsupportedOperationException("Unsafe object does not provide an implementation for this");
	}

	/**
	 * The {@link Unsafe} object does not have any method to smartAllocate memory using huge memory pages.
	 *
	 * @param size       The number of bytes to smartAllocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 * @throws UnsupportedOperationException Always.
	 */
	public static long u_allocate(final long size, final boolean contiguous) throws UnsupportedOperationException {
		if (BuildConfig.DEBUG) log.trace("Allocating {} bytes using huge memory pages and the Unsafe object", size);
		throw new UnsupportedOperationException("Unsafe object does not provide an implementation for this");
	}

	/**
	 * The {@link Unsafe} object does not have any method to smartDeallocate using huge memory pages.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @return If the operation succeeded.
	 * @throws UnsupportedOperationException Always.
	 */
	public static boolean u_deallocate(final long address, final long size) throws UnsupportedOperationException {
		if (BuildConfig.DEBUG) {
			log.trace("Deallocating {} bytes @ 0x{} using the Unsafe object", size, Long.toHexString(address));
		}
		throw new UnsupportedOperationException("Unsafe object does not provide any facility for this operation");
	}

	/**
	 * Delegates to the method {@link Unsafe#getByte(long)}.
	 *
	 * @param address The virtual address to read from.
	 * @return The read value.
	 * @see Unsafe#getByte(long)
	 */
	public static byte u_getByte(final long address) {
		if (BuildConfig.DEBUG) log.trace("Reading byte @ 0x{} using the Unsafe object", Long.toHexString(address));
		return unsafe.getByte(address);
	}

	/**
	 * Delegates to the method {@link Unsafe#getShort(long)}.
	 *
	 * @param address The virtual address to read from.
	 * @return The read value.
	 * @see Unsafe#getShort(long)
	 */
	public static short u_getShort(final long address) {
		if (BuildConfig.DEBUG) log.trace("Reading short @ 0x{} using the Unsafe object", Long.toHexString(address));
		return unsafe.getShort(address);
	}

	/**
	 * Delegates to the method {@link Unsafe#getInt(long)}.
	 *
	 * @param address The virtual address to read from.
	 * @return The read value.
	 * @see Unsafe#getInt(long)
	 */
	public static int u_getInt(final long address) {
		if (BuildConfig.DEBUG) log.trace("Reading int @ 0x{} using the Unsafe object", Long.toHexString(address));
		return unsafe.getInt(address);
	}

	/**
	 * Delegates to the method {@link Unsafe#getLong(long)}.
	 *
	 * @param address The virtual address to read from.
	 * @return The read value.
	 * @see Unsafe#getLong(long)
	 */
	public static long u_getLong(final long address) {
		if (BuildConfig.DEBUG) log.trace("Reading long @ 0x{} using the Unsafe object", Long.toHexString(address));
		return unsafe.getLong(address);
	}

	/**
	 * Delegates to the method {@link Unsafe#putByte(long, byte)}.
	 *
	 * @param address The virtual address to write to.
	 * @param value   The value to write.
	 * @see Unsafe#putByte(long, byte)
	 */
	public static void u_putByte(final long address, final byte value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			log.trace("Writing byte 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putByte(address, value);
	}

	/**
	 * Delegates to the method {@link Unsafe#putShort(long, short)}.
	 *
	 * @param address The virtual address to write to.
	 * @param value   The value to write.
	 * @see Unsafe#putShort(long, short)
	 */
	public static void u_putShort(final long address, final short value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			log.trace("Writing short 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putShort(address, value);
	}

	/**
	 * Delegates to the method {@link Unsafe#putInt(long, int)}.
	 *
	 * @param address The virtual address to write to.
	 * @param value   The value to write.
	 * @see Unsafe#putInt(long, int)
	 */
	public static void u_putInt(final long address, final int value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Integer.toHexString(value);
			log.trace("Writing byte 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putInt(address, value);
	}

	/**
	 * Delegates to the method {@link Unsafe#putLong(long, long)}.
	 *
	 * @param address The virtual address to write to.
	 * @param value   The value to write.
	 * @see Unsafe#putLong(long, long)
	 */
	public static void u_putLong(final long address, final long value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Long.toHexString(value);
			log.trace("Writing long 0x{} @ 0x{} using the Unsafe object", xvalue, xaddress);
		}
		unsafe.putLong(address, value);
	}

	/////////////////////////////////////////////////// SAFE METHODS ///////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 * <p>
	 * This method delegates to {@link #u_page_size()} or {@link #c_page_size()} depending on the value of {@link
	 * BuildConfig#UNSAFE}.
	 *
	 * @return The page size of the system.
	 * @see #c_page_size()
	 * @see #u_page_size()
	 */
	public static int smartPageSize() {
		if (BuildConfig.DEBUG) log.trace("Smart page size computation");
		return BuildConfig.UNSAFE && unsafe != null ? u_page_size() : c_page_size();
	}

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * This method delegates to {@link #u_address_size()} or {@link #c_address_size()} based on the value of {@link
	 * BuildConfig#UNSAFE}.
	 *
	 * @return The address size of the system.
	 * @see #c_address_size()
	 * @see #u_address_size()
	 */
	public static int smartAddressSize() {
		if (BuildConfig.DEBUG) log.trace("Smart virtual address size computation");
		return BuildConfig.UNSAFE && unsafe != null ? u_address_size() : c_address_size();
	}

	/**
	 * Computes the size of a huge memory page.
	 * <p>
	 * This method uses the file system to detect the support of huge memory pages or delegates to {@link
	 * #c_hugepage_size()} in {@code Windows}, because it is rather easy to implement in C.
	 *
	 * @return The size of a huge memory page.
	 * @see #c_hugepage_size()
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static long smartHugepageSize() {
		if (BuildConfig.DEBUG) log.trace("Smart huge memory page size computation");

		// If we are on a non-Linux OS or forcing the C implementation, call the JNI method
		if (!BuildConfig.UNSAFE || !System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) {
			return c_hugepage_size();
		}

		// Try parsing the file /etc/mtab
		if (BuildConfig.DEBUG) log.trace("Parsing file /etc/mtab");
		var found = false;
		try (val mtab = new BufferedReader(new InputStreamReader(new FileInputStream("/etc/mtab"), StandardCharsets.UTF_8))) {
			for (var line = mtab.readLine(); line != null; line = mtab.readLine()) {
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
				if (word.equals("hugetlbfs")) {
					found = true;
					break;
				}
			}
		} catch (IOException e) {
			log.error("The /etc/mtab cannot be found, read or closed", e);
		}
		if (!found) {
			return -1;
		}

		// Try parsing the file /proc/meminfo
		if (BuildConfig.DEBUG) log.trace("Parsing file /proc/meminfo");
		try (val meminfo = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo"), StandardCharsets.UTF_8))) {
			for (var line = meminfo.readLine(); line != null; line = meminfo.readLine()) {
				var _space = 0;
				var space  = line.indexOf(':', _space);
				var word   = line.substring(_space, space);
				if (word.equals("Hugepagesize")) {
					line  = line.substring(space + 1).trim();
					space = line.indexOf(' ', 0);
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
		} catch (IOException e) {
			log.error("The /proc/meminfo cannot be found, read or closed", e);
		}
		return 0;
	}

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * This method delegates to {@link #c_allocate(long, boolean)} since there is no other way to allocate memory using
	 * huge memory pages in Java.
	 *
	 * @param size       The number of bytes to allocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 */
	public static long smartAllocate(final long size, final boolean contiguous) {
		if (BuildConfig.DEBUG) log.trace("Smart memory allocation of {} bytes", size);
		return c_allocate(size, contiguous);
	}

	/**
	 * Deallocates a previously allocated memory region.
	 * <p>
	 * This method delegates to {@link #c_deallocate(long, long)} since there is no other way to deallocate memory using
	 * big memory pages in Java.
	 *
	 * @param address The address of the previously allocated region.
	 * @param size    The size of the allocated region.
	 * @return If the operation succeeded.
	 */
	public static boolean smartDeallocate(final long address, final long size) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Smart memory deallocation of {} bytes @ 0x{}", size, xaddress);
		}
		return c_deallocate(address, size);
	}

	/**
	 * Uses the unsafe implementation or the JNI call depending on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static byte getByte(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory read of byte @ 0x{}", Long.toHexString(address));
		return BuildConfig.UNSAFE ? u_getByte(address) : c_getByte(address);
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static short getShort(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory read of short @ 0x{}", Long.toHexString(address));
		return BuildConfig.UNSAFE ? u_getShort(address) : c_getShort(address);
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static int getInt(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory read of int @ 0x{}", Long.toHexString(address));
		return BuildConfig.UNSAFE ? u_getInt(address) : c_getInt(address);
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to read from.
	 * @return The read value.
	 */
	public static long getLong(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory read of long @ 0x{}", Long.toHexString(address));
		return BuildConfig.UNSAFE ? u_getLong(address) : c_getLong(address);
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static void putByte(final long address, final byte value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			log.trace("Smart memory write of byte 0x{} @ 0x{}", xvalue, xaddress);
		}
		if (BuildConfig.UNSAFE) {
			u_putByte(address, value);
		} else {
			c_putByte(address, value);
		}
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static void putShort(final long address, final short value) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			log.trace("Smart memory write of short 0x{} @ 0x{}", xvalue, xaddress);
		}
		if (BuildConfig.UNSAFE) {
			u_putShort(address, value);
		} else {
			c_putShort(address, value);
		}
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static void putInt(final long address, final int value) {
		if (BuildConfig.DEBUG) {
			log.trace("Smart memory write of int 0x{} @ 0x{}", Integer.toHexString(value), Long.toHexString(address));
		}
		if (BuildConfig.UNSAFE) {
			u_putInt(address, value);
		} else {
			c_putInt(address, value);
		}
	}

	/**
	 * Uses the unsafe implementation or the JNI call based on the value of {@link BuildConfig#UNSAFE}.
	 *
	 * @param address The address to write to.
	 * @param value   The value to write.
	 */
	public static void putLong(final long address, final long value) {
		if (BuildConfig.DEBUG) {
			log.trace("Smart memory write of long 0x{} @ 0x{}", Long.toHexString(value), Long.toHexString(address));
		}
		if (BuildConfig.UNSAFE) {
			u_putLong(address, value);
		} else {
			c_putLong(address, value);
		}
	}

	/**
	 * Translates a virtual address to a physical address.
	 * <p>
	 * This method is only implemented for {@code Linux}. Calling this method with any other operative system will use a
	 * dummy implementation that will always return {@code 0}.
	 *
	 * @param address The virtual address to translate.
	 * @return The physical address.
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static long virt2phys(final long address) {
		if (BuildConfig.DEBUG) log.trace("Smart memory translation of address 0x{}", Long.toHexString(address));

		// If we are on a non-Linux OS this won't work
		if (!System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("lin")) {
			return 0;
		}

		// Compute the offset, the base address and the page number
		val mask   = pageSize - 1;
		val offset = address & mask;
		val base   = address - offset;
		val page   = base / pageSize * addressSize;

		// Try parsing the file /proc/self/pagemap
		if (BuildConfig.DEBUG) log.trace("Parsing file /proc/self/pagemap");
		var phys = 0L;
		try (val pagemap = new RandomAccessFile("/proc/self/pagemap", "r")) {
			val buffer = ByteBuffer.allocate(addressSize).order(ByteOrder.nativeOrder());
			pagemap.getChannel().position(page).read(buffer);
			switch (addressSize) {
				case Long.BYTES:
					phys = buffer.flip().getLong();
					break;
				case Integer.BYTES:
					phys = buffer.flip().getInt();
					break;
				default:
					return 0;
			}

			// Convert the physical page number to an address with the same offset as the virtual address
			phys *= pageSize;
			phys += offset;
		} catch (IOException e) {
			log.error("The /proc/self/pagemap cannot be found, read, closed or we read past its size", e);
		}

		// Return the computed physical address
		return phys;
	}

	/**
	 * Allocates memory using huge memory pages and creates a {@link DmaMemory} instance containing both the virtual and
	 * physical address.
	 * <p>
	 * This method will return {@code null} if the allocation could not be completed successfully, but nothing
	 * guarantees that the physical address is correct.
	 *
	 * @param size       The amount of bytes to smartAllocate.
	 * @param contiguous If the allocated memory should be contiguous.
	 * @return The pair of virtual-physical addresses representing the allocated memory.
	 */
	public static DmaMemory allocateDma(final long size, final boolean contiguous) {
		if (BuildConfig.DEBUG) log.trace("Allocating {} bytes with DmaMemory", size);
		val virt = smartAllocate(size, contiguous);
		if (virt == 0) {
			return null;
		}
		val phys = virt2phys(virt);
		return new DmaMemory(virt, phys);
	}

}
