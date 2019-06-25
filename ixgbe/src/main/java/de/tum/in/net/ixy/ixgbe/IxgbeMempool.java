package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.PacketBuffer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

/**
 * An extension of the simple Ixy's memory pool implementation.
 *
 * @author Esaú García Sánchez-Torika
 */
@Slf4j
final class IxgbeMempool extends Mempool {

	/**
	 * Delegation constructor constructor.
	 *
	 * @param capacity The capacity of the memory pool.
	 */
	IxgbeMempool(int capacity) {
		super(capacity);
	}

	@Override
	public void allocate(@NotNull IxyMemoryManager mmanager, @NotNull IxyDmaMemory dmaMemory) {
		val capacity = getCapacity();
		if (BuildConfig.DEBUG) log.debug("Allocating {} packets @ {}", capacity, dmaMemory);
		val buffers = getBuffers();
		if (!buffers.isEmpty()) buffers.clear();
		val packetSize = getPacketSize();
		val builder = PacketBuffer.builder().manager(mmanager).size(0).pool(this);
		var virtual = dmaMemory.getVirtualAddress();
		var physical = dmaMemory.getPhysicalAddress();
		for (int i = 0; i < capacity; i += 1) {
			val packet = builder.virtual(virtual).physical(physical).build();
			if (BuildConfig.DEBUG) log.debug("Packets {}th allocated: {}", i, packet);
			buffers.add(packet);
			virtual += packetSize;
			physical += packetSize;
		}
	}

}
