package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.BuildConfig;
import de.tum.in.net.ixy.memory.JniMemoryManager;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
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
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static de.tum.in.net.ixy.generic.IxyMemoryManager.AllocationType;
import static de.tum.in.net.ixy.generic.IxyMemoryManager.LayoutType;
import static org.assertj.core.api.Assertions.assertThat;
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
final class JniMemoryManagerTest extends AbstractMemoryManagerTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** The cloned memory manager instance to test. */
	private IxyMemoryManager mmanagerClone;

	// Creates a "JniMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = JniMemoryManager.getSingleton();
		mmanagerClone = (IxyMemoryManager) allocateInstance(JniMemoryManager.class);
		if (mmanagerClone != null) {
			val hugepageSizeField = getDeclaredField(JniMemoryManager.class, "HUGE_PAGE_SIZE");
			if (hugepageSizeField != null) {
				hugepageSizeField.setAccessible(true);
				fieldSet(hugepageSizeField, mmanagerClone, 0);
				hugepageSizeField.setAccessible(false);
			}
		}
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
	final class Parameters {

		// Creates the tests that check that the API checks the parameters
		@TestFactory
		@DisabledIfOptimized
		@SuppressWarnings("JUnitTestMethodWithNoAssertions")
		Collection<DynamicTest> exceptions() {
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
	void allocate_free(long size, AllocationType allocationType, LayoutType layoutType) {
		assumeThat(mmanager).isNotNull();
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
						aligned.peek(address -> testWrite(address, (byte) rand, false))
								.forEach(address -> testWrite(address, (byte) rand, false));
						break;
					case Short.BYTES:
						aligned.peek(address -> testWrite(address, (short) rand, false))
								.forEach(address -> testWrite(address, (short) rand, false));
						break;
					case Integer.BYTES:
						aligned.peek(address -> testWrite(address, (int) rand, false))
								.forEach(address -> testWrite(address, (int) rand, false));
						break;
					case Long.BYTES:
						aligned.peek(address -> testWrite(address, rand, false))
								.forEach(address -> testWrite(address, rand, false));
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
		mmanager.free(dma.getVirtualAddress(), 1, AllocationType.STANDARD);
		val softly = new SoftAssertions();
		softly.assertThat(dma.getPhysicalAddress()).as("Physical address").isNotZero();
		softly.assertThat(pagesize).as("Page size").isPositive().withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(dma.getPhysicalAddress() & mask).as("Offset").isEqualTo(dma.getVirtualAddress() & mask);
		softly.assertAll();
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		commonTest_getputByte(number, false);
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read (volatile)")
	void getputByteVolatile() {
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		commonTest_getputByte(number, true);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		commonTest_getputShort(number, false);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read (volatile)")
	void getputShortVolatile() {
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		commonTest_getputShort(number, true);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		val number = random.nextInt();
		commonTest_getputInt(number, false);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read (volatile)")
	void getputIntVolatile() {
		val number = random.nextInt();
		commonTest_getputInt(number, true);
	}

	@Test
	@DisplayName("Arbitrary long can be written and read")
	void getputLong() {
		val number = random.nextLong();
		commonTest_getputLong(number, false);
	}

	@Test
	@DisplayName("Arbitrary long can be written and read (volatile)")
	void getputLongVolatile() {
		val number = random.nextLong();
		commonTest_getputLong(number, true);
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
	void copy(RepetitionInfo repetitionInfo) {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_copy(bytes, (repetitionInfo.getCurrentRepetition() & 1) == 0);
	}

	@RepeatedTest(2)
	@DisplayName("Direct memory can be copied to another region")
	void copyVolatile(RepetitionInfo repetitionInfo) {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val bytes = new byte[size];
		random.nextBytes(bytes);
		commonTest_copyVolatile(bytes, (repetitionInfo.getCurrentRepetition() & 1) == 0);
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		val virt = assumeAllocate(1);
		// Translate it, get the page size and compute the mask
		val phys = mmanager.virt2phys(virt);
		val pagesize = mmanager.pageSize();
		val mask = pagesize - 1;
		// Free up the memory and verify the memory addresses
		mmanager.free(virt, 1, AllocationType.STANDARD);
		val softly = new SoftAssertions();
		softly.assertThat(phys).as("Physical address").isNotZero();
		softly.assertThat(pagesize).as("Page size").isPositive().withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(phys & mask).as("Offset").isEqualTo(virt & mask);
		softly.assertAll();
	}

	@Test
	@ResourceLock(BuildConfig.LOCK)
	@DisplayName("The equals(Object) method works as expected")
	void equalsTest() {
		assumeThat(mmanager).isNotNull();
		// Assert as many different cases as possible
		val softly = new SoftAssertions();
		softly.assertThat(mmanager).isNotEqualTo(null);
		softly.assertThat(mmanagerClone).isNotEqualTo(null);
		softly.assertThat(mmanager).isNotEqualTo(softly);
		softly.assertThat(mmanagerClone).isNotEqualTo(softly);
		softly.assertThat(mmanager).isEqualTo(mmanager);
		softly.assertThat(mmanagerClone).isEqualTo(mmanagerClone);
		softly.assertThat(mmanager).isNotEqualTo(mmanagerClone);
		softly.assertThat(mmanagerClone).isNotEqualTo(mmanager);
		// Do nasty things to get 100% coverage
		Field hugepageSizeField = getDeclaredField(JniMemoryManager.class, "HUGE_PAGE_SIZE");
		if (hugepageSizeField != null) {
			hugepageSizeField.setAccessible(true);
			val hugepageSize = fieldGet(hugepageSizeField, mmanager);
			fieldSet(hugepageSizeField, mmanagerClone, hugepageSize);
			softly.assertThat(mmanager).isEqualTo(mmanagerClone);
			softly.assertThat(mmanagerClone).isEqualTo(mmanager);
			fieldSet(hugepageSizeField, mmanagerClone, 0);
			hugepageSizeField.setAccessible(false);
		}
		softly.assertAll();
	}

	@Test
	@ResourceLock(BuildConfig.LOCK)
	@DisplayName("The hashCode() method works as expected")
	void hashCodeTest() {
		assumeThat(mmanager).isNotNull();
		// Get the hashes
		val hash1 = mmanager.hashCode();
		val hash2 = mmanagerClone.hashCode();
		// Assert the values
		val softly = new SoftAssertions();
		softly.assertThat(mmanager.hashCode()).as("Hash code").isEqualTo(hash1);
		softly.assertThat(mmanagerClone.hashCode()).as("Hash code").isEqualTo(hash2);
		softly.assertThat(hash1).as("Hash code").isNotEqualTo(hash2);
		softly.assertAll();
	}

	@Test
	@SuppressWarnings("HardcodedFileSeparator")
	@DisplayName("The string representation is correct")
	void toStringTest() {
		assumeThat(mmanager).isNotNull();
		val genericPattern = "^%s\\(\\w*page\\w*=[1-9][0-9]*\\)$";
		val specificPattern = String.format(genericPattern, JniMemoryManager.class.getSimpleName());
		val pattern = Pattern.compile(specificPattern);
		assertThat(mmanager.toString()).as("String representation").matches(pattern);
	}

	/**
	 * The source of arguments for {@link #allocate_free(long, AllocationType, LayoutType)}.
	 * <p>
	 * This method will generate all the combinations that could raise exceptions or behave differently.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocate_free_Arguments() {
		val mmanager = JniMemoryManager.getSingleton();
		return commonMethodSource_allocate(mmanager.pageSize(), mmanager.hugepageSize());
	}

}
