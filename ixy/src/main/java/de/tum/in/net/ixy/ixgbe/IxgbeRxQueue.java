package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.memory.Mempool;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Nullable;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Intel 10 GbE implementation of an RX queue.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("ConstantConditions")
final class IxgbeRxQueue extends IxgbeQueue {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The offset of the address of the packet buffer header. */
	private static final int OFFSET_HEADER = 8;

	/** The offset of the write buffer error status. */
	private static final int OFFSET_WRITEBACK_ERROR_STATUS = 8;

	/** The offset of the write buffer length. */
	private static final int OFFSET_WRITEBACK_LENGTH = 12;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The memory pool. */
	@Nullable Mempool mempool;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a new RX queue with the given {@code capacity} at the given {@code virtual} address.
	 *
	 * @param virtual  The virtual address.
	 * @param capacity The capacity.
	 */
	IxgbeRxQueue(final long virtual, final short capacity) {
		super(virtual, capacity);
		if (DEBUG >= LOG_DEBUG) {
			log.debug("Creating an RX Ixgbe queue with a capacity for {} packets @ 0x{}.",capacity, leftPad(virtual));
		}
	}

	/**
	 * Sets the address of the packet buffer header stored inside a descriptor.
	 *
	 * @param descriptorAddress         The descriptor virtual address.
	 * @param packetBufferHeaderAddress The packet buffer header virtual address.
	 */
	void setPacketBufferHeaderAddress(final long descriptorAddress, final long packetBufferHeaderAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(descriptorAddress);
			val xpacketBufferHeaderAddress = leftPad(packetBufferHeaderAddress);
			log.trace("Writing packet buffer header virtual address 0x{} of descriptor @ 0x{} + {}.",
					xpacketBufferHeaderAddress, xdescriptorAddress, OFFSET_HEADER);
		}
		mmanager.putLongVolatile(descriptorAddress + OFFSET_HEADER, packetBufferHeaderAddress);
		if (!OPTIMIZED && mmanager.getLongVolatile(descriptorAddress + OFFSET_HEADER) != packetBufferHeaderAddress) {
			throw new IllegalStateException("Header buffer address was NOT written.");
		}
	}

	/**
	 * Returns the writeback error status stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @return The writeback error status.
	 */
	int getWritebackErrorStatus(final long descriptorAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(descriptorAddress);
			log.trace("Reading writeback error status from descriptor @ 0x{} + {}.",
					xdescriptorAddress, OFFSET_WRITEBACK_ERROR_STATUS);
		}
		return mmanager.getIntVolatile(descriptorAddress + OFFSET_WRITEBACK_ERROR_STATUS);
	}

	/**
	 * Returns the writeback length stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @return The write buffer length.
	 */
	short getWritebackLength(final long descriptorAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(descriptorAddress);
			log.trace("Reading writeback length from descriptor @ 0x{} + {}.",
					xdescriptorAddress, OFFSET_WRITEBACK_LENGTH);
		}
		return mmanager.getShortVolatile(descriptorAddress + OFFSET_WRITEBACK_LENGTH);
	}

}
