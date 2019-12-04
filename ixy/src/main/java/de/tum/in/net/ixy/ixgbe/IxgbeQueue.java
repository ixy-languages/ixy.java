package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.memory.JniMemoryManager;
import de.tum.in.net.ixy.memory.MemoryManager;
import de.tum.in.net.ixy.memory.SmartJniMemoryManager;
import de.tum.in.net.ixy.memory.SmartUnsafeMemoryManager;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

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
 * Common methods of {@link IxgbeRxQueue} and {@link IxgbeTxQueue}.
 * <p>
 * The main difference is that each queue assumes the descriptor type, instead of programming a generic queue with
 * different descriptors.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings({"ConstantConditions", "PMD.BeanMembersShouldSerialize"})
abstract class IxgbeQueue {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The size of a descriptor address. */
	private static final long DESCRIPTOR_SIZE = 16;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The capacity of the queue. */
	final short capacity;

	/** The index of the next descriptor to process. */
	short index;

	/** The virtual address where all the descriptors are stored. */
	private final long virtual;

	/** The virtual addresses of the packet buffer wrappers in the queue. */
	final @NotNull long[] buffers;

	/** The memory manager. */
	protected @NotNull MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a new generic queue with the given {@code capacity} at the given {@code virtual} address.
	 *
	 * @param virtual  The virtual address.
	 * @param capacity The capacity.
	 */
	IxgbeQueue(final long virtual, short capacity) {
		if (!OPTIMIZED) {
			if (capacity < 0) throw new NegativeArraySizeException("The parameter 'capacity' MUST NOT be a negative.");
			if (capacity == 0) throw new IllegalArgumentException("The parameter 'capacity' MUST be bigger than 0.");
			if ((capacity & (capacity - 1)) != 0) {
				if (DEBUG >= LOG_WARN) log.warn("The capacity of the queue is not a power of two. Adapting capacity.");
				capacity = (short) (1 << (32 - Integer.numberOfLeadingZeros(capacity)));
			}
		}
		if (DEBUG >= LOG_TRACE) {
			val xvirtual = leftPad(Long.toHexString(virtual), Long.BYTES * 2);
			log.trace("Creating a generic Ixgbe queue with a capacity for {} packets @ 0x{}.", capacity, xvirtual);
		}
		this.virtual = virtual;
		this.capacity = capacity;
		buffers = new long[capacity];
	}

	/**
	 * Returns the virtual address of a descriptor.
	 *
	 * @param index The index of the descriptor.
	 * @return The virtual address of the descriptor.
	 */
	long getDescriptorAddress(final int index) {
		if (!OPTIMIZED) {
			if (virtual == 0) throw new IllegalStateException("The queue MUST be initialized.");
			if (index < 0 || index > capacity) {
				throw new ArrayIndexOutOfBoundsException("The index MUST be inside [0, capacity).");
			}
		}
		if (DEBUG >= LOG_TRACE) log.trace("Getting #{} descriptor's address.", index);
		return virtual + index * DESCRIPTOR_SIZE;
	}

	/**
	 * Returns the packet buffer virtual address stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @return The packet buffer virtual address.
	 */
	long getPacketBufferAddress(final long descriptorAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val xdescriptorAddress = leftPad(Long.toHexString(descriptorAddress), Long.BYTES * 2);
			log.debug("Reading packet buffer virtual address from descriptor @ 0x{} + 0.", xdescriptorAddress);
		}
		return mmanager.getLongVolatile(descriptorAddress);
	}

	/**
	 * Sets the packet buffer virtual address stored inside a descriptor.
	 *
	 * @param descriptorAddress The descriptor virtual address.
	 * @param bufferAddress     The packet buffer virtual address.
	 */
	void setPacketBufferAddress(final long descriptorAddress, final long bufferAddress) {
		if (!OPTIMIZED && descriptorAddress == 0) {
			throw new IllegalArgumentException("The parameter 'descriptorAddress' MUST NOT be 0.");
		}
		if (DEBUG >= LOG_TRACE) {
			val length = Long.BYTES * 2;
			val xdescriptorAddress = leftPad(Long.toHexString(descriptorAddress), length);
			val xbufferAddress = leftPad(Long.toHexString(bufferAddress), length);
			log.debug("Writing packet buffer virtual address 0x{} of descriptor @ 0x{} + 0.",
					xbufferAddress, xdescriptorAddress);
		}
		mmanager.putLongVolatile(descriptorAddress, bufferAddress);
		if (!OPTIMIZED && mmanager.getLongVolatile(descriptorAddress) != bufferAddress) {
			throw new IllegalStateException("Buffer address is not set.");
		}
	}

}
