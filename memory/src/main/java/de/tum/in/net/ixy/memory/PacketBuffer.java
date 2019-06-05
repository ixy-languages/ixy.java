package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper for a packet buffer with a specific memory layout.
 * <p>
 * The memory layout is depicted below:
 * <pre>
 *                  64 bits
 * /---------------------------------------\
 * |       Physical Address Pointer        |
 * |---------------------------------------|
 * |          Memory Pool Pointer          |
 * |---------------------------------------| 64 bytes
 * | Memory Pool Index |    Packet Size    |
 * |---------------------------------------|
 * |          Headroom (variable)          |
 * \---------------------------------------/
 * </pre>
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class PacketBuffer implements IxyPacketBuffer {

	//////////////////////////////////////////////////// EXCEPTIONS ////////////////////////////////////////////////////

	/** Cached exception thrown when an offset is not correctly formatted. */
	private static final IllegalArgumentException OFFSET = new IllegalArgumentException("Offset must be an integer equal to or greater than 0");

	/** Cached exception thrown when a size is not correctly formatted. */
	private static final IllegalArgumentException SIZE = new IllegalArgumentException("Size must be an integer equal to or greater than 0");

	/** Cached exception thrown when a buffer is not correctly formatted. */
	private static final IllegalArgumentException BUFFER = new IllegalArgumentException("Buffer must be initialized and have a non-negative length");

	/////////////////////////////////////////////////////// SIZES //////////////////////////////////////////////////////

	/** The size of the physical address pointer field. */
	private static final int PAP_SIZE = Long.SIZE;

	/** The size of the memory pool pointer field. */
	private static final int MPP_SIZE = Long.SIZE;

	/** The size of the memory pool index field. */
	private static final int MPI_SIZE = Integer.SIZE;

	/** The size of the packet size field. */
	private static final int PKT_SIZE = Integer.SIZE;

	/** The size of the packet buffer header. */
	private static final int HEADER_SIZE = 64 * Byte.SIZE;

	/////////////////////////////////////////////////////// BYTES //////////////////////////////////////////////////////

	/** The bytes of the physical address pointer field. */
	private static final int PAP_BYTES = PAP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool pointer field. */
	private static final int MPP_BYTES = MPP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool index field. */
	private static final int MPI_BYTES = MPI_SIZE / Byte.SIZE;

	/** The bytes of the packet size field. */
	private static final int PKT_BYTES = PKT_SIZE / Byte.SIZE;

	/** The bytes of the packet buffer header. */
	public static final int HEADER_BYTES = HEADER_SIZE / Byte.SIZE;

	///////////////////////////////////////////////////// OFFSETS //////////////////////////////////////////////////////

	/** The offset of the header. */
	public static final int HEADER_OFFSET = 0;

	/** The offset of the physical address pointer field. */
	public static final int PAP_OFFSET = HEADER_OFFSET;

	/** The offset of the memory pool pointer field. */
	private static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The offset of the memory pool index field. */
	public static final int MPI_OFFSET = MPP_OFFSET + MPP_BYTES;

	/** The offset of the packet size field. */
	public static final int PKT_OFFSET = MPI_OFFSET + MPI_BYTES;

	/** The offset of the data of the buffer. */
	public static final int DATA_OFFSET = HEADER_OFFSET + HEADER_BYTES;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Common checks performed by {@link #get(int, int, byte[])} and {@link #getVolatile(int, int, byte[])}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param offset The offset to start copying from.
	 * @param size   The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@SuppressWarnings("Duplicates")
	private static void getCheck(final int offset, int size, final byte[] buffer) {
		if (offset < 0) throw OFFSET;
		else if (size < 0) throw SIZE;
		else if (buffer == null) throw BUFFER;
	}

	/**
	 * Common checks performed by {@link #put(int, int, byte[])} and {@link #putVolatile(int, int, byte[])}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param offset The offset to start copying to.
	 * @param size   The amount of data to copy.
	 * @param buffer The buffer to copy the data from.
	 */
	@SuppressWarnings("Duplicates")
	private static void putCheck(final int offset, int size, final byte[] buffer) {
		if (offset < 0) throw OFFSET;
		else if (size < 0) throw SIZE;
		else if (buffer == null) throw BUFFER;
	}

	///////////////////////////////////////////////////// MEMBERS //////////////////////////////////////////////////////

	/** Holds a reference to a memory manager. */
	private IxyMemoryManager mmanager;

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	/**
	 * The base memory address of the packet buffer.
	 * ------------------ GETTER ------------------
	 * {@inheritDoc}
	 */
	@Getter
	@Setter(AccessLevel.NONE)
	private long virtualAddress;

	/**
	 * Creates a new instance that wraps a packet buffer.
	 *
	 * @param address       The base address of the packet buffer.
	 * @param memoryManager A memory manager instance to manipulate the memory.
	 */
	public PacketBuffer(final long address, final IxyMemoryManager memoryManager) {
		if (BuildConfig.DEBUG) log.trace("Instantiating packet buffer with address 0x{}", Long.toHexString(address));
		virtualAddress = address;
		mmanager = memoryManager;
	}

	/** Checks whether a memory manager has been defined. */
	private void checkMemoryManager() {
		if (mmanager == null) throw new IllegalStateException("The memory manager is not defined");
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	public long getPhysicalAddress() {
		if (BuildConfig.DEBUG) log.trace("Reading physical address pointer field");
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getLong(virtualAddress + PAP_OFFSET);
	}

	/** {@inheritDoc} */
	@Override
	public int getMemoryPoolId() {
		if (BuildConfig.DEBUG) log.trace("Reading memory pool index field");
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getInt(virtualAddress + MPI_OFFSET);
	}

	/** {@inheritDoc} */
	@Override
	public int getSize() {
		if (BuildConfig.DEBUG) log.trace("Reading packet size field");
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getInt(virtualAddress + PKT_OFFSET);
	}

	/** {@inheritDoc} */
	@Override
	public void setSize(final int size) {
		val address = virtualAddress + PKT_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Writing packet size field @ 0x{} with value {}", xaddress, size);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putInt(virtualAddress + PKT_OFFSET, size);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByte(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data byte @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getByte(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public byte getByteVolatile(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data byte @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getByteVolatile(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public void putByte(final int offset, final byte value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data byte 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putByte(address + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putByteVolatile(final int offset, final byte value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data byte 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putByteVolatile(virtualAddress + DATA_OFFSET + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public short getShort(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data short @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getShort(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public short getShortVolatile(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data short @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getShortVolatile(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public void putShort(final int offset, final short value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data short 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putShort(address + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putShortVolatile(final int offset, final short value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data short 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putShortVolatile(virtualAddress + DATA_OFFSET + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public int getInt(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data int @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getInt(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public int getIntVolatile(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data int @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getIntVolatile(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public void putInt(final int offset, final int value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data int 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putInt(address + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putIntVolatile(final int offset, final int value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data int 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putIntVolatile(virtualAddress + DATA_OFFSET + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public long getLong(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data long @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getLong(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public long getLongVolatile(final int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data long @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		return mmanager.getLongVolatile(address + offset);
	}

	/** {@inheritDoc} */
	@Override
	public void putLong(final int offset, final long value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data long 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putLong(address + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public void putLongVolatile(final int offset, final long value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data long 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) checkMemoryManager();
		mmanager.putLongVolatile(virtualAddress + DATA_OFFSET + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public void get(final int offset, int size, final byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Reading {} bytes starting from offset 0x{}", size, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkMemoryManager();
			getCheck(offset, size, buffer);
			size = Math.min(size, buffer.length);
		}
		mmanager.get(virtualAddress + offset, size, buffer, 0);
	}

	/** {@inheritDoc} */
	@Override
	public void getVolatile(final int offset, int size, final byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Reading {} volatile bytes starting from offset 0x{}", size, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkMemoryManager();
			getCheck(offset, size, buffer);
			size = Math.min(size, buffer.length);
		}
		mmanager.getVolatile(virtualAddress + offset, size, buffer, 0);
	}

	/** {@inheritDoc} */
	@Override
	public void put(final int offset, int size, final byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Writing {} bytes starting from offset 0x{}", size, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkMemoryManager();
			putCheck(offset, size, buffer);
			size = Math.min(size, buffer.length);
		}
		mmanager.put(virtualAddress + offset, size, buffer, 0);
	}

	/** {@inheritDoc} */
	@Override
	public void putVolatile(final int offset, int size, final byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Writing {} volatile bytes starting from offset 0x{}", size, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			checkMemoryManager();
			putCheck(offset, size, buffer);
			size = Math.min(size, buffer.length);
		}
		mmanager.putVolatile(virtualAddress + offset, size, buffer, 0);
	}

}
