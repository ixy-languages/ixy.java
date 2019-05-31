package de.tum.in.net.ixy.generic;

/**
 * Minimum functionality a packet should expose.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyPacketBuffer {

	/**
	 * Returns the virtual address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of virtual memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The virtual address.
	 */
	long getVirtualAddress();

	/**
	 * Returns the physical address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of physical memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The physical address.
	 */
	long getPhysicalAddress();

	/**
	 * Returns the offset of the header.
	 * <p>
	 * The offset plus the base address, either virtual or physical, should correspond to the first writable byte of the
	 * header buffer.
	 * <p>
	 * Normally, the header is at the beginning of the packet, so this method is already implemented and returns {@code
	 * 0}.
	 *
	 * @return The header offset.
	 */
	default long getHeaderOffset() {
		return 0;
	}

	/**
	 * Returns the offset of the data.
	 * <p>
	 * The offset plus the base address, either virtual or physical, should correspond to the first writable byte of the
	 * data buffer.
	 *
	 * @return The data offset.
	 */
	long getDataOffset();

	/**
	 * Returns the size of the packet.
	 *
	 * @return The size of the packet.
	 */
	int getSize();

	/**
	 * Sets the size of the packet.
	 *
	 * @param size The size of the packet.
	 */
	void setSize(final int size);

}
