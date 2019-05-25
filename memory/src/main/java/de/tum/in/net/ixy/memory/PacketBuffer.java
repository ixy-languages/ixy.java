package de.tum.in.net.ixy.memory;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper for a packet buffer without any memory layout constraints.
 * <p>
 * The underlying memory layout is:
 * <ul>
 *   <li>Physical Address Pointer (32/64 bits).</li>
 *   <li>Memory pool pointer (32/64 bits).</li>
 *   <li>Memory pool index (32 bits).</li>
 *   <li>Packet size (32 bits).</li>
 *   <li>Headroom (variable).</li>
 * </ul>
 * The headroom has a size that makes the total size of the structure 64 bytes long.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class PacketBuffer {

	/** The offset of the physical address pointer. */
	private static final int PAP_OFFSET = 0;

	/** The size of the physical address pointer. */
	private static final int PAP_BYTES = Memory.getAddressSize() / 8;

	/** The offset of the memory pool pointer. */
	private static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The size of the memory pool pointer. */
	private static final int MPP_BYTES = Memory.getAddressSize() / 8;

	/** The offset of the memory pool index. */
	private static final int MPI_OFFSET = MPP_OFFSET + MPP_BYTES;

	/** The size of the memory pool index. */
	private static final int MPI_BYTES = 32 / 8;

	/** The offset of the packet size. */
	private static final int PKT_OFFSET = MPI_OFFSET + MPI_BYTES;

	/** The size of the packet size. */
	private static final int PKT_BYTES = 32 / 8;

	/** The offset of the extra header room. */
	private static final int HED_OFFSET = PKT_OFFSET + PKT_BYTES;

	/** The size of the extra header size. */
	private static final int HED_BYTES = 64 - HED_OFFSET;

	/** The offset to start writing the data of the packet. */
	private static final int DATA_OFFSET = HED_OFFSET + HED_BYTES;

	/**
	 * The base address of the real packet buffer.
	 * ---------------------- GETTER ----------------------
	 * Returns the base address of the real packet buffer.
	 *
	 * @return The base address of the real packet buffer.
	 */
	@Getter
	private long baseAddress;

	/**
	 * Creates a new instance that wraps a real packet buffer.
	 *
	 * @param address The base address of the packet buffer.
	 */
	public PacketBuffer(final long address) {
		if (BuildConfig.DEBUG) log.trace("Instantiating packet buffer with address 0x{}", Long.toHexString(address));
		baseAddress = address;
	}

	/**
	 * Checks if the base address is valid.
	 * <p>
	 * The check performed does not try to guess if the memory address is accessible but if it is not {@code 0}.
	 *
	 * @return The validity of this instance.
	 */
	public boolean isValid() {
		return baseAddress != 0;
	}

	/**
	 * Checks if the base address is invalid.
	 * <p>
	 * The check performed does not try to guess if the memory address is accessible but if it is {@code 0}.
	 *
	 * @return The invalidity of this instance.
	 */
	public boolean isInvalid() {
		return baseAddress == 0;
	}

	///////////////////////////////////////////////////// GETTERS //////////////////////////////////////////////////////

	/**
	 * Reads the physical address pointer of the underlying packet buffer.
	 *
	 * @return The physical address pointer.
	 */
	public long getPhysicalAddress() {
		if (BuildConfig.DEBUG) log.trace("Reading physical address pointer");
		return Memory.getAddressSize() == 64
				? Memory.getLong(baseAddress + PAP_OFFSET)
				: Memory.getInt(baseAddress + PAP_OFFSET);
	}

	/**
	 * Reads the memory pool address of the underlying packet buffer.
	 *
	 * @return The memory pool address.
	 */
	public long getMemoryPoolAddress() {
		if (BuildConfig.DEBUG) log.trace("Reading memory pool address");
		return Memory.getAddressSize() == 64
				? Memory.getLong(baseAddress + MPP_OFFSET)
				: Memory.getInt(baseAddress + MPP_OFFSET);
	}

	/**
	 * Reads the memory pool id of the underlying packet buffer.
	 *
	 * @return The memory pool id.
	 */
	public int getMemoryPoolId() {
		if (BuildConfig.DEBUG) log.trace("Reading memory pool id");
		return Memory.getInt(baseAddress + MPI_OFFSET);
	}

	/**
	 * Reads the size of the underlying packet buffer.
	 *
	 * @return The size.
	 */
	public int getSize() {
		if (BuildConfig.DEBUG) log.trace("Reading packet buffer size");
		return Memory.getInt(baseAddress + PKT_OFFSET);
	}

	/**
	 * Reads a byte from the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start reading from.
	 * @return The byte.
	 */
	public byte getByte(final long offset) {
		if (BuildConfig.DEBUG) log.trace("Reading data byte with offset {}", offset);
		return Memory.getByte(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a short from the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start reading from.
	 * @return The short.
	 */
	public short getShort(final long offset) {
		if (BuildConfig.DEBUG) log.trace("Reading data short with offset {}", offset);
		return Memory.getShort(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a int from the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start reading from.
	 * @return The int.
	 */
	public int getInt(final long offset) {
		if (BuildConfig.DEBUG) log.trace("Reading data int with offset {}", offset);
		return Memory.getInt(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a long from the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start reading from.
	 * @return The long.
	 */
	public long getLong(final long offset) {
		if (BuildConfig.DEBUG) log.trace("Reading data long with offset {}", offset);
		return Memory.getLong(baseAddress + DATA_OFFSET + offset);
	}

	///////////////////////////////////////////////////// SETTERS //////////////////////////////////////////////////////

	/**
	 * Writes the physical address pointer to the underlying packet buffer.
	 *
	 * @param address The physical address pointer to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer setPhysicalAddress(final long address) {
		if (BuildConfig.DEBUG) log.trace("Writing physical address pointer 0x{}", Long.toHexString(address));
		if (Memory.getAddressSize() == Long.SIZE) {
			Memory.putLong(baseAddress + PAP_OFFSET, address);
		} else {
			Memory.putInt(baseAddress + PAP_OFFSET, (int) address);
		}
		return this;
	}

	/**
	 * Writes the memory pool address to the underlying packet buffer.
	 *
	 * @param address The memory pool address to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer setMemoryPoolAddress(final long address) {
		if (BuildConfig.DEBUG) log.trace("Writing memory pool address 0x{}", Long.toHexString(address));
		if (Memory.getAddressSize() == Long.SIZE) {
			Memory.putLong(baseAddress + MPP_OFFSET, address);
		} else {
			Memory.putInt(baseAddress + MPP_OFFSET, (int) address);
		}
		return this;
	}

	/**
	 * Writes the memory pool id to the underlying packet buffer.
	 *
	 * @param index The memory pool id to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer setMemoryPoolId(final int index) {
		if (BuildConfig.DEBUG) log.trace("Writing memory pool id 0x{}", Integer.toHexString(index));
		Memory.putInt(baseAddress + MPI_OFFSET, index);
		return this;
	}

	/**
	 * Writes the size to the underlying packet buffer.
	 *
	 * @param size The size to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer setSize(final int size) {
		if (BuildConfig.DEBUG) log.trace("Writing packet buffer size 0x{}", Integer.toHexString(size));
		Memory.putInt(baseAddress + PKT_OFFSET, size);
		return this;
	}

	/**
	 * Writes a byte to the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The byte to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer putByte(final long offset, final byte value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			log.trace("Writing data byte 0x{} with offset {}", xvalue, offset);
		}
		Memory.putByte(baseAddress + DATA_OFFSET + offset, value);
		return this;
	}

	/**
	 * Writes a short to the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The short to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer putShort(final long offset, final short value) {
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			log.trace("Writing data short 0x{} with offset {}", xvalue, offset);
		}
		Memory.putShort(baseAddress + DATA_OFFSET + offset, value);
		return this;
	}

	/**
	 * Writes an int to the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The int to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer putInt(final long offset, final int value) {
		if (BuildConfig.DEBUG) {
			log.trace("Writing data short 0x{} with offset {}", Integer.toHexString(value), offset);
		}
		Memory.putInt(baseAddress + DATA_OFFSET + offset, value);
		return this;
	}

	/**
	 * Writes a long to the data section of the underlying packet buffer.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The long to write.
	 * @return This packet buffer.
	 */
	public PacketBuffer putLong(final long offset, final long value) {
		if (BuildConfig.DEBUG) {
			log.trace("Writing data short 0x{} with offset {}", Long.toHexString(value), offset);
		}
		Memory.putLong(baseAddress + DATA_OFFSET + offset, value);
		return this;
	}

	/**
	 * Returns an empty useless instances.
	 *
	 * @return A dummy instance.
	 */
	public static PacketBuffer empty() {
		if (BuildConfig.DEBUG) log.trace("Creating dummy packet buffer");
		return new PacketBuffer(0);
	}

}
