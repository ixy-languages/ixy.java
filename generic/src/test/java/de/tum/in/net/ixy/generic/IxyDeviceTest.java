package de.tum.in.net.ixy.generic;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Spy;
import org.mockito.internal.matchers.Null;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link IxyDevice}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("IxyDevice")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
final class IxyDeviceTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** A mocked instance of a device. */
	@Spy
	private IxyDevice device;

	@Nested
	@DisabledIfOptimized
	@DisplayName("IxyDevice (Parameters)")
	@SuppressWarnings("InnerClassMayBeStatic")
	final class Parameters {

		@Spy
		private IxyDevice deviceParam;

		@Test
		@DisplayName("Parameters are checked for rxBatch(int, IxyPacketBuffer[], int)")
		void rxBatch3() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.rxBatch(0, buffer, offset));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.rxBatch(0, buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for rxBatch(int, IxyPacketBuffer[])")
		void rxBatch2() {
			assumeThat(deviceParam).isNotNull();
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.rxBatch(0, null));
		}

		@Test
		@DisplayName("Parameters are checked for rxBusyWait(int, IxyPacketBuffer[], int, int)")
		void rxBusyWait4() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.rxBusyWait(0, buffer, offset, 0));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.rxBusyWait(0, buffer, offset, 0));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for rxBusyWait(int, IxyPacketBuffer[], int)")
		void rxBusyWait3() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.rxBusyWait(0, buffer, offset));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.rxBusyWait(0, buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for rxBusyWait(int, IxyPacketBuffer[])")
		void rxBusyWait2() {
			assumeThat(deviceParam).isNotNull();
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.rxBusyWait(0, null));
		}

		@Test
		@DisplayName("Parameters are checked for txBatch(int, IxyPacketBuffer[], int)")
		void txBatch3() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.txBatch(0, buffer, offset));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.txBatch(0, buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for txBatch(int, IxyPacketBuffer[])")
		void txBatch2() {
			assumeThat(deviceParam).isNotNull();
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.txBatch(0, null));
		}

		@Test
		@DisplayName("Parameters are checked for txBusyWait(int, IxyPacketBuffer[], int, int)")
		void txBusyWait4() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.txBusyWait(0, buffer, offset, 0));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.txBusyWait(0, buffer, offset, 0));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for txBusyWait(int, IxyPacketBuffer[], int)")
		void txBusyWait3() {
			assumeThat(deviceParam).isNotNull();
			IxyPacketBuffer[][] packets = {null, new IxyPacketBuffer[0]};
			int[] offsets = {-1, 0};
			for (val buffer : packets) {
				for (val offset : offsets) {
					if (offset < 0) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> deviceParam.txBusyWait(0, buffer, offset));
					} else if (buffer == null) {
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.txBusyWait(0, buffer, offset));
					}
				}
			}
		}

		@Test
		@DisplayName("Parameters are checked for txBusyWait(int, IxyPacketBuffer[])")
		void txBusyWait2() {
			assumeThat(deviceParam).isNotNull();
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> deviceParam.txBusyWait(0, null));
		}

	}

	@Test
	@DisplayName("Flags can be set")
	void setFlags() {
		assumeThat(device).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt(Integer.MAX_VALUE);
		val flags = random.nextInt();
		when(device.getRegister(offset)).thenReturn(value);
		doNothing().when(device).setRegister(offset, value | flags);
		device.setFlags(offset, flags);
		verify(device, times(1).description("should be written once")).setRegister(offset, value | flags);
	}

	@Test
	@DisplayName("Flags can be cleared")
	void setCleared() {
		assumeThat(device).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt(Integer.MAX_VALUE);
		val flags = random.nextInt();
		when(device.getRegister(offset)).thenReturn(value);
		doNothing().when(device).setRegister(offset, value | ~flags);
		device.clearFlags(offset, flags);
		verify(device, times(1).description("should be written once")).setRegister(offset, value | ~flags);
	}

	@Test
	@DisplayName("Thread can be blocked until flags are set")
	void waitSetRegister() {
		assumeThat(device).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt(Integer.MAX_VALUE);
		val flags = random.nextInt();
		boolean[] first = {true};
		doAnswer(invocation -> {
			if (first[0]) {
				first[0] = false;
				return value & ~flags;
			}
			return value | flags;
		}).when(device).getRegister(offset);
		device.waitSetFlags(offset, flags);
		verify(device, times(2).description("should be written once")).getRegister(offset);
	}

	@Test
	@DisplayName("Thread can be blocked until flags are cleared")
	void waitClearRegister() {
		assumeThat(device).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt(Integer.MAX_VALUE);
		val flags = random.nextInt();
		boolean[] first = {true};
		doAnswer(invocation -> {
			if (first[0]) {
				first[0] = false;
				return value | ~flags;
			}
			return value & ~flags;
		}).when(device).getRegister(offset);
		device.waitClearFlags(offset, flags);
		verify(device, times(2).description("should be written once")).getRegister(offset);
	}

	@Test
	@DisplayName("Packets can be processed in batches")
	void rxBatch() {
		assertThat(device).isNotNull();
		var packets = new IxyPacketBuffer[0];
		when(device.rxBatch(0, packets, 0, 0)).thenReturn(0);
		assertThat(device.rxBatch(0, packets, 0)).isEqualTo(0);
		assertThat(device.rxBatch(0, packets)).isEqualTo(0);
		verify(device, times(2).description("should be processed once")).rxBatch(0, packets, 0, 0);
	}

	@Test
	@DisplayName("Thread can be blocked while packets are processed in batches")
	void rxBusyWait() {
		assertThat(device).isNotNull();
		IxyPacketBuffer[] packets = {mock(IxyPacketBuffer.class), mock(IxyPacketBuffer.class)};
		when(device.rxBatch(eq(0), eq(packets), anyInt(), anyInt())).thenReturn(1);
		device.rxBusyWait(0, packets, 0, packets.length);
		device.rxBusyWait(0, packets, 0);
		device.rxBusyWait(0, packets);
		for (var i = 0; i < packets.length; i += 1) {
			verify(device, times(3).description("each packet should be processed once")).rxBatch(0, packets, i, packets.length - i);
		}
	}

	@Test
	@DisplayName("Packets can be processed in batches")
	void txBatch() {
		assertThat(device).isNotNull();
		var packets = new IxyPacketBuffer[0];
		when(device.txBatch(0, packets, 0, 0)).thenReturn(0);
		assertThat(device.txBatch(0, packets, 0)).isEqualTo(0);
		assertThat(device.txBatch(0, packets)).isEqualTo(0);
		verify(device, times(2).description("should be processed once")).txBatch(0, packets, 0, 0);
	}

	@Test
	@DisplayName("Thread can be blocked while packets are processed in batches")
	void txBusyWait() {
		assertThat(device).isNotNull();
		IxyPacketBuffer[] packets = {mock(IxyPacketBuffer.class), mock(IxyPacketBuffer.class)};
		when(device.txBatch(eq(0), eq(packets), anyInt(), anyInt())).thenReturn(1);
		device.txBusyWait(0, packets, 0, packets.length);
		device.txBusyWait(0, packets, 0);
		device.txBusyWait(0, packets);
		for (var i = 0; i < packets.length; i += 1) {
			verify(device, times(3).description("each packet should be processed once")).txBatch(0, packets, i, packets.length - i);
		}
	}

}
