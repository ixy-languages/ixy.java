package de.tum.in.net.ixy.ixgbe;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * Intel 10 GbE implementation of a TX queue.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
final class IxgbeTxQueue extends IxgbeQueue {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The offset of the length of the command type. */
	private static final int CMD_TYPE_LENGTH_OFFSET = 8;

	/** The offset of the write buffer error status. */
	private static final int OFFLOAD_STATUS_OFFSET = 12;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The index of the first descriptor to clean. */
	short cleanIndex;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a new TX queue with the given {@code capacity} at the given {@code virtual} address.
	 *
	 * @param virtual  The virtual address.
	 * @param capacity The capacity.
	 */
	IxgbeTxQueue(final long virtual, final short capacity) {
		super(virtual, capacity);
		if (DEBUG >= LOG_DEBUG) {
			log.debug("Creating an TX Ixgbe queue with a capacity for {} packets @ 0x{}.", capacity, leftPad(virtual));
		}
		cleanIndex = 0;
	}

	/**
	 * Returns the command type length stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @return The command type length.
	 */
	int getCmdTypeLength(final long descriptorAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading command type length from descriptor @ 0x{} + {}.",
					leftPad(descriptorAddress), CMD_TYPE_LENGTH_OFFSET);
		}
		return mmanager.getIntVolatile(descriptorAddress + CMD_TYPE_LENGTH_OFFSET);
	}

	/**
	 * Sets the command type length stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @param cmdTypeLength     The command type length.
	 */
	void setCmdTypeLength(final long descriptorAddress, final int cmdTypeLength) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(descriptorAddress);
			val xcmdTypeLength = leftPad(cmdTypeLength);
			log.trace("Writing command type length 0x{} of descriptor @ 0x{} + {}.",
					xcmdTypeLength, xdescriptorAddress, CMD_TYPE_LENGTH_OFFSET);
		}
		mmanager.putIntVolatile(descriptorAddress + CMD_TYPE_LENGTH_OFFSET, cmdTypeLength);
	}

	/**
	 * Returns the offloading status stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @return The offloading status.
	 */
	int getOffloadInfoStatus(final long descriptorAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Reading offloading info status from descriptor at @ 0x{} + {}.",
					leftPad(descriptorAddress), OFFLOAD_STATUS_OFFSET);
		}
		return mmanager.getIntVolatile(descriptorAddress + OFFLOAD_STATUS_OFFSET);
	}

	/**
	 * Sets the offloading status stored inside a descriptor.
	 *
	 * @param descriptorAddress    The descriptor virtual address.
	 * @param offloadingStatusInfo The offloading status.
	 */
	void setOffloadInfoStatus(long descriptorAddress, int offloadingStatusInfo) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(descriptorAddress);
			val xoffloadInfoStatus = leftPad(offloadingStatusInfo);
			log.trace("Writing offloading info status 0x{} of descriptor at @ 0x{} + {}.",
					xoffloadInfoStatus, xdescriptorAddress, OFFLOAD_STATUS_OFFSET);
		}
		mmanager.putIntVolatile(descriptorAddress + OFFLOAD_STATUS_OFFSET, offloadingStatusInfo);
	}

}
