package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.memory.PacketBuffer;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the class {@link PacketBuffer}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("PacketBuffer")
@ExtendWith(MockitoExtension.class)
final class PacketBufferTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** Holds the virtual memory address. */
	private long virtual;

	/** Holds the physical memory address. */
	private long physical;

	/** Holds the size of the packet. */
	private int size;

	/** Holds the size of the packet. */
	private int pool;

	/** Holds a mocked memory manager. */
	private IxyMemoryManager mmanager = mock(IxyMemoryManager.class);

	/** Holds an instance of {@link PacketBuffer} that will be used to test. */
	private PacketBuffer packetBuffer;

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp() {
		virtual = Math.max(2, random.nextLong());
		physical = Math.max(2, random.nextLong());
		size = random.nextInt(2 * 1024 * 1024 - PacketBuffer.HEADER_BYTES) + PacketBuffer.HEADER_BYTES;
		pool = random.nextInt();
		packetBuffer = PacketBuffer.builder().manager(mmanager)
				.virtual(virtual).physical(physical)
				.size(size).pool(pool)
				.build();
	}

	/**
	 * Tests the class {@link PacketBuffer.Builder}.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@Nested
	@DisplayName("PacketBuffer.Builder")
	final class BuilderTest {

		@Test
		@DisabledIfOptimized
		@DisplayName("Wrong arguments produce exceptions")
		void builder() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> PacketBuffer.builder().manager(null).virtual(0).build());
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> PacketBuffer.builder().manager(mmanager).virtual(0).build());
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> PacketBuffer.builder().manager(null).virtual(virtual).build());
		}

		@Test
		@DisplayName("A memory pool can be used to set the memory pool identifier")
		void poolMempool() {
			val mempool = mock(IxyMempool.class);
			assumeThat(random).isNotNull();
			assumeThat(mmanager).isNotNull();
			assumeThat(mempool).isNotNull();
			for (var i = 0; i < 2; i += 1) {
				reset(mmanager);
				reset(mempool);
				val id = random.nextInt();
				if (i % 2 == 0) when(mempool.getId()).thenReturn(id);
				val packet = PacketBuffer.builder()
						.manager(mmanager)
						.virtual(virtual)
						.physical(physical)
						.size(size)
						.pool(i % 2 == 0 ? mempool : null)
						.build();
				when(mmanager.getInt(virtual + PacketBuffer.MPI_OFFSET)).thenReturn(id);
				assertThat(packet.getMemoryPoolId()).isEqualTo(id);
				verify(mempool, (i % 2 == 0 ? times(1) : times(0)).description("the identifier should be obtained once")).getId();
				verify(mmanager, times(1).description("the identifier should be obtained once")).getInt(virtual + PacketBuffer.MPI_OFFSET);
			}
		}

		@Test
		@DisplayName("The toString() method works as expected")
		void ToString() {
			val genericPattern = "^%s\\.%s\\(\\w*manager\\w*=%s, \\w*virt\\w*=%d, \\w*phys\\w*=%d, \\w*size\\w*=%d, \\w*pool\\w*=%d\\)$";
			val specificPattern = String.format(genericPattern,
					PacketBuffer.class.getSimpleName(), PacketBuffer.Builder.class.getSimpleName(),
					mmanager.toString(), virtual, physical, size, pool);
			val builder = PacketBuffer.builder().manager(mmanager).virtual(virtual).physical(physical).size(size).pool(pool);
			assertThat(builder.toString()).matches(Pattern.compile(specificPattern));
		}

	}

	@Test
	@DisabledIfOptimized
	@DisplayName("Wrong arguments produce exceptions")
	void exceptions() {
		int[] offsets = {-1, 0};
		int[] bytes = {-1, 0};
		for (val offset : offsets) {
			for (val length : bytes) {
				if (offset < 0 || length < 0) {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(offset, length));
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(offset, length));
				}
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.get(offset, length, null));
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.getVolatile(offset, length, null));
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.put(offset, length, null));
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.putVolatile(offset, length, null));
			}
		}
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> packetBuffer.compareTo(null));
	}

	@Test
	@DisplayName("The virtual address is correct")
	void getBaseAddress() {
		assumeThat(packetBuffer).isNotNull();
		assertThat(packetBuffer.getVirtualAddress()).as("Virtual address").isEqualTo(virtual);
	}

	@Test
	@DisplayName("The physical address can be read")
	void getPhysicalAddress() {
		assume();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PAP_OFFSET;
		reset(mmanager);
		when(mmanager.getLong(virtual + offset)).thenReturn(physical);
		assertThat(packetBuffer.getPhysicalAddress()).as("Physical address").isEqualTo(physical);
		verify(mmanager, times(1).description("memory address should be read once")).getLong(virtual + offset);
	}

	@Test
	@DisplayName("The memory pool identifier can be read")
	void getMemoryPoolId() {
		assume();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.MPI_OFFSET;
		reset(mmanager);
		when(mmanager.getInt(virtual + offset)).thenReturn(pool);
		assertThat(packetBuffer.getMemoryPoolId()).as("Memory pool identifier").isEqualTo(pool);
		verify(mmanager, times(1).description("memory address should be read once")).getInt(virtual + offset);
	}

	@Test
	@DisplayName("The size can be read")
	void getSize() {
		assume();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PKT_OFFSET;
		reset(mmanager);
		when(mmanager.getInt(virtual + offset)).thenReturn(size);
		assertThat(packetBuffer.getSize()).as("Packet size").isEqualTo(size);
		verify(mmanager, times(1).description("memory address should be read once")).getInt(virtual + offset);
	}

	@Test
	@DisplayName("The size can be written")
	void setSize() {
		assume();
		val offset = PacketBuffer.HEADER_OFFSET + PacketBuffer.PKT_OFFSET;
		reset(mmanager);
		packetBuffer.setSize(size);
		verify(mmanager, times(1).description("memory address should be read once")).putInt(virtual + offset, size);
	}

	@Test
	@DisplayName("A byte can be read")
	void getByte() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			when(mmanager.getByte(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getByte(i)).as("Data byte").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getByte(base + i);
		}
	}

	@Test
	@DisplayName("A volatile byte can be read")
	void getByteVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			when(mmanager.getByteVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getByteVolatile(i)).as("Data byte").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getByteVolatile(base + i);
		}
	}

	@Test
	@DisplayName("A byte can be written")
	void putByte() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			packetBuffer.putByte(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putByte(base + i, number);
		}
	}

	@Test
	@DisplayName("A volatile byte can be written")
	void putByteVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			packetBuffer.putByteVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putByteVolatile(base + i, number);
		}
	}

	@Test
	@DisplayName("A short can be read")
	void getShort() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			when(mmanager.getShort(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getShort(i)).as("Data short").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getShort(base + i);
		}
	}

	@Test
	@DisplayName("A volatile short can be read")
	void getShortVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			when(mmanager.getShortVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getShortVolatile(i)).as("Data short").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getShortVolatile(base + i);
		}
	}

	@Test
	@DisplayName("A short can be written")
	void putShort() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			packetBuffer.putShort(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putShort(base + i, number);
		}
	}

	@Test
	@DisplayName("A volatile short can be written")
	void putShortVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			packetBuffer.putShortVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putShortVolatile(base + i, number);
		}
	}

	@Test
	@DisplayName("A int can be read")
	void getInt() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			when(mmanager.getInt(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getInt(i)).as("Data int").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getInt(base + i);
		}
	}

	@Test
	@DisplayName("A volatile int can be read")
	void getIntVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			when(mmanager.getIntVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getIntVolatile(i)).as("Data int").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getIntVolatile(base + i);
		}
	}

	@Test
	@DisplayName("A int can be written")
	void putInt() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			packetBuffer.putInt(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putInt(base + i, number);
		}
	}

	@Test
	@DisplayName("A volatile int can be written")
	void putIntVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextInt(Integer.MAX_VALUE);
			packetBuffer.putIntVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putIntVolatile(base + i, number);
		}
	}

	@Test
	@DisplayName("A long can be read")
	void getLong() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextLong();
			when(mmanager.getLong(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getLong(i)).as("Data long").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getLong(base + i);
		}
	}

	@Test
	@DisplayName("A volatile long can be read")
	void getLongVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextLong();
			when(mmanager.getLongVolatile(base + i)).thenReturn(number);
			assumeThat(packetBuffer.getLongVolatile(i)).as("Data long").isEqualTo(number);
			verify(mmanager, times(1).description("memory address should be read once")).getLongVolatile(base + i);
		}
	}

	@Test
	@DisplayName("A long can be written")
	void putLong() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextLong();
			packetBuffer.putLong(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putLong(base + i, number);
		}
	}

	@Test
	@DisplayName("A volatile long can be written")
	void putLongVolatile() {
		assume();
		val base = virtual + PacketBuffer.DATA_OFFSET;
		val length = random.nextInt(PacketBuffer.HEADER_BYTES / 2 + 1) + PacketBuffer.HEADER_BYTES / 2;
		reset(mmanager);
		for (var i = 0; i < length; i += 1) {
			val number = random.nextLong();
			packetBuffer.putLongVolatile(i, number);
			verify(mmanager, times(1).description("memory address should be read once")).putLongVolatile(base + i, number);
		}
	}

	@Test
	@DisplayName("A segment can be read")
	void get2() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		reset(mmanager);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		val copy = packetBuffer.get(0, bytes.length);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).get(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
	}

	@Test
	@DisplayName("A segment can be read (buffer parameter)")
	void get3() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		val empty = new byte[0];
		random.nextBytes(bytes);
		val copy = new byte[bytes.length];
		reset(mmanager);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).get(virtual, copy.length, copy, 0);
		packetBuffer.get(0, copy.length, copy);
		packetBuffer.get(0, 0, bytes);
		packetBuffer.get(0, bytes.length, empty);
		packetBuffer.get(0, 0, empty);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).get(virtual, copy.length, copy, 0);
	}

	@Test
	@DisplayName("A volatile segment can be read")
	void getVolatile2() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		random.nextBytes(bytes);
		reset(mmanager);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
		val copy = packetBuffer.getVolatile(0, bytes.length);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(eq(virtual), eq(bytes.length), any(byte[].class), eq(0));
	}

	@Test
	@DisplayName("A volatile segment can be read (buffer parameter)")
	void getVolatile3() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		val copy = new byte[bytes.length];
		val empty = new byte[0];
		random.nextBytes(bytes);
		reset(mmanager);
		doAnswer(invocation -> {
			val args = invocation.getArguments();
			val buff = (byte[]) args[2];
			System.arraycopy(bytes, 0, buff, 0, Math.min(bytes.length, buff.length));
			return null;
		}).when(mmanager).getVolatile(virtual, copy.length, copy, 0);
		packetBuffer.getVolatile(0, copy.length, copy);
		packetBuffer.getVolatile(0, 0, bytes);
		packetBuffer.getVolatile(0, bytes.length, empty);
		packetBuffer.getVolatile(0, 0, empty);
		assertThat(copy).isEqualTo(bytes);
		verify(mmanager, times(1).description("batch copy should be called once")).getVolatile(virtual, copy.length, copy, 0);
	}

	@Test
	@DisplayName("A segment can be written")
	void put() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		val empty = new byte[0];
		random.nextBytes(bytes);
		reset(mmanager);
		packetBuffer.put(0, bytes.length, bytes);
		packetBuffer.put(0, 0, bytes);
		packetBuffer.put(0, bytes.length, empty);
		packetBuffer.put(0, 0, empty);
		verify(mmanager, times(1).description("batch copy should be called once")).put(virtual, bytes.length, bytes, 0);
	}

	@Test
	@DisplayName("A segment can be written")
	void putVolatile() {
		assume();
		val bytes = new byte[PacketBuffer.HEADER_BYTES * 2];
		val empty = new byte[0];
		random.nextBytes(bytes);
		reset(mmanager);
		packetBuffer.putVolatile(0, bytes.length, bytes);
		packetBuffer.putVolatile(0, 0, bytes);
		packetBuffer.putVolatile(0, bytes.length, empty);
		packetBuffer.putVolatile(0, 0, empty);
		verify(mmanager, times(1).description("batch copy should be called once")).putVolatile(virtual, bytes.length, bytes, 0);
	}

	@Test
	@DisplayName("Packets can be compared")
	void compareTo() {
		assumeThat(packetBuffer).isNotNull();
		assertThat(packetBuffer.compareTo(packetBuffer)).isEqualTo(0);
		for (var i = -1; i <= 1; i += 1) {
			val packet = PacketBuffer.builder().manager(mmanager).virtual(virtual + i).build();
			assertThat(packetBuffer.compareTo(packet)).isEqualTo(-i);
			assertThat(packet.compareTo(packetBuffer)).isEqualTo(i);
		}
	}

	/** Assumes that the member variables are not {@code null}. */
	private void assume() {
		assumeThat(random).isNotNull();
		assumeThat(mmanager).isNotNull();
		assumeThat(packetBuffer).isNotNull();
	}

}
