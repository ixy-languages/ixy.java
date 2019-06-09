package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import de.tum.in.net.ixy.memory.DmaMemory;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link DmaMemory}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("DmaMemory")
final class DmaMemoryTest {

	/** A cached instance of a pseudo-random number generator. */
	private final @NotNull Random random = new SecureRandom();

	/** Holds a random number that will be used as virtual memory address. */
	private long virtualAddress;

	/** Holds a random number that will be used as physical memory address. */
	private long physicalAddress;

	/** Holds an instance of {@link DmaMemory} that will be used to test. */
	private IxyDmaMemory dmaMemory;

	// Randomizes "virtual" and "physical" and creates an "DmaMemory" instance that uses them.
	@BeforeEach
	void randomize() {
		virtualAddress = random.nextLong();
		physicalAddress = random.nextLong();
		dmaMemory = DmaMemory.of(virtualAddress, physicalAddress);
	}

	@Test
	@DisplayName("The virtual memory address is stored correctly")
	void of() {
		assumeThat(dmaMemory).isNotNull();
		val copy = DmaMemory.of(dmaMemory);
		assertThat(copy).isEqualTo(dmaMemory);
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> DmaMemory.of(null));
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

	@Test
	@SuppressWarnings("ConstantConditions")
	@DisplayName("The equals(Object) method works as expected")
	void equalsTest() {
		assumeThat(dmaMemory).isNotNull();
		val none = DmaMemory.of(virtualAddress + 1L, physicalAddress + 1L);
		val virt = DmaMemory.of(virtualAddress + 1L, physicalAddress);
		val phys = DmaMemory.of(virtualAddress,      physicalAddress + 1L);
		val same = DmaMemory.of(virtualAddress,      physicalAddress);
		val softly = new SoftAssertions();
		softly.assertThat(dmaMemory.equals(dmaMemory)).isTrue();
		softly.assertThat(dmaMemory.equals(null)).isFalse();
		softly.assertThat(dmaMemory.equals(none)).isFalse();
		softly.assertThat(dmaMemory.equals(virt)).isFalse();
		softly.assertThat(dmaMemory.equals(phys)).isFalse();
		softly.assertThat(dmaMemory.equals(same)).isTrue();
		softly.assertAll();
	}

	@Test
	@DisplayName("The hashCode() method works as expected")
	void hashCodeTest() {
		assumeThat(dmaMemory).isNotNull();
		val none = DmaMemory.of(dmaMemory.getVirtualAddress() + 1L, dmaMemory.getPhysicalAddress() + 1L);
		val virt = DmaMemory.of(dmaMemory.getVirtualAddress() + 1L, dmaMemory.getPhysicalAddress());
		val phys = DmaMemory.of(dmaMemory.getVirtualAddress(),      dmaMemory.getPhysicalAddress() + 1L);
		val same = DmaMemory.of(dmaMemory.getVirtualAddress(),      dmaMemory.getPhysicalAddress());
		val hash = dmaMemory.hashCode();
		val softly = new SoftAssertions();
		softly.assertThat(hash).as("Hash code").isEqualTo(dmaMemory.hashCode());
		softly.assertThat(hash).as("Hash code").isNotEqualTo(none.hashCode());
		softly.assertThat(hash).as("Hash code").isNotEqualTo(virt.hashCode());
		softly.assertThat(hash).as("Hash code").isNotEqualTo(phys.hashCode());
		softly.assertThat(hash).as("Hash code").isEqualTo(same.hashCode());
		softly.assertAll();
	}

	@Test
	@SuppressWarnings("HardcodedFileSeparator")
	@DisplayName("The string representation is correct")
	void toStringTest() {
		assumeThat(dmaMemory).isNotNull();
		val genericPattern = "^%s\\(virt[a-zA-Z]*=%d, phys[a-zA-Z]*=%d\\)$";
		val specificPattern = String.format(genericPattern, DmaMemory.class.getSimpleName(), virtualAddress, physicalAddress);
		val pattern = Pattern.compile(specificPattern);
		assertThat(dmaMemory.toString()).as("String representation").matches(pattern);
	}

}
