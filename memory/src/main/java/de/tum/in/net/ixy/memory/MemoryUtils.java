package de.tum.in.net.ixy.memory;

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
		if (BuildConstants.UNSAFE) {
			pagesize = unsafe.pageSize();
		} else {
			pagesize = c_pagesize();
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

}
