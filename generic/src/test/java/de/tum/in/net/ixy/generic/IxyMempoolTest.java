package de.tum.in.net.ixy.generic;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link IxyMempool}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("IxyMempool")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
final class IxyMempoolTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** A mocked instance of a memory pool. */
	@Spy
	private IxyMempool mempool;

	@Nested
	@DisabledIfOptimized
	@DisplayName("IxyMempool (Parameters)")
	final class Parameters {

		@Spy
		private IxyMempool mempoolParam;

		@Test
		@DisplayName("Parameters are checked for get(IxyPacketBuffer[])")
		void get1() {
			assumeThat(mempoolParam).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.get(null));
		}

		@Test
		@DisplayName("Parameters are checked for get(IxyPacketBuffer[], int)")
		void get2() {
			assumeThat(mempoolParam).isNotNull();
			IxyPacketBuffer[][] buffers = {null, new IxyPacketBuffer[1]};
			int[] offsets = {-1, 0, 1};
			for (val buffer : buffers) {
				for (val offset : offsets) {
					if (buffer == null || offset < 0 || offset >= buffer.length) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.get(buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for get(IxyPacketBuffer[], int, int)")
		void get3() {
			assumeThat(mempoolParam).isNotNull();
			IxyPacketBuffer[][] buffers = {null, new IxyPacketBuffer[1]};
			int[] offsets = {-1, 0, 1};
			int[] sizes = {-1, 0, 1};
			for (val buffer : buffers) {
				for (val offset : offsets) {
					for (val size : sizes) {
						if (buffer == null || offset < 0 || offset >= buffer.length || size < 0) {
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.get(buffer, offset, size));
						}
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for free(IxyPacketBuffer[])")
		void free1() {
			assumeThat(mempoolParam).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.free((IxyPacketBuffer[]) null));
		}

		@Test
		@DisplayName("Parameters are checked for free(IxyPacketBuffer[], int)")
		void free2() {
			assumeThat(mempoolParam).isNotNull();
			IxyPacketBuffer[][] buffers = {null, new IxyPacketBuffer[1]};
			int[] offsets = {-1, 0, 1};
			for (val buffer : buffers) {
				for (val offset : offsets) {
					if (buffer == null || offset < 0 || offset >= buffer.length) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.free(buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for free(IxyPacketBuffer[], int, int)")
		void free3() {
			assumeThat(mempoolParam).isNotNull();
			IxyPacketBuffer[][] buffers = {null, new IxyPacketBuffer[1]};
			int[] offsets = {-1, 0, 1};
			int[] sizes = {-1, 0, 1};
			for (val buffer : buffers) {
				for (val offset : offsets) {
					for (val size : sizes) {
						if (buffer == null || offset < 0 || offset >= buffer.length || size < 0) {
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mempoolParam.free(buffer, offset, size));
						}
					}
				}
			}
		}

	}

	@Test
	@DisplayName("Several free packets can be requested with get(IxyPacketBuffer[])")
	void get1() {
		assumeThat(mempool).isNotNull();
		val size = random.nextInt();
		when(mempool.getSize()).thenReturn(size);
		val buffer = new IxyPacketBuffer[10];
		val count = mempool.get(buffer);
		assertThat(count).isEqualTo(Math.max(0, Math.min(buffer.length, size)));
		verify(mempool, times(count).description("should extract as many packages as it said")).get();
	}

	@Test
	@DisplayName("Several free packets can be requested with get(IxyPacketBuffer[], int)")
	void get2() {
		assumeThat(mempool).isNotNull();
		val size = random.nextInt(Integer.MAX_VALUE);
		when(mempool.getSize()).thenReturn(size);
		val buffer = new IxyPacketBuffer[10];
		val offset = random.nextInt(buffer.length);
		val count = mempool.get(buffer, offset);
		assertThat(count).isEqualTo(Math.max(0, Math.min(buffer.length - offset, size)));
		verify(mempool, times(count).description("should extract as many packages as it said")).get();
	}

	@Test
	@DisplayName("Several free packets can be requested with get(IxyPacketBuffer[], int, int)")
	void get3() {
		assumeThat(mempool).isNotNull();
		val size = random.nextInt(Integer.MAX_VALUE);
		when(mempool.getSize()).thenReturn(size);
		val buffer = new IxyPacketBuffer[10];
		val offset = random.nextInt(buffer.length);
		val length = random.nextInt(Integer.MAX_VALUE);
		val count = mempool.get(buffer, offset, length);
		val minlen = Math.min(length, Math.min(size, buffer.length - offset));
		assertThat(count).isEqualTo(Math.max(0, minlen));
		verify(mempool, times(count).description("should extract as many packages as it said")).get();
	}

	@Test
	@DisplayName("Several free packets can be requested with free(IxyPacketBuffer[])")
	void free1() {
		assumeThat(mempool).isNotNull();
		val capacity = random.nextInt();
		when(mempool.getCapacity()).thenReturn(capacity);
		when(mempool.getSize()).thenReturn(0);
		val packet = mock(IxyPacketBuffer.class);
		val buffer = new IxyPacketBuffer[10];
		val len = BuildConfig.OPTIMIZED ? buffer.length : buffer.length - random.nextInt(buffer.length);
		for (var i = 0; i < len; i += 1) {
			buffer[i] = packet;
		}
		val count = mempool.free(buffer);
		assertThat(count).isEqualTo(Math.max(0, Math.min(capacity, len)));
		verify(mempool, times(count).description("should free as many packages as it said")).free(packet);
	}

	@Test
	@DisplayName("Several free packets can be requested with free(IxyPacketBuffer[], int)")
	void free2() {
		assumeThat(mempool).isNotNull();
		val capacity = random.nextInt();
		when(mempool.getCapacity()).thenReturn(capacity);
		when(mempool.getSize()).thenReturn(0);
		val packet = mock(IxyPacketBuffer.class);
		val buffer = new IxyPacketBuffer[10];
		val len = BuildConfig.OPTIMIZED ? buffer.length : buffer.length - random.nextInt(buffer.length);
		for (var i = 0; i < len; i += 1) {
			buffer[i] = packet;
		}
		val offset = random.nextInt(len);
		val count = mempool.free(buffer, offset);
		assertThat(count).isEqualTo(Math.max(0, Math.min(capacity, len - offset)));
		verify(mempool, times(count).description("should free as many packages as it said")).free(packet);
	}

	@Test
	@DisplayName("Several free packets can be requested with free(IxyPacketBuffer[], int, int)")
	void free3() {
		assumeThat(mempool).isNotNull();
		val capacity = random.nextInt();
		when(mempool.getCapacity()).thenReturn(capacity);
		when(mempool.getSize()).thenReturn(0);
		val packet = mock(IxyPacketBuffer.class);
		val buffer = new IxyPacketBuffer[10];
		val len = BuildConfig.OPTIMIZED ? buffer.length : buffer.length - random.nextInt(buffer.length);
		for (var i = 0; i < len; i += 1) {
			buffer[i] = packet;
		}
		val offset = random.nextInt(len);
		val length = random.nextInt(len);
		val count = mempool.free(buffer, offset, length);
		val minlen = Math.min(length, Math.min(capacity, len - offset));
		assertThat(count).isEqualTo(Math.max(0, minlen));
		verify(mempool, times(count).description("should free as many packages as it said")).free(packet);
	}

	@Test
	@DisplayName("Packets can be used to find memory pools")
	void find() {
		assumeThat(mempool).isNotNull();
		val id = random.nextInt();
		val packet = mock(IxyPacketBuffer.class);
		when(packet.getMemoryPoolId()).thenReturn(id);
		assertThat(mempool.find(packet)).isEqualTo(null);
		verify(mempool, times(1).description("should use the id")).find(id);
		verify(packet, times(1).description("should read the id")).getMemoryPoolId();
		assertThat(mempool.find(null)).isEqualTo(null);
		reset(mempool);
		verify(mempool, never().description("should not know what id to use")).find(anyInt());
	}

}
