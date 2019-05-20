package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;
import de.tum.in.net.ixy.memory.PacketBuffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jetbrains.annotations.NotNull;

import lombok.val;

/**
 * Checks the class {@link Packetbuffer}.
 * 
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("PacketBuffer")
class PacketBufferTest {

	//////////////////////////////////////////////// ASSERTION MESSAGES ////////////////////////////////////////////////

	private static final String MSG_CORRECT     = "the value should be the same that was written";
	private static final String MSG_VALID       = "the packet buffer should be valid";
	private static final String MSG_VALID_NOT   = "the packet buffer should not be valid";
	private static final String MSG_INVALID     = "the packet buffer should be invalid";
	private static final String MSG_INVALID_NOT = "the packet buffer should not be invalid";

	/** Holds the virtual memory address. */
	private long virtual;

	/** Holds an instance of {@link PacketBuffer} that will be used to test. */
	@NotNull
	private PacketBuffer packetBuffer;

	/**
	 * Allocates a region of memory, assigns it to {@link #virtual} and creates an instance of {@link PacketBuffer}.
	 * 
	 * @see MemoryUtils#allocate(long, boolean)
	 */
	@BeforeEach
	void allocate() {
		virtual = MemoryUtils.allocate(512, false);
		packetBuffer = new PacketBuffer(virtual);
	}

	/**
	 * Checks that an empty packet buffer can be created.
	 * 
	 * @see PacketBuffer#empty()
	 */
	@Test
	@DisplayName("Empty packet buffers can be created")
	void empty() {
		assertNotEquals(packetBuffer.getBaseAddress(), PacketBuffer.empty().getBaseAddress(), "should not be empty");
	}

	/**
	 * Checks that the base address is correct.
	 * 
	 * @see PacketBuffer#getBaseAddress()
	 */
	@Test
	@DisplayName("The packet buffer base address is correct")
	void getBaseAddress() {
		assertEquals(virtual, packetBuffer.getBaseAddress(), MSG_CORRECT);
	}

	/**
	 * Checks that the packet buffer is valid.
	 * 
	 * @see PacketBuffer#isValid()
	 */
	@Test
	@DisplayName("The packet buffer is valid")
	void isValid() {
		assertTrue(packetBuffer.isValid(),          MSG_VALID);
		assertFalse(PacketBuffer.empty().isValid(), MSG_VALID_NOT);
	}

	/**
	 * Checks that the packet buffer is not invalid.
	 * 
	 * @see PacketBuffer#isInvalid()
	 */
	@Test
	@DisplayName("The packet buffer is not invalid")
	void isInvalid() {
		assertFalse(packetBuffer.isInvalid(),        MSG_INVALID_NOT);
		assertTrue(PacketBuffer.empty().isInvalid(), MSG_INVALID);
	}

	/**
	 * Checks that the physical address can be manipulated.
	 * 
	 * @see PacketBuffer#getPhysicalAddress()
	 * @see PacketBuffer#setPhysicalAddress(long)
	 */
	@Test
	@DisplayName("The physical address can be written and read")
	void getsetPhysicalAddress() {
		val number = (long) (Math.random() * Integer.MAX_VALUE);
		packetBuffer.setPhysicalAddress(number);
		assertEquals(number, packetBuffer.getPhysicalAddress(), MSG_CORRECT);
	}

	/**
	 * Checks that the memory pool address can be manipulated.
	 * 
	 * @see PacketBuffer#getMemoryPoolAddress()
	 * @see PacketBuffer#setMemoryPoolAddress(long)
	 */
	@Test
	@DisplayName("The memory pool address can be written and read")
	void getsetMemoryPoolAddress() {
		val number = (long) (Math.random() * Integer.MAX_VALUE);
		packetBuffer.setMemoryPoolAddress(number);
		assertEquals(number, packetBuffer.getMemoryPoolAddress(), MSG_CORRECT);
	}

	/**
	 * Checks that the memory pool id can be manipulated.
	 * 
	 * @see PacketBuffer#getMemoryPoolId()
	 * @see PacketBuffer#setMemoryPoolId(int)
	 */
	@Test
	@DisplayName("The memory pool id can be written and read")
	void getsetMemoryPoolId() {
		val number = (int) (Math.random() * Integer.MAX_VALUE);
		packetBuffer.setMemoryPoolId(number);
		assertEquals(number, packetBuffer.getMemoryPoolId(), MSG_CORRECT);
	}

	/**
	 * Checks that the size can be manipulated.
	 * 
	 * @see PacketBuffer#getSize()
	 * @see PacketBuffer#setSize(int)
	 */
	@Test
	@DisplayName("The size can be written and read")
	void getsetSize() {
		val number = (int) (Math.random() * Integer.MAX_VALUE);
		packetBuffer.setSize(number);
		assertEquals(number, packetBuffer.getSize(), MSG_CORRECT);
	}

	/**
	 * Checks that a byte can be read and written.
	 * 
	 * @see PacketBuffer#getByte(long)
	 * @see PacketBuffer#putByte(long, byte)
	 */
	@Test
	@DisplayName("A byte can be written and read")
	void getsetByte() {
		val number = (byte) (Math.random() * Byte.MAX_VALUE);
		packetBuffer.putByte(0, number);
		assertEquals(number, packetBuffer.getByte(0), MSG_CORRECT);
	}

	/**
	 * Checks that a short can be read and written.
	 * 
	 * @see PacketBuffer#getShort(long)
	 * @see PacketBuffer#putShort(long, short)
	 */
	@Test
	@DisplayName("A short can be written and read")
	void getsetShort() {
		val number = (short) (Math.random() * Short.MAX_VALUE);
		packetBuffer.putShort(0, number);
		assertEquals(number, packetBuffer.getShort(0), MSG_CORRECT);
	}

	/**
	 * Checks that a int can be read and written.
	 * 
	 * @see PacketBuffer#getInt(long)
	 * @see PacketBuffer#putInt(long, int)
	 */
	@Test
	@DisplayName("A int can be written and read")
	void getsetInt() {
		val number = (int) (Math.random() * Integer.MAX_VALUE);
		packetBuffer.putInt(0, number);
		assertEquals(number, packetBuffer.getInt(0), MSG_CORRECT);
	}

	/**
	 * Checks that a long can be read and written.
	 * 
	 * @see PacketBuffer#getLong(long)
	 * @see PacketBuffer#putLong(long, long)
	 */
	@Test
	@DisplayName("A long can be written and read")
	void getsetLong() {
		val number = (long) (Math.random() * Long.MAX_VALUE);
		packetBuffer.putLong(0, number);
		assertEquals(number, packetBuffer.getLong(0), MSG_CORRECT);
	}

	/**
	 * Deallocates the memory region that was allocated during {@link #allocate()}.
	 * 
	 * @see MemoryUtils#deallocate(long, long)
	 */
	@AfterEach
	void deallocate() {
		MemoryUtils.deallocate(virtual, 512);
	}

}
