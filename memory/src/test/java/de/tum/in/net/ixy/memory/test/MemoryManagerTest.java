package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.JniMemoryManager;
import de.tum.in.net.ixy.memory.SmartMemoryManager;
import de.tum.in.net.ixy.memory.UnsafeMemoryManager;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static de.tum.in.net.ixy.generic.IxyMemoryManager.AllocationType;
import static de.tum.in.net.ixy.generic.IxyMemoryManager.LayoutType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link SmartMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("Unsafe, JNI & Smart memory managers")
@Execution(ExecutionMode.SAME_THREAD)
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
