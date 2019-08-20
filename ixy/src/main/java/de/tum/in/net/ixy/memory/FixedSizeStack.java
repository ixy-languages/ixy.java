package de.tum.in.net.ixy.memory;

import lombok.ToString;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;

/**
 * A non-thread-safe fixed-size stack for packet buffer wrappers with little to no parameter checking.
 * <p>
 * This class was used to debug an error with the memory pool, after solving the error it was tested and it offers no
 * noticeable speed up.
 *
 * @author Esaú García Sánchez-Torija
 */
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
final class FixedSizeStack {

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The capacity of the stack. */
	@ToString.Include(rank = 2)
	private final int capacity;

	/** The internal data store. */
	private final PacketBufferWrapper[] buffers;

	/** The index of the stack. */
	@ToString.Include(rank = 1)
	private int top = -1;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Initializes the internal store with the fixed size.
	 *
	 * @param capacity The capacity.
	 */
	FixedSizeStack(final int capacity) {
		this.capacity = capacity;
		buffers = new PacketBufferWrapper[capacity];
	}

	/**
	 * Pushes a new {@link PacketBufferWrapper packet buffer wrapper}.
	 *
	 * @param packetBufferWrapper The packet buffer wrapper.
	 */
	void push(final @NotNull PacketBufferWrapper packetBufferWrapper) {
		if (!OPTIMIZED) {
			if (packetBufferWrapper == null) throw new IllegalArgumentException("Null packets are not accepted.");
			if (top >= capacity - 1) throw new IllegalStateException("The stack is full.");
		}
		buffers[++top] = packetBufferWrapper;
	}

	/**
	 * Pops a new {@link PacketBufferWrapper packet buffer wrapper}.
	 *
	 * @return The packet buffer wrapper.
	 */
	@Nullable PacketBufferWrapper pop() {
		if (top < 0) return null;
		if (OPTIMIZED) {
			return buffers[top--];
		} else {
			val packetBufferWrapper = buffers[top];
			buffers[top--] = null;
			return packetBufferWrapper;
		}
	}

	//////////////////////////////////////////////// COLLECTION METHODS ////////////////////////////////////////////////

	/**
	 * Returns the capacity of the stack.
	 *
	 * @return The capacity of the stack.
	 */
	@Contract(pure = true)
	int capacity() {
		return capacity;
	}

	/**
	 * Returns the size of the stack.
	 *
	 * @return The size of the stack.
	 */
	@Contract(pure = true)
	int size() {
		return top + 1;
	}

}
