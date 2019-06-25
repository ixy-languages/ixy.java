package de.tum.in.net.ixy.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the class {@link Mempool}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("Mempool")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
final class MempoolTest {

//	/** A cached instance of a pseudo-random number generator. */
//	private final Random random = new SecureRandom();
//
//	/** Holds the capacity of the memory pool. */
//	private int capacity;
//
//	/** Holds an instance of {@link Mempool} that will be used to test. */
//	private Mempool mempool;
//
//	// Generates random data and prepare the mocks
//	@BeforeEach
//	void setUp(@Mock IxyMemoryManager mmanager) {
//		// Generate a random capacity and compute the maximum packet size
//		capacity = random.nextInt(8) + 8;
//		val packetSize = Math.max(random.nextInt(PacketBuffer.HEADER_BYTES * 2), PacketBuffer.HEADER_BYTES + Long.BYTES);
//		// Create the memory pool
//		// TODO
//	}
//
//	@Test
//	@DisplayName("Wrong arguments produce exceptions")
//	void exceptions() {
//		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> mempool.compareTo(null));
//	}
//
//	@Test
//	@DisplayName("The memory pool capacity is correct")
//	void getCapacity() {
//		assumeThat(mempool).isNotNull();
//		assertThat(mempool.getCapacity()).as("Capacity").isEqualTo(capacity);
//	}

}
