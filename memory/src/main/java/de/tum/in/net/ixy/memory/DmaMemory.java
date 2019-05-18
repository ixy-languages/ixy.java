package de.tum.in.net.ixy.memory;

import lombok.Getter;

/**
 * This data class holds a memory address in two different formats, virtual and physical.
 * <p>
 * This class does not offer any manipulation methods and is an immutable data class. It is meant to be used to simply
 * keep track of virtual address and their corresponding physical counterpart.
 */
public final class DmaMemory {

	/** The virtual address. */
	@Getter
	private long virtual;

	/** The physical address. */
	@Getter
	private long physical;

	/**
	 * Constructs a new instance with the given {@code virtual} and {@code physical} addresses.
	 * 
	 * @param virtual  The virtual address.
	 * @param physical The physical address.
	 */
	public DmaMemory(final long virtual, final long physical) {
		this.virtual = virtual;
		this.physical = physical;
	}

}
