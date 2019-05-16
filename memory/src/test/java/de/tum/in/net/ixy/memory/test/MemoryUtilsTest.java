package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.val;
import sun.misc.Unsafe;

/** Checks the class {@link MemoryUtils}. */
@DisplayName("DMA manipulation")
class MemoryUtilsTest {

	/** Checks that the page size of the system can be correctly computed. */
	@Test
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

	/** Checks that the address size of the system can be correctly computed. */
	@ParameterizedTest(name = "Memory can be allocated using bigger memory pages (size={0}; contiguous={1})")
	@MethodSource("allocateSource")
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
				unsafe.putByte(addr + fj, one);
			}, String.format("address 0x%x + offset 0x%x is byte-writable", addr, fj));
			assertDoesNotThrow(() -> {
				assertEquals(one, unsafe.getByte(addr + fj), "the read number should be correct");
			}, String.format("address 0x%x + offset 0x%x is byte-readable", addr, fj));

			// Short granularity
			if (j < hpsz - 1) {
				assertDoesNotThrow(() -> {
					unsafe.putShort(addr + fj, two);
				}, String.format("address 0x%x + offset 0x%x is short-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(two, unsafe.getShort(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is short-readable", addr, fj));
			}

			// Integer granularity
			if (j < hpsz - 3) {
				assertDoesNotThrow(() -> {
					unsafe.putInt(addr + fj, four);
				}, String.format("address 0x%x + offset 0x%x is int-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(four, unsafe.getInt(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is int-readable", addr, fj));
			}

			// Long granularity
			if (j < hpsz - 7) {
				assertDoesNotThrow(() -> {
					unsafe.putLong(addr + fj, eight);
				}, String.format("address 0x%x + offset 0x%x is long-writable", addr, fj));
				assertDoesNotThrow(() -> {
					assertEquals(eight, unsafe.getLong(addr + fj), "the read number should be correct");
				}, String.format("address 0x%x + offset 0x%x is long-readable", addr, fj));
			}
		}
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
