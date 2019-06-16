package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeMap;

/**
 * Simple implementation of Ixy's memory pool specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class Mempool implements IxyMempool, Comparable<Mempool> {

	////////////////////////////////////////////////// STATIC MEMBERS //////////////////////////////////////////////////

	/** Holds a reference to every {@link Mempool} ever created. */
	private static final TreeMap<Integer, Mempool> pools = new TreeMap<>();

	/** A variable that indicates if an overflow of {@link #pools} keys has been reached. */
	private static boolean reversed = false;

	/**
	 * Computes an identifier that is not already being used by another memory pool.
	 *
	 * @return An identifier that is not being used.
	 */
	private static int getValidId() {
		// By default increase the id until overflow, then change order because items are never removed
		int id;
		if (reversed) {
			val first = pools.firstKey();
			id = first - 1;
			if (id > first) {
				reversed = true;
				id = getValidId();
			}
		} else {
			val last = pools.lastKey();
			id = last + 1;
			if (id < last) throw new RuntimeException("No more memory pool ids available");
		}
		if (BuildConfig.DEBUG) log.trace("Found valid memory pool id {}", id);
		return id;
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** Double ended queue with a bunch a pre-allocated {@link IxyPacketBuffer} instances. */
	@EqualsAndHashCode.Include
	@ToString.Include
	private @NotNull Deque<IxyPacketBuffer> buffers;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	@Getter(onMethod_ = {@Contract(pure = true)})
	@EqualsAndHashCode.Include
	@ToString.Include
	private int id;

	@Getter(onMethod_ = {@Contract(pure = true)})
	@EqualsAndHashCode.Include
	@ToString.Include
	private final int capacity;

	@Getter(onMethod_ = {@Contract(pure = true)})
	@EqualsAndHashCode.Include
	@ToString.Include
	private final int packetSize;

	/**
	 * Creates a memory pool that manages a finite amount of packets.
	 *
	 * @param capacity   The capacity of the memory pool.
	 * @param packetSize The size of each packet.
	 */
	public Mempool(int capacity, int packetSize) {
		id = 0;
		this.capacity = capacity;
		this.packetSize = packetSize;
		buffers = new ArrayDeque<>(capacity);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int getSize() {
		return buffers.size();
	}

	@Override
	public @Nullable IxyPacketBuffer get() {
		if (BuildConfig.DEBUG) log.info("Popping free packet");
		if (!BuildConfig.OPTIMIZED) {
			return buffers.isEmpty() ? null : buffers.pop();
		} else {
			return buffers.pop();
		}
	}

	@Override
	@Contract("null -> fail")
	public void free(@NotNull IxyPacketBuffer packet) {
		if (BuildConfig.DEBUG) log.info("Pushing new packet: {}", packet);
		if (!BuildConfig.OPTIMIZED && packet == null) throw new InvalidNullParameterException("packet");
		buffers.push(packet);
	}

	@Override
	public void register() {
		if (!buffers.isEmpty()) id = getValidId();
		pools.put(id, this);
		if (BuildConfig.DEBUG) log.info("There are {} memory pools registered", pools.size());
	}

	@Override
	@Contract(pure = true)
	public @Nullable IxyMempool find(int id) {
		return pools.get(id);
	}

	@Override
	@Contract(pure = true)
	public int compareTo(@NotNull Mempool o) {
		if (BuildConfig.DEBUG) log.trace("Comparing with another Mempool");
		if (!BuildConfig.OPTIMIZED) throw new InvalidNullParameterException("mempool");
		return Integer.compare(id, o.id);
	}

}