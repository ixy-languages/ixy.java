package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import lombok.val;
import sun.misc.Unsafe;

/** Checks the class {@link MemoryUtils}. */
@DisplayName("DMA manipulation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryUtilsTest {

	/** Checks that the page size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Pagesize can be computed")
	void pagesize() {
		val cached = MemoryUtils.getPagesize();
		val jni    = MemoryUtils.c_pagesize();
		val unsafe = MemoryUtils.u_pagesize();
		val smart  = MemoryUtils.pagesize();
		assertNotEquals(cached, 0, "pagesize is not 0");
		assertEquals(cached & -cached, cached, "pagesize is a power of two");
		assertEquals(cached, jni, "cached pagesize should be the same as the computed by JNI");
		assertEquals(cached, unsafe, "cached pagesize should be the same as the computed by the Unsafe object");
		assertEquals(cached, smart, "cached pagesize should be the same as the computed by the smart method");
	}

	/** Checks that the address size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Virtual address size can be computed")
	void addrsize() {
		val cached = MemoryUtils.getAddrsize();
		val jni    = MemoryUtils.c_addrsize();
		val unsafe = MemoryUtils.u_addrsize();
		val smart  = MemoryUtils.addrsize();
		assertNotEquals(cached, 0, "address size is not 0");
		assertEquals(cached & -cached, cached, "address size is a power of two");
		assertEquals(cached, jni, "cached address size should be the same as the computed by JNI");
		assertEquals(cached, unsafe, "cached address size should be the same as the computed by the Unsafe object");
		assertEquals(cached, smart, "cached address size should be the same as the computed by the smart method");
	}

	/** Checks that the address size of the system can be correctly computed. */
	@Test
	@Order(-1)
	@DisplayName("Hugepage size can be computed")
	void hugepage() {
		val cached = MemoryUtils.getHugepagesize();
		val jni    = MemoryUtils.c_hugepage();
		val smart  = MemoryUtils.hugepage();
		assertNotEquals(cached, 0, "hugepage size is not 0");
		assertEquals(cached & -cached, cached, "hugepage size is a power of two");
		assertEquals(cached, jni, "cached hugepage size should be the same as the computed by JNI");
		assertThrows(UnsupportedOperationException.class, MemoryUtils::u_hugepage, "the Unsafe object should throw");
		assertEquals(cached, smart, "cached hugepage size should be the same as the computed by the smart method");
	}

	/** Contains all the allcated pages during the test execution of {@link #allocate(Long, Boolean)}. */
	private static Deque<Long> pages = new ArrayDeque<Long>();

	/** Checks that the address size of the system can be correctly computed. */
	@ParameterizedTest(name = "Memory can be allocated using bigger memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
	@Order(0)
	void allocate(final Long size, final Boolean contiguous) {

		// The Unsafe-based implementation should not work
		assertThrows(UnsupportedOperationException.class, () -> {
			MemoryUtils.u_allocate(size, contiguous);
		}, "the Unsafe-based implementation should throw");

		// Load the Unsafe object
		Unsafe _unsafe = null;
		try {
			val singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			_unsafe = (Unsafe) singleoneInstanceField.get(null);
		} catch (NoSuchFieldException e) {
			fail("Error getting Unsafe object", e);
		} catch (IllegalAccessException e) {
			fail("Error accessing the Unsafe object", e);
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
			assertEquals(0, caddr, "the base address is null");
			assertEquals(0, addr, "the base address is null");
			return;
		} else {
			assertNotEquals(0, caddr, "the base address is not null");
			assertNotEquals(0, addr, "the base address is not null");
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
				val msb  = 1 << (bytes * 8 - 2);
				val max  = (msb - 1) | msb;
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
		assertThrows(UnsupportedOperationException.class, () -> {
			MemoryUtils.u_deallocate(caddr, size);
		}, "the Unsafe-based implementation should throw");
		assertThrows(UnsupportedOperationException.class, () -> {
			MemoryUtils.u_deallocate(addr, size);
		}, "the Unsafe-based implementation should throw");

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
		assertNotEquals(0, virt, "the virtual address is not null");
		assertNotEquals(0, phys, "the physical address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "the virtual address is a page base address");
		assertEquals(0, phys & MemoryUtils.getPagesize(), "the physical address is a page base address");
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be allocated using {@link DmaMemory}. */
	@Test
	@Order(2)
	@DisplayName("DmaMemory can be allocated")
	void allocateDma() {
		val dma = MemoryUtils.allocateDma(1, true);
		assertNotNull(dma, "allocation is successful");
		val virt = dma.getVirtual();
		val phys = dma.getPhysical();
		assertNotEquals(0, virt, "virtual address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "virtual address is a page base address");
		if (System.getProperty("os.name").toLowerCase().contains("lin")) {
			assertNotEquals(0, phys, "physical address is not null");
		}
		assertEquals(0, phys & MemoryUtils.getPagesize(), "physical address is a page base address");
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be written and read arbitrarily using bytes. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		val virt = MemoryUtils.allocate(1, true);
		assertNotEquals(0, virt, "virtual address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "virtual address is a page base address");
		for (var i = 0; i < 3; i += 1) {
			val number = (byte) (Math.random() * Byte.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putByte(virt, number); 
			else if (i == 1) MemoryUtils.u_putByte(virt, number); 
			else if (i == 2) MemoryUtils.putByte(virt, number);
			assertEquals(number, MemoryUtils.c_getByte(virt), "the byte should be correct");
			assertEquals(number, MemoryUtils.u_getByte(virt), "the byte should be correct");
			assertEquals(number, MemoryUtils.getByte(virt), "the byte should be correct");
		}
		MemoryUtils.deallocate(virt, 1);
	}

	/** Checks that memory can be written and read arbitrarily using short. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		val virt = MemoryUtils.allocate(2, true);
		assertNotEquals(0, virt, "virtual address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "virtual address is a page base address");
		for (var i = 0; i < 3; i += 1) {
			val number = (short) (Math.random() * Short.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putShort(virt, number); 
			else if (i == 1) MemoryUtils.u_putShort(virt, number); 
			else if (i == 2) MemoryUtils.putShort(virt, number);
			assertEquals(number, MemoryUtils.c_getShort(virt), "the short should be correct");
			assertEquals(number, MemoryUtils.u_getShort(virt), "the short should be correct");
			assertEquals(number, MemoryUtils.getShort(virt), "the short should be correct");
		}
		MemoryUtils.deallocate(virt, 2);
	}

	/** Checks that memory can be written and read arbitrarily using int. */
	@Test
	@Order(3)
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		val virt = MemoryUtils.allocate(4, true);
		assertNotEquals(0, virt, "virtual address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "virtual address is a page base address");
		for (var i = 0; i < 3; i += 1) {
			val number = (int) (Math.random() * Integer.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putInt(virt, number); 
			else if (i == 1) MemoryUtils.u_putInt(virt, number); 
			else if (i == 2) MemoryUtils.putInt(virt, number);
			assertEquals(number, MemoryUtils.c_getInt(virt), "the int should be correct");
			assertEquals(number, MemoryUtils.u_getInt(virt), "the int should be correct");
			assertEquals(number, MemoryUtils.getInt(virt), "the int should be correct");
		}
		MemoryUtils.deallocate(virt, 4);
	}

	/** Checks that memory can be written and read arbitrarily using long. */
	@Test
	@Order(4)
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		val virt = MemoryUtils.allocate(8, true);
		assertNotEquals(0, virt, "virtual address is not null");
		assertEquals(0, virt & MemoryUtils.getPagesize(), "virtual address is a page base address");
		for (var i = 0; i < 3; i += 1) {
			val number = (long) (Math.random() * Long.MAX_VALUE);
			if      (i == 0) MemoryUtils.c_putLong(virt, number); 
			else if (i == 1) MemoryUtils.u_putLong(virt, number); 
			else if (i == 2) MemoryUtils.putLong(virt, number);
			assertEquals(number, MemoryUtils.c_getLong(virt), "the long should be correct");
			assertEquals(number, MemoryUtils.u_getLong(virt), "the long should be correct");
			assertEquals(number, MemoryUtils.getLong(virt), "the long should be correct");
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
		assertDoesNotThrow(() -> {
			unsafe.putByte(address, value);
		}, String.format("address 0x%x is byte-writable", address));
		assertDoesNotThrow(() -> {
			assertEquals(value, unsafe.getByte(address), "the read number should be correct");
		}, String.format("address 0x%x is byte-readable", address));
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 * 
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteShort(final Unsafe unsafe, final long address, final short value) {
		assertDoesNotThrow(() -> {
			unsafe.putShort(address, value);
		}, String.format("address 0x%x is short-writable", address));
		assertDoesNotThrow(() -> {
			assertEquals(value, unsafe.getShort(address), "the read number should be correct");
		}, String.format("address 0x%x is short-readable", address));
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 * 
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteInt(final Unsafe unsafe, final long address, final int value) {
		assertDoesNotThrow(() -> {
			unsafe.putInt(address, value);
		}, String.format("address 0x%x is int-writable", address));
		assertDoesNotThrow(() -> {
			assertEquals(value, unsafe.getInt(address), "the read number should be correct");
		}, String.format("address 0x%x is int-readable", address));
	}

	/**
	 * Asserts that an arbitrary random address can be written and read.
	 * 
	 * @param unsafe  The unsafe object that allows writting arbitrary memory regions.
	 * @param address The address to read/write from/to.
	 * @param value   The value to read/write.
	 */
	private static void testWriteLong(final Unsafe unsafe, final long address, final long value) {
		assertDoesNotThrow(() -> {
			unsafe.putLong(address, value);
		}, String.format("address 0x%x is long-writable", address));
		assertDoesNotThrow(() -> {
			assertEquals(value, unsafe.getLong(address), "the read number should be correct");
		}, String.format("address 0x%x is long-readable", address));
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
