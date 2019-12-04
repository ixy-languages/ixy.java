package de.tum.in.net.ixy.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

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
import static de.tum.in.net.ixy.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI_FULL;

/**
 * A collection of {@link PacketBufferWrapper wrapped packet buffers}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class Mempool {

	////////////////////////////////////////////////// STATIC MEMBERS //////////////////////////////////////////////////

	/** The memory manager. */
	@SuppressWarnings("NestedConditionalExpression")
	private static final MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	/** Holds a reference to every {@link Mempool memory pool} ever created. */
	private static final TreeMap<Long, Mempool> pools = new TreeMap<>();

	/** A variable that indicates the next id to use. */
	private static final AtomicLong nextId = new AtomicLong(0);

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Computes an identifier that is not being used by any other memory pool.
	 *
	 * @return An identifier that is not being used.
	 */
	@Contract(pure = true)
	private static long getValidId() {
		long id;
		do {
			id = nextId.getAndIncrement();
		} while (pools.containsKey(id));
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
	public static @Nullable Mempool find(final @NotNull PacketBufferWrapper packetBufferWrapper) {
		return find(packetBufferWrapper.getMemoryPoolPointer());
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/**
	 * A bunch of pre-allocated {@link PacketBufferWrapper} instances.
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
		id = getValidId();
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
		if (DEBUG >= LOG_DEBUG) log.debug("Allocating {} packets @ {}.", capacity, dma);

		// The base virtual address which will be incremented on every iteration
		val virtual = dma.getVirtual();

		// Allocate the packet buffer wrappers
		for (var i = capacity - 1; i >= 0; i--) {
			val addr = virtual + i*entrySize;
			val packet = new PacketBufferWrapper(addr);
			packet.setPhysicalAddress(mmanager.virt2phys(addr));
			packet.setMemoryPoolPointer(id);
			packet.setSize(entrySize - PacketBufferWrapperConstants.HEADER_BYTES);

			// Trace message
			if (DEBUG >= LOG_TRACE) log.trace("Allocated packet buffer wrapper #{}: {}", i, packet);

			// Add the packet buffer wrapper
			packetBufferWrappers.push(packet);
		}
	}

	/**
	 * Pops up to {@code size} elements from the original store and stores them in the parameter {@code buffers},
	 * starting at the index {@code offset}.
	 *
	 * @param buffers The array where the packet buffer wrappers will be stored.
	 * @param offset  The offset to start storing to.
	 * @param size    The maximum amount of packets to extract.
	 * @return The amount of extracted buffers.
	 */
	@Contract(mutates = "param1")
	@SuppressWarnings("PMD.AssignmentInOperand")
	private int pop(final @NotNull PacketBufferWrapper[] buffers, int offset, int size) {
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
				if ((buffers[offset++] = packetBufferWrappers.pop()) == null) {
					size += 1;
					break;
				}
			}
			return sizeCopy - size - 1;
		} else {
			var counter = 0;
			while (size-- > 0) {
				val buffer = packetBufferWrappers.pop();
				if (buffer != null) {
					buffers[offset++] = buffer;
					counter += 1;
				} else {
					break;
				}
			}
			return counter;
		}
	}

	/**
	 * Wrapper of {@link #pop(PacketBufferWrapper[], int, int)} with the {@code offset} set to {@code 0} and the {@code
	 * size} to the maximum possible value.
	 *
	 * @param buffers The array where the packet buffer wrappers will be stored.
	 * @return The amount of extracted buffers.
	 */
	@Contract(mutates = "param1")
	public int pop(final @NotNull PacketBufferWrapper[] buffers) {
		return pop(buffers, 0, buffers.length);
	}

	//////////////////////////////////////////////// DELEGATED METHODS /////////////////////////////////////////////////

	/**
	 * Frees a {@link PacketBufferWrapper packet buffer wrapper}.
	 *
	 * @param buffer The packet buffer wrapper.
	 */
	public void push(final @NotNull PacketBufferWrapper buffer) {
		if (!OPTIMIZED && buffer == null) throw new NullPointerException("The parameter 'buffer' MUST NOT be null.");
		if (DEBUG >= LOG_TRACE) log.trace("Pushing packet buffer wrapper to the memory pool: {}", buffer);
		packetBufferWrappers.offerFirst(buffer);
	}

	/**
	 * Allocates a {@link PacketBufferWrapper packet buffer wrapper}.
	 *
	 * @return The packet buffer wrapper.
	 */
	public @Nullable PacketBufferWrapper pop() {
		if (DEBUG >= LOG_TRACE) log.trace("Extracting packet buffer wrapper or null.");
		return packetBufferWrappers.pollFirst();
	}

	/**
	 * Returns the capacity of the memory pool.
	 *
	 * @return The capacity of the memory pool.
	 */
	@Contract(pure = true)
	public int capacity() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking capacity.");
		return capacity;
	}

	/**
	 * Returns the size of the memory pool.
	 *
	 * @return The size of the memory pool.
	 */
	@Contract(pure = true)
	public int size() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking size.");
		return packetBufferWrappers.size();
	}

}