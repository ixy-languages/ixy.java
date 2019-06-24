package de.tum.in.net.ixy.generic;

import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Ixy's packet specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface IxyPacketBuffer {

	/**
	 * Returns the virtual address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of virtual memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The virtual address.
	 */
	@Contract(pure = true)
	long getVirtualAddress();

	/**
	 * Returns the physical memory address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of physical memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The physical memory address.
	 */
	@Contract(pure = true)
	long getPhysicalAddress();

	/**
	 * Reads the memory pool id of the underlying packet buffer.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @return The memory pool id.
	 */
	@Contract(pure = true)
	int getMemoryPoolId();

	/**
	 * Returns the size of the packet.
	 *
	 * @return The size of the packet.
	 */
	@Contract(pure = true)
	int getSize();

	/**
	 * Sets the size of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param size The size of the packet.
	 */
	@Contract(pure = true)
	void setSize(int size);

	/**
	 * Reads a {@code byte} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByte(int offset);

	/**
	 * Reads a {@code byte} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByteVolatile(int offset);
	
	/**
	 * Writes a {@code byte} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByte(int offset, byte value);

	/**
	 * Writes a {@code byte} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByteVolatile(int offset, byte value);

	/**
	 * Replaces a {@code byte} from the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndPutByte(int offset, byte value) {
		val old = getByte(offset);
		putByte(offset, value);
		return old;
	}

	/**
	 * Replaces a {@code byte} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndPutByteVolatile(int offset, byte value) {
		val old = getByteVolatile(offset);
		putByteVolatile(offset, value);
		return old;
	}

	/**
	 * Adds a {@code byte} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 */
	@Contract(pure = true)
	default void addByte(int offset, byte value) {
		val old = getByte(offset);
		putByte(offset, (byte) (old + value));
	}

	/**
	 * Adds a {@code byte} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 */
	@Contract(pure = true)
	default void addByteVolatile(int offset, byte value) {
		val old = getByteVolatile(offset);
		putByteVolatile(offset, (byte) (old + value));
	}

	/**
	 * Adds a {@code byte} to the packet data and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndAddByte(int offset, byte value) {
		val old = getByte(offset);
		putByte(offset, (byte) (old + value));
		return old;
	}

	/**
	 * Adds a {@code byte} to the packet data using volatile memory addresses and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default byte getAndAddByteVolatile(int offset, byte value) {
		val old = getByteVolatile(offset);
		putByteVolatile(offset, (byte) (old + value));
		return old;
	}

	/**
	 * Adds a {@code byte} to the packet data and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default byte addAndGetByte(int offset, byte value) {
		val old = getByte(offset);
		val neu = (byte) (old + value);
		putByte(offset, neu);
		return neu;
	}

	/**
	 * Adds a {@code byte} to the packet data using volatile memory addresses and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default byte addAndGetByteVolatile(int offset, byte value) {
		val old = getByteVolatile(offset);
		val neu = (byte) (old + value);
		putByteVolatile(offset, neu);
		return neu;
	}

	/**
	 * Reads a {@code short} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShort(int offset);

	/**
	 * Reads a {@code short} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShortVolatile(int offset);

	/**
	 * Writes a {@code short} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShort(int offset, short value);

	/**
	 * Writes a {@code short} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShortVolatile(int offset, short value);

	/**
	 * Replaces a {@code short} from the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndPutShort(int offset, short value) {
		val old = getShort(offset);
		putShort(offset, value);
		return old;
	}

	/**
	 * Replaces a {@code short} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndPutShortVolatile(int offset, short value) {
		val old = getShortVolatile(offset);
		putShortVolatile(offset, value);
		return old;
	}

	/**
	 * Adds a {@code short} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 */
	@Contract(pure = true)
	default void addShort(int offset, short value) {
		val old = getShort(offset);
		putShort(offset, (short) (old + value));
	}

	/**
	 * Adds a {@code short} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 */
	@Contract(pure = true)
	default void addShortVolatile(int offset, short value) {
		val old = getShortVolatile(offset);
		putShortVolatile(offset, (short) (old + value));
	}

	/**
	 * Adds a {@code short} to the packet data and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndAddShort(int offset, short value) {
		val old = getShort(offset);
		putShort(offset, (short) (old + value));
		return old;
	}

	/**
	 * Adds a {@code short} to the packet data using volatile memory addresses and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default short getAndAddShortVolatile(int offset, short value) {
		val old = getShortVolatile(offset);
		putShortVolatile(offset, (short) (old + value));
		return old;
	}

	/**
	 * Adds a {@code short} to the packet data and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default short addAndGetShort(int offset, short value) {
		val old = getShort(offset);
		val neu = (short) (old + value);
		putShort(offset, neu);
		return neu;
	}

	/**
	 * Adds a {@code short} to the packet data using volatile memory addresses and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default short addAndGetShortVolatile(int offset, short value) {
		val old = getShortVolatile(offset);
		val neu = (short) (old + value);
		putShortVolatile(offset, neu);
		return neu;
	}
	
	/**
	 * Reads a {@code int} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getInt(int offset);

	/**
	 * Reads a {@code int} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getIntVolatile(int offset);

	/**
	 * Writes a {@code int} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	@Contract(pure = true)
	void putInt(int offset, int value);

	/**
	 * Writes a {@code int} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	@Contract(pure = true)
	void putIntVolatile(int offset, int value);

	/**
	 * Replaces a {@code int} from the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndPutInt(int offset, int value) {
		val old = getInt(offset);
		putInt(offset, value);
		return old;
	}

	/**
	 * Replaces a {@code int} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndPutIntVolatile(int offset, int value) {
		val old = getIntVolatile(offset);
		putIntVolatile(offset, value);
		return old;
	}

	/**
	 * Adds a {@code int} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 */
	@Contract(pure = true)
	default void addInt(int offset, int value) {
		val old = getInt(offset);
		putInt(offset, old + value);
	}

	/**
	 * Adds a {@code int} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 */
	@Contract(pure = true)
	default void addIntVolatile(int offset, int value) {
		val old = getIntVolatile(offset);
		putIntVolatile(offset, old + value);
	}

	/**
	 * Adds a {@code int} to the packet data and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndAddInt(int offset, int value) {
		val old = getInt(offset);
		putInt(offset, old + value);
		return old;
	}

	/**
	 * Adds a {@code int} to the packet data using volatile memory addresses and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default int getAndAddIntVolatile(int offset, int value) {
		val old = getIntVolatile(offset);
		putIntVolatile(offset, old + value);
		return old;
	}

	/**
	 * Adds a {@code int} to the packet data and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default int addAndGetInt(int offset, int value) {
		val old = getInt(offset);
		val neu = old + value;
		putInt(offset, neu);
		return neu;
	}

	/**
	 * Adds a {@code int} to the packet data using volatile memory addresses and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default int addAndGetIntVolatile(int offset, int value) {
		val old = getIntVolatile(offset);
		val neu = old + value;
		putIntVolatile(offset, neu);
		return neu;
	}

	/**
	 * Reads a {@code long} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLong(int offset);

	/**
	 * Reads a {@code long} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLongVolatile(int offset);

	/**
	 * Writes a {@code long} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLong(int offset, long value);

	/**
	 * Writes a {@code long} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLongVolatile(int offset, long value);

	/**
	 * Replaces a {@code long} from the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndPutLong(int offset, long value) {
		val old = getLong(offset);
		putLong(offset, value);
		return old;
	}

	/**
	 * Replaces a {@code long} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndPutLongVolatile(int offset, long value) {
		val old = getLongVolatile(offset);
		putLongVolatile(offset, value);
		return old;
	}

	/**
	 * Adds a {@code long} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 */
	@Contract(pure = true)
	default void addLong(int offset, long value) {
		val old = getLong(offset);
		putLong(offset, old + value);
	}

	/**
	 * Adds a {@code long} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 */
	@Contract(pure = true)
	default void addLongVolatile(int offset, long value) {
		val old = getLongVolatile(offset);
		putLongVolatile(offset, old + value);
	}

	/**
	 * Adds a {@code long} to the packet data and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndAddLong(int offset, long value) {
		val old = getLong(offset);
		putLong(offset, old + value);
		return old;
	}

	/**
	 * Adds a {@code long} to the packet data using volatile memory addresses and returns the old value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 * @return The old value.
	 */
	@Contract(pure = true)
	default long getAndAddLongVolatile(int offset, long value) {
		val old = getLongVolatile(offset);
		putLongVolatile(offset, old + value);
		return old;
	}

	/**
	 * Adds a {@code long} to the packet data and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default long addAndGetLong(int offset, long value) {
		val old = getLong(offset);
		val neu = old + value;
		putLong(offset, neu);
		return neu;
	}

	/**
	 * Adds a {@code long} to the packet data using volatile memory addresses and returns the new value.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to add.
	 * @return The new value.
	 */
	@Contract(pure = true)
	default long addAndGetLongVolatile(int offset, long value) {
		val old = getLongVolatile(offset);
		val neu = old + value;
		putLongVolatile(offset, neu);
		return neu;
	}

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(mutates = "param3")
	void get(int offset, int bytes, @NotNull byte[] buffer);

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @return A copy the packet segment.
	 */
	@Contract(pure = true)
	default @NotNull byte[] get(int offset, int bytes) {
		if (!BuildConfig.OPTIMIZED) {
			if (bytes < 0) {
				throw new IllegalArgumentException("The parameter 'bytes' is an invalid size");
			}
		}
		val buffer = new byte[bytes];
		get(offset, bytes, buffer);
		return buffer;
	}

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(value = "_, _, null -> fail", mutates = "param3")
	void getVolatile(int offset, int bytes, @NotNull byte[] buffer);

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @return A copy the packet segment.
	 */
	@Contract(pure = true)
	default @NotNull byte[] getVolatile(int offset, int bytes) {
		if (!BuildConfig.OPTIMIZED) {
			if (bytes < 0) {
				throw new IllegalArgumentException("The parameter 'bytes' is an invalid size");
			}
		}
		val buffer = new byte[bytes];
		getVolatile(offset, bytes, buffer);
		return buffer;
	}

	/**
	 * Writes a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(pure = true)
	void put(int offset, int bytes, @NotNull byte[] buffer);

	/**
	 * Writes a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(pure = true)
	void putVolatile(int offset, int bytes, @NotNull byte[] buffer);

}
