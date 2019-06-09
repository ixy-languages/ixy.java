package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;

/**
 * Ixy's direct memory addresses specification.
 * <p>
 * The specification is based on the <em>principle of least knowledge</em> or <em>Law of Demeter</em> (LoD).
 * This means that only the methods needed to build a packet forwarder and generator will be exposed.
 * Any driver-dependent methods must be implemented and exposed in the driver's package and module.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDmaMemory {

	/**
	 * Returns the virtual address.
	 *
	 * @return The virtual address.
	 */
	@Contract(pure = true)
	long getVirtualAddress();

	/**
	 * Returns the physical address.
	 *
	 * @return The physical address.
	 */
	@Contract(pure = true)
	long getPhysicalAddress();

}
