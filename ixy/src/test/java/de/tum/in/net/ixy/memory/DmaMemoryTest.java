package de.tum.in.net.ixy.memory;

import java.security.SecureRandom;
import java.util.Random;

import lombok.val;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link DmaMemory}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("DmaMemory")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
final class DmaMemoryTest {

	/** A pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	@Test
	@DisplayName("getVirtual()")
	void getVirtual() {
		val virtual = random.nextLong();
		val dma = new DmaMemory(virtual, random.nextLong());
		assertThat(dma.getVirtual()).isEqualTo(virtual);
	}

	@Test
	@DisplayName("getPhysical()")
	void getPhysical() {
		val physical = random.nextLong();
		val dma = new DmaMemory(random.nextLong(), physical);
		assertThat(dma.getPhysical()).isEqualTo(physical);
	}

	@Test
	@DisplayName("toString()")
	void ToString() {
		val dma = new DmaMemory(random.nextLong(), random.nextLong());
		assertThat(dma.toString()).matches("^DmaMemory\\(virtual=0x[0-9a-f]{16}, physical=0x[0-9a-f]{16}\\)$");
	}

	@Test
	@DisplayName("equals(Object) && hashCode()")
	@SuppressWarnings("PointlessArithmeticExpression")
	void equalsAndHashCode() {
		val virtual = random.nextLong();
		val physical = random.nextLong();
		val dma = new DmaMemory(virtual, physical);
		val same = new DmaMemory(virtual, physical);
		val diff1 = new DmaMemory(virtual + 0, physical + 1);
		val diff2 = new DmaMemory(virtual + 1, physical + 0);
		val diff3 = new DmaMemory(virtual + 1, physical + 1);
		assertThat(dma).isEqualTo(dma).hasSameHashCodeAs(dma)
				.isEqualTo(same).hasSameHashCodeAs(same)
				.isNotEqualTo(diff1)
				.isNotEqualTo(diff2)
				.isNotEqualTo(diff3);
	}

}
