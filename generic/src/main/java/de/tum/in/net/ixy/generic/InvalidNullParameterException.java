package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when {@code null} is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidNullParameterException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -5748165912647099808L;

	/**
	 * Builds the error message given the parameter name which contains {@code null}.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	InvalidNullParameterException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is null", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}
