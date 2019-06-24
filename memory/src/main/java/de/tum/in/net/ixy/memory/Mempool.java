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
import java.util.Objects;
import java.util.TreeMap;

/**
 * Simple implementation of Ixy's memory pool specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
public final class Mempool extends IxyMempool implements Comparable<Mempool> {

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
	private final @NotNull Deque<IxyPacketBuffer> buffers;

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
		if (BuildConfig.DEBUG) log.info("Popping a free packet.");
		if (BuildConfig.OPTIMIZED) {
			return buffers.pop();
		} else {
			return buffers.isEmpty() ? null : buffers.pop();
		}
	}

	@Override
	@Contract(value = "null, _, _ -> fail", mutates = "param1")
	public int get(@Nullable IxyPacketBuffer[] buffer, int offset, int size) {
		if (!BuildConfig.OPTIMIZED) {
			if (buffer == null) throw new InvalidNullParameterException("buffer");
			if (offset < 0) throw new InvalidOffsetException("offset");
			if (size < 0) throw new InvalidSizeException("size");
			size = Math.min(buffer.length - offset, size);
		}
		val max = Math.min(size, getSize());
		if (BuildConfig.DEBUG) log.debug("Extracting {} packets starting at index {}", max, offset);
		var i = 0;
		while (i < max) {
			buffer[i++] = buffers.pop();
		}
		return i;
	}

	@Override
	@Contract(value = "null, _ -> fail", mutates = "param1")
	public int get(@Nullable IxyPacketBuffer[] buffer, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (buffer == null) throw new InvalidNullParameterException("buffer");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val max = Math.min(buffer.length - offset, getSize());
		if (BuildConfig.DEBUG) log.debug("Extracting {} packets starting at index {}", max, offset);
		var i = 0;
		while (i < max) {
			buffer[i++] = buffers.pop();
		}
		return i;
	}

	@Contract(value = "null -> fail", mutates = "param1")
	public int get(@Nullable IxyPacketBuffer[] buffer) {
		if (!BuildConfig.OPTIMIZED && buffer == null) throw new InvalidNullParameterException("buffer");
		val max = Math.min(buffer.length, getSize());
		if (BuildConfig.DEBUG) log.debug("Extracting {} packets starting at index 0", max);
		var i = 0;
		while (i < max) {
			buffer[i++] = buffers.pop();
		}
		return i;
	}

	@Override
	public void free(@NotNull IxyPacketBuffer packet) {
		if (BuildConfig.DEBUG) log.info("Pushing free packet: {}", packet);
		if (BuildConfig.OPTIMIZED) {
			buffers.push(packet);
		} else {
			if (packet == null) throw new InvalidNullParameterException("packet");
			if (buffers.size() < capacity) buffers.push(packet);
		}
	}

	@SuppressWarnings("PMD.NullAssignment")
	@Contract(value = "null, _, _ -> fail", mutates = "param1")
	public int free(@Nullable IxyPacketBuffer[] packets, int offset, int size) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
			if (size < 0) throw new InvalidSizeException("size");
			size = Math.min(packets.length - offset, size);
		}
		val max = Math.min(size, getSize());
		if (BuildConfig.DEBUG) log.debug("Pushing {} free packets starting at index {}", max, offset);
		if (BuildConfig.OPTIMIZED) {
			var i = 0;
			while (i < max) {
				buffers.push(packets[i]);
				packets[i++] = null;
			}
			return i;
		} else {
			var count = 0;
			for (var i = 0; i < max; i += 1) {
				val packet = packets[i];
				if (packet != null) {
					buffers.push(packet);
					count += 1;
				}
				packets[i++] = null;
			}
			return count;
		}
	}

	@SuppressWarnings("PMD.NullAssignment")
	@Contract(value = "null, _ -> fail", mutates = "param1")
	public int free(@Nullable IxyPacketBuffer[] packets, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val max = Math.min(packets.length - offset, getSize());
		if (BuildConfig.DEBUG) log.debug("Pushing {} free packets starting at index {}", max, offset);
		if (BuildConfig.OPTIMIZED) {
			var i = 0;
			while (i < max) {
				buffers.push(packets[i]);
				packets[i++] = null;
			}
			return i;
		} else {
			var count = 0;
			for (var i = 0; i < max; i += 1) {
				val packet = packets[i];
				if (packet != null) {
					buffers.push(packet);
					count += 1;
				}
				packets[i++] = null;
			}
			return count;
		}
	}

	@SuppressWarnings("PMD.NullAssignment")
	@Contract(value = "null -> fail", mutates = "param1")
	public int free(@Nullable IxyPacketBuffer[] packets) {
		if (!BuildConfig.OPTIMIZED && packets == null) throw new InvalidNullParameterException("packets");
		val max = Math.min(packets.length, getSize());
		if (BuildConfig.DEBUG) log.debug("Pushing {} free packets starting at index 0", max);
		if (BuildConfig.OPTIMIZED) {
			var i = 0;
			while (i < max) {
				buffers.push(packets[i]);
				packets[i++] = null;
			}
			return i;
		} else {
			var count = 0;
			for (var i = 0; i < max; i += 1) {
				val packet = packets[i];
				if (packet != null) {
					buffers.push(packet);
					count += 1;
				}
				packets[i++] = null;
			}
			return count;
		}
	}

	@Override
	public void register() {
		if (BuildConfig.DEBUG) log.info("Registering memory pool.");
		if (!buffers.isEmpty()) id = getValidId();
		pools.put(id, this);
		if (BuildConfig.DEBUG) log.info("There are {} memory pools registered.", pools.size());
	}

	@Override
	public void deregister() {
		if (BuildConfig.DEBUG) log.info("Deregistering memory pool.");
		val pool = pools.get(id);
		if (Objects.equals(this, pool)) {
			pools.remove(id);
			if (BuildConfig.DEBUG) log.info("There are {} memory pools registered.", pools.size());
		}
	}

	@Override
	@Contract(pure = true)
	public @Nullable IxyMempool find(int id) {
		return pools.get(id);
	}

	@Override
	@Contract(pure = true)
	public int compareTo(@NotNull Mempool o) {
		if (BuildConfig.DEBUG) log.trace("Comparing with another Mempool.");
		return Integer.compare(id, o.id);
	}

}