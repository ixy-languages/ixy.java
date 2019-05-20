package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryPool;
import de.tum.in.net.ixy.memory.MemoryUtils;
import de.tum.in.net.ixy.memory.PacketBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
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
	private static MemoryPool mempool;

	/**
	 * Allocates a region of memory, assigns it to {@link #virtual} and creates an instance of {@link PacketBuffer}.
	 * 
	 * @see MemoryPool#MemoryPool(long, int, int)
	 * @see MemoryPool#allocate()
	 */
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
		mempool = new MemoryPool(virtual, size, numEntries);
		mempool.allocate();
	}

	/**
	 * Checks that not all the methods from the {@link Collection} interface are implemented.
	 * 
	 * @see MemoryPool#iterator()
	 * @see MemoryPool#forEach(java.util.function.Consumer)
	 * @see MemoryPool#spliterator()
	 * @see MemoryPool#contains(Object)
	 * @see MemoryPool#containsAll(java.util.Collection)
	 * @see MemoryPool#toArray()
	 * @see MemoryPool#toArray(Object[])
	 * @see MemoryPool#toArray(IntFunction)
	 * @see MemoryPool#remove(Object)
	 * @see MemoryPool#removeIf(java.util.function.Predicate)
	 * @see MemoryPool#removeAll(java.util.Collection)
	 * @see MemoryPool#clear()
	 * @see MemoryPool#stream()
	 * @see MemoryPool#parallelStream()
	 */
	@Test
	@Order(0)
	@DisplayName("Unsupported methods throw")
	void unsupported() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.iterator());
		assertThrows(UnsupportedOperationException.class, () -> mempool.forEach(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.spliterator());
		assertThrows(UnsupportedOperationException.class, () -> mempool.contains(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.containsAll(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray());
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray((PacketBuffer[]) null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray((IntFunction<PacketBuffer[]>) null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.remove(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.removeIf(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.removeAll(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.retainAll(null));
		assertThrows(UnsupportedOperationException.class, () -> mempool.clear());
		assertThrows(UnsupportedOperationException.class, () -> mempool.stream());
		assertThrows(UnsupportedOperationException.class, () -> mempool.parallelStream());
	}

	/** Holds temporarily the packet buffers. */
	private static ArrayList<PacketBuffer> buffers;

	/**
	 * Checks that the {@link PacketBuffer}s can be obtained from the memory pool.
	 * 
	 * @see MemoryPool#pop()
	 */
	@Test
	@Order(0)
	@DisplayName("Packet buffers can be obtained from the memory pool")
	void pop() {
		buffers = new ArrayList<>(numEntries);
		var size = mempool.size();
		while (size > 0 && !mempool.isEmpty()) {
			val buffer = mempool.pop();
			val tmpSize = mempool.size();
			buffers.add(buffer);
			assertEquals(size - 1, tmpSize);
			size = tmpSize;
		}
	}

	/** Holds the virtual addresses of the {@link PacketBuffer}s. */
	private static Set<Long> virtualAddresses = new HashSet<>();

	/** Holds the physical addresses of the {@link PacketBuffer}s. */
	private static Set<Long> physicalAddresses = new HashSet<>();

	/** Checks that the {@link PacketBuffer}s have the correct properties. */
	@Test
	@Order(1)
	@DisplayName("The packet buffers are correctly initialized")
	void packetBufferTest() {
		for (val packetBuffer : buffers) {
			assertNotNull(packetBuffer);
			val vaddress = packetBuffer.getBaseAddress();
			val paddress = MemoryUtils.virt2phys(vaddress);
			assertNotEquals(0, vaddress);
			assertNotEquals(0, paddress);
			assertEquals(0, packetBuffer.getSize());
			assertFalse(virtualAddresses.contains(vaddress));
			assertFalse(virtualAddresses.contains(paddress));
			virtualAddresses.add(vaddress);
			physicalAddresses.add(paddress);
		}
	}

	/**
	 * Checks that the memory pool cannot provide more {@link PacketBuffer}s than the pre-allocated amount.
	 * 
	 * @see MemoryPool#pop()
	 */
	@Test
	@Order(1)
	@DisplayName("Obtaining more packets than available results in an empty packet")
	void getFreePacketBuffer2() {
		val packet = mempool.pop();
		assertNotNull(packet);
		assertEquals(0, packet.getBaseAddress());
	}

	/**
	 * Checks that the {@link PacketBuffer}s can be freed.
	 * 
	 * @see MemoryPool#add(PacketBuffer)
	 */
	@Test
	@Order(2)
	@DisplayName("The packet buffers can be freed")
	void add() {
		var size = mempool.size();
		for (val packetBuffer : buffers) {
			assertTrue(mempool.add(packetBuffer));
			val tmpSize = mempool.size();
			assertEquals(size + 1, tmpSize);
			size = tmpSize;
		}
		assertFalse(mempool.add(PacketBuffer.empty()));
		assertFalse(mempool.add(null));
		assertEquals(size, mempool.size());
	}

	/**
	 * Checks that different memory pools have different ids.
	 * 
	 * @see MemoryPool#getId()
	 * @see MemoryPool#compareTo(MemoryPool)
	 * @see MemoryPool#addMempool(MemoryPool)
	 */
	@Test
	@Order(2)
	@DisplayName("Different pools have different ids")
	void mempoolId() {
		val pool = new MemoryPool(0, 0, 0);
		val id = pool.getId();
		assertNotEquals(mempool.getId(), id);
		assertTrue(mempool.compareTo(pool) < 0);
		assertTrue(pool.compareTo(mempool) > 0);
		assertTrue(MemoryPool.addMempool(pool));
		assertFalse(MemoryPool.addMempool(null));
		assertNotEquals(pool.getId(), id);
		assertThrows(NullPointerException.class, () -> mempool.compareTo(null));
	}

	/**
	 * Checks that the {@link PacketBuffer}s can be freed using a {@link Collection}.
	 * 
	 * @see MemoryPool#addAll(java.util.Collection)
	 */
	@Test
	@Order(3)
	@DisplayName("The packet buffers can be freed in collection")
	void addAll() {
		buffers.clear();
		while (!mempool.isEmpty()) {
			buffers.add(mempool.pop());
		}
		buffers.add(0, null);
		assertFalse(mempool.addAll(null));
		assertFalse(mempool.addAll(List.of()));
		assertFalse(mempool.addAll(Arrays.asList((PacketBuffer) null)));
		assertTrue(mempool.addAll(buffers));
		assertFalse(mempool.addAll(null));
		assertFalse(mempool.addAll(List.of()));
		assertFalse(mempool.addAll(Arrays.asList((PacketBuffer) null)));
	}

	/**
	 * Deallocates the memory region that was allocated during {@link #allocate()}.
	 * 
	 * @see MemoryUtils#deallocate(long, long)
	 */
	@AfterAll
	static void deallocate() {
		MemoryUtils.deallocate(virtual, MemoryUtils.getHugepagesize());
	}

}
