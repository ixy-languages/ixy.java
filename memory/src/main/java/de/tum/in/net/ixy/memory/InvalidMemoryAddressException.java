package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when an invalid memory address is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidMemoryAddressException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -2119042123555344311L;

	/** Builds the error message. */
	InvalidMemoryAddressException() {
		super("A parameter is an invalid memory address");
	}

	/**
	 * Builds the error message given the parameter name which contains the invalid memory address.
	 *
	 * @param parameter The name of the parameter that is wrong.
	 */
	@SuppressWarnings("HardCodedStringLiteral")
	InvalidMemoryAddressException(String parameter) {
		super(String.format("The parameter '%s' is an invalid memory address", parameter));
	}

}
