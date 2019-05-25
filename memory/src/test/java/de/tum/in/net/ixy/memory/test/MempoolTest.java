package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.Memory;
import de.tum.in.net.ixy.memory.PacketBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static de.tum.in.net.ixy.memory.test.Messages.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import lombok.val;
import sun.misc.Unsafe;

/**
 * Tests the class {@link Mempool}.
 *
 * @author Esaú García Sánchez-Torija
 * @see Mempool
 */
@DisplayName("Mempool")
@Execution(ExecutionMode.CONCURRENT)
class MempoolTest {

	/** A cached instance of the {@link Unsafe} object. */
	private static Unsafe unsafe;

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

	/** Holds the packet buffer size of the memory pool. */
	private int size;

	/** Holds the number of entries of the memory pool. */
	private int numEntries;

	/** Holds an instance of {@link Mempool} that will be used to test. */
	private Mempool mempool;

	@BeforeEach
	void setUp() {
		size = (int) Memory.getHugepagesize();
		val perfectSizes = IntStream.rangeClosed(64 + 8, size)
				.parallel()
				.filter(x -> Integer.remainderUnsigned(x, size) == 0)
				.map(x -> Integer.divideUnsigned(x, size))
				.toArray();
		virtual = unsafe.allocateMemory(size);
		numEntries = perfectSizes[(int) (Math.random() * perfectSizes.length)];
		size /= numEntries;
		mempool = new Mempool(virtual, size, numEntries);
		mempool.allocate();
	}

	@Test
	@DisplayName("Different pools have different ids")
	void getId() {
		val pool = new Mempool(0, 0, 0);
		val id = pool.getId();
		assertNotEquals(mempool.getId(), id);
		assertTrue(mempool.compareTo(pool) < 0);
		assertTrue(pool.compareTo(mempool) > 0);
		assertTrue(Mempool.addMempool(pool));
		assertFalse(Mempool.addMempool(null));
		assertNotEquals(pool.getId(), id);
		assertThrows(NullPointerException.class, () -> mempool.compareTo(null));
	}

