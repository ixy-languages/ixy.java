package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.PacketBuffer;

import java.util.Random;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.val;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atMostOnce;

/**
 * Tests the class {@link PacketBuffer}.
 *
 * @author Esaú García Sánchez-Torija
 * @see PacketBuffer
 */
@DisplayName("PacketBuffer")
@ExtendWith(MockitoExtension.class)
final class PacketBufferTest {

	@Nested
	final class BuilderTest {

		/** A constant expression used to resource lock the mocked memory manager. */
		private static final String MOCK_LOCK = "MOCK";

		/** A cached instance of a pseudo-random number generator. */
		private final Random random = new Random();

		/** Holds the packet builder. */
		private transient PacketBuffer.Builder builder;

		/** Holds a mocked memory manager. */
		@Mock
		private IxyMemoryManager mmanager;

		// Creates the packet builder with the mocked memory manager
		@BeforeEach
		void setUp() {
			builder = new PacketBuffer.Builder(mmanager);
		}

		@Test
		@DisplayName("Wrong arguments produce exceptions")
		void exceptions() {
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new PacketBuffer.Builder(null));
		}

		@Test
		@ResourceLock(MOCK_LOCK)
		@DisplayName("The virtual address can be written and read")
		void getsetVirtualAddress() {
			assumeThat(random).isNotNull();
			assumeThat(builder).isNotNull();
			val number = random.nextLong();
			builder.setVirtualAddress(number);
			assertThat(builder.getVirtualAddress()).as("Virtual address").isEqualTo(number);
		}

