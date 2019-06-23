package de.tum.in.net.ixy.generic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static de.tum.in.net.ixy.generic.IxyMemoryManager.AllocationType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link AllocationType}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("AllocationType")
@Execution(ExecutionMode.CONCURRENT)
final class AllocationTypeTest {

	@Test
	@DisplayName("Allocation type HUGE is unique")
	void HUGE() {
		assertThat(AllocationType.HUGE).isEqualTo(AllocationType.HUGE);
		assertThat(AllocationType.HUGE).isNotEqualTo(AllocationType.STANDARD);
	}

	@Test
	@DisplayName("Allocation type STANDARD is unique")
	void STANDARD() {
		assertThat(AllocationType.STANDARD).isEqualTo(AllocationType.STANDARD);
		assertThat(AllocationType.STANDARD).isNotEqualTo(AllocationType.HUGE);
	}

}
