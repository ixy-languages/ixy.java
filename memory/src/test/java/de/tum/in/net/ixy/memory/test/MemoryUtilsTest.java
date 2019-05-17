package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayDeque;
import java.util.Deque;
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

		// Test each memory address for read and write using the Unsafe object
		val hpsz = MemoryUtils.getHugepagesize();
		for (var j = 0; j < hpsz; j += 1) {
			val fj    = j;
			val rand  = Math.random();
			val one   = (byte)  (rand * Byte.MAX_VALUE);
			val two   = (short) (rand * Short.MAX_VALUE);
			val four  = (int)   (rand * Integer.MAX_VALUE);
			val eight = (long)  (rand * Long.MAX_VALUE);
			
			// Byte granularity
			assertDoesNotThrow(() -> {
				unsafe.putByte(caddr + fj, one);
				unsafe.putByte(addr + fj, one);
			}, String.format("address 0x%x + offset 0x%x is byte-writable", addr, fj));
			assertDoesNotThrow(() -> {
				assertEquals(one, unsafe.getByte(caddr + fj), "the read number should be correct");
				assertEquals(one, unsafe.getByte(addr + fj), "the read number should be correct");
			}, String.format("address 0x%x + offset 0x%x is byte-readable", addr, fj));

			// Short granularity
			if (j < hpsz - 1) {
				assertDoesNotThrow(() -> {
					unsafe.putShort(caddr + fj, two);
					unsafe.putShort(addr + fj, two);
				}, String.format("address 0x%x + offset 0x%x is short-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(two, unsafe.getShort(caddr + fj), "the read number should be correct");
					assertEquals(two, unsafe.getShort(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is short-readable", addr, fj));
			}

			// Integer granularity
			if (j < hpsz - 3) {
				assertDoesNotThrow(() -> {
					unsafe.putInt(caddr + fj, four);
					unsafe.putInt(addr + fj, four);
				}, String.format("address 0x%x + offset 0x%x is int-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(four, unsafe.getInt(caddr + fj), "the read number should be correct");
					assertEquals(four, unsafe.getInt(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is int-readable", addr, fj));
			}

			// Long granularity
			if (j < hpsz - 7) {
				assertDoesNotThrow(() -> {
					unsafe.putLong(caddr + fj, eight);
					unsafe.putLong(addr + fj, eight);
				}, String.format("address 0x%x + offset 0x%x is long-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(eight, unsafe.getLong(caddr + fj), "the read number should be correct");
					assertEquals(eight, unsafe.getLong(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is long-readable", addr, fj));
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
		assertEquals(0, virt & MemoryUtils.getHugepagesize(), "the virtual address is a hugepage base address");
		assertEquals(0, phys & MemoryUtils.getPagesize(), "the physical address is a page base address");
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
