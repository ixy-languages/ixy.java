package de.tum.in.net.ixy.generic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static de.tum.in.net.ixy.generic.IxyMemoryManager.LayoutType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link LayoutType}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("AllocationType")
@Execution(ExecutionMode.CONCURRENT)
final class LayoutTypeTest {

	@Test
	@DisplayName("Layout type CONTIGUOUS is unique")
	void HUGE() {
		assertThat(LayoutType.CONTIGUOUS).isEqualTo(LayoutType.CONTIGUOUS);
		assertThat(LayoutType.CONTIGUOUS).isNotEqualTo(LayoutType.STANDARD);
	}

	@Test
	@DisplayName("Layout type STANDARD is unique")
	void STANDARD() {
		assertThat(LayoutType.STANDARD).isEqualTo(LayoutType.STANDARD);
		assertThat(LayoutType.STANDARD).isNotEqualTo(LayoutType.CONTIGUOUS);
	}

}
