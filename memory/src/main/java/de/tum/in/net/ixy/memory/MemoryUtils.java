package de.tum.in.net.ixy.memory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

/** Collection of static methods used to directly manipulate memory. */
@Slf4j
public final class MemoryUtils {

	/** The {@link Unsafe} instance used to get low-level information about the system. */
	private static Unsafe unsafe;

	/** The page size of the host system. */
	@Getter
	private static int pagesize;

	/** The address size of the host system. */
	@Getter
	private static int addrsize;

	/** The number of bits that can be used as offset inside a single bigger memory page. */
	private static int HUGE_PAGE_BITS = 21;

	/** The size of a bigger memory page. */
	private static long HUGE_PAGE_SIZE = (1 << HUGE_PAGE_BITS);

	/** Load things statically. */
	static {
		
		// Load the native library
		System.loadLibrary("ixy");

		// Load the Unsafe object
		try {
			val singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			unsafe = (Unsafe) singleoneInstanceField.get(null);
		} catch (NoSuchFieldException e) {
			log.error("Error getting Unsafe object", e);
		} catch (IllegalAccessException e) {
			log.error("Error accessing the Unsafe object", e);
		}

		// Cache the values in private static fields with public getters using the configured method
		if (BuildConstants.UNSAFE && unsafe != null) {
			pagesize = unsafe.pageSize();
			addrsize = unsafe.addressSize();
		} else {
			pagesize = c_pagesize();
			addrsize = c_addrsize();
		}

		// Cache other values
		val size = hugepage();
		if (size > 0) {
			HUGE_PAGE_SIZE = size;
			HUGE_PAGE_BITS = Long.numberOfTrailingZeros(HUGE_PAGE_SIZE);
		}
	}

	/**
	 * Private constructor declared to disallow instantiation of the class.
	 * 
	 * @throws UnsupportedOperationException Thrown always to prevents instance
	 *                                       creation.
	 * @deprecated
	 */
	@Deprecated
	private MemoryUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
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
	public static native int c_pagesize();

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * This method is portable and should work in all operative systems. The result will always be a power of two.
	 * 
	 * @return The address size of the system.
	 */
	public static native int c_addrsize();

