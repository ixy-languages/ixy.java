package de.tum.in.net.ixy.stats.test;

import de.tum.in.net.ixy.generic.IxyPciDevice;
import de.tum.in.net.ixy.generic.IxyStats;
import de.tum.in.net.ixy.stats.Stats;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link Stats}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("Stats")
@ExtendWith(MockitoExtension.class)
final class StatsTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** Holds a reference to the stats. */
	private IxyStats stats;

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp(@Mock IxyPciDevice device) {
		stats = Stats.of(device);
	}

	@Nested
	@DisabledIfOptimized
	@DisplayName("Stats (Parameters)")
	final class Parameters {

		@Test
		@DisplayName("The parameters are checked for setRxPackets(int)")
		void setRxPackets() {
			assumeThat(stats).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.setRxPackets(-1));
		}

		@Test
		@DisplayName("The parameters are checked for setTxPackets(int)")
		void setTxPackets() {
			assumeThat(stats).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.setTxPackets(-1));
		}

		@Test
		@DisplayName("The parameters are checked for setRxBytes(long)")
		void setRxBytes() {
			assumeThat(stats).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.setRxBytes(-1));
		}

		@Test
		@DisplayName("The parameters are checked for setTxBytes(long)")
		void setTxBytes() {
			assumeThat(stats).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.setTxBytes(-1));
		}

	}

	@Test
	@DisplayName("The number of read packets is correct")
	void getsetRxPackets() {
		assumeThat(stats).isNotNull();
		val rxPackets = random.nextInt(Integer.MAX_VALUE);
		stats.setRxPackets(rxPackets);
		assertThat(stats.getRxPackets()).isEqualTo(rxPackets);
	}

	@Test
	@DisplayName("The number of written packets is correct")
	void getTxPackets() {
		assumeThat(stats).isNotNull();
		val txPackets = random.nextInt(Integer.MAX_VALUE);
		stats.setTxPackets(txPackets);
		assertThat(stats.getTxPackets()).isEqualTo(txPackets);
	}

	@Test
	@DisplayName("The number of read bytes is correct")
	void getRxBytes() {
		assumeThat(stats).isNotNull();
		val rxBytes = Math.max(random.nextLong(), 0);
		stats.setRxBytes(rxBytes);
		assertThat(stats.getRxBytes()).isEqualTo(rxBytes);
	}

	@Test
	@DisplayName("The number of written bytes is correct")
	void getTxBytes() {
		assumeThat(stats).isNotNull();
		val txBytes = Math.max(random.nextLong(), 0);
		stats.setTxBytes(txBytes);
		assertThat(stats.getTxBytes()).isEqualTo(txBytes);
	}

}
