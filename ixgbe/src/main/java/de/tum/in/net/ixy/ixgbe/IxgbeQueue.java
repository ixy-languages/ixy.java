package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.InvalidSizeException;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.generic.IxyQueue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction of {@link IxgbeRxQueue} and {@link IxgbeTxQueue}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
abstract class IxgbeQueue implements IxyQueue {

	/** The size of a descriptor address. */
	private static final long DESCRIPTOR_SIZE = 16L;

	/** The memory manager used to access memory */
	@Getter(AccessLevel.PACKAGE)
	private final @NotNull IxyMemoryManager memoryManager;

	/** The virtual addresses of the packets in the queue. */
	private final long[] addresses;

	/** The base address where all the descriptors are stored. */
	private final long baseDescriptorAddress;

	/**
	 * Creates a new queue with the given capacity.
	 *
	 * @param memoryManager The memory manager.
	 * @param capacity      The capacity of the queue.
	 */
	IxgbeQueue(@NotNull IxyMemoryManager memoryManager, int capacity) {
		if (BuildConfig.DEBUG) log.debug("Creating a generic Ixgbe queue with a capacity of {}", capacity);
		if (!BuildConfig.OPTIMIZED) {
			if (memoryManager == null) throw new InvalidNullParameterException("memoryManager");
			if (capacity <= 0) throw new InvalidSizeException("capacity");
		}
		this.memoryManager = memoryManager;
		addresses = new long[capacity];
		baseDescriptorAddress = 0;
	}

	/**
	 * Returns the address of the nth descriptor.
	 *
	 * @param index The index of the descriptor.
	 * @return The address of the descriptor.
	 */
	long getDescriptorAddress(int index) {
		if (BuildConfig.DEBUG) log.debug("Getting {}th descriptor address", index);
		if (!BuildConfig.OPTIMIZED) {
			if (baseDescriptorAddress == 0) {
				throw new IllegalStateException("Cannot get descriptor address because the queue is not initialized");
			}
		}
		return baseDescriptorAddress + index * DESCRIPTOR_SIZE;
	}

	/**
	 * Reads the buffer memory address given the descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @return The buffer memory address.
	 */
	long getBufferAddress(long descriptorAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			log.debug("Reading descriptor address @ 0x{}", xdescriptorAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		return memoryManager.getLong(descriptorAddress);
	}

	/**
	 * Writes the buffer memory address given the descriptor memory address.
	 *
	 * @param descriptorAddress The descriptor memory address.
	 * @param bufferAddress     The buffer memory address.
	 */
	void setBufferAddress(long descriptorAddress, long bufferAddress) {
		if (BuildConfig.DEBUG) {
			val xdescriptorAddress = Long.toHexString(descriptorAddress);
			val xbufferAddress = Long.toHexString(bufferAddress);
			log.debug("Writing descriptor address @ 0x{} to 0x{}", xdescriptorAddress, xbufferAddress);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (descriptorAddress == 0) throw new InvalidMemoryAddressException("descriptorAddress");
		}
		memoryManager.putLong(descriptorAddress, bufferAddress);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Getter(onMethod_ = {@Contract(pure = true)})
	private int capacity;

	@Getter(onMethod_ = {@Contract(pure = true)})
	private int index;

}
