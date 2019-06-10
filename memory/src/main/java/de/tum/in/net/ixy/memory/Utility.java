package de.tum.in.net.ixy.memory;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Collection of static methods extracted to reduce code duplication.
 *
 * @author Esaú García Sánchez-Torija
 */
enum Utility {
	;

	/**
	 * Grouped checks done to an address, a size, a buffer and an offset.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param address The memory address.
	 * @param bytes   The number of bytes.
	 * @param buffer  The buffer.
	 * @param offset  The offset.
	 * @return Whether the operation should stop.
	 * @see UnsafeMemoryManager
	 * @see JniMemoryManager
	 */
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	static boolean check(long address, int bytes, @Nullable byte[] buffer, int offset) {
		if (address == 0) throw new InvalidMemoryAddressException("address");
		if (bytes < 0) throw new InvalidSizeException("bytes");
		if (buffer == null) throw new InvalidBufferException("buffer");
		if (offset < 0 || offset >= buffer.length) throw new InvalidOffsetException("offset");
		return (bytes == 0);
	}

	/**
	 * Grouped checks done to two addresses and a size.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param addr1 The memory address.
	 * @param addr2 The memory address.
	 * @param bytes The number of bytes.
	 * @return Whether the operation should stop.
	 * @see UnsafeMemoryManager
	 * @see JniMemoryManager
	 */
	@Contract(pure = true)
	static boolean check(long addr1, long addr2, int bytes) {
		if (addr1 == 0) throw new InvalidMemoryAddressException("addr1");
		if (addr2 == 0) throw new InvalidMemoryAddressException("addr2");
		if (bytes < 0) throw new InvalidSizeException("bytes");
		return (bytes == 0 || addr1 == addr2);
	}

}
