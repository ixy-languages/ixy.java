package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.InvalidOffsetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the class {@link InvalidOffsetException}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidOffsetExceptionTest {

	@Test
	@DisplayName("Null or blank constructor fails")
	void constructor() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidOffsetException(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidOffsetException(""));
	}

}
