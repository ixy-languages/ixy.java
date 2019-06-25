package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

/**
 * Intel 10 GbE implementation of a TX queue.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
final class IxgbeTxQueue extends IxgbeQueue {

	/** The index of a clean virtual address. */
	private final int cleanIndex;

	/**
	 * Creates a new queue with the given capacity.
	 *
	 * @param memoryManager The memory manager.
	 * @param capacity      The capacity.
	 */
	IxgbeTxQueue(@NotNull IxyMemoryManager memoryManager, int capacity) {
		super(memoryManager, capacity);
		if (BuildConfig.DEBUG) log.debug("Creating a TX Ixgbe queue with a capacity of {}", capacity);
		if (!BuildConfig.OPTIMIZED) {
			if (memoryManager == null) throw new InvalidNullParameterException("memoryManager");
			if (capacity <= 0) throw new InvalidSizeException("capacity");
		}
		cleanIndex = 0;
	}

	/**
	 * Reads the command type length given a descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @return The command type length.
	 */
	public int getCmdTypeLength(long descriptorAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			log.debug("Reading command type length @ 0x{}", xdescriptorAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		return getMemoryManager().getInt(descriptorAddress + 8);
	}

	/**
	 * Writes the command type length given a descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @param cmdTypeLen        The command type length.
	 */
	public void setCmdTypeLength(long descriptorAddress, int cmdTypeLen) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			log.debug("Writing command type length @ 0x{} to {}", xdescriptorAddress, cmdTypeLen);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		getMemoryManager().putInt(descriptorAddress + 8, cmdTypeLen);
	}

	/**
	 * Reads the write buffer status given a descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @return The write buffer status.
	 */
	public int getWbStatus(long descriptorAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			log.debug("Reading write buffer status @ 0x{}", xdescriptorAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		return getMemoryManager().getInt(descriptorAddress + 12);
	}

	/**
	 * Writes the command type length given a descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @param wbStatus          The write buffer status.
	 */
	public void setWbStatus(long descriptorAddress, int wbStatus) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			val xwbStatus = Integer.toHexString(wbStatus);
			log.debug("Writing write buffer status @ 0x{} to 0x{}", xdescriptorAddress, xwbStatus);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		getMemoryManager().putInt(descriptorAddress + 12, wbStatus);
	}

}
