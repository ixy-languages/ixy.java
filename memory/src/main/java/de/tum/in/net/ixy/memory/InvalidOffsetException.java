package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when an invalid offset is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidOffsetException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -1473824312920979353L;

	/** Builds the error message. */
	InvalidOffsetException() {
		super("A parameter is an invalid offset");
	}

}
