package de.tum.in.net.ixy.generic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ixy's memory pool specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public abstract class IxyMempool {

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * The identifier of the memory pool.
	 * ----------------- GETTER -----------------
	 * Returns the identifier of the memory pool.
	 *
	 * @return The memory pool identifier.
	 */
	@Getter(onMethod_ = {@Contract(pure = true)})
	@Setter(AccessLevel.PROTECTED)
	@SuppressWarnings("JavaDoc")
	@EqualsAndHashCode.Include
	@ToString.Include
	private int id;

	/**
	 * The capacity of the memory pool.
	 * ------------------------- GETTER -------------------------
	 * Returns the number of packets slots this memory pool has.
	 *
	 * @return The capacity of the memory pool.
	 */
	@Getter(onMethod_ = {@Contract(pure = true)})
	@Setter(AccessLevel.PROTECTED)
	@SuppressWarnings("JavaDoc")
	@EqualsAndHashCode.Include
	@ToString.Include
	private int capacity;

	/**
	 * The packet size of the memory pool.
	 * ------------------ GETTER ------------------
	 * Returns the packet size of the memory pool.
	 *
	 * @return The packet size of the memory pool.
	 */
	@Getter(onMethod_ = {@Contract(pure = true)})
	@SuppressWarnings("JavaDoc")
	@EqualsAndHashCode.Include
	@ToString.Include
	@Setter
	private int packetSize;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Returns the number of free packets this memory pool has available.
	 *
	 * @return The number of free packets.
	 */
	@Contract(pure = true)
	public abstract int getSize();

	/**
	 * Allocates the packages using the given direct memory address.
	 *
	 * @param mmanager  The memory manager.
	 * @param dmaMemory The direct memory address.
	 */
	public abstract void allocate(@NotNull IxyMemoryManager mmanager, @NotNull IxyDmaMemory dmaMemory);

	/**
	 * Returns a free packet.
	 * <p>
	 * If no free packets are available, {@code null} will be returned.
	 *
	 * @return A free packet.
	 */
	public abstract @Nullable IxyPacketBuffer get();

	/**
	 * Populates the given array with the given amount of packets (if possible) starting at the given offset.
	 *
	 * @param buffer The array where the packets will be saved.
	 * @param offset The position to start extracting packets to.
	 * @param size   The amount of packets to extract.
	 * @return The number of packet buffers.
	 */
	@Contract(mutates = "param1")
	@SuppressWarnings("ConstantConditions")
	public int get(@NotNull IxyPacketBuffer[] buffer, int offset, int size) {
		if (!BuildConfig.OPTIMIZED) {
			if (offset < 0 || offset >= buffer.length) throw new InvalidOffsetException("offset");
			if (size < 0) throw new InvalidSizeException("size");
			size = Math.min(buffer.length - offset, size);
		}
		if (BuildConfig.DEBUG) log.debug("Extracting {} packets starting at index {}", size, offset);
		val max = Math.min(size, getSize());
		var i = 0;
		while (i < max) {
			buffer[i++] = get();
		}
		return i;
	}

	/**
	 * Populates the given array with the maximum amount of packets (if possible) starting at the given offset.
	 *
	 * @param buffer The array where the packets will be saved.
	 * @param offset The position to start extracting packets to.
	 * @return The number of packet buffers.
	 */
	@Contract(mutates = "param1")
	public int get(@NotNull IxyPacketBuffer[] buffer, int offset) {
		return get(buffer, offset, buffer.length - offset);
	}

	/**
	 * Populates the given array with the maximum amount of packets (if possible).
	 *
	 * @param buffer The array where the packets will be saved.
	 * @return The number of packet buffers.
	 */
	@Contract(mutates = "param1")
	public int get(@NotNull IxyPacketBuffer[] buffer) {
		return get(buffer, 0, buffer.length);
	}

	/**
	 * Frees a packet.
	 * <p>
	 * This does not free the memory allocated to the packet, but registers it as free so that it can be reused again.
	 *
	 * @param packet The packet to free.
	 */
	public abstract void free(@NotNull IxyPacketBuffer packet);

	/**
	 * Frees the given amount of packets (if possible) starting from the given offset.
	 * <p>
	 * This does not free the memory allocated to the packets, but registers it as free so that it can be reused again.
	 *
	 * @param packets The packet to free.
	 * @param offset  The position to start extracting packets from.
	 * @param size    The amount of packets to extract.
	 * @return The number of packets that have been freed.
	 */
	@Contract(mutates = "param1")
	@SuppressWarnings({"AssignmentToNull", "ConstantConditions", "PMD.NullAssignment"})
	public int free(@NotNull IxyPacketBuffer[] packets, int offset, int size) {
		if (!BuildConfig.OPTIMIZED) {
			if (offset < 0 || offset >= packets.length) throw new InvalidOffsetException("offset");
			if (size < 0) throw new InvalidSizeException("size");
			size = Math.min(packets.length - offset, size);
		}
		if (BuildConfig.DEBUG) log.debug("Freeing {} packets starting at index {}", size, offset);
		val end = offset + Math.min(size, capacity - getSize());
		if (BuildConfig.OPTIMIZED) {
			var i = offset;
			while (i < end) {
				free(packets[i]);
				packets[i++] = null;
			}
			return i - offset;
		} else {
			var count = 0;
			for (var i = offset; i < end; i += 1) {
				val packet = packets[i];
				if (packet != null) {
					free(packet);
					count += 1;
				}
				packets[i] = null;
			}
			return count;
		}
	}

	/**
	 * Frees the maximum amount of packets starting from the given offset.
	 * <p>
	 * This does not free the memory allocated to the packets, but registers it as free so that it can be reused again.
	 * @param offset  The position to start extracting packets from.
	 * @param packets The packet to free.
	 *
	 * @return The number of packets that have been freed.
	 */
	@Contract(mutates = "param1")
	public int free(@NotNull IxyPacketBuffer[] packets, int offset) {
		return free(packets, offset, packets.length - offset);
	}

	/**
	 * Frees the maximum amount of packets.
	 * <p>
	 * This does not free the memory allocated to the packets, but registers it as free so that it can be reused again.
	 *
	 * @param packets The packet to free.
	 * @return The number of packets that have been freed.
	 */
	@Contract(mutates = "param1")
	public int free(@NotNull IxyPacketBuffer[] packets) {
		return free(packets, 0, packets.length);
	}

	/**
	 * Registers the memory pool.
	 * <p>
	 * Idempotently registers the memory pool in the system.
	 */
	public abstract void register();

	/**
	 * Deregisters the memory pool.
	 * <p>
	 * Idempotently registers the memory pool in the system.
	 */
	public abstract void deregister();

	/**
	 * Finds a memory pool given its identifier.
	 *
	 * @param id The memory pool identifier.
	 * @return The memory pool.
	 */
	@Contract(pure = true)
	public abstract @Nullable IxyMempool find(int id);

	/**
	 * Finds the memory pool that owns a packet.
	 *
	 * @param packet The packet whose memory pool will be searched.
	 * @return The memory pool that owns a packet.
	 */
	@Contract(value = "null -> null", pure = true)
	public @Nullable IxyMempool find(@Nullable IxyPacketBuffer packet) {
		return packet == null ? null : find(packet.getMemoryPoolId());
	}

}
