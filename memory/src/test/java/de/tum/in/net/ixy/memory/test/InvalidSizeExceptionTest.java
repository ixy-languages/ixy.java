package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.InvalidSizeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the class {@link InvalidSizeException}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidSizeExceptionTest {

	@Test
	@DisplayName("Null or blank constructor fails")
	void constructor() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidSizeException(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidSizeException(""));
	}

}
