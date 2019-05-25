package de.tum.in.net.ixy.memory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a memory region reserved for a specific number of {@link PacketBuffer}s.
 * <p>
 * This class implements the {@link Comparable} and {@link Collection} interfaces to better integrate with the Java
 * ecosystem, although it is not entirely compliant nor useful. This class only implements the methods that allow adding
 * elements, that is to say, the methods that allow to add back again the free {@link PacketBuffer}.
 * <p>
 * The main goal of this class is to enable easy management of {@link PacketBuffer} instances.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class Mempool implements Comparable<Mempool>, Collection<PacketBuffer> {

	/** The message of the exceptions thrown by the unsupported methods */
	private static final String UNSUPPORTED_MESSAGE = "Only methods that return or add packet buffers are implemented";

	/** Stores all the memory pools ever created. */
	private static final TreeMap<Integer, Mempool> pools = new TreeMap<>();

	/**
	 * Computes an id that is not already being used by another memory pool.
	 *
	 * @param id The initial id to be used.
	 * @return An id that is not being used.
	 */
	private static int getValidId(int id) {
		while (pools.containsKey(id)) {
			id += 1;
		}
		if (BuildConfig.DEBUG) log.trace("Found a valid memory pool id {}", id);
		return id;
	}

	/**
	 * Adds a memory pool to {@link #pools}.
	 * <p>
	 * If the memory pool id is already in the {@link #pools} collection, then a new id is generated and the instance
	 * is updated.
	 *
	 * @param mempool The memory pool to add.
	 * @return Whether the memory pool collection has been updated or not.
	 * @see Mempool#getValidId(int)
	 */
	public static boolean addMempool(final Mempool mempool) {
		if (BuildConfig.DEBUG) log.trace("Adding a memory pool");
		if (mempool == null) {
			return false;
		}
		val id = getValidId(mempool.id);
		mempool.id = id;
		pools.put(id, mempool);
		return true;
	}

	/**
	 * The unique identifier of the memory pool.
	 * --------------------- GETTER ---------------------
	 * Returns the unique identifier of the memory pool.
	 *
	 * @return The unique identifier of the memory pool.
	 */
	@Getter
	private int id;

	/**
	 * The base address of the memory pool.
	 * ------------------ GETTER ------------------
	 * Returns the base address of the memory pool.
	 *
	 * @return The base address of the memory pool.
	 */
	@Getter
	private long baseAddress;

	/**
	 * The size of the packet buffers.
	 * ---------------- GETTER ----------------
	 * Returns the size of the packet buffers.
	 *
	 * @return The size of the packet buffers.
	 */
	@Getter
	private int packetBufferSize;

	/**
	 * The number of entries the pool has.
	 * ------------------ GETTER ------------------
	 * Returns the number of entries the pool has.
	 *
	 * @return The number of entries the pool has.
	 */
	@Getter
	private int entrySize;

	/** Double ended queue with a bunch a pre-allocated {@link PacketBuffer} instances. */
	private transient Deque<PacketBuffer> buffers;

	/**
	 * Creates an instance an empty instance.
	 * <p>
	 * In order to use the instance, all the {@link PacketBuffer}s must be allocated.
	 * <p>
	 * This constructor automatically keeps track of the created instances by adding them to the list {@link #pools}.
	 *
	 * @param address The base address of the memory pool.
	 * @param size    The size of each buffer inside the memory pool.
	 * @param entries The number of entries to smartAllocate.
	 * @see #allocate()
	 */
	public Mempool(final long address, final int size, final int entries) {
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Creating memory pool with {} entries, a total size of {} bytes @ 0x{}", entries, size, xaddress);
		}
		baseAddress = address;
		packetBufferSize = size;
		entrySize = entries;
		id = pools.isEmpty() ? 0 : getValidId(pools.lastKey());
		pools.put(id, this);
		if (BuildConfig.DEBUG) log.info("There are {} memory pools", pools.size());
	}

	/** Pre-allocates all the {@link PacketBuffer}s. */
	public void allocate() {
		if (BuildConfig.DEBUG) log.trace("Allocating packet buffers");
		buffers = new ArrayDeque<>(entrySize);
		for (var i = 0; i < entrySize; i += 1) {
			val virt = baseAddress + i * packetBufferSize;
			val buffer = new PacketBuffer(virt);
			buffer.setPhysicalAddress(Memory.virt2phys(virt));
			buffer.setSize(0);
			buffers.push(buffer);
		}
	}

	/**
	 * Returns a free pre-allocated packet buffer instance.
	 * <p>
	 * If there are no more free {@link PacketBuffer}s available, then an empty instance is returned.
	 *
	 * @return A free packet buffer or a dummy instance.
	 * @see PacketBuffer#empty()
	 */
	@NonNull
	public PacketBuffer pop() {
		if (BuildConfig.DEBUG) {
			log.trace("Obtaining a free packet buffer");
			if (buffers.isEmpty()) {
				log.warn("There are no free packet buffers, a dummy packet buffer will be returned");
				return PacketBuffer.empty();
			}
			return buffers.pop();
		} else {
			return buffers.isEmpty() ? PacketBuffer.empty() : buffers.pop();
		}
	}

	//////////////////////////////////////////////// OVERRIDEN METHODS /////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object mempool) {
		if (BuildConfig.DEBUG) log.trace("Comparing with another Object");
		if (mempool instanceof Mempool) {
			val casted = (Mempool) mempool;
			return id == casted.id && baseAddress == casted.baseAddress;
		} else {
			return super.equals(mempool);
		}
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		if (BuildConfig.DEBUG) log.trace("Computing hash code");
		return Objects.hash(id, baseAddress);
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(@NonNull final Mempool mempool) {
		if (BuildConfig.DEBUG) log.trace("Comparing with another Mempool");
		return id - mempool.id;
	}

	/** {@inheritDoc} */
	public int size() {
		return buffers.size();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmpty() {
		if (BuildConfig.DEBUG) log.trace("Checking if Mempool is empty");
		return buffers.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean add(final PacketBuffer packetBuffer) {
		if (BuildConfig.DEBUG) log.trace("Adding PacketBuffer to the memory pool");
		if (packetBuffer == null) {
			if (BuildConfig.DEBUG) log.warn("Skipping invalid PacketBuffer that is null");
			return false;
		} else if (buffers.size() >= entrySize) {
			if (BuildConfig.DEBUG) log.warn("Memory pool queue is already full, cannot add free PacketBuffer");
			return false;
		}
		buffers.add(packetBuffer);
		return true;
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public boolean addAll(final Collection<? extends PacketBuffer> packetBuffers) {
		if (BuildConfig.DEBUG) log.trace("Adding more than one PacketBuffer to the memory pool");
		if (packetBuffers == null) {
			return false;
		}
		val amount = packetBuffers.size();
		val size = buffers.size();
		if (amount <= 0) {
			return false;
		} else if (size >= entrySize) {
			if (BuildConfig.DEBUG) log.warn("Memory pool queue is already full, cannot add any free PacketBuffer");
			return false;
		}
		var remaining = entrySize - size;
		if (BuildConfig.DEBUG && amount > remaining) {
			log.warn("Memory pool queue will be full, cannot add all free PacketBuffers");
		}
		for (val packetBuffer : packetBuffers) {
			if (packetBuffer != null) {
				buffers.add(packetBuffer);
				remaining -= 1;
				if (remaining == 0) break;
			} else if (BuildConfig.DEBUG) {
				log.warn("Skipping invalid PacketBuffer that is null");
			}
		}
		return size != buffers.size();
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public Iterator<PacketBuffer> iterator() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public void forEach(Consumer<? super PacketBuffer> action) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public Spliterator<PacketBuffer> spliterator() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}


	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean contains(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean containsAll(final Collection<?> c) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public Object[] toArray() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public <T> T[] toArray(final T[] a) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	@Override
	@Deprecated
	public <T> T[] toArray(final IntFunction<T[]> generator) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean removeIf(Predicate<? super PacketBuffer> filter) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean removeAll(final Collection<?> packetBuffers) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public boolean retainAll(final Collection<?> packetBuffers) {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public Stream<PacketBuffer> stream() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public Stream<PacketBuffer> parallelStream() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

	/** {@inheritDoc} */
	@Override
	@Deprecated
	public void clear() {
		throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
	}

}