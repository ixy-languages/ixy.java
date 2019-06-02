package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.DmaMemory;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the class {@link DmaMemory}.
 *
 * @author Esaú García Sánchez-Torija
 * @see DmaMemory
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("DmaMemory")
final class DmaMemoryTest {

	/** Holds a random number that will be used as virtual memory address. */
	private long virtual;

	/** Holds a random number that will be used as physical memory address. */
	private long physical;

	/** Holds an instance of {@link DmaMemory} that will be used to test. */
	private DmaMemory dmaMemory;

	/**
	 * Randomizes {@link #virtual} and {@link #physical} and creates an instance of {@link DmaMemory} that uses them.
	 *
	 * @see DmaMemory#DmaMemory(long, long)
	 */
	@BeforeEach
	void randomize() {
		virtual = (long) (Math.random() * Long.MAX_VALUE);
		physical = (long) (Math.random() * Long.MAX_VALUE);
		dmaMemory = new DmaMemory(virtual, physical);
	}

	/**
	 * Tests that the virtual memory address is correct.
	 *
	 * @see DmaMemory#getVirtualAddress()
	 */
	@Test
	@DisplayName("The virtual memory address is stored correctly")
	void getVirtual() {
		assumeTrue(Objects.nonNull(dmaMemory));
		assertEquals(virtual, dmaMemory.getVirtualAddress(), "the virtual memory address should be correct");
	}

	/**
	 * Tests that the physical memory address is correct.
	 *
	 * @see DmaMemory#getPhysicalAddress()
	 */
	@Test
	@DisplayName("The physical memory address is stored correctly")
	void getPhysical() {
		assumeTrue(Objects.nonNull(dmaMemory));
		assertEquals(physical, dmaMemory.getPhysicalAddress(), "the physical memory address should be correct");
	}

}
