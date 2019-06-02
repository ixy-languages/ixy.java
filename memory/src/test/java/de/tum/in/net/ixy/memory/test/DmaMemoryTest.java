package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.DmaMemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link DmaMemory}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("DmaMemory")
final class DmaMemoryTest {

	/** Holds a random number that will be used as virtual memory address. */
	private transient long virtual;

	/** Holds a random number that will be used as physical memory address. */
	private transient long physical;

	/** Holds an instance of {@link DmaMemory} that will be used to test. */
	private transient DmaMemory dmaMemory;

	// Randomizes "virtual" and "physical" and creates an "DmaMemory" instance that uses them.
	@BeforeEach
	void randomize() {
		virtual = (long) (Math.random() * Long.MAX_VALUE);
		physical = (long) (Math.random() * Long.MAX_VALUE);
		dmaMemory = new DmaMemory(virtual, physical);
	}

	@Test
	@DisplayName("The virtual memory address is stored correctly")
	void getVirtual() {
		assertThat(dmaMemory).isNotNull();
		assertThat(dmaMemory.getVirtualAddress()).isEqualTo(virtual);
	}

	@Test
	@DisplayName("The physical memory address is stored correctly")
	void getPhysical() {
		assertThat(dmaMemory).isNotNull();
		assertThat(dmaMemory.getPhysicalAddress()).isEqualTo(physical);
	}

}
