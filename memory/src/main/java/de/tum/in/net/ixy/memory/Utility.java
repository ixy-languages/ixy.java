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
	 * @param size    The size.
	 * @param buffer  The buffer.
	 * @param offset  The offset.
	 * @return Whether the operation should continue.
	 * @see UnsafeMemoryManager
	 * @see JniMemoryManager
	 */
	@SuppressWarnings("HardCodedStringLiteral")
	@Contract(value = "_, _, null, _ -> fail", pure = true)
	static boolean check(long address, int size, @Nullable byte[] buffer, int offset) {
		if (address == 0) throw new InvalidMemoryAddressException();
		if (size < 0) throw new InvalidSizeException();
		if (buffer == null) throw new InvalidBufferException();
		if (offset < 0 || offset >= buffer.length) throw new InvalidOffsetException();
		return (size > 0);
	}

	/**
	 * Grouped checks done to two addresses and a size.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param addr1 The memory address.
	 * @param addr2 The memory address.
	 * @param size  The size.
	 * @return Whether the operation should continue.
	 * @see UnsafeMemoryManager
	 * @see JniMemoryManager
	 */
	@Contract(pure = true)
	static boolean check(long addr1, long addr2, int size) {
		if (addr1 == 0 || addr2 == 0) throw new InvalidMemoryAddressException();
		if (size < 0) throw new InvalidSizeException();
		return (size > 0 && addr1 != addr2);
	}

}