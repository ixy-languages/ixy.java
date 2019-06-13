package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.InvalidBufferException;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests the class {@link InvalidBufferException}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("InvalidBufferException")
final class InvalidBufferExceptionTest {

	@Test
	@DisabledIfOptimized
	@DisplayName("Null or blank constructor fails")
	void exceptions() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidBufferException(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new InvalidBufferException(""));
	}

	@Test
	@DisplayName("The constructor works as expected")
	void constructor() {
		val array = new byte[8];
		new SecureRandom().nextBytes(array);
		val parameter = new String(array, StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> new InvalidBufferException(parameter));
	}

}
