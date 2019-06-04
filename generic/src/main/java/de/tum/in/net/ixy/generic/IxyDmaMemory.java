package de.tum.in.net.ixy.generic;

/**
 * Represents a memory address in its two states: virtual and physical.
 * <p>
 * Both addresses should have the same offset given a page size, but no checks are enforced as that depends on {@link
 * IxyMemoryManager#pageSize()} or {@link IxyMemoryManager#hugepageSize()} and loosely coupled programming is a good
 * practice.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDmaMemory {

	/**
	 * Returns the virtual address.
	 *
	 * @return The virtual address.
	 */
	long getVirtualAddress();

	/**
	 * Returns the physical address.
	 *
	 * @return The physical address.
	 */
	long getPhysicalAddress();

}
