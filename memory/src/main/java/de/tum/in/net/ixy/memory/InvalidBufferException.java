package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when an invalid buffer is passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidBufferException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = -8823678074225048611L;

	/** Builds the error message. */
	InvalidBufferException() {
		super("A parameter is an invalid buffer");
	}

}