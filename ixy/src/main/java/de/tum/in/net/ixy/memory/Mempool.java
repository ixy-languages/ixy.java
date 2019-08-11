package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.memory.internal.ImmutableIterator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeMap;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;

/**
 * A collection of {@link PacketBufferWrapper wrapped packet buffers}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class Mempool implements Queue<PacketBufferWrapper>, Comparable<Mempool> {

	////////////////////////////////////////////////// STATIC MEMBERS //////////////////////////////////////////////////

	/** Holds a reference to every {@link Mempool memory pool} ever created. */
	private static final TreeMap<Long, Mempool> pools = new TreeMap<>();

	/** A variable that indicates if an overflow of {@link #pools} keys has been reached. */
	private static boolean notOverflow = true;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Computes an identifier that is not being used by any other memory pool.
	 *
	 * @return An identifier that is not being used.
	 */
	@Contract(pure = true)
	private static long getValidId() {
		long id;
		if (notOverflow) {
			val last = pools.lastKey();
			id = last + 1;
			if (id < last) {
				notOverflow = false;
				id = getValidId();
			}
		} else {
			val last = pools.firstKey();
			id = last - 1;
			if (id > last) throw new IllegalStateException("No more memory pool ids available.");
		}
		if (DEBUG >= LOG_DEBUG) log.debug("Found valid memory pool id '{}'.", id);
		return id;
	}

	/**
	 * Returns the memory pool instance that has the given {@code id}.
	 *
	 * @param id The memory pool id.
	 * @return The memory pool instance.
	 */
	@Contract(pure = true)
	private static @Nullable Mempool find(final long id) {
		return pools.get(id);
	}

	/**
	 * Returns the memory pool instance that has the same identifier as the given packet buffer wrapper.
	 *
	 * @param packetBufferWrapper The packet buffer wrapper.
	 * @return The memory pool instance.
	 */
	@Contract(pure = true)
	public static @Nullable Mempool find(final @Nullable PacketBufferWrapper packetBufferWrapper) {
		return packetBufferWrapper == null ? null : find(packetBufferWrapper.getMemoryPoolId());
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * Double ended queue with a bunch of pre-allocated {@link PacketBufferWrapper} instances.
	 * -- GETTER --
	 * Returns the internal storage of packets.
	 *
	 * @return The internal packet storage.
	 */
	@ToString.Include
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@Getter(AccessLevel.PROTECTED)
	private final @NotNull Deque<PacketBufferWrapper> packetBufferWrappers;

	/**
	 * The unique identifier of the memory pool.
	 * -- GETTER --
	 * Returns the memory pool identifier.
	 *
	 * @return The memory pool identifier.
	 */
	@Getter
	@SuppressWarnings("JavaDoc")
	private final long id;

	/**
	 * The capacity of the memory pool.
	 * -- GETTER --
	 * Returns the memory pool capacity.
	 *
	 * @return The memory pool capacity.
	 */
	@Getter
	@SuppressWarnings("JavaDoc")
	private final int capacity;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a memory pool that manages a finite amount of packets.
	 *
	 * @param capacity The capacity of the memory pool.
	 */
	@SuppressWarnings("ThisEscapedInObjectConstruction")
	public Mempool(final int capacity) {
		if (DEBUG >= LOG_TRACE) log.trace("Creating memory pool.");
		this.capacity = capacity;
		this.packetBufferWrappers = new ArrayDeque<>(capacity);
		id = pools.isEmpty() ? 0 : getValidId();
		pools.put(id, this);
	}

	/**
	 * Allocates as many packet buffers as indicated by {@link #capacity} and configures their virtual and physical
	 * addresses with the given parameter {@code dma}.
	 *
	 * @param entrySize The size of a packet buffer wrapper.
	 * @param dma       The base address of the allocated memory region.
	 */
	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	@SuppressWarnings({"PMD.AssignmentInOperand", "PMD.DataflowAnomalyAnalysis"})
	public void allocate(final int entrySize, final @NotNull DmaMemory dma) {
		if (DEBUG >= LOG_DEBUG) log.debug("Allocating {} packets @ {}", capacity, dma);

		// Discard previous packets, if any
		if (!packetBufferWrappers.isEmpty()) {
			if (DEBUG >= LOG_TRACE) log.warn("Discarding previous packet buffer wrapper instances.");
			var size = packetBufferWrappers.size();
			while (size-- > 0) {
				val buffer = packetBufferWrappers.pollFirst();
				val bufferId = buffer.getMemoryPoolId();
				if (bufferId != id) {
					find(bufferId).packetBufferWrappers.offerLast(buffer);
				}
			}
		}

		// The base virtual and physical address which will be incremented on every iteration
		var virtual = dma.getVirtual();
		var physical = dma.getPhysical();

		// Allocate the packet buffer wrappers
		for (int i = 0; i < capacity; i += 1) {
			val packet = new PacketBufferWrapper(virtual);
			packet.setPhysicalAddress(physical);
			packet.setMemoryPoolId(id);
			packet.setSize(entrySize - PacketBufferWrapper.DATA_OFFSET);

			// Trace message
			if (DEBUG >= LOG_TRACE) log.trace("Allocated packet buffer wrapper #{}: {}", i, packet);

			// Add the packet buffer wrapper and update the addresses
			packetBufferWrappers.offerLast(packet);
			virtual += entrySize;
			physical += entrySize;
		}
	}

	/**
	 * Polls up to {@code size} elements from the original store and stores them in the parameter {@code buffers},
	 * starting at the index {@code offset}.
	 *
	 * @param buffers The array where the packet buffer wrappers will be stored.
	 * @param offset  The offset to start storing to.
	 * @param size    The maximum amount of packets to extract.
	 * @return The amount of extracted buffers.
	 */
	@Contract(mutates = "param1")
	@SuppressWarnings("PMD.AssignmentInOperand")
	private int pollFirst(final @NotNull PacketBufferWrapper[] buffers, int offset, int size) {
		if (!OPTIMIZED) {
			if (buffers == null) throw new NullPointerException("The parameter 'buffers' MUST NOT be null.");
			if (offset < 0) throw new IndexOutOfBoundsException("The parameter 'offset' MUST be positive.");
			if (size < 0) throw new IllegalArgumentException("The parameter 'size' MUST be positive.");
			if (offset >= buffers.length) {
				throw new IllegalArgumentException("The parameter 'offset' MUST be smaller than the length of the "
						+ "parameter 'buffer'.");
			}

			// Adapt the size based on the offset
			val diff = buffers.length - offset;
			if (diff < size) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to extract {} PacketBufferWrappers but the buffer can only store up to {}."
							+ " Adapting size.", size, diff);
				}
				size = diff;
			}
		}

		// Adapt the size based on the amount of free instances
		val available = packetBufferWrappers.size();
		if (available < size) {
			if (DEBUG >= LOG_WARN) {
				log.warn("You are trying to extract {} PacketBufferWrappers but there are only {} available."
						+ " Adapting size.", size, available);
			}
			size = available;
		}

		// Trace message
		if (DEBUG >= LOG_TRACE) {
			val max = offset + size;
			log.trace("Extracting {} packets starting @ #{}.", max, offset);
		}

		// Extract the packet buffer wrappers
		if (OPTIMIZED) {
			val sizeCopy = size;
			while (size-- > 0) {
				buffers[offset++] = packetBufferWrappers.pollFirst();
			}
			return sizeCopy;
		} else {
			var counter = 0;
			while (size-- > 0) {
				val buffer = packetBufferWrappers.pollFirst();
				if (buffer != null) {
					buffers[offset++] = buffer;
					counter += 1;
				}
			}
			return counter;
		}
	}

	/**
	 * Wrapper of {@link #pollFirst(PacketBufferWrapper[], int, int)} with the {@code offset} set to {@code 0} and the
	 * {@code size} to the maximum possible value.
	 *
	 * @param buffers The array where the packet buffer wrappers will be stored.
	 * @return The amount of extracted buffers.
	 */
	@Contract(mutates = "param1")
	public int pollFirst(final @NotNull PacketBufferWrapper[] buffers) {
		return pollFirst(buffers, 0, buffers.length);
	}

	//////////////////////////////////////////////// COLLECTION METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int size() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking size.");
		return packetBufferWrappers.size();
	}

	@Override
	@Contract(pure = true)
	public boolean isEmpty() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking emptiness.");
		return packetBufferWrappers.isEmpty();
	}

	@Override
	@Contract(pure = true)
	public boolean contains(final @NotNull Object buffer) {
		if (!OPTIMIZED && buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) {
			log.trace("Checking whether packet buffer wrapper is inside the memory pool: {}", buffer);
		}
		return packetBufferWrappers.contains(buffer);
	}

	@Override
	@Contract(pure = true)
	public @NotNull Iterator<PacketBufferWrapper> iterator() {
		if (DEBUG >= LOG_TRACE) log.trace("Creating immutable iterator.");
		return new ImmutableIterator<>(packetBufferWrappers.iterator());
	}

	@Override
	@Contract(pure = true)
	public @NotNull Object[] toArray() {
		if (DEBUG >= LOG_TRACE) log.trace("Returning Object array store.");
		return packetBufferWrappers.toArray();
	}

	@Override
	@Contract(pure = true)
	public <T> @NotNull T[] toArray(final @NotNull T[] a) {
		if (!OPTIMIZED && a == null) throw new NullPointerException("Null arrays are not supported.");
		if (DEBUG >= LOG_TRACE) log.trace("Returning T array store.");
		return packetBufferWrappers.toArray(a);
	}

	@Override
	public boolean add(final @NotNull PacketBufferWrapper buffer) {
		if (!OPTIMIZED && buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) log.trace("Adding packet buffer wrapper to the memory pool.");
		return packetBufferWrappers.add(buffer);
	}

	@Override
	@Deprecated
	@Contract(value = "_ -> fail", pure = true)
	public boolean remove(final @NotNull Object buffer) {
		if (!OPTIMIZED && buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
		throw new UnsupportedOperationException("Cannot remove elements from the collection without returning them.");
	}

	@Override
	@Contract(pure = true)
	public boolean containsAll(final @NotNull Collection<?> buffers) {
		if (!OPTIMIZED && buffers == null) throw new NullPointerException("The parameter 'buffers' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) log.trace("Checking whether packet buffer wrapper are inside the memory pool.");
		return packetBufferWrappers.containsAll(buffers);
	}

	@Override
	public boolean addAll(final @NotNull Collection<? extends PacketBufferWrapper> buffers) {
		if (!OPTIMIZED && buffers == null) throw new NullPointerException("The parameter 'buffers' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) log.trace("Adding packet buffer wrappers to the memory pool.");
		return packetBufferWrappers.addAll(buffers);
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_ -> fail", pure = true)
	public boolean removeAll(final @NotNull Collection<?> buffers) {
		if (!OPTIMIZED && buffers == null) throw new NullPointerException("The parameter 'buffers' MUST NOT be null.");
		throw new UnsupportedOperationException("Cannot remove elements from the collection without returning them.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = "_ -> fail", pure = true)
	public boolean retainAll(final @NotNull Collection<?> buffers) {
		if (!OPTIMIZED && buffers == null) throw new NullPointerException("The parameter 'buffers' MUST NOT be null.");
		throw new UnsupportedOperationException("Cannot remove elements from the collection without returning them.");
	}

	/**
	 * {@inheritDoc}
	 * @deprecated Not supported.
	 */
	@Override
	@Deprecated
	@Contract(value = " -> fail", pure = true)
	public void clear() {
		if (DEBUG >= LOG_TRACE) log.trace("Clearing memory pool.");
		throw new UnsupportedOperationException("Cannot remove elements from the collection without returning them.");
	}

	////////////////////////////////////////////////// QUEUE METHODS ///////////////////////////////////////////////////

	@Override
	public boolean offer(final @NotNull PacketBufferWrapper buffer) {
		if (!OPTIMIZED && buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) log.trace("Adding packet buffer wrappers to the memory pool: {}", buffer);
		return packetBufferWrappers.offer(buffer);
	}

	@Override
	@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
	public @NotNull PacketBufferWrapper remove() {
		if (DEBUG >= LOG_TRACE) log.trace("Extracting packet buffer wrapper or failing.");
		return packetBufferWrappers.remove();
	}

	@Override
	public @Nullable PacketBufferWrapper poll() {
		if (DEBUG >= LOG_TRACE) log.trace("Extracting packet buffer wrapper or null.");
		return packetBufferWrappers.poll();
	}

	@Override
	@Contract(pure = true)
	public @NotNull PacketBufferWrapper element() {
		if (DEBUG >= LOG_TRACE) log.trace("Returning the head or failing.");
		return packetBufferWrappers.element();
	}

	@Override
	@Contract(pure = true)
	public @Nullable PacketBufferWrapper peek() {
		if (DEBUG >= LOG_TRACE) log.trace("Returning the head or null.");
		return packetBufferWrappers.peek();
	}

	//////////////////////////////////////////////// COMPARABLE METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public int compareTo(final @NotNull Mempool mempool) {
		if (DEBUG >= LOG_TRACE) log.trace("Comparing with another Mempool.");
		return Long.compare(id, mempool.id);
	}

}