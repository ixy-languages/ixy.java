package de.tum.in.net.ixy.generic;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests the class {@link InvalidSizeException}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("InvalidSizeException")
final class InvalidSizeExceptionTest {

	@Nested
	@DisabledIfOptimized
	@DisplayName("InvalidSizeException (Parameters)")
	final class Parameters {

		@Test
		@DisplayName("Null or blank constructor fails")
		void exceptions() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidSizeException(null));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidSizeException(""));
		}

	}

	@Test
	@DisplayName("The constructor works as expected")
	void constructor() {
		val array = new byte[8];
		new SecureRandom().nextBytes(array);
		val parameter = new String(array, StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> new InvalidSizeException(parameter));
	}

}
