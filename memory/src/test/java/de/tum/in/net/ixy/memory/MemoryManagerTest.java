package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.security.SecureRandom;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link SmartMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("Unsafe, JNI & Smart memory managers")
final class MemoryManagerTest extends AbstractMemoryTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final @NotNull Random random = new SecureRandom();

	/** A cached instance of an Unsafe-based memory manager. */
	private static final @NotNull IxyMemoryManager unsafe = UnsafeMemoryManager.getSingleton();

	/** A cached instance of a JNI-based memory manager. */
	private static final @NotNull IxyMemoryManager jni = JniMemoryManager.getSingleton();

	/** A cached instance of a smart-based memory manager. */
	private static final @NotNull IxyMemoryManager smart = SmartMemoryManager.getSingleton();

	@Test
	@DisplayName("Page size is common")
	void pageSize() {
		val unsafePageSize = unsafe.pageSize();
		val jniPageSize = jni.pageSize();
		val smartPageSize = smart.pageSize();
		assertThat(unsafePageSize).isEqualTo(jniPageSize);
		assertThat(jniPageSize).isEqualTo(smartPageSize);
	}

	@Test
	@DisplayName("Address size is common")
	void addressSize() {
		val unsafeAddressSize = unsafe.addressSize();
		val jniAddressSize = jni.addressSize();
		val smartAddressSize = smart.addressSize();
		assertThat(unsafeAddressSize).isEqualTo(jniAddressSize);
		assertThat(jniAddressSize).isEqualTo(smartAddressSize);
	}

	@Test
	@DisplayName("Objects can be translated to memory addresses")
	void obj2virt() {
		val obj = "Hello World!";
		val unsafeVirt = unsafe.obj2virt(obj);
		val smartVirt = smart.obj2virt(obj);
		assertThat(unsafeVirt).isEqualTo(smartVirt);
	}

}
