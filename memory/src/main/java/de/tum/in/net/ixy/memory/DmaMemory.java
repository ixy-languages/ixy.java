package de.tum.in.net.ixy.memory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Data class for memory addresses in two different formats, virtual and physical.
 * <p>
 * This class does not offer any manipulation methods and is an immutable data class. It is meant to be used to simply
 * keep track of virtual address and their corresponding physical counterpart.
 *
 * @author Esaú García Sánchez-Torija
 * @see Memory#allocateDma(long, boolean)
 */
@Slf4j
public final class DmaMemory {

	/**
	 * The virtual address.
	 * -------- GETTER --------
	 * Returns virtual address.
	 *
	 * @return The virtual address.
	 */
	@Getter
	private long virtual;

	/**
	 * The physical address.
	 * ----------- GETTER -----------
	 * Returns the physical address.
	 *
	 * @return The physical address.
	 */
	@Getter
	private long physical;

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
		this.virtual = virtual;
		this.physical = physical;
	}

}
