package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.PacketBuffer;

import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static de.tum.in.net.ixy.memory.test.Messages.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import lombok.val;
import sun.misc.Unsafe;

/**
 * Tests the class {@link PacketBuffer}.
 *
 * @author Esaú García Sánchez-Torija
 * @see PacketBuffer
 */
@DisplayName("PacketBuffer")
@Execution(ExecutionMode.CONCURRENT)
class PacketBufferTest {

	/** A cached instance of the {@link Unsafe} object. */
	private static Unsafe unsafe;

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new Random();

	// Load the Unsafe object
	static {
		try {
			val theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			unsafe = (Unsafe) theUnsafe.get(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/** Holds the virtual memory address. */
	private long virtual;

	/** Holds an instance of {@link PacketBuffer} that will be used to test. */
	private PacketBuffer packetBuffer;

	@BeforeEach
	void allocate() {
		virtual = unsafe.allocateMemory(512);
		packetBuffer = new PacketBuffer(virtual);
	}

	@Test
	@DisplayName("Empty packet buffers can be created")
	void empty() {
		assertEquals(0, PacketBuffer.empty().getBaseAddress(), "should not be empty");
	}

	@Test
	@DisplayName("The packet buffer base address is correct")
	void getBaseAddress() {
		assumeTrue(Objects.nonNull(packetBuffer));
		assertEquals(virtual, packetBuffer.getBaseAddress(), MSG_CORRECT);
	}

	@Test
	@DisplayName("The packet buffer is valid")
	void isValid() {
		assumeTrue(Objects.nonNull(packetBuffer));
		assertTrue(packetBuffer.isValid(), MSG_VALID);
		assertFalse(PacketBuffer.empty().isValid(), MSG_VALID_NOT);
	}

	@Test
	@DisplayName("The packet buffer is not invalid")
	void isInvalid() {
		assumeTrue(Objects.nonNull(packetBuffer));
		assertFalse(packetBuffer.isInvalid(), MSG_INVALID_NOT);
		assertTrue(PacketBuffer.empty().isInvalid(), MSG_INVALID);
	}

	@Test
	@DisplayName("The physical address can be written and read")
	void getsetPhysicalAddress() {
		val number = (long) (Math.random() * Integer.MAX_VALUE);
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.setPhysicalAddress(number);
		assertEquals(number, packetBuffer.getPhysicalAddress(), MSG_CORRECT);
	}

	@Test
	@DisplayName("The memory pool address can be written and read")
	void getsetMemoryPoolAddress() {
		val number = (long) (Math.random() * Integer.MAX_VALUE);
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.setMemoryPoolAddress(number);
		assertEquals(number, packetBuffer.getMemoryPoolAddress(), MSG_CORRECT);
	}

	@Test
	@DisplayName("The memory pool id can be written and read")
	void getsetMemoryPoolId() {
		val number = random.nextInt();
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.setMemoryPoolId(number);
		assertEquals(number, packetBuffer.getMemoryPoolId(), MSG_CORRECT);
	}

	@Test
	@DisplayName("The size can be written and read")
	void getsetSize() {
		val number = random.nextInt();
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.setSize(number);
		assertEquals(number, packetBuffer.getSize(), MSG_CORRECT);
	}

	@Test
	@DisplayName("A byte can be written and read")
	void getsetByte() {
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.putByte(0, number);
		assertEquals(number, packetBuffer.getByte(0), MSG_CORRECT);
	}

	@Test
	@DisplayName("A short can be written and read")
	void getsetShort() {
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.putShort(0, number);
		assertEquals(number, packetBuffer.getShort(0), MSG_CORRECT);
	}

	@Test
	@DisplayName("A int can be written and read")
	void getsetInt() {
		val number = random.nextInt();
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.putInt(0, number);
		assertEquals(number, packetBuffer.getInt(0), MSG_CORRECT);
	}

	@Test
	@DisplayName("A long can be written and read")
	void getsetLong() {
		val number = random.nextLong();
		assumeTrue(Objects.nonNull(packetBuffer));
		packetBuffer.putLong(0, number);
		assertEquals(number, packetBuffer.getLong(0), MSG_CORRECT);
	}

	@AfterEach
	void free() {
		unsafe.freeMemory(virtual);
	}

}
