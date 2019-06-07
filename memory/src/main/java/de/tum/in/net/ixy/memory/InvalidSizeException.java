package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when an invalid size is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidSizeException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -2321976395455129161L;

	/** Builds the error message. */
	InvalidSizeException() {
		super("A parameter is an invalid size");
	}

	/**
	 * Builds the error message given the parameter name which contains the invalid size.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@SuppressWarnings("HardCodedStringLiteral")
	InvalidSizeException(String parameter) {
		super(String.format("The parameter '%s' is an invalid size", parameter));
	}

}
