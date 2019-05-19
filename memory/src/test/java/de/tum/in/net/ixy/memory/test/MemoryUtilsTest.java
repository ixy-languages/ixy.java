package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/** Checks the class {@link MemoryUtils}. */
@Slf4j
@DisplayName("MemoryUtils")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryUtilsTest {

	//////////////////////////////////////////////// ASSERTION MESSAGES ////////////////////////////////////////////////

	private static final String MSG_NULL         = "should be 0";
	private static final String MSG_NOT_NULL     = "should not be 0";
	private static final String MSG_POWER_OF_TWO = "should be a power of 2";
	private static final String MSG_SAME_JNI     = "JNI-based value should be the same";
	private static final String MSG_SAME_UNSAFE  = "Unsafe-based value should be the same";
	private static final String MSG_SAME_SMART   = "Smart-based value should be the same";
	private static final String MSG_UNSAFE_THROW = "the Unsafe-based implementation should throw";
	private static final String MSG_ALIGNED      = "the address should be aligned";
	private static final String MSG_CORRECT      = "the value should be the same that was written";
	private static final String MSG_ADDRESS_FMTR = "the address 0x%x should be %s-readable";
	private static final String MSG_ADDRESS_FMTW = "the address 0x%x should be %s-writable";

	/** Check that the constructor throws an exception. */
	@Test
	@Order(-1)
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		try {
			val constructor = MemoryUtils.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			val exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
			assertEquals(UnsupportedOperationException.class, exception.getTargetException().getClass(),
					"the exception is correct");
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Could not test constructor exception", e);
		}
	}

	/** Checks that the page size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Pagesize can be computed")
	void pagesize() {
		val cached = MemoryUtils.getPagesize();
		val jni = MemoryUtils.c_pagesize();
		val unsafe = MemoryUtils.u_pagesize();
		val smart = MemoryUtils.pagesize();
		assertNotEquals(0,             cached, MSG_NOT_NULL);
		assertEquals(cached & -cached, cached, MSG_POWER_OF_TWO);
		assertEquals(cached,           jni,    MSG_SAME_JNI);
		assertEquals(cached,           unsafe, MSG_SAME_UNSAFE);
		assertEquals(cached,           smart,  MSG_SAME_SMART);
	}

	/** Checks that the address size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Virtual address size can be computed")
	void addrsize() {
		val cached = MemoryUtils.getAddrsize();
		val jni = MemoryUtils.c_addrsize();
		val unsafe = MemoryUtils.u_addrsize();
		val smart = MemoryUtils.addrsize();
		assertNotEquals(0,             cached, MSG_NOT_NULL);
		assertEquals(cached & -cached, cached, MSG_POWER_OF_TWO);
		assertEquals(cached,           jni,    MSG_SAME_JNI);
		assertEquals(cached,           unsafe, MSG_SAME_UNSAFE);
		assertEquals(cached,           smart,  MSG_SAME_SMART);
	}

	/** Checks that the address size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Hugepage size can be computed")
	void hugepage() {
		val cached = MemoryUtils.getHugepagesize();
		val jni = MemoryUtils.c_hugepage();
		val smart = MemoryUtils.hugepage();
		assertNotEquals(0,                                cached,                  MSG_NOT_NULL);
		assertEquals(cached & -cached,                    cached,                  MSG_POWER_OF_TWO);
		assertEquals(cached,                              jni,                     MSG_SAME_JNI);
		assertThrows(UnsupportedOperationException.class, MemoryUtils::u_hugepage, MSG_UNSAFE_THROW);
		assertEquals(cached,                              smart,                   MSG_SAME_SMART);
	}

	/**
	 * Contains all the allcated pages during the test execution of
	 * {@link #allocate(Long, Boolean)}.
	 */
	private static Deque<Long> pages = new ArrayDeque<Long>();

	/** Checks that the address size of the system can be correctly computed. */
	@ParameterizedTest(name = "Memory can be allocated using bigger memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
	@Order(0)
	void allocate(final Long size, final Boolean contiguous) {
		// The Unsafe-based implementation should not work
		assertThrows(UnsupportedOperationException.class, () -> MemoryUtils.u_allocate(size, contiguous), MSG_UNSAFE_THROW);

		// Load the Unsafe object
		@val
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
		val caddr = MemoryUtils.c_allocate(size, contiguous);
		val addr = MemoryUtils.allocate(size, contiguous);

		// Add them to the tracking list
		pages.add(caddr);
		pages.add(addr);
		
		// Allocating more than a memory page can hold results in the allocation being aborted
		if (contiguous && size > MemoryUtils.getHugepagesize()) {
			assertEquals(0, caddr, MSG_NULL);
			assertEquals(0, addr,  MSG_NULL);
			return;
		} else {
			assertNotEquals(0, caddr, MSG_NOT_NULL);
			assertNotEquals(0, addr,  MSG_NOT_NULL);
		}

		// Test each memory address for read and write using the Unsafe object for each different aritmethic size and
		// avoid overlapping reads and writes by parallizing all the operations that use addresses with enough distance
		// This is 5x faster than doing a sequential loop for each address
		val hpsz  = MemoryUtils.getHugepagesize();
		for (val bytes : new int[]{1, 2, 4, 8}) {
			for (var i = 0; i < bytes; i += 1) {
				val alignment = i;
				val indexes = LongStream.range(0, hpsz - bytes - 1)
						.parallel()
						.filter(index -> index % bytes == alignment);
				val msb = 1 << (bytes * 8 - 2);
				val max = (msb - 1) | msb;
				val rand = (long) (Math.random() * max);
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
							testWriteLong(unsafe, caddr + index, (long) rand);
							testWriteLong(unsafe, addr + index, (long) rand);
						});
						break;
				}
			}
		}
	}

	/** Checks that the address size of the system can be correctly computed. */
	@ParameterizedTest(name = "Memory can be deallocated using bigger memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
	@Order(1)
	void deallocate(final Long size, final Boolean contiguous) {
		// There is two address per "allocate" test
		val caddr = pages.remove();
		val addr = pages.remove();

		// Skip the test if the addresses are not valid
		if (caddr == 0 || addr == 0) {
			return;
		}

		// The Unsafe-based implementation should not work
		assertThrows(UnsupportedOperationException.class, () -> MemoryUtils.u_deallocate(caddr, size), MSG_UNSAFE_THROW);
		assertThrows(UnsupportedOperationException.class, () -> MemoryUtils.u_deallocate(addr, size), MSG_UNSAFE_THROW);

		// Wrong address should succeed as long as the base address can be deduced
		assertTrue(MemoryUtils.c_deallocate(caddr + 1, size), "wrong offset should succeed");
		assertTrue(MemoryUtils.deallocate(addr + 1, size), "wrong offset should succeed");

		// The base address should also succeed
		assertTrue(MemoryUtils.c_deallocate(MemoryUtils.c_allocate(size, contiguous), size),
				"correct deallocation should succeed");
		assertTrue(MemoryUtils.deallocate(MemoryUtils.allocate(size, contiguous), size),
				"correct deallocation should succeed");
	}

	/** Checks that the virtual addresses can be translated to physical addresses. */
	@Test
	@Order(2)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	@EnabledOnOs(OS.LINUX)
	void virt2phys() {
		val virt = MemoryUtils.allocate(1, true);
		val phys = MemoryUtils.virt2phys(virt);
		assertNotEquals(0, virt,                             MSG_NOT_NULL);
		assertNotEquals(0, phys,                             MSG_NOT_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		assertEquals(0,    phys & MemoryUtils.getPagesize(), MSG_ALIGNED);
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be allocated using {@link DmaMemory}. */
	@Test
	@Order(2)
	@DisplayName("DmaMemory can be allocated")
	void allocateDma() {
		val dma = MemoryUtils.allocateDma(1, true);
		assertNotNull(dma, MSG_NOT_NULL);
		val virt = dma.getVirtual();
		val phys = dma.getPhysical();
		assertNotEquals(0, virt,                             MSG_NOT_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		if (System.getProperty("os.name").toLowerCase().contains("lin")) {
			assertNotEquals(0, phys, MSG_NOT_NULL);
		}
		assertEquals(0, phys & MemoryUtils.getPagesize(), MSG_ALIGNED);
		assertNull(MemoryUtils.allocateDma(0, false),     MSG_NULL);
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be written and read arbitrarily using bytes. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		val virt = MemoryUtils.allocate(1, true);
		assertNotEquals(0, virt,                             MSG_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		for (var i = 0; i < 3; i += 1) {
			val number = (byte) (Math.random() * Byte.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putByte(virt, number); 
			else if (i == 1) MemoryUtils.u_putByte(virt, number); 
			else if (i == 2) MemoryUtils.putByte(virt,   number);
			assertEquals(number, MemoryUtils.c_getByte(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.u_getByte(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.getByte(virt),   MSG_CORRECT);
		}
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be written and read arbitrarily using short. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		val virt = MemoryUtils.allocate(2, true);
		assertNotEquals(0, virt,                             MSG_NOT_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		for (var i = 0; i < 3; i += 1) {
			val number = (short) (Math.random() * Short.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putShort(virt, number); 
			else if (i == 1) MemoryUtils.u_putShort(virt, number); 
			else if (i == 2) MemoryUtils.putShort(virt,   number);
			assertEquals(number, MemoryUtils.c_getShort(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.u_getShort(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.getShort(virt),   MSG_CORRECT);
		}
		MemoryUtils.deallocate(virt, 2);
	}

	/** Checks that memory can be written and read arbitrarily using int. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		val virt = MemoryUtils.allocate(4, true);
		assertNotEquals(0, virt,                             MSG_NOT_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		for (var i = 0; i < 3; i += 1) {
			val number = (int) (Math.random() * Integer.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putInt(virt, number); 
			else if (i == 1) MemoryUtils.u_putInt(virt, number); 
			else if (i == 2) MemoryUtils.putInt(virt,   number);
			assertEquals(number, MemoryUtils.c_getInt(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.u_getInt(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.getInt(virt),   MSG_CORRECT);
		}
		MemoryUtils.deallocate(virt, 4);
	}

	/** Checks that memory can be written and read arbitrarily using long. */
	@Test
	@Order(4)
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		val virt = MemoryUtils.allocate(8, true);
		assertNotEquals(0, virt,                             MSG_NOT_NULL);
		assertEquals(0,    virt & MemoryUtils.getPagesize(), MSG_ALIGNED);
		for (var i = 0; i < 3; i += 1) {
			val number = (long) (Math.random() * Long.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putLong(virt, number); 
			else if (i == 1) MemoryUtils.u_putLong(virt, number); 
			else if (i == 2) MemoryUtils.putLong(virt,   number);
			assertEquals(number, MemoryUtils.c_getLong(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.u_getLong(virt), MSG_CORRECT);
			assertEquals(number, MemoryUtils.getLong(virt),   MSG_CORRECT);
		}
		MemoryUtils.deallocate(virt, 8);
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 * 
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteByte(final Unsafe unsafe, final long address, final byte value) {
		assertDoesNotThrow(() -> unsafe.putByte(address, value),  String.format(MSG_ADDRESS_FMTR, address, "byte"));
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
		assertDoesNotThrow(() -> unsafe.putShort(address, value),  String.format(MSG_ADDRESS_FMTR, address, "short"));
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
		assertDoesNotThrow(() -> unsafe.putInt(address, value),  String.format(MSG_ADDRESS_FMTR, address, "int"));
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
		assertDoesNotThrow(() -> unsafe.putLong(address, value),  String.format(MSG_ADDRESS_FMTR, address, "long"));
		val v = assertDoesNotThrow(() -> unsafe.getLong(address), String.format(MSG_ADDRESS_FMTW, address, "long"));
		assertEquals(value, v, MSG_CORRECT);
	}

	/**
	 * Creates a {@link Stream} of {@link Arguments} that contains all the possible combinations used with the {@code
	 * allocate} methods.
	 * 
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocateSource() {
		long sizes[] = {MemoryUtils.getHugepagesize() / 2, MemoryUtils.getHugepagesize() * 2};
		boolean contiguousity[] = {false, true};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val size : sizes) {
			for (val contiguous : contiguousity) {
				builder.add(Arguments.of(size, contiguous));
			}
		}
		return builder.build();
	}

}
