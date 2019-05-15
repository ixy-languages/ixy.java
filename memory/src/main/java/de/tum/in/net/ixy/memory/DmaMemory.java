package de.tum.in.net.ixy.memory;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This data class holds a memory address in two different formats, virtual and physical.
 * <p>
 * This class does not offer any manipulation methods and is an immutable data class. It is meant to be used to simply
 * keep track of virtual address and their corresponding physical memory.
 */
public final class DmaMemory {

	/** The virtual address. */
	@Setter(AccessLevel.NONE)
	private long virtual;

	/** The physical address. */
	@Setter(AccessLevel.NONE)
	private long physical;

	/**
	 * Constructs a new instance with the given {@code virtual} and {@code physical} addresses.
	 * 
	 * @param virtual  The virtual address.
	 * @param physical The physical address.
	 */
	public DmaMemory(final long virtual, final long physical) {
		this.virtual  = virtual;
		this.physical = physical;
	}

}
