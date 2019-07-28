package de.tum.in.net.ixy.memory;

import lombok.Value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Immutable data class for virtual and physical addresses.
 *
 * @author Esaú García Sánchez-Torija
 */
@Value
@SuppressWarnings("JavaDoc")
public final class DmaMemory {

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * The virtual address.
	 * -- GETTER --
	 * Returns the virtual address.
	 *
	 * @return The virtual address.
	 */
	private final long virtual;

	/**
	 * The physical address.
	 * -- GETTER --
	 * Returns the physical address.
	 *
	 * @return The physical address.
	 */
	private final long physical;

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public @NotNull String toString() {
		return "DmaMemory"
				+ '('
				+ "virtual=0x" + leftPad(virtual) + ", "
				+ "physical=0x" + leftPad(physical)
				+ ')';
	}

}
