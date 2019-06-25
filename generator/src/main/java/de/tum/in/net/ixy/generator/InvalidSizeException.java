package de.tum.in.net.ixy.generator;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an invalid size is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidSizeException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -2321976395455129161L;

	/**
	 * Builds the error message given the parameter name which contains the invalid size.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	InvalidSizeException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is an invalid size", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}