	/**
	 * Computes the size of a bigger memory page.
	 * <p>
	 * Although the name uses the nomenclature {@code hugepage}, other operative systems use the same technology but
	 * with different name, for example, {@code largepage} on {@code Windows}, {@code superpage} in {@code BSD} or
	 * {@code bigpage} in {@code RHEL}.
	 * <p>
	 * When there is an error, two different numbers can be returned, although it is not consistent across operative
	 * systems:
	 * <ul>
	 *   <li>When there is an error or the mount point of the {@code hugetlbfs} is not found, a {@code -1} is returned
	 *       in {@code Linux} and {@code Windows}.</li>
	 *   <li>If the mount point was found but the hugepage size could not be computed, a {@code 0} is returned in
	 *       {@code Linux}. This error code will never be returned under {@code Windows}.</li>
	 * </ul>
	 * This method is only implemented for {@code Linux} and {@code Windows}. Calling this method with another
	 * operative system will use a dummy implementation that will always return {@code -1}.
	 * 
	 * @return The size of a bigger memory page.
	 */
	public static native long c_hugepage();

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * The allocated memory region can be contiguous if requested, but since the implementation uses {@code hugepages}
	 * to do so, if the requested size is bigger than the bytes a hugepage can allocate, the allocation will return the
	 * invalid address {@code 0}.
	 * <p>
	 * The implementation assumes that a {@code hugetlbfs} is mounted at {@link BuildConstants#HUGE_MNT} on Linux.
	 * On Windows it uses {@code VirtualAlloc} to allocate a huge page.
	 * 
	 * @param size       The number of bytes to allocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 */
	public static native long c_allocate(final long size, final boolean contiguous);

	////////////////////////////////////////////////// UNSAFE METHODS //////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 * 
	 * @return The page size of the system.
	 */
	public static int u_pagesize() {
		log.trace("Computing page size using Unsafe object");
		return unsafe.pageSize();
	}

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * The result will always be a power of two.
	 * 
	 * @return The address size of the system.
	 */
	public static int u_addrsize() {
		log.trace("Computing virtual address size using Unsafe object");
		return unsafe.addressSize();
	}

	/**
	 * The {@link Unsafe} object does not have any method to check for bigger memory pages.
	 * 
	 * @return The size of a bigger memory page.
	 * @throws UnsupportedOperationException Always.
	 */
	public static long u_hugepage() throws UnsupportedOperationException {
		log.trace("Computing bigger page size using Unsafe object");
		throw new UnsupportedOperationException("Unsafe object does not provide any facility for this check");
	}

	/**
	 * The {@link Unsafe} object does not have any method to check for bigger memory pages.
	 * 
	 * @param size       The number of bytes to allocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 * @throws UnsupportedOperationException Always.
	 */
	public static long u_allocate(final long size, final boolean contiguous) throws UnsupportedOperationException {
		log.trace("Allocating bigger memory page size using Unsafe object");
		throw new UnsupportedOperationException("Unsafe object does not provide any facility for this check");
	}

	/////////////////////////////////////////////////// SAFE METHODS ///////////////////////////////////////////////////

	/**
	 * Computes the page size of the host system.
	 * <p>
	 * This method delegates to {@link #u_pagesize()} or {@link #c_pagesize()} based on the value of {@link
	 * BuildConstants#UNSAFE}.
	 * 
	 * @return The page size of the system.
	 * @see #c_pagesize()
	 * @see #u_pagesize()
	 */
	public static int pagesize() {
		log.trace("Smart page size computation");
		return BuildConstants.UNSAFE && unsafe != null ? u_pagesize() : c_pagesize();
	}

	/**
	 * Computes the size of a memory address.
	 * <p>
	 * This method delegates to {@link #u_addrsize()} or {@link #c_addrsize()} based on the value of {@link
	 * BuildConstants#UNSAFE}.
	 * 
	 * @return The address size of the system.
	 * @see #c_addrsize()
	 * @see #u_addrsize()
	 */
	public static int addrsize() {
		log.trace("Smart virtual address size computation");
		return BuildConstants.UNSAFE && unsafe != null ? u_addrsize() : c_addrsize();
	}

	/**
	 * Computes the size of a bigger memory page.
	 * <p>
	 * This method uses the file system to detect the support of hugepage or delegates to {@link #c_hugepage()} in
	 * {@code Windows}, because it's only a function call. 
	 * 
	 * @return The size of a bigger memory page.
	 * @see #c_hugepage()
	 */
	public static long hugepage() {
		log.trace("Smart bigger page size computation");

		// If we are on Windows or forcing the C implementation, call the JNI method
		if (System.getProperty("os.name").toLowerCase().contains("win") || !BuildConstants.UNSAFE || unsafe == null) {
			return c_hugepage();
		}

		// Try parsing the file /etc/mtab
		var found = false;
		try (val mtab = new FileReader("/etc/mtab")) {
			val bufferedReader = new BufferedReader(mtab);
			for (var line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
				var _space = 0;
				var space  = line.indexOf(' ', _space);
				var word   = line.substring(_space, space);
				if (!word.equals("hugetlbfs")) continue; // Not the file system we want
				_space = space + 1;
				space  = line.indexOf(' ', _space);
				word   = line.substring(_space, space);
				if (!word.equals(BuildConstants.HUGE_MNT)) continue; // Not the mount point we want
				_space = space + 1;
				space  = line.indexOf(' ', _space);
				word   = line.substring(_space, space);
				if (!word.equals("hugetlbfs")) continue; // Not mount type we want
				found = true;
				break;
			}
		} catch (FileNotFoundException e) {
			log.error("The /etc/mtab cannot be found, maybe we screwed detecting the operative system", e);
		} catch (IOException e) {
			log.error("Error while reading or closing the file /etc/mtab", e);
		}
		if (!found) {
			return -1;
		}

		// Parse the /proc/meminfo
		try (val mtab = new FileReader("/proc/meminfo")) {
			val bufferedReader = new BufferedReader(mtab);
			for (var line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
				var _space = 0;
				var space  = line.indexOf(':', _space);
				var word   = line.substring(_space, space);
				if (!word.equals("Hugepagesize")) continue; // Not the key we want
				line  = line.substring(space + 1).trim();
				space = line.indexOf(' ', 0);
				var bytes = Long.parseLong(line.substring(0, space));
				val units = line.substring(space + 1).trim();
				switch (units) {
					case "GB":
						bytes *= 1024;
					case "MB":
						bytes *= 1024;
					case "kB":
						bytes *= 1024;
					case "B":
						break;
					default:
						return 0;
				}
				return bytes;
			}
		} catch (FileNotFoundException e) {
			log.error("The /proc/meminfo cannot be found, maybe we screwed detecting the operative system", e);
		} catch (IOException e) {
			log.error("Error while reading or closing the file /proc/meminfo", e);
		}
		return 0;
	}

	/**
	 * Allocates {@code size} bytes.
	 * <p>
	 * This method delegates to {@link #c_allocate()} since there is no other way to allocate memory using big memory
	 * pages in Java.
	 * 
	 * @param size       The number of bytes to allocate.
	 * @param contiguous If the allocated bytes should be contiguous.
	 * @return The base address of the allocated memory region.
	 */
	public static long allocate(long size, final boolean contiguous) {
		log.trace("Smart memory allocation");
		return c_allocate(size, contiguous);
	}

}
