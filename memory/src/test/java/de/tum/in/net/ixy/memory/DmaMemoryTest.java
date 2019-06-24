package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link DmaMemory}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("DmaMemory")
@Execution(ExecutionMode.CONCURRENT)
final class DmaMemoryTest {

	/** A cached instance of a pseudo-random number generator. */
	private final @NotNull Random random = new SecureRandom();

	/** Holds a random number that will be used as virtual memory address. */
	private long virtualAddress;

	/** Holds a random number that will be used as physical memory address. */
	private long physicalAddress;

	/** Holds an instance of {@link DmaMemory} that will be used to test. */
	private DmaMemory dmaMemory;

	// Randomizes "virtual" and "physical" and creates an "DmaMemory" instance that uses them.
	@BeforeEach
	void randomize() {
		virtualAddress = random.nextLong();
		physicalAddress = random.nextLong();
		dmaMemory = DmaMemory.of(virtualAddress, physicalAddress);
	}

	@Test
	@DisabledIfOptimized
	@DisplayName("Wrong parameters produce exceptions")
	void of_exceptionsNotOptimized() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> DmaMemory.of(null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> DmaMemory.of((IxyDmaMemory) null));
	}

	@Test
	@DisplayName("Instances can be copied with the constructor")
	void of() {
		assumeThat(dmaMemory).isNotNull();
		assertThat(DmaMemory.of(dmaMemory)).isEqualTo(dmaMemory);
		assertThat(DmaMemory.of((IxyDmaMemory) dmaMemory)).isEqualTo(dmaMemory);
	}

	@Test
	@DisplayName("The virtual memory address is stored correctly")
	void getVirtualAddress() {
		assumeThat(dmaMemory).isNotNull();
		assertThat(dmaMemory.getVirtualAddress()).as("Virtual memory address").isEqualTo(virtualAddress);
	}

	@Test
	@DisplayName("The physical memory address is stored correctly")
	void getPhysicalAddress() {
		assumeThat(dmaMemory).isNotNull();
		assertThat(dmaMemory.getPhysicalAddress()).as("Physical memory address").isEqualTo(physicalAddress);
	}

}
