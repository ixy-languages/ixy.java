package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.InvalidMemoryAddressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the class {@link InvalidMemoryAddressException}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class InvalidMemoryAddressExceptionTest {

	@Test
	@DisplayName("Null or blank constructor fails")
	void constructor() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidMemoryAddressException(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidMemoryAddressException(""));
	}

}
