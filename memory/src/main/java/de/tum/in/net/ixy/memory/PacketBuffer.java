package de.tum.in.net.ixy.memory;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper for the buffer of a packet without any memory layout constraints.
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
 */
@Slf4j
public final class PacketBuffer {

	/** The offset of the physical address pointer. */
	public static final int PAP_OFFSET = 0;

	/** The size of the physical address pointer. */
	public static final int PAP_BYTES = MemoryUtils.getAddrsize() / 8;

	/** The offset of the memory pool pointer. */
	public static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The size of the memory pool pointer. */
	public static final int MPP_BYTES = MemoryUtils.getAddrsize() / 8;

	/** The offset of the memory pool index. */
	public static final int MPI_OFFSET = MPP_OFFSET + MPP_BYTES;

	/** The size of the memory pool index. */
	public static final int MPI_BYTES = 32 / 8;

	/** The offset of the packet size. */
	public static final int PKT_OFFSET = MPI_OFFSET + MPI_BYTES;

	/** The size of the packet size. */
	public static final int PKT_BYTES = 32 / 8;

	/** The offset of the extra header room. */
	public static final int HED_OFFSET = PKT_OFFSET + PKT_BYTES;

	/** The size of the extra header size. */
	public static final int HED_BYTES = 64 - HED_OFFSET;

	/** The offset to start writing the data of the packet. */
	public static final int DATA_OFFSET = HED_OFFSET + HED_BYTES;

	/** Stores the base address of the real packet buffer. */
	@Getter
	private long baseAddress;

	/**
	 * Creates a new instance that wraps a real packet buffer.
	 * 
	 * @param address The base address of the packet buffer.
	 */
	public PacketBuffer(final long address) {
		if (BuildConstants.DEBUG) log.trace("Instantiating packet buffer with address 0x{}", Long.toHexString(address));
		baseAddress = address;
	}

	/** Checks if the base address is valid. */
	public boolean isValid() {
		return baseAddress != 0; 
	}

	/** Checks if the base address is invalid. */
	public boolean isInvalid() {
		return baseAddress == 0; 
	}

	/**
	 * Reads the physical address pointer of the underlying packet buffer.
	 * 
	 * @return The physical address pointer.
	 */
	public long getPhysicalAddress() {
		if (BuildConstants.DEBUG) log.trace("Reading physical address pointer");
		return MemoryUtils.getAddrsize() == 64
				? MemoryUtils.getLong(baseAddress + PAP_OFFSET)
				: MemoryUtils.getInt(baseAddress + PAP_OFFSET);
	}

	/**
	 * Writes the physical address pointer to the underlying packet buffer.
	 * 
	 * @param address The physical address pointer to write.
	 */
	public void setPhysicalAddress(final long address) {
		if (BuildConstants.DEBUG) log.trace("Writing physical address pointer 0x{}", Long.toHexString(address));
		if (MemoryUtils.getAddrsize() == 64) {
			MemoryUtils.putLong(baseAddress + PAP_OFFSET, address);
		} else {
			MemoryUtils.putInt(baseAddress + PAP_OFFSET, (int) address);
		}
	}

	/**
	 * Reads the memory pool address of the underlying packet buffer.
	 * 
	 * @return The memory pool address.
	 */
	public long getMemoryPoolAddress() {
		if (BuildConstants.DEBUG) log.trace("Reading memory pool address");
		return MemoryUtils.getAddrsize() == 64
				? MemoryUtils.getLong(baseAddress + MPP_OFFSET)
				: MemoryUtils.getInt(baseAddress + MPP_OFFSET);
	}

	/**
	 * Writes the memory pool address to the underlying packet buffer.
	 * 
	 * @param address The memory pool address to write.
	 */
	public void setMemoryPoolAddress(final long address) {
		if (BuildConstants.DEBUG) log.trace("Writing memory pool address 0x{}", Long.toHexString(address));
		if (MemoryUtils.getAddrsize() == 64) {
			MemoryUtils.putLong(baseAddress + MPP_OFFSET, address);
		} else {
			MemoryUtils.putInt(baseAddress + MPP_OFFSET, (int) address);
		}
	}

