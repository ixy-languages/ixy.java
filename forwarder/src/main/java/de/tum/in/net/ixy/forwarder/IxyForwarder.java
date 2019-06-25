package de.tum.in.net.ixy.forwarder;

import de.tum.in.net.ixy.generic.IxyDevice;
import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

/**
 * Ixy's implementation of a packet forwarder.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@Builder(builderClassName = "Builder")
public class IxyForwarder implements Runnable {

	/**
	 * The device the manipulates the first NIC.
	 * ----------------- SETTER -----------------
	 * Sets the first NIC to forward from.
	 *
	 * @param firstNic The first NIC.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private final @NotNull IxyDevice firstNic;

	/**
	 * The device the manipulates the second NIC.
	 * ----------------- SETTER -----------------
	 * Sets the second NIC to forward from.
	 *
	 * @param secondNic The second NIC.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private final @NotNull IxyDevice secondNic;

	/**
	 * The memory pool that manages the packets.
	 * ----------------- SETTER -----------------
	 * Sets the memory pool that manages the packets.
	 *
	 * @param memoryPool The memory pool.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private @NotNull IxyMempool memoryPool;

	/**
	 * The batch size.
	 * ---- SETTER ----
	 * Sets the maximum batch size.
	 *
	 * @param batchSize The batch size.
	 * @return This builder.
	 */
	@SuppressWarnings("JavaDoc")
	private final int batchSize;

	/**
	 * Creates a packet forwarder given 2 different devices.
	 *
	 * @param firstNic   The first NIC.
	 * @param secondNic  The second NIC.
	 * @param memoryPool The memory pool.
	 * @param batchSize  The batch size.
	 */
	@SuppressWarnings("ConstantConditions")
	public IxyForwarder(@NotNull IxyDevice firstNic, @NotNull IxyDevice secondNic, @NotNull IxyMempool memoryPool, int batchSize) {
		if (!BuildConfig.OPTIMIZED) {
			if (firstNic == null) throw new InvalidNullParameterException("firstNic");
			if (secondNic == null) throw new InvalidNullParameterException("secondNic");
			if (memoryPool == null) throw new InvalidNullParameterException("memoryPool");
			if (batchSize <= 0) throw new InvalidSizeException("batchSize");
		}
		this.firstNic = firstNic;
		this.secondNic = secondNic;
		this.memoryPool = memoryPool;
		this.batchSize = batchSize;
	}

	@Override
	public void run() {
		val buffers = new IxyPacketBuffer[batchSize];
		while (true) {
			forward(firstNic, 0, secondNic, 0, buffers);
			forward(secondNic, 0, firstNic, 0, buffers);
		}
	}

	/**
	 * Forwards as many packets as it can in one round.
	 *
	 * @param srcNic  The source NIC to read packets from.
	 * @param rxQueue The read queue.
	 * @param destNic The destination NIC to write packets to.
	 * @param txQueue The destination queue.
	 * @param buffers The array that holds the packet references.
	 */
	@SuppressWarnings("ConstantConditions")
	private void forward(@NotNull IxyDevice srcNic, int rxQueue, @NotNull IxyDevice destNic, int txQueue, @NotNull IxyPacketBuffer[] buffers) {
		if (!BuildConfig.OPTIMIZED) {
			if (srcNic == null) throw new InvalidNullParameterException("srcNic");
			if (destNic == null) throw new InvalidNullParameterException("destNic");
			if (buffers == null) throw new InvalidNullParameterException("buffers");
			if (buffers.length <= 0) return;
		}
		// Receive packets
		val rxBufferCount = srcNic.rxBatch(rxQueue, buffers);
		if (rxBufferCount > 0) {
			// Update all buffers to simulate a realistic scenario
			for (var i = 0; i < rxBufferCount; i += 1) {
				var value = buffers[i].getByte(0) + 1;
				buffers[i].putByte(0, (byte) value);
			}
			// Send the packets
			val txBuffCount = destNic.txBatch(txQueue, buffers, 0, rxBufferCount);
			// Free the resources using the appropriate memory pool
			val pool = memoryPool.find(buffers[0]);
			if (pool != null) {
				memoryPool = pool;
				for (var i = 0; i < txBuffCount; i += 1) {
					memoryPool.free(buffers[i]);
				}
			}
		}
	}

}
