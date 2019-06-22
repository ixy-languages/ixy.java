package de.tum.in.net.ixy.stats;

import de.tum.in.net.ixy.generic.IxyDevice;
import de.tum.in.net.ixy.generic.IxyStats;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the class {@link Stats}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("Stats")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
final class StatsTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** Holds a reference to the stats. */
	private IxyStats stats;

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp(@Mock IxyDevice device) {
		Mockito.lenient().when(device.getName()).thenReturn("0000:00:00.0");
		stats = Stats.of(device);
	}

	@Nested
	@DisabledIfOptimized
	@DisplayName("Stats (Parameters)")
	final class Parameters {

		@Test
		@DisplayName("Parameters are checked for writeStats(OutputStream)")
		void writeStats() {
			assumeThat(stats).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.writeStats(null));
		}

		@Test
		@DisplayName("Parameters are checked for writeStats(OutputStream, IxyStats, long)")
		void writeStats2() {
			assumeThat(stats).isNotNull();
			OutputStream[] streamsArr = {null, mock(OutputStream.class)};
			IxyStats[] statsArr = {null, mock(IxyStats.class)};
			long[] nanosArr = {-1, 0, 1};
			for (val stream : streamsArr) {
				for (val stat : statsArr) {
					for (val nano : nanosArr) {
						if (stream == null || stat == null || nano <= 0) {
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> stats.writeStats(stream, stat, nano));
						}
					}
				}
			}
		}

	}

	@Test
	@DisplayName("The number of read packets is correct")
	void getaddRxPackets() {
		assumeThat(stats).isNotNull();
		val rxPackets = random.nextInt(Integer.MAX_VALUE);
		val oRxPackets = stats.getRxPackets();
		stats.addRxPackets(rxPackets);
		assertThat(stats.getRxPackets()).usingComparator(Integer::compareUnsigned).isGreaterThan(oRxPackets);
		for (var i = 0; i < 5; i += 1) {
			stats.addRxPackets(Integer.MAX_VALUE);
		}
		assertThat(stats.getRxPackets()).isEqualTo(-1);
	}

	@Test
	@DisplayName("The number of written packets is correct")
	void getTxPackets() {
		assumeThat(stats).isNotNull();
		val txPackets = random.nextInt(Integer.MAX_VALUE);
		val oTxPackets = stats.getTxPackets();
		stats.addTxPackets(txPackets);
		assertThat(stats.getTxPackets()).usingComparator(Integer::compareUnsigned).isGreaterThan(oTxPackets);
		for (var i = 0; i < 5; i += 1) {
			stats.addTxPackets(Integer.MAX_VALUE);
		}
		assertThat(stats.getTxPackets()).isEqualTo(-1);
	}

	@Test
	@DisplayName("The number of read bytes is correct")
	void getRxBytes() {
		assumeThat(stats).isNotNull();
		val rxBytes = random.nextLong();
		val oRxBytes = stats.getRxBytes();
		stats.addRxBytes(rxBytes);
		assertThat(stats.getRxBytes()).usingComparator(Long::compareUnsigned).isGreaterThan(oRxBytes);
		for (var i = 0; i < 5; i += 1) {
			stats.addRxBytes(Long.MAX_VALUE);
		}
		assertThat(stats.getRxBytes()).isEqualTo(-1);
	}

	@Test
	@DisplayName("The number of written bytes is correct")
	void getTxBytes() {
		assumeThat(stats).isNotNull();
		val txBytes = random.nextLong();
		val oTxBytes = stats.getTxBytes();
		stats.addTxBytes(txBytes);
		assertThat(stats.getTxBytes()).usingComparator(Long::compareUnsigned).isGreaterThan(oTxBytes);
		for (var i = 0; i < 5; i += 1) {
			stats.addTxBytes(Long.MAX_VALUE);
		}
		assertThat(stats.getTxBytes()).isEqualTo(-1);
	}

	@Test
	@DisplayName("The counters can be reset")
	void reset() {
		assumeThat(stats).isNotNull();
		val rxPackets = random.nextInt(Integer.MAX_VALUE);
		val txPackets = random.nextInt(Integer.MAX_VALUE);
		val rxBytes = random.nextLong();
		val txBytes = random.nextLong();
		val oRxPackets = stats.getRxPackets();
		val oTxPackets = stats.getTxPackets();
		val oRxBytes = stats.getRxBytes();
		val oTxBytes = stats.getTxBytes();
		stats.addRxPackets(rxPackets);
		stats.addTxPackets(txPackets);
		stats.addRxBytes(rxBytes);
		stats.addTxBytes(txBytes);
		assertThat(stats.getRxPackets()).usingComparator(Integer::compareUnsigned).isGreaterThan(oRxPackets);
		assertThat(stats.getTxPackets()).usingComparator(Integer::compareUnsigned).isGreaterThan(oTxPackets);
		assertThat(stats.getRxBytes()).usingComparator(Long::compareUnsigned).isGreaterThan(oRxBytes);
		assertThat(stats.getTxBytes()).usingComparator(Long::compareUnsigned).isGreaterThan(oTxBytes);
		stats.reset();
		assertThat(stats.getRxPackets()).isZero();
		assertThat(stats.getTxPackets()).isZero();
		assertThat(stats.getRxBytes()).isZero();
		assertThat(stats.getTxBytes()).isZero();
	}

	@Test
	@DisplayName("The stats can be written")
	void writeStats() {
		assumeThat(stats).isNotNull();
		val rxPackets = random.nextInt(Integer.MAX_VALUE);
		val txPackets = random.nextInt(Integer.MAX_VALUE);
		val rxBytes = random.nextLong();
		val txBytes = random.nextLong();
		stats.addRxPackets(rxPackets);
		stats.addTxPackets(txPackets);
		stats.addRxBytes(rxBytes);
		stats.addTxBytes(txBytes);
		assertThat(stats.getRxPackets()).isNotZero();
		assertThat(stats.getTxPackets()).isNotZero();
		assertThat(stats.getRxBytes()).isNotZero();
		assertThat(stats.getTxBytes()).isNotZero();
		val pattern = Pattern.compile("^\\d{4}:\\d{2}:\\d{2}.\\d (R|T)X: \\d+ packets \\| \\d+ bytes$");
		try (val bufferStream = new ByteArrayOutputStream(8 * 1024)) {
			stats.writeStats(bufferStream);
			val message = bufferStream.toString(StandardCharsets.UTF_8);
			for (val line : message.split(System.lineSeparator())) {
				assertThat(line).matches(pattern);
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("The connection stats can be written")
	void writeStats(@Mock IxyDevice device) {
		assumeThat(stats).isNotNull();
		val oldStats = Stats.of(device);
		val rxPackets = random.nextInt(Integer.MAX_VALUE);
		val txPackets = random.nextInt(Integer.MAX_VALUE);
		val rxBytes = random.nextLong();
		val txBytes = random.nextLong();
		stats.addRxPackets(rxPackets);
		stats.addTxPackets(txPackets);
		stats.addRxBytes(rxBytes);
		stats.addTxBytes(txBytes);
		assertThat(stats.getRxPackets()).isNotZero();
		assertThat(stats.getTxPackets()).isNotZero();
		assertThat(stats.getRxBytes()).isNotZero();
		assertThat(stats.getTxBytes()).isNotZero();
		val pattern = Pattern.compile("^\\d{4}:\\d{2}:\\d{2}.\\d (R|T)X: -?\\d+(\\.\\d+(E\\d+)?)? Mpps \\| -?\\d+(\\.\\d+(E\\d+)?)? Mbit/s$");
		try (val bufferStream = new ByteArrayOutputStream(8 * 1024)) {
			stats.writeStats(bufferStream, oldStats, 1_000_000_000);
			val message = bufferStream.toString(StandardCharsets.UTF_8);
			for (val line : message.split(System.lineSeparator())) {
				assertThat(line).matches(pattern);
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}

}
