package de.tum.in.net.ixy.memory;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an invalid buffer is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
public final class InvalidBufferException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -8823678074225048611L;

	/**
	 * Builds the error message given the parameter name which contains the invalid buffer.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@Contract("null -> fail")
	public InvalidBufferException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is an invalid size", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}
