package de.tum.in.net.ixy.memory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import lombok.val;

import org.jetbrains.annotations.Contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link PacketBufferWrapper}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("PacketBufferWrapper")
@Execution(ExecutionMode.CONCURRENT)
final class PacketBufferWrapperTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** The maximum hugepage size, which is usually 2MB. */
	private static final int MAX_HUGEPAGE = 2 * 1024 * 1024;

	/** The sizes of the allocated regions. */
	private long[] sizes;

	/** The allocated regions. */
	private long[] virtuals;

	/** A memory manager. */
	private final MemoryManager mmanager = SmartUnsafeMemoryManager.getSingleton();

	/** Holds an instance of {@link PacketBufferWrapper} that will be used to test. */
	private PacketBufferWrapper packetBufferWrapper;

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp() {
		virtuals = new long[4];
		sizes = new long[4];
		var i = 0;
		for (val huge : Arrays.asList(false, true)) {
			for (val lock : Arrays.asList(false, true)) {
				sizes[i] = createSize(0xFFFF);
				virtuals[i] = mmanager.allocate(sizes[i], huge, lock);
				i += 1;
			}
		}
	}

//	@Test
//	@DisplayName("Wrong arguments produce exceptions")
//	void exceptions() {
//		int[] offsets = {-1, 0};
//		int[] bytes = {-1, 0};
//		for (val offset : offsets) {
//			for (val length : bytes) {
//				Class<? extends RuntimeException> exceptionClass1 = null;
//				Class<? extends RuntimeException> exceptionClass2 = null;
//				if (BuildConfig.OPTIMIZED) {
//					if (length < 0) exceptionClass1 = NegativeArraySizeException.class;
//				} else {
//					if (offset < 0 || length < 0) {
//						exceptionClass1 = IllegalArgumentException.class;
//						exceptionClass2 = IllegalArgumentException.class;
//					} else {
//						exceptionClass2 = NullPointerException.class;
//					}
//				}
//				if (exceptionClass1 != null) {
//					assertThatExceptionOfType(exceptionClass1).isThrownBy(() -> packetBufferWrapper.get(offset, length));
//					assertThatExceptionOfType(exceptionClass1).isThrownBy(() -> packetBufferWrapper.getVolatile(offset, length));
//				}
//				if (exceptionClass2 != null) {
//					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packetBufferWrapper.get(offset, length, null));
//					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packetBufferWrapper.getVolatile(offset, length, null));
//					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packetBufferWrapper.put(offset, length, null));
//					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packetBufferWrapper.putVolatile(offset, length, null));
//				}
//			}
//		}
//		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> packetBufferWrapper.compareTo(null));
//	}
//
	@Test
	@DisplayName("The virtual address is correct")
	void getBaseAddress() {
		for (val virtual : virtuals) {
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getVirtual()).isEqualTo(virtual);
		}
	}