	@Test
	@DisplayName("Packet buffers can be obtained from the memory pool")
	void pop() {
		var size = mempool.size();
		while (!mempool.isEmpty()) {
			val buffer = mempool.pop();
			val tmpSize = mempool.size();
			assertNotNull(buffer);
			assertEquals(size - 1, tmpSize, MSG_SIZE_LESS);
			size = tmpSize;
		}
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Packet buffers are correctly initialized")
	void packetBuffer_linux() {
		val virtualAddresses = new HashSet<>();
		val physicalAddresses = new HashSet<>();
		while (!mempool.isEmpty()) {
			val packetBuffer = mempool.pop();
			assumeTrue(Objects.nonNull(packetBuffer));
			val vaddress = packetBuffer.getBaseAddress();
			val paddress = Memory.virt2phys(vaddress);
			assertNotEquals(0, vaddress, MSG_NOT_NULL);
			assertNotEquals(0, paddress, MSG_NOT_NULL);
			assertEquals(0, packetBuffer.getSize(), MSG_SIZE_NOT_0);
			assertFalse(virtualAddresses.contains(vaddress), MSG_UNIQUE);
			assertFalse(physicalAddresses.contains(paddress), MSG_UNIQUE);
			virtualAddresses.add(vaddress);
			physicalAddresses.add(paddress);
		}
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	@DisplayName("Packet buffers are correctly initialized")
	void packetBuffer_win32() {
		val virtualAddresses = new HashSet<>();
		val physicalAddresses = new HashSet<>();
		while (!mempool.isEmpty()) {
			val packetBuffer = mempool.pop();
			assumeTrue(Objects.nonNull(packetBuffer));
			val vaddress = packetBuffer.getBaseAddress();
			val paddress = Memory.virt2phys(vaddress);
			assertNotEquals(0, vaddress, MSG_NOT_NULL);
			assertEquals(0, packetBuffer.getSize(), MSG_SIZE_NOT_0);
			assertFalse(virtualAddresses.contains(vaddress), MSG_UNIQUE);
			assertFalse(physicalAddresses.contains(paddress), MSG_UNIQUE);
			virtualAddresses.add(vaddress);
			physicalAddresses.add(paddress);
		}
	}

	@Test
	@DisplayName("Obtaining more packets than available results in an empty packet")
	void popEmpty() {
		while (!mempool.isEmpty()) {
			mempool.pop();
		}
		assertEquals(0, mempool.size(), MSG_SIZE_0);
		val packet = mempool.pop();
		assumeTrue(Objects.nonNull(packet));
		assertEquals(0, packet.getBaseAddress(), MSG_NULL);
	}

	@Test
	@DisplayName("The packet buffers can be freed")
	void add() {
		val buffers = new HashSet<PacketBuffer>();
		while (!mempool.isEmpty()) {
			buffers.add(mempool.pop());
		}
		var size = mempool.size();
		for (val packetBuffer : buffers) {
			assertTrue(mempool.add(packetBuffer), MSG_ADDED);
			val tmpSize = mempool.size();
			assertEquals(size + 1, tmpSize, MSG_SIZE_MORE);
			size = tmpSize;
		}
		assertFalse(mempool.add(PacketBuffer.empty()), MSG_ADDED_NOT);
		assertFalse(mempool.add(null), MSG_ADDED_NOT);
		assertEquals(size, mempool.size());
	}

	@Test
	@DisplayName("The packet buffers can be freed using a collection")
	void addAll() {
		val buffers = new ArrayList<PacketBuffer>();
		while (!mempool.isEmpty()) {
			buffers.add(mempool.pop());
		}
		buffers.add(0, null);
		buffers.add(null);
		assertFalse(mempool.addAll(null),      MSG_ADDED_NOT);
		assertFalse(mempool.addAll(List.of()), MSG_ADDED_NOT);
		assertFalse(mempool.addAll(Collections.singletonList(null)),    MSG_ADDED_NOT);
		assertTrue(mempool.addAll(buffers),    MSG_ADDED);
		assertFalse(mempool.addAll(null),      MSG_ADDED_NOT);
		assertFalse(mempool.addAll(List.of()), MSG_ADDED_NOT);
		assertFalse(mempool.addAll(Collections.singletonList(null)),    MSG_ADDED_NOT);
	}

	@AfterEach
	void deallocate() {
		unsafe.freeMemory(virtual);
	}

	/////////////////////////////////////////////// UNSUPPORTED METHODS ////////////////////////////////////////////////

	@Test
	@DisplayName("Unsupported method iterator() throws")
	void iterator() {
		assertThrows(UnsupportedOperationException.class, mempool::iterator);
	}

	@Test
	@DisplayName("Unsupported method forEach(Consumer) throws")
	void forEach() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.forEach(null));
	}

	@Test
	@DisplayName("Unsupported method spliterator() throws")
	void spliterator() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.spliterator());
	}

	@Test
	@DisplayName("Unsupported method contains(Object) throws")
	void contains() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.contains(null));
	}

	@Test
	@DisplayName("Unsupported method containsAll(Collection) throws")
	void containsAll() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.containsAll(null));
	}

	@Test
	@DisplayName("Unsupported method toArray() throws")
	void toArray() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray());
	}

	@Test
	@DisplayName("Unsupported method toArray(PacketBuffer[]) throws")
	void toArrayPacketBuffer() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray((PacketBuffer[]) null));
	}

	@Test
	@DisplayName("Unsupported method toArray(IntFunction) throws")
	void toArrayIntFunction() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.toArray((IntFunction<PacketBuffer[]>) null));
	}

	@Test
	@DisplayName("Unsupported method remove(Object) throws")
	void remove() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.remove(null));
	}

	@Test
	@DisplayName("Unsupported method removeIf(Predicate) throws")
	void removeIf() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.removeIf(null));
	}

	@Test
	@DisplayName("Unsupported method removeAll(Collection) throws")
	void removeAll() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.removeAll(null));
	}

	@Test
	@DisplayName("Unsupported method retainAll(Object) throws")
	void retainAll() {
		assertThrows(UnsupportedOperationException.class, () -> mempool.retainAll(null));
	}

	@Test
	@DisplayName("Unsupported method clear() throws")
	void clear() {
		assertThrows(UnsupportedOperationException.class, mempool::clear);
	}

	@Test
	@DisplayName("Unsupported method stream() throws")
	void stream() {
		assertThrows(UnsupportedOperationException.class, mempool::stream);
	}

	@Test
	@DisplayName("Unsupported method parallelStream() throws")
	void parallelStream() {
		assertThrows(UnsupportedOperationException.class, mempool::parallelStream);
	}

	@Test
	@DisplayName("The size is the number of entries")
	void size() {
		assertEquals(numEntries, mempool.size());
	}

}