	/**
	 * Reads the memory pool id of the underlying packet buffer.
	 * 
	 * @return The memory pool id.
	 */
	public int getMemoryPoolId() {
		if (BuildConstants.DEBUG) log.trace("Reading memory pool id");
		return MemoryUtils.getInt(baseAddress + MPI_OFFSET);
	}

	/**
	 * Writes the memory pool id to the underlying packet buffer.
	 * 
	 * @param index The memory pool id to write.
	 */
	public void setMemoryPoolId(final int index) {
		if (BuildConstants.DEBUG) log.trace("Writing memory pool id 0x{}", Integer.toHexString(index));
		MemoryUtils.putInt(baseAddress + MPI_OFFSET, index);
	}

	/**
	 * Reads the size of the underlying packet buffer.
	 * 
	 * @return The size.
	 */
	public int getSize() {
		if (BuildConstants.DEBUG) log.trace("Reading packet buffer size");
		return MemoryUtils.getInt(baseAddress + PKT_OFFSET);
	}

	/**
	 * Writes the size to the underlying packet buffer.
	 * 
	 * @param size The size to write.
	 */
	public void setSize(final int size) {
		if (BuildConstants.DEBUG) log.trace("Writing packet buffer size 0x{}", Integer.toHexString(size));
		MemoryUtils.putInt(baseAddress + PKT_OFFSET, size);
	}

	/**
	 * Reads a byte from the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start reading from.
	 * @return The byte.
	 */
	public byte getByte(final long offset) {
		if (BuildConstants.DEBUG) log.trace("Reading data byte with offset {}", offset);
		return MemoryUtils.getByte(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a short from the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start reading from.
	 * @return The short.
	 */
	public short getShort(final long offset) {
		if (BuildConstants.DEBUG) log.trace("Reading data short with offset {}", offset);
		return MemoryUtils.getShort(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a int from the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start reading from.
	 * @return The int.
	 */
	public int getInt(final long offset) {
		if (BuildConstants.DEBUG) log.trace("Reading data int with offset {}", offset);
		return MemoryUtils.getInt(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Reads a long from the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start reading from.
	 * @return The long.
	 */
	public long getLong(final long offset) {
		if (BuildConstants.DEBUG) log.trace("Reading data long with offset {}", offset);
		return MemoryUtils.getLong(baseAddress + DATA_OFFSET + offset);
	}

	/**
	 * Writes a byte to the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start writing to.
	 * @param value  The byte to write.
	 */
	public void putByte(final long offset, final byte value) {
		if (BuildConstants.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			log.trace("Writing data byte 0x{} with offset {}", xvalue, offset);
		}
		MemoryUtils.putByte(baseAddress + DATA_OFFSET + offset, value);
	}

	/**
	 * Writes a short to the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start writing to.
	 * @param value  The short to write.
	 */
	public void putShort(final long offset, final short value) {
		if (BuildConstants.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			log.trace("Writing data short 0x{} with offset {}", xvalue, offset);
		}
		MemoryUtils.putShort(baseAddress + DATA_OFFSET + offset, value);
	}

	/**
	 * Writes an int to the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start writing to.
	 * @param value  The int to write.
	 */
	public void putInt(final long offset, final int value) {
		if (BuildConstants.DEBUG) {
			log.trace("Writing data short 0x{} with offset {}", Integer.toHexString(value), offset);
		}
		MemoryUtils.putInt(baseAddress + DATA_OFFSET + offset, value);
	}

	/**
	 * Writes a long to the data section of the underlying packet buffer.
	 * 
	 * @param offset The offset to start writing to.
	 * @param value  The long to write.
	 */
	public void putLong(final long offset, final long value) {
		if (BuildConstants.DEBUG) {
			log.trace("Writing data short 0x{} with offset {}", Long.toHexString(value), offset);
		}
		MemoryUtils.putLong(baseAddress + DATA_OFFSET + offset, value);
	}

	/**
	 * Returns an empty useless instances.
	 * 
	 * @return A dummy instance.
	 */
	public static PacketBuffer empty() {
		if (BuildConstants.DEBUG) log.trace("Creating dummy packet buffer");
		return new PacketBuffer(0);
	}

}
