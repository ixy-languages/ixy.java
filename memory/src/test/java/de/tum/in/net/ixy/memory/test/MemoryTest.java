package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.Memory;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import sun.misc.Unsafe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static de.tum.in.net.ixy.memory.test.Messages.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests the class {@link Memory}.
 *
 * @author Esaú García Sánchez-Torija
 * @see Memory
 */
@Slf4j
@DisplayName("Memory")
@Execution(ExecutionMode.CONCURRENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new Random();

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

	@Test
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		try {
			val constructor = Memory.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			val exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
			assertEquals(UnsupportedOperationException.class, exception.getTargetException().getClass());
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Could not test constructor exception", e);
		}
	}

	@Test
	@DisplayName("Memory page size can be computed")
	void pageSize() {
		val cached = Memory.getPageSize();
		val jni    = Memory.c_page_size();
		val unsafe = Memory.u_page_size();
		val smart  = Memory.smartPageSize();
		assertNotEquals(0,   cached,           MSG_NOT_NULL);
		assertEquals(cached, cached & -cached, MSG_POWER_OF_TWO);
		assertEquals(cached, jni,              MSG_SAME_JNI);
		assertEquals(cached, unsafe,           MSG_SAME_UNSAFE);
		assertEquals(cached, smart,            MSG_SAME_SMART);
	}

	@Test
	@DisplayName("Virtual address size can be computed")
	void addressSize() {
		val cached = Memory.getAddressSize();
		val jni    = Memory.c_address_size();
		val unsafe = Memory.u_address_size();
		val smart  = Memory.smartAddressSize();
		assertNotEquals(0,   cached,           MSG_NOT_NULL);
		assertEquals(cached, cached & -cached, MSG_POWER_OF_TWO);
		assertEquals(cached, jni,              MSG_SAME_JNI);
		assertEquals(cached, unsafe,           MSG_SAME_UNSAFE);
		assertEquals(cached, smart,            MSG_SAME_SMART);
	}

	@Test
	@DisplayName("Huge memory page size can be computed")
	void hugepageSize() {
		val cached = Memory.getHugepagesize();
		val jni    = Memory.c_hugepage_size();
		val smart  = Memory.smartHugepageSize();
		assertNotEquals(0, cached,             MSG_NOT_NULL);
		assertEquals(cached, cached & -cached, MSG_POWER_OF_TWO);
		assertEquals(cached, jni,              MSG_SAME_JNI);
		assertEquals(cached, smart,            MSG_SAME_SMART);
		assertThrows(UnsupportedOperationException.class, Memory::u_hugepage_size, MSG_UNSAFE_THROW);
	}

	@ParameterizedTest(name = "Memory can be allocated using huge memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
	@EnabledIfRoot
	void allocate(final Long size, final Boolean contiguous) {
		// The Unsafe-based implementation should not work
		assertThrows(UnsupportedOperationException.class, () -> Memory.u_allocate(size, contiguous), MSG_UNSAFE_THROW);

		// Load the Unsafe object
		Unsafe _unsafe = null;
		try {
			val singletoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singletoneInstanceField.setAccessible(true);
			_unsafe = (Unsafe) singletoneInstanceField.get(null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
		val unsafe = _unsafe;

		// Allocate memory with the JNI call and with the smart method
		val caddr = Memory.c_allocate(size, contiguous);
		val addr = Memory.smartAllocate(size, contiguous);

		// Allocating more than a memory page can hold results in the allocation being aborted
		if (contiguous && size > Memory.getHugepagesize()) {
			assertEquals(0, caddr, MSG_NULL);
			assertEquals(0, addr,  MSG_NULL);
			return;
		} else {
			assertNotEquals(0, caddr, MSG_NOT_NULL);
			assertNotEquals(0, addr,  MSG_NOT_NULL);
		}

		// Test each memory address for read and write using the Unsafe object for each different arithmetic size and
		// avoid overlapping reads and writes by doing all the operations that use addresses with enough distance in
		// parallel. This is 5x faster than doing a sequential loop for each address in real hardware
		val hpsz  = Memory.getHugepagesize();
		for (val bytes : new int[]{Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES}) {
			for (var i = 0; i < bytes; i += 1) {
				val alignment = i;
				val indexes = LongStream.range(0, hpsz - bytes - 1)
						.parallel()
						.filter(index -> index % bytes == alignment);
				val msb = 1 << (bytes * 8 - 2);
				val max = (msb - 1) | msb;
				val rand = random.nextInt(max - 1);
				switch (bytes) {
					case Byte.BYTES:
						indexes.forEach(index -> {
							testWriteByte(unsafe, caddr + index, (byte) rand);
							testWriteByte(unsafe, addr + index, (byte) rand);
						});
						break;
					case Short.BYTES:
						indexes.forEach(index -> {
							testWriteShort(unsafe, caddr + index, (short) rand);
							testWriteShort(unsafe, addr + index, (short) rand);
						});
						break;
					case Integer.BYTES:
						indexes.forEach(index -> {
							testWriteInt(unsafe, caddr + index, (int) rand);
							testWriteInt(unsafe, addr + index, (int) rand);
						});
						break;
					case Long.BYTES:
						indexes.forEach(index -> {
							testWriteLong(unsafe, caddr + index, rand);
							testWriteLong(unsafe, addr + index, rand);
						});
						break;
					default:
						fail("the number of bytes makes no sense");
				}
			}
		}

		// Deallocate memory with the JNI call and with the smart method
		assertTrue(Memory.c_deallocate(caddr, size));
		assertTrue(Memory.smartDeallocate(addr, size));
	}

	@ParameterizedTest(name = "Memory can be deallocated using huge memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
	@EnabledIfRoot
	void deallocate(final Long size, final Boolean contiguous) {
		// Allocate with the JNI and the smart implementations
		val caddr = Memory.c_allocate(size, contiguous);
		val addr = Memory.smartAllocate(size, contiguous);

		// Skip testing with null address
		if (caddr == 0 || addr == 0) return;

		// The Unsafe-based implementation should not work
		assertThrows(UnsupportedOperationException.class, () -> Memory.u_deallocate(caddr, size), MSG_UNSAFE_THROW);
		assertThrows(UnsupportedOperationException.class, () -> Memory.u_deallocate(addr, size), MSG_UNSAFE_THROW);

		// The base address should also succeed
		assertTrue(Memory.c_deallocate(caddr, size));
		assertTrue(Memory.smartDeallocate(addr, size));

		// Wrong address should succeed as long as the base address can be deduced
		assertTrue(Memory.c_deallocate(Memory.c_allocate(size, contiguous) + 1, size));
		assertTrue(Memory.smartDeallocate(Memory.smartAllocate(size, contiguous) + 1, size));
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		val virt = unsafe.allocateMemory(1);
		val phys = Memory.virt2phys(virt);
		assertNotEquals(0, virt, MSG_NOT_NULL);
		assertNotEquals(0, phys, MSG_NOT_NULL);
		unsafe.freeMemory(virt);
	}

	@Test
	@EnabledIfRoot
	@DisplayName("DmaMemory can be allocated")
	void allocateDma() {
		val dma = Memory.allocateDma(1, true);
		assertNotNull(dma, MSG_NOT_NULL);
		val virt = dma.getVirtual();
		val phys = dma.getPhysical();
		assertNotEquals(0, virt,                        MSG_NOT_NULL);
		assertEquals(0,    virt & Memory.getPageSize(), MSG_ALIGNED);
		if (System.getProperty("os.name").toLowerCase().contains("lin")) {
			assertNotEquals(0, phys, MSG_NOT_NULL);
		}
		assertEquals(0, phys & Memory.getPageSize(), MSG_ALIGNED);
		assertNull(Memory.allocateDma(0, false), MSG_NULL);
		Memory.smartDeallocate(virt, 1);
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		val virt = unsafe.allocateMemory(Byte.BYTES);
		assertNotEquals(0, virt, MSG_NULL);
		for (var i = 0; i < 3; i += 1) {
			val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
			switch (i) {
				case 0:
					Memory.c_putByte(virt, number);
					break;
				case 1:
					Memory.u_putByte(virt, number);
					break;
				case 2:
					Memory.putByte(virt, number);
					break;
				default:
					fail("the loop count makes no sense");
			}
			assertEquals(number, Memory.c_getByte(virt), MSG_CORRECT);
			assertEquals(number, Memory.u_getByte(virt), MSG_CORRECT);
			assertEquals(number, Memory.getByte(virt),   MSG_CORRECT);
		}
		unsafe.freeMemory(virt);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		val virt = unsafe.allocateMemory(Short.BYTES);
		assertNotEquals(0, virt, MSG_NOT_NULL);
		for (var i = 0; i < 3; i += 1) {
			val number = (short) random.nextInt(Short.MAX_VALUE + 1);
			switch (i) {
				case 0:
					Memory.c_putShort(virt, number);
					break;
				case 1:
					Memory.u_putShort(virt, number);
					break;
				case 2:
					Memory.putShort(virt, number);
					break;
				default:
					fail("the loop count makes no sense");
			}
			assertEquals(number, Memory.c_getShort(virt), MSG_CORRECT);
			assertEquals(number, Memory.u_getShort(virt), MSG_CORRECT);
			assertEquals(number, Memory.getShort(virt),   MSG_CORRECT);
		}
		unsafe.freeMemory(virt);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		val virt = unsafe.allocateMemory(Integer.BYTES);
		assertNotEquals(0, virt, MSG_NOT_NULL);
		for (var i = 0; i < 3; i += 1) {
			val number = random.nextInt();
			switch (i) {
				case 0:
					Memory.c_putInt(virt, number);
					break;
				case 1:
					Memory.u_putInt(virt, number);
					break;
				case 2:
					Memory.putInt(virt, number);
					break;
				default:
					fail("the loop count makes no sense");
			}
			assertEquals(number, Memory.c_getInt(virt), MSG_CORRECT);
			assertEquals(number, Memory.u_getInt(virt), MSG_CORRECT);
			assertEquals(number, Memory.getInt(virt),   MSG_CORRECT);
		}
		unsafe.freeMemory(virt);
	}

	@Test
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		val virt = unsafe.allocateMemory(Long.BYTES);
		assertNotEquals(0, virt, MSG_NOT_NULL);
		for (var i = 0; i < 3; i += 1) {
			val number = random.nextLong();
			switch (i) {
				case 0:
					Memory.c_putLong(virt, number);
					break;
				case 1:
					Memory.u_putLong(virt, number);
					break;
				case 2:
					Memory.putLong(virt, number);
					break;
				default:
					fail("the loop count makes no sense");
			}
			assertEquals(number, Memory.c_getLong(virt), MSG_CORRECT);
			assertEquals(number, Memory.u_getLong(virt), MSG_CORRECT);
			assertEquals(number, Memory.getLong(virt),   MSG_CORRECT);
		}
		unsafe.freeMemory(virt);
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 *
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteByte(final Unsafe unsafe, final long address, final byte value) {
		assertDoesNotThrow(() -> unsafe.putByte(address, value), String.format(MSG_ADDRESS_FMTR, address, "byte"));
		val v = assertDoesNotThrow(() -> unsafe.getByte(address), String.format(MSG_ADDRESS_FMTW, address, "byte"));
		assertEquals(value, v, MSG_CORRECT);
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 *
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteShort(final Unsafe unsafe, final long address, final short value) {
		assertDoesNotThrow(() -> unsafe.putShort(address, value), String.format(MSG_ADDRESS_FMTR, address, "short"));
		val v = assertDoesNotThrow(() -> unsafe.getShort(address), String.format(MSG_ADDRESS_FMTW, address, "short"));
		assertEquals(value, v, MSG_CORRECT);
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 *
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteInt(final Unsafe unsafe, final long address, final int value) {
		assertDoesNotThrow(() -> unsafe.putInt(address, value), String.format(MSG_ADDRESS_FMTR, address, "int"));
		val v = assertDoesNotThrow(() -> unsafe.getInt(address), String.format(MSG_ADDRESS_FMTW, address, "int"));
		assertEquals(value, v, MSG_CORRECT);
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 *
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteLong(final Unsafe unsafe, final long address, final long value) {
		assertDoesNotThrow(() -> unsafe.putLong(address, value), String.format(MSG_ADDRESS_FMTR, address, "long"));
		val v = assertDoesNotThrow(() -> unsafe.getLong(address), String.format(MSG_ADDRESS_FMTW, address, "long"));
		assertEquals(value, v, MSG_CORRECT);
	}

	/**
	 * Creates a {@link Stream<Arguments>} that contains all the possible combinations used with the {@code smartAllocate}
	 * methods.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocateSource() {
		long[] sizes = {Memory.getHugepagesize() / 2, Memory.getHugepagesize() * 2};
		boolean[] contiguousity = {false, true};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val size : sizes) {
			for (val contiguous : contiguousity) {
				builder.add(Arguments.of(size, contiguous));
			}
		}
		return builder.build();
	}

}
