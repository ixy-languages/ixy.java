package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when {@code null} is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidNullParameterException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = 7832836913711210110L;

	/** Builds the error message. */
	InvalidNullParameterException() {
		super("A parameter is null");
	}

	/**
	 * Builds the error message given the parameter name which contains {@code null}.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@SuppressWarnings("HardCodedStringLiteral")
	InvalidNullParameterException(String parameter) {
		super(String.format("The parameter '%s' is null", parameter));
	}

}
