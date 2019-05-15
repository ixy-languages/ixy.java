package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.MemoryUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

}
