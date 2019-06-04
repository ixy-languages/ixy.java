package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * A simple implementation of a dual memory address (virtualAddress and physicalAddress).
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class DmaMemory implements IxyDmaMemory {

	/**
	 * The virtual address.
	 * -------- GETTER --------
	 * Returns virtual address.
	 *
	 * @return The virtual address.
	 */
	@Getter
	private long virtualAddress;

	/**
	 * The physical address.
	 * ----------- GETTER -----------
	 * Returns the physical address.
	 *
	 * @return The physical address.
	 */
	@Getter
	private long physicalAddress;

	/**
	 * Constructs a new instance with the given {@code virtual} and {@code physical} addresses.
	 *
	 * @param virtual  The virtual address.
	 * @param physical The physical address.
	 */
	public DmaMemory(final long virtual, final long physical) {
		if (BuildConfig.DEBUG) {
			val xvirtual = Long.toHexString(virtual);
			val xphysical = Long.toHexString(physical);
			log.debug("Creating DmaMemory [virt=0x{}, phys=0x{}]", xvirtual, xphysical);
		}
		virtualAddress = virtual;
		physicalAddress = physical;
	}

}