//
//	@Test
//	@DisplayName("The physical address can be read")
//	void getPhysicalAddress() {
//		assume();
//		val offset = PacketBufferWrapper.HEADER_OFFSET + PacketBufferWrapper.PAP_OFFSET;
//		reset(mmanager);
//		when(mmanager.getLong(virtual + offset)).thenReturn(physical);
//		assertThat(packetBufferWrapper.getPhysicalAddress()).as("Physical address").isEqualTo(physical);
//		verify(mmanager, times(1).description("memory address should be read once")).getLong(virtual + offset);
//	}
//
//	@Test
//	@DisplayName("The memory pool identifier can be read")
//	void getMemoryPoolId() {
//		assume();
//		val offset = PacketBufferWrapper.HEADER_OFFSET + PacketBufferWrapper.MPI_OFFSET;
//		reset(mmanager);
//		when(mmanager.getInt(virtual + offset)).thenReturn(pool);
//		assertThat(packetBufferWrapper.getMemoryPoolId()).as("Memory pool identifier").isEqualTo(pool);
//		verify(mmanager, times(1).description("memory address should be read once")).getInt(virtual + offset);
//	}
//
//	@Test
//	@DisplayName("The size can be read")
//	void getSize() {
//		assume();
//		val offset = PacketBufferWrapper.HEADER_OFFSET + PacketBufferWrapper.PKT_OFFSET;
//		reset(mmanager);
//		when(mmanager.getInt(virtual + offset)).thenReturn(size);
//		assertThat(packetBufferWrapper.getSize()).as("Packet size").isEqualTo(size);
//		verify(mmanager, times(1).description("memory address should be read once")).getInt(virtual + offset);
//	}
//
//	@Test
//	@DisplayName("The size can be written")
//	void setSize() {
//		assume();
//		val offset = PacketBufferWrapper.HEADER_OFFSET + PacketBufferWrapper.PKT_OFFSET;
//		reset(mmanager);
//		packetBufferWrapper.setSize(size);
//		verify(mmanager, times(1).description("memory address should be read once")).putInt(virtual + offset, size);
//	}
//
//	@Test
//	@DisplayName("A byte can be read")
//	void getByte() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
//			when(mmanager.getByte(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getByte(i)).as("Data byte").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getByte(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile byte can be read")
//	void getByteVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
//			when(mmanager.getByteVolatile(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getByteVolatile(i)).as("Data byte").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getByteVolatile(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A byte can be written")
//	void putByte() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
//			packetBufferWrapper.putByte(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putByte(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile byte can be written")
//	void putByteVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
//			packetBufferWrapper.putByteVolatile(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putByteVolatile(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A short can be read")
//	void getShort() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
//			when(mmanager.getShort(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getShort(i)).as("Data short").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getShort(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile short can be read")
//	void getShortVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
//			when(mmanager.getShortVolatile(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getShortVolatile(i)).as("Data short").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getShortVolatile(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A short can be written")
//	void putShort() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
//			packetBufferWrapper.putShort(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putShort(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile short can be written")
//	void putShortVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
//			packetBufferWrapper.putShortVolatile(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putShortVolatile(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A int can be read")
//	void getInt() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextInt(Integer.MAX_VALUE);
//			when(mmanager.getInt(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getInt(i)).as("Data int").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getInt(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile int can be read")
//	void getIntVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextInt(Integer.MAX_VALUE);
//			when(mmanager.getIntVolatile(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getIntVolatile(i)).as("Data int").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getIntVolatile(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A int can be written")
//	void putInt() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextInt(Integer.MAX_VALUE);
//			packetBufferWrapper.putInt(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putInt(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile int can be written")
//	void putIntVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextInt(Integer.MAX_VALUE);
//			packetBufferWrapper.putIntVolatile(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putIntVolatile(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A long can be read")
//	void getLong() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextLong();
//			when(mmanager.getLong(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getLong(i)).as("Data long").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getLong(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile long can be read")
//	void getLongVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextLong();
//			when(mmanager.getLongVolatile(base + i)).thenReturn(number);
//			assumeThat(packetBufferWrapper.getLongVolatile(i)).as("Data long").isEqualTo(number);
//			verify(mmanager, times(1).description("memory address should be read once")).getLongVolatile(base + i);
//		}
//	}
//
//	@Test
//	@DisplayName("A long can be written")
//	void putLong() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextLong();
//			packetBufferWrapper.putLong(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putLong(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A volatile long can be written")
//	void putLongVolatile() {
//		assume();
//		val base = virtual + PacketBufferWrapper.DATA_OFFSET;
//		val length = random.nextInt(PacketBufferWrapper.HEADER_BYTES / 2 + 1) + PacketBufferWrapper.HEADER_BYTES / 2;
//		reset(mmanager);
//		for (var i = 0; i < length; i += 1) {
//			val number = random.nextLong();
//			packetBufferWrapper.putLongVolatile(i, number);
//			verify(mmanager, times(1).description("memory address should be read once")).putLongVolatile(base + i, number);
//		}
//	}
//
//	@Test
//	@DisplayName("A segment can be read")
//	void get2() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		random.nextBytes(bytes);
//		reset(mmanager);
//		doAnswer(invocation -> {
//			val args = invocation.getArguments();
//			val buff = (byte[]) args[2];
//			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
//			return null;
//		}).when(mmanager).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
//		val copy = packetBufferWrapper.get(0, bytes.length);
//		assertThat(copy).isEqualTo(bytes);
//		verify(mmanager, times(1).description("batch copy should be called once")).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
//	}
//
//	@Test
//	@DisplayName("A segment can be read (buffer parameter)")
//	void get3() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		val empty = new byte[0];
//		random.nextBytes(bytes);
//		val copy = new byte[bytes.length];
//		reset(mmanager);
//		doAnswer(invocation -> {
//			val args = invocation.getArguments();
//			val buff = (byte[]) args[2];
//			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
//			return null;
//		}).when(mmanager).get(virtual, copy.length, copy, 0);
//		packetBufferWrapper.get(0, copy.length, copy);
//		packetBufferWrapper.get(0, 0, bytes);
//		packetBufferWrapper.get(0, bytes.length, empty);
//		packetBufferWrapper.get(0, 0, empty);
//		assertThat(copy).isEqualTo(bytes);
//		verify(mmanager, times(1).description("batch copy should be called once")).get(virtual, copy.length, copy, 0);
//	}
//
//	@Test
//	@DisplayName("A volatile segment can be read")
//	void getVolatile2() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		random.nextBytes(bytes);
//		reset(mmanager);
//		doAnswer(invocation -> {
//			val args = invocation.getArguments();
//			val buff = (byte[]) args[2];
//			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
//			return null;
//		}).when(mmanager).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
//		val copy = packetBufferWrapper.getVolatile(0, bytes.length);
//		assertThat(copy).isEqualTo(bytes);
//		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
//	}
//
//	@Test
//	@DisplayName("A volatile segment can be read (buffer parameter)")
//	void getVolatile3() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		val copy = new byte[bytes.length];
//		val empty = new byte[0];
//		random.nextBytes(bytes);
//		reset(mmanager);
//		doAnswer(invocation -> {
//			val args = invocation.getArguments();
//			val buff = (byte[]) args[2];
//			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
//			return null;
//		}).when(mmanager).getVolatile(virtual, copy.length, copy, 0);
//		packetBufferWrapper.getVolatile(0, copy.length, copy);
//		packetBufferWrapper.getVolatile(0, 0, bytes);
//		packetBufferWrapper.getVolatile(0, bytes.length, empty);
//		packetBufferWrapper.getVolatile(0, 0, empty);
//		assertThat(copy).isEqualTo(bytes);
//		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(virtual, copy.length, copy, 0);
//	}
//
//	@Test
//	@DisplayName("A segment can be written")
//	void put() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		val empty = new byte[0];
//		random.nextBytes(bytes);
//		reset(mmanager);
//		packetBufferWrapper.put(0, bytes.length, bytes);
//		packetBufferWrapper.put(0, 0, bytes);
//		packetBufferWrapper.put(0, bytes.length, empty);
//		packetBufferWrapper.put(0, 0, empty);
//		verify(mmanager, times(1).description("batch copy should be called once")).put(virtual, bytes.length, bytes, 0);
//	}
//
//	@Test
//	@DisplayName("A segment can be written")
//	void putVolatile() {
//		assume();
//		val bytes = new byte[PacketBufferWrapper.HEADER_BYTES * 2];
//		val empty = new byte[0];
//		random.nextBytes(bytes);
//		reset(mmanager);
//		packetBufferWrapper.putVolatile(0, bytes.length, bytes);
//		packetBufferWrapper.putVolatile(0, 0, bytes);
//		packetBufferWrapper.putVolatile(0, bytes.length, empty);
//		packetBufferWrapper.putVolatile(0, 0, empty);
//		verify(mmanager, times(1).description("batch copy should be called once")).putVolatile(virtual, bytes.length, bytes, 0);
//	}
//
//	@Test
//	@DisplayName("Packets can be compared")
//	void compareTo() {
//		assumeThat(packetBufferWrapper).isNotNull();
//		assertThat(packetBufferWrapper.compareTo(packetBufferWrapper)).isEqualTo(0);
//		for (var i = -1; i <= 1; i += 1) {
//			val packet = PacketBufferWrapper.builder().manager(mmanager).virtual(virtual + i).build();
//			assertThat(packetBufferWrapper.compareTo(packet)).isEqualTo(-i);
//			assertThat(packet.compareTo(packetBufferWrapper)).isEqualTo(i);
//		}
//	}
//
//	/** Assumes that the member variables are not {@code null}. */
//	private void assume() {
//		assumeThat(random).isNotNull();
//		assumeThat(mmanager).isNotNull();
//		assumeThat(packetBufferWrapper).isNotNull();
//	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Creates a valid size given a mask to apply.
	 *
	 * @param mask The mask.
	 * @return A valid size.
	 */
	@Contract(pure = true)
	private long createSize(final long mask) {
		var bytes = 0L;
		while (bytes == 0) {
			while (bytes <= 0) {
				bytes = random.nextLong();
			}
			bytes &= mask;
		}
		return bytes;
	}

}
