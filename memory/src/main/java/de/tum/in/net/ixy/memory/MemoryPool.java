package de.tum.in.net.ixy.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeMap;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a memory region reserved for a specific number of {@link PacketBuffer}s.
 * <p>
 * The main goal of this class is to enable easy management of {@link PacketBuffer} instances.
 */
@Slf4j
public final class MemoryPool implements Comparable<MemoryPool> {

	/** Stores all the memory pools ever created. */
	public static final TreeMap<Integer, MemoryPool> memoryPools = new TreeMap<>();

	/**
	 * Given an {@code id}, computes one that is not already being used by another memory pool.
	 * 
	 * @param id The initial id to be used.
	 * @return An id that is not being used.
	 */
	private static int getValidId(int id) {
		while (memoryPools.containsKey(id)) {
			id += 1;
		}
		if (BuildConstants.DEBUG) log.trace("Found a valid memory pool id {}", id);
		return id;
	}

	/**
	 * Adds a memory pool to {@link #memoryPools}.
	 * 
	 * @param memoryPool The memory pool to add.
	 */
	public static void addMemoryPool(final MemoryPool memoryPool) {
		if (BuildConstants.DEBUG) log.trace("Adding memory pool");
		val id = getValidId(memoryPool.id);
		memoryPool.id = id;
		memoryPools.put(id, memoryPool);
	}

	/** The unique identifier of the memory pool. */
	@Getter
	private int id;
	
	/** The base address of the memory pool. */
	@Getter
	private long baseAddress;

	/** The size of the buffer. */
	@Getter
	private int bufferSize;

	/** The number of entries the pool has. */
	@Getter
	private int entrySize;

	/** double ended queue with a bunch a pre-allocated {@link PacketBuffer} instances. */
	private Deque<PacketBuffer> buffers;

	/**
	 * Creates an instance of the memory pool.
	 * <p>
	 * This constructor automatically keeps track of the created instances by adding them to the list {@link
	 * #memoryPools}.
	 * 
	 * @param address The base address of the memory pool.
	 * @param size    The size of each buffer inside the memory pool.
	 * @param entries The number of entries to allocate.
	 */
	public MemoryPool(final long address, final int size, final int entries) {
		if (BuildConstants.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Creating memory pool with {} entries, a total size of {} bytes @ 0x{}", entries, size, xaddress);
		}
		baseAddress = address;
		bufferSize = size;
		entrySize = entries;
		id = memoryPools.isEmpty() ? 0 : getValidId(memoryPools.lastKey());
		memoryPools.put(id, this);
		if (BuildConstants.DEBUG) log.info("There are {} memory pools", memoryPools.size());
	}

	/** Fills the collection {@link #buffers} with {@link #entrySize} {@link PacketBuffer} instances. */
	public void allocatePacketBuffers() {
		if (BuildConstants.DEBUG) log.trace("Allocating packet buffers");
		buffers = new ArrayDeque<PacketBuffer>(entrySize);
		for (var i = 0; i < entrySize; i += 1) {
			val virt = baseAddress + i * bufferSize;
			val buffer = new PacketBuffer(virt);
			buffer.setPhysicalAddress(MemoryUtils.virt2phys(virt));
			buffer.setSize(0);
			buffers.add(buffer);
		}
	}

	/**
	 * Returns a free pre-allocated packet buffer instance.
	 * <p>
	 * If the collection storing the free buffers is empty, then a dummy instance is returned.
	 * 
	 * @return A free packet buffer or a dummy instance.
	 */
	public PacketBuffer getFreePacketBuffer() {
		if (BuildConstants.DEBUG) {
			log.trace("Obtaining a free packet buffer");
			val empty = buffers.isEmpty();
			if (empty) {
				log.warn("There are no free packet buffers, a dummy packet buffer will be returned");
				return PacketBuffer.empty();
			}
			return buffers.pop();
		} else {
			return buffers.isEmpty() ? PacketBuffer.empty() : buffers.pop();
		}
	}

	/**
	 * Frees a packet buffer, that is to say, adds it to the collection of free packet buffers.
	 * <p>
	 * This method will do nothing if the size of {@link #buffers} has already reached {@link #entrySize}.
	 * 
	 * @param buffer The packet buffer to free.
	 */
	public void freePacketBuffer(final PacketBuffer buffer) {
		if (buffers.size() >= entrySize) {
			log.warn("The packet buffer cannot be freed because the stack is full");
			return;
		}
		buffers.push(buffer);
	}

	//////////////////////////////////////////////// OVERRIDEN METHODS /////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(MemoryPool memoryPool) {
		return id - memoryPool.id;
	}

}