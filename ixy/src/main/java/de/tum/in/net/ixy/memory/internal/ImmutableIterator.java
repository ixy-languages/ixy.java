package de.tum.in.net.ixy.memory.internal;

import java.util.Iterator;
import java.util.function.Consumer;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable {@link Iterator iterator} wrapper.
 * <p>
 * Wraps and delegates an {@link Iterator iterator} except for {@link #remove()}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
public final class ImmutableIterator<E> implements Iterator<E> {

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The wrapped iterator. */
	@EqualsAndHashCode.Include
	@ToString.Include(rank = 1)
	private final @NotNull Iterator<E> it;

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public E next() {
		return it.next();
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		it.forEachRemaining(action);
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	@Override
	@Deprecated
	@Contract(value = " -> fail", pure = true)
	public void remove() {
		throw new UnsupportedOperationException("An ImmutableIterator prevents calling the original remove() method.");
	}

}
