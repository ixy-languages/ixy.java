package de.tum.in.net.ixy.memory;

/**
 * Exception thrown when two overlapping memory regions are passed as a parameter.
 *
 * @author Esaú García Sánchez-Torija
 */
final class OverlappingMemoryRegionsException extends IllegalArgumentException {

	/** Serial used for serialization purposes. */
	private static final long serialVersionUID = 746184939792981895L;

	/** Builds the error message. */
	OverlappingMemoryRegionsException() {
		super("Two memory regions are overlapping");
	}

	/**
	 * Builds the error message given the names of the parameters which contains the overlapping memory regions.
	 *
	 * @param parameter1 The name of the parameter that is overlapping.
	 * @param parameter2 The name of the parameter that is overlapping.
	 */
	@SuppressWarnings("HardCodedStringLiteral")
	OverlappingMemoryRegionsException(String parameter1, String parameter2) {
		super(String.format("The parameters '%s' and '%s' are overlapping memory regions", parameter1, parameter2));
	}

}
