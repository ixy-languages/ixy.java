package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ixy's memory pool specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyMempool {

	/**
	 * Returns the identifier of the memory pool.
	 *
	 * @return The memory pool identifier.
	 */
	@Contract(pure = true)
	int getId();

	/**
	 * Returns the number of packets slots this memory pool has.
	 *
	 * @return The capacity of the memory pool.
	 */
	@Contract(pure = true)
	int getCapacity();

	/**
	 * Returns the number of packets slots this memory pool has filled.
	 *
	 * @return The size of the memory pool.
	 */
	@Contract(pure = true)
	int getSize();

	/**
	 * Returns the number of packets this memory pool needs to reach its maximum capacity.
	 *
	 * @return The number of remaining packet buffer slots.
	 */
	@Contract(pure = true)
	default int getRemaining() {
		return getCapacity() - getSize();
	}

	/**
	 * Returns the size of a packet.
	 *
	 * @return The size of a packet.
	 */
	@Contract(pure = true)
	int getPacketSize();

	/**
	 * Returns a free packet.
	 * <p>
	 * If no free packets are available, {@code null} will be returned.
	 *
	 * @return A free packet.
	 */
	@Nullable IxyPacketBuffer get();

	/**
	 * Frees a packet.
	 * <p>
	 * This does not free the memory allocated to the packet, but registers it as free so that it can be reused again.
	 *
	 * @param packet The packet to free.
	 */
	@Contract("null -> fail")
	void free(@NotNull IxyPacketBuffer packet);

	/**
	 * Finds a memory pool given its identifier.
	 *
	 * @param id The memory pool identifier.
	 * @return The memory pool.
	 */
	@Nullable IxyMempool find(int id);

	/**
	 * Finds the memory pool that owns a packet.
	 *
	 * @param packet The packet whose memory pool will be searched.
	 * @return The memory pool that owns a packet.
	 */
	default @Nullable IxyMempool find(@Nullable IxyPacketBuffer packet) {
		return packet == null ? null : find(packet.getMemoryPoolId());
	}

}
