package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.val;

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

}
