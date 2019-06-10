package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.InvalidNullParameterException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the class {@link InvalidNullParameterException}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidNullParameterExceptionTest {

	@Test
	@DisplayName("Null or blank constructor fails")
	void constructor() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidNullParameterException(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidNullParameterException(""));
	}

}
