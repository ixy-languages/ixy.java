package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.JniMemoryManager;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link JniMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("JniMemoryManager")
@Execution(ExecutionMode.SAME_THREAD)
final class JniMemoryManagerTest extends AbstractMemoryTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final @NotNull Random random = new SecureRandom();

	// Creates a "JniMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = JniMemoryManager.getSingleton();
	}

	@Test
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		Constructor<JniMemoryManager> constructor = null;
		try {
			constructor = JniMemoryManager.class.getDeclaredConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
//			e.printStackTrace();
		}
		if (constructor != null) {
			constructor.setAccessible(true);
			val exception = catchThrowable(constructor::newInstance);
			assertThat(exception).isInstanceOf(InvocationTargetException.class);
			val original = ((InvocationTargetException) exception).getTargetException();
			assertThat(original).isInstanceOf(IllegalStateException.class);
			constructor.setAccessible(false);
		}
	}

	/**
	 * Checks that the parameters are checked by the functions.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@Nested
	@DisplayName("JniMemoryManager (Parameters)")
	final class Parameters {

		// Creates the tests that check that the API checks the parameters
		@TestFactory
		@DisabledIfOptimized
		@Contract(value = " -> new", pure = true)
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		@NotNull Collection<@NotNull DynamicTest> exceptions() {
			return commonTest_parameters(mmanager);
		}

	}

	@Test
	@DisplayName("Page size can be computed")
	void pageSize() {
		commonTest_pageSize();
	}

	@Test
	@DisplayName("Address size can be computed")
	void addressSize() {
		commonTest_addressSize();
	}

	@Test
	@DisplayName("Huge memory page size can be computed")
	void hugepageSize() {
		commonTest_hugepageSize();
	}

	@ParameterizedTest(name = "Memory can be allocated and freed (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocate_free_Arguments")
	@EnabledIfRoot
	void allocate_free(long size, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		// Create a clone of the memory manager without calling the constructor
		val mmanagerClone = (IxyMemoryManager) allocateInstance(mmanager.getClass());
		// Make sure the managers are available
		assumeThat(mmanager).isNotNull();
		assumeThat(mmanagerClone).isNotNull();
		// Make the cloned memory manager have a wrong value on the field HUGE_PAGE_SIZE
		val hugepageSizeField = getDeclaredField(mmanager.getClass(), "HUGE_PAGE_SIZE");
		if (hugepageSizeField != null) {
			hugepageSizeField.setAccessible(true);
			fieldSet(hugepageSizeField, mmanagerClone, 0);
			hugepageSizeField.setAccessible(false);
		}
		// Make sure we can extract the huge page size
		val hpsz = mmanager.hugepageSize();
		assumeThat(hpsz).as("Huge memory page size").isPositive().withFailMessage("should be a power of two").isEqualTo(hpsz & -hpsz);
		// Allocate the memory and make sure it's valid
		val addr = mmanager.allocate(size, allocationType, layoutType);
		if (allocationType == AllocationType.HUGE) {
			assertThat(mmanagerClone.allocate(hpsz, AllocationType.HUGE, layoutType)).as("Address").isZero();
			assertThat(mmanagerClone.free(1, hpsz, AllocationType.HUGE)).as("Address").isFalse();
			if (layoutType == LayoutType.CONTIGUOUS && size > hpsz) {
				assertThat(addr).as("Address").isZero();
				// Perform an extra check to achieve better coverage
				val notRoundSize = mmanager.allocate(hpsz, AllocationType.HUGE, LayoutType.CONTIGUOUS);
				assertThat(notRoundSize).as("Address").isNotZero();
				assumeThat(mmanager.free(notRoundSize, hpsz, AllocationType.HUGE)).isTrue();
				return;
			}
		}
		assertThat(addr).as("Address").isNotZero();
		// Test all memory addresses for every granularity using non-overlapping parallel write and read operations
		for (val bytes : new int[]{Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES}) {
			for (var i = 0; i < bytes; i += 1) {
				// Compute all the addresses that can be used without overlapping
				val alignment = i;
				val aligned = LongStream.range(0, size - 1 - bytes)
						.parallel()
						.filter(x -> x % bytes == alignment)
						.map(x -> addr + x);
				// Compute the most significant bit (excluding the last one which defines the sign) and maximum value
				val msb = 1 << (bytes * 8 - 1 - 1);
				val max = (msb - 1) | msb;
				val rand = random.nextLong() & max;
				// Test the addresses with the correct data type
				switch (bytes) {
					case Byte.BYTES:
						aligned.peek(address -> testWrite(address, (byte) rand, false)).forEach(address -> testWrite(address, (byte) rand, false));
						break;
					case Short.BYTES:
						aligned.peek(address -> testWrite(address, (short) rand, false)).forEach(address -> testWrite(address, (short) rand, false));
						break;
					case Integer.BYTES:
						aligned.peek(address -> testWrite(address, (int) rand, false)).forEach(address -> testWrite(address, (int) rand, false));
						break;
					case Long.BYTES:
						aligned.peek(address -> testWrite(address, rand, false)).forEach(address -> testWrite(address, rand, false));
						break;
					default:
						fail("the number of bytes makes no sense");
				}
			}
		}
		// Free the memory
		if (allocationType == AllocationType.HUGE) {
			assertThat(mmanager.free(addr + 1, size, AllocationType.HUGE)).as("Freeing").isTrue();
		} else {
			assertThat(mmanager.free(addr, size, allocationType)).as("Freeing").isTrue();
		}
	}

	@Test
	@EnabledIfRoot
	@DisplayName("DmaMemory can be allocated")
	void dmaAllocate() {
		assumeThat(mmanager).isNotNull();
		// Allocate some memory
		val dma = mmanager.dmaAllocate(1, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(dma).isNotNull();
		// Get the page size and compute the mask
		val pagesize = mmanager.pageSize();
		val mask = pagesize - 1;
		// Free up the memory and verify the memory addresses
		val softly = new SoftAssertions();
		softly.assertThat(mmanager.free(dma.getVirtualAddress(), 1, AllocationType.STANDARD)).as("Freeing").isTrue();
		softly.assertThat(dma.getPhysicalAddress()).as("Physical address").isNotZero();
		softly.assertThat(pagesize).as("Page size").isPositive().withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(dma.getPhysicalAddress() & mask).as("Offset").isEqualTo(dma.getVirtualAddress() & mask);
		softly.assertAll();
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read")
	void getputaddByte() {
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		commonTest_getputaddByte(number, false);
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read (volatile)")
	void getputaddByteVolatile() {
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		commonTest_getputaddByte(number, true);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read")
	void getputaddShort() {
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		commonTest_getputaddShort(number, false);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read (volatile)")
	void getputaddShortVolatile() {
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		commonTest_getputaddShort(number, true);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read")
	void getputaddInt() {
		val number = random.nextInt();
		commonTest_getputaddInt(number, false);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read (volatile)")
	void getputaddIntVolatile() {
		val number = random.nextInt();
		commonTest_getputaddInt(number, true);
	}

	@Test
	@DisplayName("Arbitrary long can be written and read")
	void getputaddLong() {
		val number = random.nextLong();
		commonTest_getputaddLong(number, false);
	}

	@Test
	@DisplayName("Arbitrary long can be written and read (volatile)")
	void getputaddLongVolatile() {
		val number = random.nextLong();
		commonTest_getputaddLong(number, true);
	}

	@Test
	@DisplayName("Direct memory can be copied from|to the JVM heap")
	void getput() {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_getput(bytes);
	}

	@Test
	@DisplayName("Direct memory can be copied from|to the JVM heap (volatile)")
	void getputVolatile() {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_getputVolatile(bytes);
	}

	@RepeatedTest(2)
	@DisplayName("Direct memory can be copied to another region")
	void copy(@NotNull RepetitionInfo repetitionInfo) {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_copy(bytes, (repetitionInfo.getCurrentRepetition() & 1) == 0);
	}

	@RepeatedTest(2)
	@DisplayName("Direct memory can be copied to another region")
	void copyVolatile(@NotNull RepetitionInfo repetitionInfo) {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_copyVolatile(bytes, (repetitionInfo.getCurrentRepetition() & 1) == 0);
	}

	@Test
	@DisplayName("Objects can be translated to memory addresses")
	void obj2pvirt() {
		assumeThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> mmanager.obj2virt(""));
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		commonTest_virt2phys();
	}

	/**
	 * The source of arguments for {@link #allocate_free(long, AllocationType, LayoutType)}.
	 * <p>
	 * This method will generate all the combinations that could raise exceptions or behave differently.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	@Contract(value = " -> new", pure = true)
	private static @NotNull Stream<@NotNull Arguments> allocate_free_Arguments() {
		val mmanager = JniMemoryManager.getSingleton();
		return commonMethodSource_allocate(mmanager.pageSize(), mmanager.hugepageSize());
	}

}
