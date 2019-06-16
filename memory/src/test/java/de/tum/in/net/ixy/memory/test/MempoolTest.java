package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.PacketBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link Mempool}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("Mempool")
final class MempoolTest {

	/** A cached instance of a pseudo-random number generator. */
	private final Random random = new Random();

	/** Holds the capacity of the memory pool. */
	private int capacity;

	/** Holds the packet buffer size of the memory pool. */
	private int packetSize;

	/** Holds an instance of {@link Mempool} that will be used to test. */
	private Mempool mempool;

	// Generates random data and prepare the mocks
	@BeforeEach
	void setUp() {
		// Generate a random capacity and compute the maximum packet size
		capacity = Math.max(random.nextInt(16), 8);
		packetSize = Math.max(random.nextInt(PacketBuffer.HEADER_BYTES * 2), PacketBuffer.HEADER_BYTES + Long.BYTES);
		// Create the memory pool
		mempool = new Mempool(capacity, packetSize);
	}

	@Test
	@DisplayName("Wrong arguments produce exceptions")
	void exceptions() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempool.compareTo(null));
	}

	@Test
	@DisplayName("The memory pool capacity is correct")
	void getCapacity() {
		assumeThat(mempool).isNotNull();
		assertThat(mempool.getCapacity()).as("Capacity").isEqualTo(capacity);
	}

}
