package de.tum.in.net.ixy.memory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import lombok.val;

import org.jetbrains.annotations.Contract;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static de.tum.in.net.ixy.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI_FULL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the class {@link PacketBufferWrapper}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("PacketBufferWrapper")
@Execution(ExecutionMode.SAME_THREAD)
final class PacketBufferWrapperTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** The sizes of the allocated regions. */
	private long[] sizes;

	/** The allocated regions. */
	private long[] virtuals;

	/** The memory manager. */
	@SuppressWarnings("NestedConditionalExpression")
	private static final MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	// Generates a random virtual address and creates the packet buffer instance using the mocked memory manager
	@BeforeEach
	void setUp() {
		virtuals = new long[4];
		sizes = new long[4];
		var i = 0;
		for (val huge : Arrays.asList(false, true)) {
			for (val lock : Arrays.asList(false, true)) {
				sizes[i] = PacketBufferWrapperConstants.HEADER_BYTES + createSize(0xFF);
				virtuals[i] = mmanager.allocate(sizes[i], huge, lock);
				i += 1;
			}
		}
	}

	// Releases the memory allocated by setUp()
	@AfterEach
	void tearDown() {
		var i = 0;
		for (val huge : Arrays.asList(false, true)) {
			for (val lock : Arrays.asList(false, true)) {
				val virtual = virtuals[i];
				if (virtual != 0) mmanager.free(virtual, sizes[i], huge, lock);
				i += 1;
			}
		}
	}

	@Test
	@DisplayName("Wrong arguments produce exceptions")
	void exceptions() {
		assumeTrue(!OPTIMIZED);
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val packet = new PacketBufferWrapper(virtual);
			int[] offsets = {-1, 0};
			int[] bytes = {-1, 0};
			for (val offset : offsets) {
				for (val length : bytes) {
					Class<? extends RuntimeException> exceptionClass2 = (offset < 0 || length < 0)
					? IllegalArgumentException.class
					: NullPointerException.class;
					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packet.get(offset, length, null));
					assertThatExceptionOfType(exceptionClass2).isThrownBy(() -> packet.put(offset, length, null));
				}
			}
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> packet.compareTo(null));
		}
	}

	@Test
	@DisplayName("The virtual address is correct")
	void getVirtualAddress() {
		for (val virtual : virtuals) {
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getVirtualAddress()).isEqualTo(virtual);
		}
	}

	@Test
	@DisplayName("The physical address can be read")
	void getPhysicalAddress() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.PAP_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val physical = random.nextLong();
			mmanager.putLongVolatile(virtual + offset, physical);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getPhysicalAddress()).as("Physical address").isEqualTo(physical);
		}
	}

	@Test
	@DisplayName("The physical address can be written")
	void setPhysicalAddress() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.PAP_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val packet = new PacketBufferWrapper(virtual);
			val physicalAddress = random.nextLong();
			packet.setPhysicalAddress(physicalAddress);
			assertThat(mmanager.getLongVolatile(virtual + offset)).as("Physical address").isEqualTo(physicalAddress);
		}
	}

	@Test
	@DisplayName("The memory pool pointer can be read")
	void getMemoryPoolPointer() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.MPP_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val pool = random.nextLong();
			mmanager.putLongVolatile(virtual + offset, pool);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getMemoryPoolPointer()).as("Mempool pointer").isEqualTo(pool);
		}
	}

	@Test
	@DisplayName("The memory pool pointer can be written")
	void setMemoryPoolPointer() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.MPP_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val packet = new PacketBufferWrapper(virtual);
			val memoryPoolPointer = random.nextLong();
			packet.setMemoryPoolPointer(memoryPoolPointer);
			assertThat(mmanager.getLongVolatile(virtual + offset)).as("Mempool pointer").isEqualTo(memoryPoolPointer);
		}
	}

	@Test
	@DisplayName("The size can be read")
	void getSize() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.PKT_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val size = random.nextInt();
			mmanager.putIntVolatile(virtual + offset, size);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getSize()).as("Packet size").isEqualTo(size);
		}
	}

	@Test
	@DisplayName("The size can be written")
	void setSize() {
		assume();
		val offset = PacketBufferWrapperConstants.HEADER_OFFSET + PacketBufferWrapperConstants.PKT_OFFSET;
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val packet = new PacketBufferWrapper(virtual);
			val size = random.nextInt();
			packet.setSize(size);
			assertThat(mmanager.getIntVolatile(virtual + offset)).as("Packet size").isEqualTo(size);
		}
	}

	@Test
	@DisplayName("A byte can be read")
	void getByte() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			mmanager.putByteVolatile(base, number);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getByte(0)).as("Payload byte").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A byte can be written")
	void putByte() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			val packet = new PacketBufferWrapper(virtual);
			packet.putByte(0, number);
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			assertThat(mmanager.getByteVolatile(base)).as("Payload byte").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A short can be read")
	void getShort() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			mmanager.putShortVolatile(base, number);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getShort(0)).as("Payload short").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A short can be written")
	void putShort() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			val packet = new PacketBufferWrapper(virtual);
			packet.putShort(0, number);
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			assertThat(mmanager.getShortVolatile(base)).as("Payload short").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("An int can be read")
	void getInt() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			val number = random.nextInt();
			mmanager.putIntVolatile(base, number);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getInt(0)).as("Payload int").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("An int can be written")
	void putInt() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val number = random.nextInt();
			val packet = new PacketBufferWrapper(virtual);
			packet.putInt(0, number);
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			assertThat(mmanager.getIntVolatile(base)).as("Payload int").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A long can be read")
	void getLong() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			val number = random.nextLong();
			mmanager.putLongVolatile(base, number);
			val packet = new PacketBufferWrapper(virtual);
			assertThat(packet.getLong(0)).as("Payload long").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A long can be written")
	void putLong() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val number = random.nextLong();
			val packet = new PacketBufferWrapper(virtual);
			packet.putLong(0, number);
			val base = virtual + PacketBufferWrapperConstants.PAYLOAD_OFFSET;
			assertThat(mmanager.getLongVolatile(base)).as("Payload long").isEqualTo(number);
		}
	}

	@Test
	@DisplayName("A chunk can be read")
	void get() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val bytes = new byte[Long.BYTES];
			random.nextBytes(bytes);
			val packet = new PacketBufferWrapper(virtual);
			packet.get(0, 0, bytes);
			val copy = new byte[bytes.length];
			packet.get(0, copy.length, copy);
			packet.get(0, bytes.length, bytes);
			assertThat(copy).isEqualTo(bytes);
		}
	}

	@Test
	@DisplayName("A chunk can be written")
	void put() {
		assume();
		for (val virtual : virtuals) {
			assertThat(virtual).isNotZero();
			val bytes = new byte[Long.BYTES];
			random.nextBytes(bytes);
			val packet = new PacketBufferWrapper(virtual);
			packet.put(0, bytes.length, bytes);
			val copy = new byte[bytes.length];
			packet.get(0, copy.length, copy);
			assertThat(copy).isEqualTo(bytes);
		}
	}

	@Test
	@DisplayName("Packets can be compared")
	void compareTo() {
		assume();
		for (var i = 0; i < virtuals.length; i++) {
			val virtual = virtuals[i];
			val size = sizes[i];
			for (var j = 0; j < size - (Long.BYTES - 1); j++) {
				val packet = new PacketBufferWrapper(virtual + j);
				assertThat(packet.compareTo(packet)).isEqualTo(0);
				val next = new PacketBufferWrapper(virtual + j + 1);
				assertThat(packet.compareTo(next)).isEqualTo(-1);
				assertThat(next.compareTo(packet)).isEqualTo(1);
			}
		}
	}

	/** Assumes that the member variables are not {@code null}. */
	private void assume() {
		assumeTrue(random != null);
		assumeTrue(mmanager != null);
	}

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
			while (bytes < Long.BYTES) {
				bytes = random.nextLong();
			}
			bytes &= mask;
		}
		return bytes;
	}

}
