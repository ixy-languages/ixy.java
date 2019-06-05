package de.tum.in.net.ixy.generic;

import lombok.val;

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
	 * Reads the memory pool id of the underlying packet buffer.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @return The memory pool id.
	 */
	int getMemoryPoolId();

	/**
	 * Returns the size of the packet.
	 *
	 * @return The size of the packet.
	 */
	int getSize();

	/**
	 * Sets the size of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param size The size of the packet.
	 */
	void setSize(final int size);

	/**
	 * Reads a {@code byte} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	byte getByte(final int offset);

	/**
	 * Reads a {@code byte} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	byte getByteVolatile(final int offset);
	
	/**
	 * Writes a {@code byte} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	void putByte(final int offset, final byte value);

	/**
	 * Writes a {@code byte} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	void putByteVolatile(final int offset, final byte value);

	/**
	 * Reads a {@code short} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	short getShort(final int offset);

	/**
	 * Reads a {@code short} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	short getShortVolatile(final int offset);

	/**
	 * Writes a {@code short} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	void putShort(final int offset, final short value);

	/**
	 * Writes a {@code short} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	void putShortVolatile(final int offset, final short value);
	
	/**
	 * Reads a {@code int} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	int getInt(final int offset);

	/**
	 * Reads a {@code int} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	int getIntVolatile(final int offset);

	/**
	 * Writes a {@code int} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	void putInt(final int offset, final int value);

	/**
	 * Writes a {@code int} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	void putIntVolatile(final int offset, final int value);

	/**
	 * Reads a {@code long} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	long getLong(final int offset);

	/**
	 * Reads a {@code long} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	long getLongVolatile(final int offset);

	/**
	 * Writes a {@code long} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	void putLong(final int offset, final long value);

	/**
	 * Writes a {@code long} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	void putLongVolatile(final int offset, final long value);

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	void get(final int offset, final int length, final byte[] buffer);

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @return A copy the packet segment.
	 */
	default byte[] get(final int offset, final int length) {
		val buffer = new byte[length];
		get(offset, length, buffer);
		return buffer;
	}

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	void getVolatile(final int offset, final int length, final byte[] buffer);

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @return A copy the packet segment.
	 */
	default byte[] getVolatile(final int offset, final int length) {
		val buffer = new byte[length];
		getVolatile(offset, length, buffer);
		return buffer;
	}

	/**
	 * Writes a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	void put(final int offset, final int length, final byte[] buffer);

	/**
	 * Writes a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	void putVolatile(final int offset, final int length, final byte[] buffer);

}
