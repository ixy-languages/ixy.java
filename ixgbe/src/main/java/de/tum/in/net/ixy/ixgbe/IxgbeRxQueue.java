package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.InvalidSizeException;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

/**
 * Intel 10 GbE implementation of an RX queue.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public class IxgbeRxQueue extends IxgbeQueue {

	/**
	 * Creates a new queue with the given capacity.
	 *
	 * @param memoryManager The memory manager.
	 * @param capacity      The capacity.
	 */
	public IxgbeRxQueue(@NotNull IxyMemoryManager memoryManager, int capacity) {
		super(memoryManager, capacity);
		if (BuildConfig.DEBUG) log.debug("Creating an RX Ixgbe queue with a capacity of {}", capacity);
		if (!BuildConfig.OPTIMIZED) {
			if (memoryManager == null) throw new InvalidNullParameterException("memoryManager");
			if (capacity <= 0) throw new InvalidSizeException("capacity");
		}
	}

	/**
	 * Reads the command type length given a descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @return The header buffer memory address.
	 */
	public long getHeaderBufferAddress(long descriptorAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			log.debug("Reading header buffer memory address @ 0x{}", xdescriptorAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		return getMemoryManager().getLong(descriptorAddress + 8);
	}

	/**
	 * Writes the header buffer memory address given a descriptor memory address.
	 *
	 * @param descriptorAddress   The descriptor memory address.
	 * @param headerBufferAddress The header buffer memory address.
	 */
	public void setHeaderBufferAddress(long descriptorAddress, long headerBufferAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			val xheaderBufferAddress = Long.toHexString(headerBufferAddress);
			log.debug("Writing header buffer memory address @ 0x{} to 0x{}", xdescriptorAddress, xheaderBufferAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		getMemoryManager().putLong(descriptorAddress + 8, headerBufferAddress);
	}

}
