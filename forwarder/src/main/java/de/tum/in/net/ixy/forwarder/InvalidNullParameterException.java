package de.tum.in.net.ixy.forwarder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when {@code null} is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
public final class InvalidNullParameterException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = 7832836913711210110L;

	/**
	 * Builds the error message given the parameter name which contains {@code null}.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@Contract("null -> fail")
	public InvalidNullParameterException(@NotNull String parameter) {
		super(String.format("The parameter '%s' is null", parameter));
		if (!BuildConfig.OPTIMIZED && (parameter == null || parameter.isBlank())) {
			throw new IllegalArgumentException("The exception message could not be constructed correctly");
		}
	}

}
