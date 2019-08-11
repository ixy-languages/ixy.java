package de.tum.in.net.ixy.memory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI_FULL;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Simple implementation of Ixy's packet buffer specification.
 * <p>
 * The memory layout is depicted below:
 * <pre>
 *                  64 bits
 * /---------------------------------------\
 * |       Physical Address Pointer        |
 * |---------------------------------------|
 * |          Memory Pool Pointer          |
 * |---------------------------------------|
 * | Memory Pool Index |    Packet Size    | 64 bytes
 * |---------------------------------------|
 * |          Headroom (variable)          |
 * \---------------------------------------/
 * </pre>
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("ConstantConditions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class PacketBufferWrapper implements Comparable<PacketBufferWrapper> {

	/////////////////////////////////////////////////////// SIZES //////////////////////////////////////////////////////

	/** The size of the physical address pointer field. */
	private static final int PAP_SIZE = Long.SIZE;

	/** The size of the memory pool pointer field. */
	private static final int MPP_SIZE = Long.SIZE;

	/** The size of the memory pool index field. */
	private static final int MPI_SIZE = Integer.SIZE;

//	/** The size of the packet size field. */
//	private static final int PKT_SIZE = Integer.SIZE;

	/** The size of the packet buffer header. */
	private static final int HEADER_SIZE = 64 * Byte.SIZE;

	/////////////////////////////////////////////////////// BYTES //////////////////////////////////////////////////////

	/** The bytes of the physical address pointer field. */
	private static final int PAP_BYTES = PAP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool pointer field. */
	private static final int MPP_BYTES = MPP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool index field. */
	private static final int MPI_BYTES = MPI_SIZE / Byte.SIZE;

//	/** The bytes of the packet size field. */
//	private static final int PKT_BYTES = PKT_SIZE / Byte.SIZE;

	/** The bytes of the packet buffer header. */
	private static final int HEADER_BYTES = HEADER_SIZE / Byte.SIZE;

	///////////////////////////////////////////////////// OFFSETS //////////////////////////////////////////////////////

	/** The offset of the header. */
	private static final int HEADER_OFFSET = 0;

	/** The offset of the physical address pointer field. */
	private static final int PAP_OFFSET = HEADER_OFFSET;

	/** The offset of the memory pool pointer field. */
	private static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The offset of the memory pool index field. */
	private static final int MPI_OFFSET = MPP_OFFSET + MPP_BYTES;

	/** The offset of the packet size field. */
	private static final int PKT_OFFSET = MPI_OFFSET + MPI_BYTES;

	/** The offset of the data of the buffer. */
	public static final int DATA_OFFSET = HEADER_OFFSET + HEADER_BYTES;

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The memory manager. */
	@SuppressWarnings("NestedConditionalExpression")
	private static final MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * The virtual address where the actual packet buffer resides.
	 * -- GETTER --
	 * Returns the virtual address of the wrapped packet buffer.
	 *
	 * @return The virtual address.
	 */
	@Getter
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	private final long virtual;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Returns the physical address in which this packet buffer is allocated.
	 *
	 * @return The physical address.
	 */
	@Contract(pure = true)
	public long getPhysicalAddress() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading physical address pointer field @ 0x{} + {}.", leftPad(virtual), PAP_OFFSET);
		}
		return mmanager.getLongVolatile(virtual + PAP_OFFSET);
	}

	/**
	 * Sets the physical address in which this packet buffer is allocated.
	 *
	 * @param physicalAddress The physical address.
	 */
	void setPhysicalAddress(final long physicalAddress) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing physical address pointer field @ 0x{} + {}.", leftPad(virtual), PAP_OFFSET);
		}
		mmanager.putLongVolatile(virtual + PAP_OFFSET, physicalAddress);
	}

	/**
	 * Returns the memory pool identifier of the memory pool that manages this packet buffer.
	 *
	 * @return The memory pool identifier.
	 */
	@Contract(pure = true)
	long getMemoryPoolId() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading memory pool identifier field @ 0x{} + {}.", leftPad(virtual), MPP_OFFSET);
		}
		return mmanager.getLongVolatile(virtual + MPP_OFFSET);
	}

	/**
	 * Sets the memory pool identifier of the memory pool that manages this packet buffer.
	 *
	 * @param memoryPoolId The memory pool identifier.
	 */
	void setMemoryPoolId(final long memoryPoolId) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing memory pool identifier field @ 0x{} + {}.", leftPad(virtual), MPP_OFFSET);
		}
		mmanager.putLongVolatile(virtual + MPP_OFFSET, memoryPoolId);
	}

	/**
	 * Returns the packet size of this packet buffer.
	 *
	 * @return The packet size.
	 */
	@Contract(pure = true)
	public int getSize() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading packet size field @ 0x{} + {}.", leftPad(virtual), PKT_OFFSET);
		}
		return mmanager.getIntVolatile(virtual + PKT_OFFSET);
	}

	/**
	 * Sets the packet size of this packet buffer.
	 *
	 * @param size The packet size.
	 */
	public void setSize(final int size) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing packet size field @ 0x{} + {}.", leftPad(virtual), PKT_OFFSET);
		}
		mmanager.putIntVolatile(virtual + PKT_OFFSET, size);
	}

	/**
	 * Writes an {@code int} to the packet data.
	 *
	 * @param offset The offset.
	 * @param value  The value to store.
	 */
	public void putInt(int offset, int value) {
		val address = virtual + DATA_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing data long @ 0x{} + {}.", leftPad(virtual), offset);
		}
		mmanager.putInt(address + offset, value);
	}

	/**
	 * Copies into a packet data region using a primitive byte array as store.
	 *
	 * @param offset The offset from which to start copying to {@code dest}.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The primitive array to copy from.
	 */
	public void put(final int offset, int bytes, final @NotNull byte[] buffer) {
		if (!OPTIMIZED) {
			if (buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
			if (offset < 0 || offset >= buffer.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be in [0, buffer.length).");
			}
			if (bytes < 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
			if (buffer.length < bytes) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to write more bytes than the buffer can hold. Adapting bytes.");
				}
				bytes = buffer.length;
			}
			if (bytes == 0) return;
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing packet data region of {} bytes @ offset '{}'.", bytes, leftPad(offset));
		}
		mmanager.put(virtual + DATA_OFFSET, bytes, buffer, 0);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int compareTo(final @NotNull PacketBufferWrapper o) {
		if (DEBUG >= LOG_TRACE) log.trace("Comparing with another packet buffer wrapper.");
		return Long.compare(virtual, o.virtual);
	}

	@Override
	@Contract(pure = true)
	public @NotNull String toString() {
		return "PacketBufferWrapper"
				+ "("
				+ "virtual=0x" + leftPad(virtual)
				+ ")";
	}

}
