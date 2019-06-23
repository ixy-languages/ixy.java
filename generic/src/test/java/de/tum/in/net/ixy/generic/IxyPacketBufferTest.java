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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link IxyPacketBuffer}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("IxyPacketBuffer")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
final class IxyPacketBufferTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** A mocked instance of a packet. */
	@Spy
	private IxyPacketBuffer packet;

	@Nested
	@DisabledIfOptimized
	@DisplayName("IxyPacketBuffer (Parameters)")
	final class Parameters {

		@Spy
		private IxyPacketBuffer packetParam;

		@Test
		@DisplayName("Parameters are checked for get(int, int)")
		void get() {
			assumeThat(packetParam).isNotNull();
			val offset = random.nextInt();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetParam.get(offset, -1));
		}

		@Test
		@DisplayName("Parameters are checked for getVolatile(int, int)")
		void getVolatile() {
			assumeThat(packetParam).isNotNull();
			val offset = random.nextInt();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetParam.getVolatile(offset, -1));
		}

	}

	@Test
	@DisplayName("[Byte] Packet data can be fetched and updated at the same time")
	void getAndPutByte() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(packet.getByte(offset)).thenReturn(old);
		when(packet.getByteVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndPutByte(offset, value)).isEqualTo(old);
		assertThat(packet.getAndPutByteVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getByte(offset);
		verify(packet, times(1).description("should be read once")).getByteVolatile(offset);
		verify(packet, times(1).description("should be written once")).putByte(offset, value);
		verify(packet, times(1).description("should be written once")).putByteVolatile(offset, value);
	}

	@Test
	@DisplayName("[Short] Packet data can be fetched and updated at the same time")
	void getAndPutShort() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(packet.getShort(offset)).thenReturn(old);
		when(packet.getShortVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndPutShort(offset, value)).isEqualTo(old);
		assertThat(packet.getAndPutShortVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getShort(offset);
		verify(packet, times(1).description("should be read once")).getShortVolatile(offset);
		verify(packet, times(1).description("should be written once")).putShort(offset, value);
		verify(packet, times(1).description("should be written once")).putShortVolatile(offset, value);
	}

	@Test
	@DisplayName("[Int] Packet data can be fetched and updated at the same time")
	void getAndPutInt() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt();
		val old = random.nextInt();
		when(packet.getInt(offset)).thenReturn(old);
		when(packet.getIntVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndPutInt(offset, value)).isEqualTo(old);
		assertThat(packet.getAndPutIntVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getInt(offset);
		verify(packet, times(1).description("should be read once")).getIntVolatile(offset);
		verify(packet, times(1).description("should be written once")).putInt(offset, value);
		verify(packet, times(1).description("should be written once")).putIntVolatile(offset, value);
	}

	@Test
	@DisplayName("[Long] Packet data can be fetched and updated at the same time")
	void getAndPutLong() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextLong();
		val old = random.nextLong();
		when(packet.getLong(offset)).thenReturn(old);
		when(packet.getLongVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndPutLong(offset, value)).isEqualTo(old);
		assertThat(packet.getAndPutLongVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getLong(offset);
		verify(packet, times(1).description("should be read once")).getLongVolatile(offset);
		verify(packet, times(1).description("should be written once")).putLong(offset, value);
		verify(packet, times(1).description("should be written once")).putLongVolatile(offset, value);
	}

	@Test
	@DisplayName("[Byte] Packet data can be fetched and added at the same time")
	void getAndAddByte() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(packet.getByte(offset)).thenReturn(old);
		when(packet.getByteVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndAddByte(offset, value)).isEqualTo(old);
		assertThat(packet.getAndAddByteVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getByte(offset);
		verify(packet, times(1).description("should be read once")).getByteVolatile(offset);
		verify(packet, times(1).description("should be written once")).putByte(offset, (byte) (value + old));
		verify(packet, times(1).description("should be written once")).putByteVolatile(offset, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Packet data can be fetched and added at the same time")
	void getAndAddShort() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(packet.getShort(offset)).thenReturn(old);
		when(packet.getShortVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndAddShort(offset, value)).isEqualTo(old);
		assertThat(packet.getAndAddShortVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getShort(offset);
		verify(packet, times(1).description("should be read once")).getShortVolatile(offset);
		verify(packet, times(1).description("should be written once")).putShort(offset, (short) (value + old));
		verify(packet, times(1).description("should be written once")).putShortVolatile(offset, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Packet data can be fetched and added at the same time")
	void getAndAddInt() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt();
		val old = random.nextInt();
		when(packet.getInt(offset)).thenReturn(old);
		when(packet.getIntVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndAddInt(offset, value)).isEqualTo(old);
		assertThat(packet.getAndAddIntVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getInt(offset);
		verify(packet, times(1).description("should be read once")).getIntVolatile(offset);
		verify(packet, times(1).description("should be written once")).putInt(offset, value + old);
		verify(packet, times(1).description("should be written once")).putIntVolatile(offset, value + old);
	}

	@Test
	@DisplayName("[Long] Packet data can be fetched and added at the same time")
	void getAndAddLong() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextLong();
		val old = random.nextLong();
		when(packet.getLong(offset)).thenReturn(old);
		when(packet.getLongVolatile(offset)).thenReturn(old);
		assertThat(packet.getAndAddLong(offset, value)).isEqualTo(old);
		assertThat(packet.getAndAddLongVolatile(offset, value)).isEqualTo(old);
		verify(packet, times(1).description("should be read once")).getLong(offset);
		verify(packet, times(1).description("should be read once")).getLongVolatile(offset);
		verify(packet, times(1).description("should be written once")).putLong(offset, value + old);
		verify(packet, times(1).description("should be written once")).putLongVolatile(offset, value + old);
	}

	@Test
	@DisplayName("[Byte] Packet data can be added")
	void addByte() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(packet.getByte(offset)).thenReturn(old);
		when(packet.getByteVolatile(offset)).thenReturn(old);
		packet.addByte(offset, value);
		packet.addByteVolatile(offset, value);
		verify(packet, times(1).description("should be read once")).getByte(offset);
		verify(packet, times(1).description("should be read once")).getByteVolatile(offset);
		verify(packet, times(1).description("should be written once")).putByte(offset, (byte) (value + old));
		verify(packet, times(1).description("should be written once")).putByteVolatile(offset, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Packet data can be added")
	void addShort() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(packet.getShort(offset)).thenReturn(old);
		when(packet.getShortVolatile(offset)).thenReturn(old);
		packet.addShort(offset, value);
		packet.addShortVolatile(offset, value);
		verify(packet, times(1).description("should be read once")).getShort(offset);
		verify(packet, times(1).description("should be read once")).getShortVolatile(offset);
		verify(packet, times(1).description("should be written once")).putShort(offset, (short) (value + old));
		verify(packet, times(1).description("should be written once")).putShortVolatile(offset, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Packet data can be added")
	void addInt() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt();
		val old = random.nextInt();
		when(packet.getInt(offset)).thenReturn(old);
		when(packet.getIntVolatile(offset)).thenReturn(old);
		packet.addInt(offset, value);
		packet.addIntVolatile(offset, value);
		verify(packet, times(1).description("should be read once")).getInt(offset);
		verify(packet, times(1).description("should be read once")).getIntVolatile(offset);
		verify(packet, times(1).description("should be written once")).putInt(offset, value + old);
		verify(packet, times(1).description("should be written once")).putIntVolatile(offset, value + old);
	}

	@Test
	@DisplayName("[Long] Packet data can be added")
	void addLong() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextLong();
		val old = random.nextLong();
		when(packet.getLong(offset)).thenReturn(old);
		when(packet.getLongVolatile(offset)).thenReturn(old);
		packet.addLong(offset, value);
		packet.addLongVolatile(offset, value);
		verify(packet, times(1).description("should be read once")).getLong(offset);
		verify(packet, times(1).description("should be read once")).getLongVolatile(offset);
		verify(packet, times(1).description("should be written once")).putLong(offset, value + old);
		verify(packet, times(1).description("should be written once")).putLongVolatile(offset, value + old);
	}

	@Test
	@DisplayName("[Byte] Packet data can be added and fetched at the same time")
	void addAndGetByte() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(packet.getByte(offset)).thenReturn(old);
		when(packet.getByteVolatile(offset)).thenReturn(old);
		assertThat(packet.addAndGetByte(offset, value)).isEqualTo((byte) (value + old));
		assertThat(packet.addAndGetByteVolatile(offset, value)).isEqualTo((byte) (value + old));
		verify(packet, times(1).description("should be read once")).getByte(offset);
		verify(packet, times(1).description("should be read once")).getByteVolatile(offset);
		verify(packet, times(1).description("should be written once")).putByte(offset, (byte) (value + old));
		verify(packet, times(1).description("should be written once")).putByteVolatile(offset, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Packet data can be added and fetched at the same time")
	void addAndGetShort() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(packet.getShort(offset)).thenReturn(old);
		when(packet.getShortVolatile(offset)).thenReturn(old);
		assertThat(packet.addAndGetShort(offset, value)).isEqualTo((short) (value + old));
		assertThat(packet.addAndGetShortVolatile(offset, value)).isEqualTo((short) (value + old));
		verify(packet, times(1).description("should be read once")).getShort(offset);
		verify(packet, times(1).description("should be read once")).getShortVolatile(offset);
		verify(packet, times(1).description("should be written once")).putShort(offset, (short) (value + old));
		verify(packet, times(1).description("should be written once")).putShortVolatile(offset, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Packet data can be added and fetched at the same time")
	void addAndGetInt() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextInt();
		val old = random.nextInt();
		when(packet.getInt(offset)).thenReturn(old);
		when(packet.getIntVolatile(offset)).thenReturn(old);
		assertThat(packet.addAndGetInt(offset, value)).isEqualTo(value + old);
		assertThat(packet.addAndGetIntVolatile(offset, value)).isEqualTo(value + old);
		verify(packet, times(1).description("should be read once")).getInt(offset);
		verify(packet, times(1).description("should be read once")).getIntVolatile(offset);
		verify(packet, times(1).description("should be written once")).putInt(offset, value + old);
		verify(packet, times(1).description("should be written once")).putIntVolatile(offset, value + old);
	}

	@Test
	@DisplayName("[Long] Packet data can be added and fetched at the same time")
	void addAndGetLong() {
		assumeThat(packet).isNotNull();
		val offset = random.nextInt(Integer.MAX_VALUE);
		val value = random.nextLong();
		val old = random.nextLong();
		when(packet.getLong(offset)).thenReturn(old);
		when(packet.getLongVolatile(offset)).thenReturn(old);
		assertThat(packet.addAndGetLong(offset, value)).isEqualTo(value + old);
		assertThat(packet.addAndGetLongVolatile(offset, value)).isEqualTo(value + old);
		verify(packet, times(1).description("should be read once")).getLong(offset);
		verify(packet, times(1).description("should be read once")).getLongVolatile(offset);
		verify(packet, times(1).description("should be written once")).putLong(offset, value + old);
		verify(packet, times(1).description("should be written once")).putLongVolatile(offset, value + old);
	}

	@Test
	@DisplayName("Packet data can be fetched as an array")
	void get() {
		assumeThat(packet).isNotNull();
		val bytes = random.nextInt(100) + 2;
		val offset = random.nextInt(bytes);
		doNothing().when(packet).get(eq(offset), eq(bytes), any(byte[].class));
		doNothing().when(packet).getVolatile(eq(offset), eq(bytes), any(byte[].class));
		assertThat(packet.get(offset, bytes)).hasSize(bytes);
		assertThat(packet.getVolatile(offset, bytes)).hasSize(bytes);
		verify(packet, times(1).description("should be written once")).get(eq(offset), eq(bytes), any(byte[].class));
		verify(packet, times(1).description("should be written once")).getVolatile(eq(offset), eq(bytes), any(byte[].class));
	}

}
