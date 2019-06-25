package de.tum.in.net.ixy.ixgbe;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an invalid offset is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidOffsetException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -1244797029030629273L;

	/**
	 * Builds the error message given the parameter name which contains the invalid offset.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	InvalidOffsetException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is an invalid offset", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}
