package de.tum.in.net.ixy.generic;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link IxyMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("IxyMemoryManager")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
final class IxyMemoryManagerTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** A mocked instance of a memory manager. */
	@Spy
	private IxyMemoryManager mmanager;

	@Test
	@DisplayName("[Byte] Memory address can be fetched and updated at the same time")
	void getAndPutByte() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(mmanager.getByte(address)).thenReturn(old);
		when(mmanager.getByteVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndPutByte(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndPutByteVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getByte(address);
		verify(mmanager, times(1).description("should be read once")).getByteVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putByte(address, value);
		verify(mmanager, times(1).description("should be written once")).putByteVolatile(address, value);
	}

	@Test
	@DisplayName("[Short] Memory address can be fetched and updated at the same time")
	void getAndPutShort() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(mmanager.getShort(address)).thenReturn(old);
		when(mmanager.getShortVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndPutShort(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndPutShortVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getShort(address);
		verify(mmanager, times(1).description("should be read once")).getShortVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putShort(address, value);
		verify(mmanager, times(1).description("should be written once")).putShortVolatile(address, value);
	}

	@Test
	@DisplayName("[Int] Memory address can be fetched and updated at the same time")
	void getAndPutInt() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextInt();
		val old = random.nextInt();
		when(mmanager.getInt(address)).thenReturn(old);
		when(mmanager.getIntVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndPutInt(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndPutIntVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getInt(address);
		verify(mmanager, times(1).description("should be read once")).getIntVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putInt(address, value);
		verify(mmanager, times(1).description("should be written once")).putIntVolatile(address, value);
	}

	@Test
	@DisplayName("[Long] Memory address can be fetched and updated at the same time")
	void getAndPutLong() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextLong();
		val old = random.nextLong();
		when(mmanager.getLong(address)).thenReturn(old);
		when(mmanager.getLongVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndPutLong(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndPutLongVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getLong(address);
		verify(mmanager, times(1).description("should be read once")).getLongVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putLong(address, value);
		verify(mmanager, times(1).description("should be written once")).putLongVolatile(address, value);
	}

	@Test
	@DisplayName("[Byte] Memory address can be fetched and added at the same time")
	void getAndAddByte() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(mmanager.getByte(address)).thenReturn(old);
		when(mmanager.getByteVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndAddByte(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndAddByteVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getByte(address);
		verify(mmanager, times(1).description("should be read once")).getByteVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putByte(address, (byte) (value + old));
		verify(mmanager, times(1).description("should be written once")).putByteVolatile(address, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Memory address can be fetched and added at the same time")
	void getAndAddShort() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(mmanager.getShort(address)).thenReturn(old);
		when(mmanager.getShortVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndAddShort(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndAddShortVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getShort(address);
		verify(mmanager, times(1).description("should be read once")).getShortVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putShort(address, (short) (value + old));
		verify(mmanager, times(1).description("should be written once")).putShortVolatile(address, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Memory address can be fetched and added at the same time")
	void getAndAddInt() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextInt();
		val old = random.nextInt();
		when(mmanager.getInt(address)).thenReturn(old);
		when(mmanager.getIntVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndAddInt(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndAddIntVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getInt(address);
		verify(mmanager, times(1).description("should be read once")).getIntVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putInt(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putIntVolatile(address, value + old);
	}

	@Test
	@DisplayName("[Long] Memory address can be fetched and added at the same time")
	void getAndAddLong() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextLong();
		val old = random.nextLong();
		when(mmanager.getLong(address)).thenReturn(old);
		when(mmanager.getLongVolatile(address)).thenReturn(old);
		assertThat(mmanager.getAndAddLong(address, value)).isEqualTo(old);
		assertThat(mmanager.getAndAddLongVolatile(address, value)).isEqualTo(old);
		verify(mmanager, times(1).description("should be read once")).getLong(address);
		verify(mmanager, times(1).description("should be read once")).getLongVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putLong(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putLongVolatile(address, value + old);
	}

	@Test
	@DisplayName("[Byte] Memory address can be added")
	void addByte() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(mmanager.getByte(address)).thenReturn(old);
		when(mmanager.getByteVolatile(address)).thenReturn(old);
		mmanager.addByte(address, value);
		mmanager.addByteVolatile(address, value);
		verify(mmanager, times(1).description("should be read once")).getByte(address);
		verify(mmanager, times(1).description("should be read once")).getByteVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putByte(address, (byte) (value + old));
		verify(mmanager, times(1).description("should be written once")).putByteVolatile(address, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Memory address can be added")
	void addShort() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(mmanager.getShort(address)).thenReturn(old);
		when(mmanager.getShortVolatile(address)).thenReturn(old);
		mmanager.addShort(address, value);
		mmanager.addShortVolatile(address, value);
		verify(mmanager, times(1).description("should be read once")).getShort(address);
		verify(mmanager, times(1).description("should be read once")).getShortVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putShort(address, (short) (value + old));
		verify(mmanager, times(1).description("should be written once")).putShortVolatile(address, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Memory address can be added")
	void addInt() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextInt();
		val old = random.nextInt();
		when(mmanager.getInt(address)).thenReturn(old);
		when(mmanager.getIntVolatile(address)).thenReturn(old);
		mmanager.addInt(address, value);
		mmanager.addIntVolatile(address, value);
		verify(mmanager, times(1).description("should be read once")).getInt(address);
		verify(mmanager, times(1).description("should be read once")).getIntVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putInt(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putIntVolatile(address, value + old);
	}

	@Test
	@DisplayName("[Long] Memory address can be added")
	void addLong() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextLong();
		val old = random.nextLong();
		when(mmanager.getLong(address)).thenReturn(old);
		when(mmanager.getLongVolatile(address)).thenReturn(old);
		mmanager.addLong(address, value);
		mmanager.addLongVolatile(address, value);
		verify(mmanager, times(1).description("should be read once")).getLong(address);
		verify(mmanager, times(1).description("should be read once")).getLongVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putLong(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putLongVolatile(address, value + old);
	}

	@Test
	@DisplayName("[Byte] Memory address can be added and fetched at the same time")
	void addAndGetByte() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (byte) (random.nextInt() & 0xFF);
		val old = (byte) (random.nextInt() & 0xFF);
		when(mmanager.getByte(address)).thenReturn(old);
		when(mmanager.getByteVolatile(address)).thenReturn(old);
		assertThat(mmanager.addAndGetByte(address, value)).isEqualTo((byte) (value + old));
		assertThat(mmanager.addAndGetByteVolatile(address, value)).isEqualTo((byte) (value + old));
		verify(mmanager, times(1).description("should be read once")).getByte(address);
		verify(mmanager, times(1).description("should be read once")).getByteVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putByte(address, (byte) (value + old));
		verify(mmanager, times(1).description("should be written once")).putByteVolatile(address, (byte) (value + old));
	}

	@Test
	@DisplayName("[Short] Memory address can be added and fetched at the same time")
	void addAndGetShort() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = (short) (random.nextInt() & 0xFFFF);
		val old = (short) (random.nextInt() & 0xFFFF);
		when(mmanager.getShort(address)).thenReturn(old);
		when(mmanager.getShortVolatile(address)).thenReturn(old);
		assertThat(mmanager.addAndGetShort(address, value)).isEqualTo((short) (value + old));
		assertThat(mmanager.addAndGetShortVolatile(address, value)).isEqualTo((short) (value + old));
		verify(mmanager, times(1).description("should be read once")).getShort(address);
		verify(mmanager, times(1).description("should be read once")).getShortVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putShort(address, (short) (value + old));
		verify(mmanager, times(1).description("should be written once")).putShortVolatile(address, (short) (value + old));
	}

	@Test
	@DisplayName("[Int] Memory address can be added and fetched at the same time")
	void addAndGetInt() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextInt();
		val old = random.nextInt();
		when(mmanager.getInt(address)).thenReturn(old);
		when(mmanager.getIntVolatile(address)).thenReturn(old);
		assertThat(mmanager.addAndGetInt(address, value)).isEqualTo(value + old);
		assertThat(mmanager.addAndGetIntVolatile(address, value)).isEqualTo(value + old);
		verify(mmanager, times(1).description("should be read once")).getInt(address);
		verify(mmanager, times(1).description("should be read once")).getIntVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putInt(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putIntVolatile(address, value + old);
	}

	@Test
	@DisplayName("[Long] Memory address can be added and fetched at the same time")
	void addAndGetLong() {
		assumeThat(mmanager).isNotNull();
		val address = random.nextLong();
		val value = random.nextLong();
		val old = random.nextLong();
		when(mmanager.getLong(address)).thenReturn(old);
		when(mmanager.getLongVolatile(address)).thenReturn(old);
		assertThat(mmanager.addAndGetLong(address, value)).isEqualTo(value + old);
		assertThat(mmanager.addAndGetLongVolatile(address, value)).isEqualTo(value + old);
		verify(mmanager, times(1).description("should be read once")).getLong(address);
		verify(mmanager, times(1).description("should be read once")).getLongVolatile(address);
		verify(mmanager, times(1).description("should be written once")).putLong(address, value + old);
		verify(mmanager, times(1).description("should be written once")).putLongVolatile(address, value + old);
	}

}
