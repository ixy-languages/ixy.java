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
import static de.tum.in.net.ixy.memory.PacketBufferWrapperConstants.MPP_OFFSET;
import static de.tum.in.net.ixy.memory.PacketBufferWrapperConstants.PAP_OFFSET;
import static de.tum.in.net.ixy.memory.PacketBufferWrapperConstants.PAYLOAD_OFFSET;
import static de.tum.in.net.ixy.memory.PacketBufferWrapperConstants.PKT_OFFSET;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Simple implementation of Ixy's packet buffer specification.
 *
 * @author Esaú García Sánchez-Torija
 * @see PacketBufferWrapperConstants
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("ConstantConditions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class PacketBufferWrapper implements Comparable<PacketBufferWrapper> {

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
	private final long virtualAddress;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Returns the physical address in which this packet buffer is allocated.
	 *
	 * @return The physical address.
	 */
	@Contract(pure = true)
	public long getPhysicalAddress() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading physical address pointer field @ 0x{} + {}.", leftPad(virtualAddress), PAP_OFFSET);
		}
		return mmanager.getLongVolatile(virtualAddress + PAP_OFFSET);
	}

	/**
	 * Sets the physical address in which this packet buffer is allocated.
	 *
	 * @param physicalAddress The physical address.
	 */
	void setPhysicalAddress(final long physicalAddress) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing physical address pointer field @ 0x{} + {}.", leftPad(virtualAddress), PAP_OFFSET);
		}
		mmanager.putLongVolatile(virtualAddress + PAP_OFFSET, physicalAddress);
	}

	/**
	 * Returns the memory pool pointer of the memory pool that manages this packet buffer.
	 *
	 * @return The memory pool pointer.
	 */
	@Contract(pure = true)
	long getMemoryPoolPointer() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading memory pool identifier field @ 0x{} + {}.", leftPad(virtualAddress), MPP_OFFSET);
		}
		return mmanager.getLongVolatile(virtualAddress + MPP_OFFSET);
	}

	/**
	 * Sets the memory pool pointer of the memory pool that manages this packet buffer.
	 *
	 * @param memoryPoolId The memory pool pointer.
	 */
	void setMemoryPoolPointer(final long memoryPoolId) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing memory pool identifier field @ 0x{} + {}.", leftPad(virtualAddress), MPP_OFFSET);
		}
		mmanager.putLongVolatile(virtualAddress + MPP_OFFSET, memoryPoolId);
	}

	/**
	 * Returns the packet size of this packet buffer.
	 *
	 * @return The packet size.
	 */
	@Contract(pure = true)
	public int getSize() {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading packet size field @ 0x{} + {}.", leftPad(virtualAddress), PKT_OFFSET);
		}
		return mmanager.getIntVolatile(virtualAddress + PKT_OFFSET);
	}

	/**
	 * Sets the packet size of this packet buffer.
	 *
	 * @param size The packet size.
	 */
	public void setSize(final int size) {
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing packet size field @ 0x{} + {}.", leftPad(virtualAddress), PKT_OFFSET);
		}
		mmanager.putIntVolatile(virtualAddress + PKT_OFFSET, size);
	}

	/**
	 * Reads a {@code byte} from the packet payload.
	 *
	 * @param offset The offset.
	 */
	public byte getByte(final int offset) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading payload byte @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		return mmanager.getByte(address + offset);
	}

	/**
	 * Writes a {@code byte} to the packet payload.
	 *
	 * @param offset The offset.
	 * @param value  The value to store.
	 */
	public void putByte(final int offset, final byte value) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing payload byte @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		mmanager.putByte(address + offset, value);
	}

	/**
	 * Reads a {@code short} from the packet payload.
	 *
	 * @param offset The offset.
	 */
	public short getShort(final int offset) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading payload short @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		return mmanager.getShort(address + offset);
	}

	/**
	 * Writes a {@code short} to the packet payload.
	 *
	 * @param offset The offset.
	 * @param value  The value to store.
	 */
	public void putShort(final int offset, final short value) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing payload short @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		mmanager.putShort(address + offset, value);
	}

	/**
	 * Reads an {@code int} from the packet payload.
	 *
	 * @param offset The offset.
	 */
	public int getInt(final int offset) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading payload int @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		return mmanager.getInt(address + offset);
	}

	/**
	 * Writes an {@code int} to the packet payload.
	 *
	 * @param offset The offset.
	 * @param value  The value to store.
	 */
	public void putInt(final int offset, final int value) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing payload int @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		mmanager.putInt(address + offset, value);
	}

	/**
	 * Reads a {@code long} from the packet payload.
	 *
	 * @param offset The offset.
	 */
	public long getLong(final int offset) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading payload long @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		return mmanager.getLong(address + offset);
	}

	/**
	 * Writes a {@code long} to the packet payload.
	 *
	 * @param offset The offset.
	 * @param value  The value to store.
	 */
	public void putLong(final int offset, final long value) {
		val address = virtualAddress + PAYLOAD_OFFSET;
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing payload long @ 0x{} + {}.", leftPad(virtualAddress), offset);
		}
		mmanager.putLong(address + offset, value);
	}

	/**
	 * Copies from a packet data region using a primitive byte array as store.
	 *
	 * @param offset The offset from which to start copying to {@code buffer}.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The primitive array to copy to.
	 */
	public void get(final int offset, int bytes, final @NotNull byte[] buffer) {
		if (!OPTIMIZED) {
			if (buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
			if (offset < 0 || offset >= buffer.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be in [0, buffer.length).");
			}
			if (bytes < 0) throw new IllegalArgumentException("The parameter 'bytes' MUST be positive.");
			if (buffer.length < bytes) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to read more bytes than the buffer can hold. Adapting bytes.");
				}
				bytes = buffer.length;
			}
			if (bytes == 0) return;
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading packet payload chunk of {} bytes @ offset '{}'.", bytes, leftPad(offset));
		}
		mmanager.get(virtualAddress + PAYLOAD_OFFSET, bytes, buffer, offset);
	}

	/**
	 * Copies into a packet data region using a primitive byte array as store.
	 *
	 * @param offset The offset from which to start copying from {@code buffer}.
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
			log.trace("Writing packet payload chunk of {} bytes @ offset '{}'.", bytes, leftPad(offset));
		}
		mmanager.put(virtualAddress + PAYLOAD_OFFSET, bytes, buffer, offset);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int compareTo(final @NotNull PacketBufferWrapper o) {
		if (DEBUG >= LOG_TRACE) log.trace("Comparing with another packet buffer wrapper.");
		return Long.compare(virtualAddress, o.virtualAddress);
	}

	@Override
	@Contract(pure = true)
	public @NotNull String toString() {
		return "PacketBufferWrapper"
				+ "("
				+ "virtual=0x" + leftPad(virtualAddress)
				+ ")";
	}

}
