package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.DmaMemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jetbrains.annotations.NotNull;

/** Checks the class {@link DmaMemory}. */
@DisplayName("DmaMemory")
class DmaMemoryTest {

	/** Holds a random number that will be used as virtual memory address. */
	private long virtual;

	/** Holds a random number that will be used as physical memory address. */
	private long physical;

	/** Holds an instance of {@link DmaMemory} that will be used to test. */
	@NotNull
	private DmaMemory dmaMemory;

	/**
	 * Randomizes {@link #virtual} and {@link #physical} and creates an instance of {@link DmaMemory} that uses them.
	 */
	@BeforeEach
	void randomize() {
		virtual = (long) (Math.random() * Long.MAX_VALUE);
		physical = (long) (Math.random() * Long.MAX_VALUE);
		dmaMemory = new DmaMemory(virtual, physical);
	}
	
	/** Tests that the method {@link DmaMemory#getVirtual()} works correctly. */
	@Test
	@DisplayName("The virtual memory address is stored correctly")
	void getVirtual() {
		assertEquals(virtual, dmaMemory.getVirtual(), "the virtual memory address should be correct");
	}

	/** Tests that the method {@link DmaMemory#getPhysical()} works correctly. */
	@Test
	@DisplayName("The physical memory address is stored correctly")
	void getPhysical() {
		assertEquals(physical, dmaMemory.getPhysical(), "the physical memory address should be correct");
	}

}