		@Test
		@ResourceLock(MOCK_LOCK)
		@DisplayName("The packet can be built")
		void build() {
			assumeThat(random).isNotNull();
			assumeThat(builder).isNotNull();
			assumeThat(mmanager).isNotNull();

			// Generate the values
			val virtual = random.nextLong();
			val physical = random.nextLong();
			val size = random.nextInt();
			val id = random.nextInt();

			// Set the values
			builder.setVirtualAddress(virtual);
			builder.setPhysicalAddress(physical);
			builder.setSize(size);
			builder.setMemoryPoolId(id);
			reset(mmanager);
			when(mmanager.getLong(virtual + PacketBuffer.PAP_OFFSET)).thenReturn(physical);
			when(mmanager.getLongVolatile(virtual + PacketBuffer.PAP_OFFSET)).thenReturn(physical);
			when(mmanager.getInt(virtual + PacketBuffer.MPI_OFFSET)).thenReturn(id);
			when(mmanager.getIntVolatile(virtual + PacketBuffer.MPI_OFFSET)).thenReturn(id);
			when(mmanager.getInt(virtual + PacketBuffer.PKT_OFFSET)).thenReturn(size);
			when(mmanager.getIntVolatile(virtual + PacketBuffer.PKT_OFFSET)).thenReturn(size);

			// Assert the values on the builder
			var softly = new SoftAssertions();
			softly.assertThat(builder.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
			softly.assertThat(builder.getPhysicalAddress()).as("Physical address").isEqualTo(physical);
			softly.assertThat(builder.getMemoryPoolId()).as("Memory pool id").isEqualTo(id);
			softly.assertThat(builder.getSize()).as("Size").isEqualTo(size);
			softly.assertAll();

			// Assert the values on the built packet
			var packet = builder.build();
			verify(mmanager, atMost(1).description("pointers should have been written")).putLong(anyLong(), anyLong());
			verify(mmanager, atMost(1).description("pointers should have been written")).putLongVolatile(anyLong(), anyLong());
			verify(mmanager, atMost(2).description("values should have been written")).putInt(anyLong(), anyInt());
			verify(mmanager, atMost(2).description("values should have been written")).putIntVolatile(anyLong(), anyInt());
			assertThat(packet).as("Packet").isNotNull();
			softly = new SoftAssertions();
			softly.assertThat(packet.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
			softly.assertThat(packet.getPhysicalAddress()).as("Physical address").isEqualTo(physical);
			softly.assertThat(packet.getMemoryPoolId()).as("Memory pool id").isEqualTo(id);
			softly.assertThat(packet.getSize()).as("Size").isEqualTo(size);
			softly.assertAll();

			// Don't write anything to memory
			builder.setPhysicalAddress(null);
			builder.setMemoryPoolId(null);
			builder.setSize(null);
			reset(mmanager);

			// Assert again with the missing values
			softly = new SoftAssertions();
			softly.assertThat(builder.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
			softly.assertThat(builder.getPhysicalAddress()).as("Physical address").isNull();
			softly.assertThat(builder.getMemoryPoolId()).as("Memory pool id").isNull();
			softly.assertThat(builder.getSize()).as("Size").isNull();
			softly.assertAll();

			// Assert the values on the built packet
			packet = builder.build();
			verify(mmanager, times(0).description("pointers should not have been written")).putLong(anyLong(), anyLong());
			verify(mmanager, times(0).description("pointers should not have been written")).putLongVolatile(anyLong(), anyLong());
			verify(mmanager, times(0).description("values should not have been written")).putInt(anyLong(), anyInt());
			verify(mmanager, times(0).description("values should not have been written")).putIntVolatile(anyLong(), anyInt());
			assertThat(packet).as("Packet").isNotNull();
			assertThat(packet.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
		}

	}

	/** A constant expression used to resource lock the mocked memory manager. */
	private static final String MOCK_LOCK = "MOCK";

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new Random();

	/** Holds the virtual memory address. */
	private long virtual;

	/** Holds a mocked memory manager. */
	@Mock
	private IxyMemoryManager mmanager;

	/** Holds an instance of {@link PacketBuffer} that will be used to test. */
	private PacketBuffer packetBuffer;

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp() {
		virtual = Math.max(1, random.nextLong());
		packetBuffer = new PacketBuffer(virtual, mmanager);
	}

	@Test
	@DisabledIfOptimized
	@DisplayName("Wrong arguments produce exceptions")
	void exceptions() {
		assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.get(-1, -1));
		assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.get(0, -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(-1, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(-1, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(0, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(0, 0, null));
		assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, -1));
		assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.getVolatile(0, -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(0, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(0, 0, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.put(-1, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.put(0, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.put(0, 0, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.putVolatile(-1, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.putVolatile(0, -1, null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.putVolatile(0, 0, null));
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> packetBuffer.compareTo(null));
	}

	@Test
	@DisabledIfOptimized
	@DisplayName("Missing memory manager produces exceptions")
	void stateExceptions() {
		try {
			val mmanagerField = PacketBuffer.class.getDeclaredField("mmanager");
			mmanagerField.setAccessible(true);
			val _mmanager = mmanagerField.get(packetBuffer);
			synchronized (packetBuffer) {
				mmanagerField.set(packetBuffer, null);
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getPhysicalAddress());
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getMemoryPoolId());
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getSize());
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.setSize(0));
				assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.get(-1, -1));
				assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.get(0, -1));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.get(-1, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.get(-1, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.get(0, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.get(0, 0, null));
				assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, -1));
				assertThatExceptionOfType(NegativeArraySizeException.class).isThrownBy(() -> packetBuffer.getVolatile(0, -1));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getVolatile(-1, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getVolatile(0, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.getVolatile(0, 0, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.put(-1, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.put(0, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.put(0, 0, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.putVolatile(-1, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.putVolatile(0, -1, null));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> packetBuffer.putVolatile(0, 0, null));
				mmanagerField.set(packetBuffer, _mmanager);
			}
		} catch (final NoSuchFieldException | IllegalAccessException e) {
//			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("The virtual address is correct")
	void getBaseAddress() {
		assumeThat(packetBuffer).isNotNull();
		assertThat(packetBuffer.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The physical address can be read")
	void getPhysicalAddress() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextLong();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PAP_OFFSET;
		when(mmanager.getLong(virtual + offset)).thenReturn(number);
		assertThat(packetBuffer.getPhysicalAddress()).as("Physical address").isEqualTo(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getLong(virtual + offset);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getLongVolatile(virtual + offset);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The physical address can be written")
	void setPhysicalAddress() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextLong();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PAP_OFFSET;
		packetBuffer.setPhysicalAddress(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putLong(virtual + offset, number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putLongVolatile(virtual + offset, number);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The memory pool index can be read")
	void getMemoryPoolId() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextInt();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.MPI_OFFSET;
		when(mmanager.getInt(virtual + offset)).thenReturn(number);
		assertThat(packetBuffer.getMemoryPoolId()).as("Memory pool index").isEqualTo(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getInt(virtual + offset);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getIntVolatile(virtual + offset);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The memory pool index can be written")
	void setMemoryPoolId() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextInt();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.MPI_OFFSET;
		packetBuffer.setMemoryPoolId(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putInt(virtual + offset, number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putIntVolatile(virtual + offset, number);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The size can be read")
	void getSize() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextInt();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PKT_OFFSET;
		when(mmanager.getInt(virtual + offset)).thenReturn(number);
		assertThat(packetBuffer.getSize()).as("Packet size").isEqualTo(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getInt(virtual + offset);
		verify(mmanager, atMostOnce().description("memory address should be read once")).getIntVolatile(virtual + offset);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("The size can be written")
	void setSize() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val number = random.nextInt();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PKT_OFFSET;
		packetBuffer.setSize(number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putInt(virtual + offset, number);
		verify(mmanager, atMostOnce().description("memory address should be read once")).putIntVolatile(virtual + offset, number);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A byte can be read")
	void getByte() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			when(mmanager.getByte(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getByte(i)).as("Data byte").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getByte(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile byte can be read")
	void getByteVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			when(mmanager.getByteVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getByteVolatile(i)).as("Data byte").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getByteVolatile(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A byte can be written")
	void putByte() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			packetBuffer.putByte(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putByte(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile byte can be written")
	void putByteVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			packetBuffer.putByteVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putByteVolatile(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A short can be read")
	void getShort() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			when(mmanager.getShort(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getShort(i)).as("Data short").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getShort(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile short can be read")
	void getShortVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			when(mmanager.getShortVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getShortVolatile(i)).as("Data short").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getShortVolatile(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A short can be written")
	void putShort() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			packetBuffer.putShort(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putShort(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile short can be written")
	void putShortVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			packetBuffer.putShortVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putShortVolatile(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A int can be read")
	void getInt() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			when(mmanager.getInt(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getInt(i)).as("Data int").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getInt(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile int can be read")
	void getIntVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			when(mmanager.getIntVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getIntVolatile(i)).as("Data int").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getIntVolatile(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A int can be written")
	void putInt() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			packetBuffer.putInt(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putInt(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile int can be written")
	void putIntVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			packetBuffer.putIntVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putIntVolatile(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A long can be read")
	void getLong() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextLong();
			when(mmanager.getLong(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getLong(i)).as("Data long").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getLong(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile long can be read")
	void getLongVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextLong();
			when(mmanager.getLongVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getLongVolatile(i)).as("Data long").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getLongVolatile(base + i);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A long can be written")
	void putLong() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextLong();
			packetBuffer.putLong(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putLong(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile long can be written")
	void putLongVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val size = random.nextInt(PacketBuffer.HEADER_BYTES/2 + 1) + PacketBuffer.HEADER_BYTES/2;
		for (var i = 0; i < size; i += 1) {
			val number = random.nextLong();
			packetBuffer.putLongVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putLongVolatile(base + i, number);
		}
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A segment can be read (buffer parameter)")
	void get3() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		val copy = new byte[bytes.length];
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).get(virtual, copy.length, copy, 0);
		packetBuffer.get(0, copy.length, copy);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).get(virtual, copy.length, copy, 0);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A segment can be read")
	void get2() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		val copy = packetBuffer.get(0, bytes.length);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile segment can be read (buffer parameter)")
	void getVolatile3() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		val copy = new byte[bytes.length];
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(copy.length, buff.length));
			return null;
		}).when(mmanager).getVolatile(virtual, copy.length, copy, 0);
		packetBuffer.getVolatile(0, copy.length, copy);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(virtual, copy.length, copy, 0);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A volatile segment can be read")
	void getVolatile2() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		val copy = packetBuffer.getVolatile(0, bytes.length);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A segment can be written")
	void put() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		packetBuffer.put(0, bytes.length, bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).put(virtual, bytes.length, bytes, 0);
		reset(mmanager);
	}

	@Test
	@ResourceLock(MOCK_LOCK)
	@DisplayName("A segment can be written")
	void putVolatile() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		packetBuffer.putVolatile(0, bytes.length, bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).putVolatile(virtual, bytes.length, bytes, 0);
		reset(mmanager);
	}

	@Test
	@DisplayName("Packets can be compared")
	void compareTo() {
		assumeThat(packetBuffer).isNotNull();
		assertThat(packetBuffer.compareTo(packetBuffer)).isEqualTo(0);
		for (var i = -1; i <= 1; i += 1 ) {
			val packet = new PacketBuffer(virtual + i, mmanager);
			assertThat(packetBuffer.compareTo(packet)).isEqualTo(-i);
			assertThat(packet.compareTo(packetBuffer)).isEqualTo(i);
		}
	}

	@Test
	@DisplayName("Packets can be cloned")
	void clonable() {
		assumeThat(packetBuffer).isNotNull();
		try {
			val cloned = packetBuffer.clone();
			assertThat(cloned).isNotEqualTo(packetBuffer);
			assertThat(cloned).isEqualByComparingTo(packetBuffer);
		} catch (final CloneNotSupportedException e) {
			fail("Failed to clone", e);
		}
	}

}
