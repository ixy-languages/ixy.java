package de.tum.in.net.ixy.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

import org.jetbrains.annotations.NotNull;

/**
 * String utils used only for debugging purposes.
 * <p>
 * The methods are not very optimized because debugging messages are not designed for high performance.
 *
 * @author Esaú García Sánchez-Torija
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Strings {

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Repeats the character {@code 0} {@code n} times.
	 *
	 * @param n The repetition count.
	 * @return The repeated character string.
	 */
	@SuppressWarnings({"MagicCharacter", "PMD.DataflowAnomalyAnalysis"})
	private static @NotNull String repeat(int n) {
		val buff = new char[n];
		while (n > 0) {
			buff[--n] = '0';
		}
		return new String(buff);
	}

	/**
	 * Pads a string on the left with the character {@code 0} until the length {@code n} is reached.
	 *
	 * @param str The string.
	 * @param n   The length.
	 * @return The padded string.
	 */
	public static @NotNull String leftPad(final @NotNull String str, final int n) {
		val length = str.length();
		return (length >= n) ? str : repeat(n - length) + str;
	}

	/**
	 * Converts a {@code byte} to a {@link String} and pads it on the left with the character {@code 0} until the length
	 * is appropriate.
	 *
	 * @param value The byte.
	 * @return The {@code 0} padded byte.
	 */
	public static @NotNull String leftPad(final byte value) {
		val str = Integer.toHexString(Byte.toUnsignedInt(value));
		return leftPad(str, Byte.BYTES * 2);
	}

	/**
	 * Converts a {@code short} to a {@link String} and pads it on the left with the character {@code 0} until the
	 * length is appropriate.
	 *
	 * @param value The short.
	 * @return The {@code 0} padded short.
	 */
	public static @NotNull String leftPad(final short value) {
		val str = Integer.toHexString(Short.toUnsignedInt(value));
		return leftPad(str, Short.BYTES * 2);
	}

	/**
	 * Converts a {@code int} to a {@link String} and pads it on the left with the character {@code 0} until the length
	 * is appropriate.
	 *
	 * @param value The int.
	 * @return The {@code 0} padded int.
	 */
	public static @NotNull String leftPad(final int value) {
		val str = Integer.toHexString(value);
		return leftPad(str, Integer.BYTES * 2);
	}

	/**
	 * Converts a {@code long} to a {@link String} and pads it on the left with the character {@code 0} until the length
	 * is appropriate.
	 *
	 * @param value The long.
	 * @return The {@code 0} padded long.
	 */
	public static @NotNull String leftPad(final long value) {
		val str = Long.toHexString(value);
		return leftPad(str, Long.BYTES * 2);
	}

}
