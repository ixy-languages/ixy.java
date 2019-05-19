package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryPool;
import de.tum.in.net.ixy.memory.MemoryUtils;
import de.tum.in.net.ixy.memory.PacketBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jetbrains.annotations.NotNull;

import lombok.val;

/** Checks the class {@link MemoryPool}. */
@DisplayName("MemoryPool")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryPoolTest {

	/** Holds the virtual memory address. */
	private static long virtual;

	/** Holds the packet buffer size of the memory pool. */
	private static int size;

	/** Holds the number of entries of the memory pool. */
	private static int numEntries;

	/** Holds an instance of {@link MemoryPool} that will be used to test. */
	@NotNull
	private static MemoryPool memoryPool;

	//////////////////////////////////////////////// ASSERTION MESSAGES ////////////////////////////////////////////////

	private static final String MSG_NOT_NULL = "should not be null";
	private static final String MSG_NULL     = "should be null";
	private static final String MSG_NOT_0    = "should not be 0";
	private static final String MSG_UNIQUE   = "should be unique";

	/** Allocates a region of memory, assigns it to {@link #virtual} and creates an instance of {@link PacketBuffer}. */
	@BeforeAll
	static void allocate() {
		size = (int) MemoryUtils.getHugepagesize();
		val perfectSizes = IntStream.rangeClosed(64 + 8, size)
				.parallel()
				.filter(x -> Integer.remainderUnsigned(x, size) == 0)
				.map(x -> Integer.divideUnsigned(x, size))
				.toArray();
		virtual = MemoryUtils.allocate(size, false);
		numEntries = perfectSizes[(int) (Math.random() * perfectSizes.length)];
		size /= numEntries;
		memoryPool = new MemoryPool(virtual, size, numEntries);
		memoryPool.allocatePacketBuffers();
	}

	/** Holds temporarily the packet buffers. */
	private static List<PacketBuffer> buffers;

	/**
	 * Checks that the packet buffers can be obtained from the memory pool.
	 * 
	 * @see MemoryPool#getFreePacketBuffer()
	 */
	@Test
	@Order(0)
	@DisplayName("Packet buffers can be obtained from the memory pool")
	void getFreePacketBuffer() {
		buffers = new ArrayList<>(numEntries);
		var size = memoryPool.size();
		while (size > 0) {
			val buffer = memoryPool.getFreePacketBuffer();
			val tmpSize = memoryPool.size();
			buffers.add(buffer);
			assertEquals(size - 1, tmpSize, "the size should be 1 unit less");
			size = tmpSize;
		}
	}

	/** Holds the virtual addresses of the packet buffers. */
	private static Set<Long> virtualAddresses = new HashSet<>();

	/** Holds the physical addresses of the packet buffers. */
	private static Set<Long> physicalAddresses = new HashSet<>();

	/** Checks that the packet buffers have the correct properties. */
	@Test
	@Order(1)
	@DisplayName("The packet buffers are correctly initialized")
	void packetBufferTest() {
		for (val packetBuffer : buffers) {
			assertNotNull(packetBuffer, MSG_NOT_NULL);
			val vaddress = packetBuffer.getBaseAddress();
			val paddress = MemoryUtils.virt2phys(vaddress);
			assertNotEquals(0, vaddress,               MSG_NOT_NULL);
			assertNotEquals(0, paddress,               MSG_NOT_NULL);
			assertEquals(0,    packetBuffer.getSize(), MSG_NOT_0);
			assertFalse(virtualAddresses.contains(vaddress), MSG_UNIQUE);
			assertFalse(virtualAddresses.contains(paddress), MSG_UNIQUE);
			virtualAddresses.add(vaddress);
			physicalAddresses.add(paddress);
		}
	}

	/** Checks that the memory pool cannot provide more packet buffers than the pre-allocated amount. */
	@Test
	@Order(1)
	@DisplayName("Obtaining more packets than available results in an empty packet")
	void getFreePacketBuffer2() {
		val packet = memoryPool.getFreePacketBuffer();
		assertNotNull(packet, MSG_NOT_NULL);
		assertEquals(0, packet.getBaseAddress(), MSG_NULL);
	}

	/** Checks that the packet buffers can be freed. */
	@Test
	@Order(2)
	@DisplayName("The packet buffers can be freed")
	void freePacketBuffer() {
		var size = memoryPool.size();
		for (val packetBuffer : buffers) {
			memoryPool.freePacketBuffer(packetBuffer);
			val tmpSize = memoryPool.size();
			assertEquals(size + 1, tmpSize, "the size should be 1 unit more");
			size = tmpSize;
		}
		memoryPool.freePacketBuffer(PacketBuffer.empty());
		assertEquals(size, memoryPool.size(), "the size should be the same");
		assertThrows(NullPointerException.class, () -> memoryPool.freePacketBuffer(null));
	}

	/** Checks that pools have different ids. */
	@Test
	@Order(2)
	@DisplayName("Different pools have different ids")
	void memoryPoolId() {
		val pool = new MemoryPool(0, 0, 0);
		val id = pool.getId();
		assertNotEquals(memoryPool.getId(), id,    "the id should not be the same");
		assertTrue(memoryPool.compareTo(pool) < 0, "the id should be smaller");
		assertTrue(pool.compareTo(memoryPool) > 0, "the id should be bigger");
		MemoryPool.addMemoryPool(pool);
		assertNotEquals(pool.getId(), id, "the id should have changed");
		assertThrows(NullPointerException.class, () -> memoryPool.compareTo(null));
		assertThrows(NullPointerException.class, () -> MemoryPool.addMemoryPool(null));
	}

	/** Deallocates the memory region that was allocated during {@link #allocate()}. */
	@AfterAll
	static void deallocate() {
		MemoryUtils.deallocate(virtual, MemoryUtils.getHugepagesize());
	}

}
