package de.tum.in.net.ixy.ixgbe;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an invalid memory address is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
public final class InvalidMemoryAddressException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -2119042123555344311L;

	/**
	 * Builds the error message given the parameter name which contains the invalid memory address.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@Contract("null -> fail")
	public InvalidMemoryAddressException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is an invalid memory address", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}
