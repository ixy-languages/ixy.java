package de.tum.in.net.ixy.memory.internal;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import lombok.val;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link ImmutableIterator}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("ImmutableIterator")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
final class ImmutableIteratorTest {

	@Test
	@DisplayName("hasNext(): true")
	@SuppressWarnings("ResultOfMethodCallIgnored")
	void hasNext_true(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		when(it.hasNext()).thenReturn(true);
		assertThat(imit.hasNext()).isTrue();
		verify(it, times(1)).hasNext();
	}

	@Test
	@DisplayName("hasNext(): false")
	@SuppressWarnings("ResultOfMethodCallIgnored")
	void hasNext_false(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		assertThat(imit.hasNext()).isFalse();
		verify(it, times(1)).hasNext();
	}

	@Test
	@DisplayName("next(): null")
	void next_nullable(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		when(it.next()).thenReturn(null);
		assertThat(imit.next()).isNull();
		verify(it, times(1)).next();
	}

	@Test
	@DisplayName("next(): not null")
	void next_notNull(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		val obj = new Object();
		when(it.next()).thenReturn(obj);
		assertThat(imit.next()).isNotNull().isEqualTo(obj);
		verify(it, times(1)).next();
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName("forEachRemaining()")
	void forEachRemaining() {
		val it = (Iterator<Object>) spy(Iterator.class);
		val imit = new ImmutableIterator<>(it);
		final int[] i = {0};
		final Object[] values = {new Object(), new Object(), new Object(), new Object()};
		when(it.hasNext()).then(invocation -> 0 <= i[0] && i[0] < values.length);
		when(it.next()).then(invocation -> values[i[0]++]);
		imit.forEachRemaining(o -> {
			assertThat(o).as("check iterated value").isEqualTo(values[i[0] - 1]);
		});
		verify(it, times(1)).forEachRemaining(any());
	}

	@Test
	@DisplayName("remove()")
	@SuppressWarnings("deprecation")
	void remove(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(imit::remove);
		verify(it, times(0)).remove();
	}

	@Test
	@DisplayName("toString()")
	void ToString(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		val itStr = Pattern.quote(it.toString());
		assertThat(imit.toString()).matches(String.format("^ImmutableIterator\\(it=%s\\)$", itStr));
	}

	@Test
	@DisplayName("equals(Object) && hashCode()")
	@SuppressWarnings("unchecked")
	void equalsAndHashCode(final @Mock Iterator<Object> it) {
		val imit = new ImmutableIterator<>(it);
		val same = new ImmutableIterator<>(it);
		val diff = new ImmutableIterator<>(mock(it.getClass()));
		assertThat(imit).isEqualTo(imit).hasSameHashCodeAs(imit)
				.isEqualTo(same).hasSameHashCodeAs(same)
				.isNotEqualTo(diff);
	}

}
