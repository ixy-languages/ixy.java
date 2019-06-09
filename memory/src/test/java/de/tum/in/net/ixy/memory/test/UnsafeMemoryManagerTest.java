package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.BuildConfig;
import de.tum.in.net.ixy.memory.UnsafeMemoryManager;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Pattern;
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
 * Tests the class {@link UnsafeMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("UnsafeMemoryManager")
@SuppressWarnings({"HardCodedStringLiteral", "DuplicateStringLiteralInspection", "ResultOfMethodCallIgnored"})
final class UnsafeMemoryManagerTest extends AbstractMemoryManagerTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final @NotNull Random random = new SecureRandom();

	/** The cloned memory manager instance to test. */
	private @Nullable IxyMemoryManager mmanagerClone;

	// Creates an "UnsafeMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = UnsafeMemoryManager.getSingleton();
		mmanagerClone = (IxyMemoryManager) allocateInstance(UnsafeMemoryManager.class);
	}

	@Test
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		Constructor<UnsafeMemoryManager> constructor = null;
		try {
			constructor = UnsafeMemoryManager.class.getDeclaredConstructor();
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
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Nested
	final class Parameters {

		// Creates the tests that check that the API checks the parameters
		@TestFactory
		@DisabledIfOptimized
		@Contract(value = " -> new", pure = true)
		@NotNull Collection<@NotNull DynamicTest> exceptions() {
			var expected = 0;
			expected += 2 * 3 * 3 * 3; // get
			expected += 2 * 3 * 3 * 3; // getVolatile
			expected += 2 * 3 * 3 * 3; // put
			expected += 2 * 3 * 3 * 3; // putVolatile
			expected += 3 * 2 * 2;     // copy
			expected += 3 * 2 * 2;     // copyVolatile
			expected += 3 * 3 * 3;     // allocate
			expected += 3 * 3 * 2;     // free
			Collection<DynamicTest> tests = new ArrayList<>(expected);

			// Create the tests for get(Volatile)/put(Volatile)
			long[] addresses = {0L, 1L};
			int[] sizes = {-1, 0, 1};
			byte[][] buffers = {null, new byte[0], new byte[1]};
			int[] offsets = {-1, 0, 1};
			for (val address : addresses) {
				for (val size : sizes) {
					for (val buffer : buffers) {
						for (val offset : offsets) {
							if (address == 0 || size < 0 || buffer == null || offset < 0 || offset >= buffer.length) {
								val buff = buffer == null ? "null" : String.format("byte[%d]", buffer.length);
								var name = String.format("Parameters are checked for get(%d, %d, %s, %d)", address, size, buff, offset);
								tests.add(DynamicTest.dynamicTest(name, () -> {
									assumeManagers();
									assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(address, size, buffer, offset));
									assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.get(address, size, buffer, offset));
								}));
								name = String.format("Arguments are checked for getVolatile(%d, %d, %s, %d)", address, size, buff, offset);
								tests.add(DynamicTest.dynamicTest(name, () -> {
									assumeManagers();
									assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(address, size, buffer, offset));
									assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getVolatile(address, size, buffer, offset));
								}));
								name = String.format("Arguments are checked for put(%d, %d, %s, %d)", address, size, buff, offset);
								tests.add(DynamicTest.dynamicTest(name, () -> {
									assumeManagers();
									assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(address, size, buffer, offset));
									assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.put(address, size, buffer, offset));
								}));
								name = String.format("Arguments are checked for putVolatile(%d, %d, %s, %d)", address, size, buff, offset);
								tests.add(DynamicTest.dynamicTest(name, () -> {
									assumeManagers();
									assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(address, size, buffer, offset));
									assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putVolatile(address, size, buffer, offset));
								}));
							}
						}
					}
				}
			}

			// Create the tests for copy(Volatile)
			addresses = new long[]{0L, 1L};
			sizes = new int[]{-1, 0, 1};
			for (val src : addresses) {
				for (val size : sizes) {
					for (val dest : addresses) {
						if (src == 0 || size < 0 || dest == 0) {
							var name = String.format("Parameters are checked for copy(%d, %d, %d)", src, size, dest);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeManagers();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copy(src, size, dest));
								assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.copy(src, size, dest));
							}));
							name = String.format("Parameters are checked for copyVolatile(%d, %d, %d)", src, size, dest);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeManagers();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copyVolatile(src, size, dest));
								assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.copyVolatile(src, size, dest));
							}));
						}
					}
				}
			}

			// Create the tests for allocate/free
			addresses = new long[]{0L, 1L};
			sizes = new int[]{-1, 0, 1};
			AllocationType[] allocationTypes = {null, AllocationType.STANDARD, AllocationType.HUGE};
			LayoutType[] layoutTypes = {null, LayoutType.STANDARD, LayoutType.CONTIGUOUS};
			for (val size : sizes) {
				for (val allocationType : allocationTypes) {
					for (val layoutType : layoutTypes) {
						if (size == 0 || allocationType == null || layoutType == null) {
							var name = String.format("Parameters are checked for allocate(%d, %s, %s)", size, allocationType, layoutType);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeManagers();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.allocate(size, allocationType, layoutType));
								assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.allocate(size, allocationType, layoutType));
							}));
						}
					}
					for (val address : addresses) {
						if (address == 0 || size == 0 || allocationType == null) {
							val name = String.format("Parameters are checked for free(%d, %d, %s)", address, size, allocationType);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeManagers();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.free(address, size, allocationType));
								assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.free(address, size, allocationType));
							}));
						}
					}
				}
			}

			/// Add other tests that cannot be added with a loop
			// For bytes
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getByte(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByte(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getByte(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getByteVolatile(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByteVolatile(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getByteVolatile(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putByte(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByte(0L, (byte) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putByte(0L, (byte) 0));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putByteVolatile(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByteVolatile(0L, (byte) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putByteVolatile(0L, (byte) 0));
			}));
			// For shorts
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getShort(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShort(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getShort(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getShortVolatile(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShortVolatile(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getShortVolatile(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putShort(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShort(0L, (short) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putShort(0L, (short) 0));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putShortVolatile(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShortVolatile(0L, (short) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putShortVolatile(0L, (short) 0));
			}));
			// For ints
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getInt(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getInt(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getInt(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getIntVolatile(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getIntVolatile(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getIntVolatile(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putInt(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putInt(0L, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putInt(0L, 0));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putIntVolatile(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putIntVolatile(0L, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putIntVolatile(0L, 0));
			}));
			// For longs
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getLong(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLong(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getLong(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for getLongVolatile(0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLongVolatile(0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getLongVolatile(0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putLong(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLong(0L, 0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putLong(0L, 0L));
			}));
			tests.add(DynamicTest.dynamicTest("Parameters are checked for putLongVolatile(0, 0)", () -> {
				assumeManagers();
				assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLongVolatile(0L, 0L));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putLongVolatile(0L, 0L));
			}));

			return tests;
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
	@DisplayName("Huge memory page size cannot be computed")
	void hugepageSize() {
		assumeThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(mmanager::hugepageSize);
	}

	@ParameterizedTest(name = "Memory can be allocated and freed (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocate_free_Arguments")
	@EnabledIfRoot
	void allocate_free(long size, @NotNull AllocationType allocationType, @NotNull LayoutType layoutType) {
		assumeThat(mmanager).isNotNull();
		// Huge memory pages are not supported
		if (allocationType == AllocationType.HUGE) {
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Allocation").isThrownBy(() -> mmanager.allocate(size, AllocationType.HUGE, layoutType));
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Freeing").isThrownBy(() -> mmanager.free(1, size, AllocationType.HUGE));
			return;
		}
		// Allocate the memory and make sure it's valid
		val addr = mmanager.allocate(size, allocationType, layoutType);
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
				val msb = 1 << ((bytes << 3) - 1 - 1);
				val max = (msb - 1) | msb;
				val rand1 = random.nextLong() & max;
				val rand2 = random.nextLong() & max;
				// Test the addresses with the correct data type
				switch (bytes) {
					case Byte.BYTES:
						aligned.peek(address -> testWrite(address, (byte) rand1, false))
								.forEach(address -> testWrite(address, (byte) rand2, true));
						break;
					case Short.BYTES:
						aligned.peek(address -> testWrite(address, (short) rand1, false))
								.forEach(address -> testWrite(address, (short) rand2, true));
						break;
					case Integer.BYTES:
						aligned.peek(address -> testWrite(address, (int) rand1, false))
								.forEach(address -> testWrite(address, (int) rand2, true));
						break;
					case Long.BYTES:
						aligned.peek(address -> testWrite(address, rand1, false))
								.forEach(address -> testWrite(address, rand2, true));
						break;
					default:
						fail("the number of bytes makes no sense");
				}
			}
		}
		// Free the memory
		assertThat(mmanager.free(addr, size, allocationType)).as("Freeing").isTrue();
	}

	@Test
	@EnabledIfRoot
	@DisplayName("DmaMemory cannot be allocated")
	void dmaAllocate() {
		assumeThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class).as("Translation")
				.isThrownBy(() -> mmanager.dmaAllocate(1, AllocationType.STANDARD, LayoutType.STANDARD));
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
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		val virt = assumeAllocate(1);
		assertThatExceptionOfType(UnsupportedOperationException.class).as("Translation").isThrownBy(() -> mmanager.virt2phys(virt));
		mmanager.free(virt, 1, AllocationType.STANDARD);
	}

	@Test
	@ResourceLock(BuildConfig.LOCK)
	@DisplayName("The equals(Object) method works as expected")
	void equalsTest() {
		assumeManagers();
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
		val unsafeField = getDeclaredField(UnsafeMemoryManager.class, "unsafe");
		if (unsafeField != null) {
			unsafeField.setAccessible(true);
			val unsafe = fieldGet(unsafeField, mmanager);
			fieldSet(unsafeField, mmanagerClone, unsafe);
			softly.assertThat(mmanager).isEqualTo(mmanagerClone);
			softly.assertThat(mmanagerClone).isEqualTo(mmanager);
			fieldSet(unsafeField, mmanagerClone, null);
			unsafeField.setAccessible(false);
		}
		softly.assertAll();
	}

	@Test
	@ResourceLock(BuildConfig.LOCK)
	@DisplayName("The hashCode() method works as expected")
	void hashCodeTest() {
		assumeManagers();
		// Assert as many different cases as possible
		val softly = new SoftAssertions();
		softly.assertThat(mmanagerClone.hashCode()).as("Hash code").isNotEqualTo(mmanager.hashCode());
		// Do nasty things to get 100% coverage
		val unsafeField = getDeclaredField(UnsafeMemoryManager.class, "unsafe");
		if (unsafeField != null) {
			unsafeField.setAccessible(true);
			val unsafe = fieldGet(unsafeField, mmanager);
			fieldSet(unsafeField, mmanagerClone, unsafe);
			softly.assertThat(mmanagerClone.hashCode()).as("Hash code").isEqualTo(mmanager.hashCode());
			fieldSet(unsafeField, mmanagerClone, null);
			unsafeField.setAccessible(false);
		}
		softly.assertAll();
	}

	@Test
	@SuppressWarnings("HardcodedFileSeparator")
	@DisplayName("The string representation is correct")
	void toStringTest() {
		assumeThat(mmanager).isNotNull();
		val genericPattern = "^%s\\([a-zA-Z]+=(sun\\.misc\\.)?Unsafe@[0-9a-z]+\\)$";
		val specificPattern = String.format(genericPattern, UnsafeMemoryManager.class.getSimpleName());
		val pattern = Pattern.compile(specificPattern);
		assertThat(mmanager.toString()).as("String representation").matches(pattern);
	}

	/** Assumes the managers {@link #mmanager} and {@link #mmanagerClone} are not {@code null}. */
	private void assumeManagers() {
		assumeThat(mmanager).isNotNull();
		assumeThat(mmanagerClone).isNotNull();
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
		val pageSize = UnsafeMemoryManager.getSingleton().pageSize();
		AllocationType[] hugity = {AllocationType.STANDARD, AllocationType.HUGE};
		LayoutType[] contigity = {LayoutType.STANDARD, LayoutType.CONTIGUOUS};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val huge : hugity) {
			for (val contiguous : contigity) {
				if (huge == hugity[0] && contiguous == contigity[0]) {
					builder.add(Arguments.of(pageSize << 1, huge, contiguous));
				} else {
					builder.add(Arguments.of(pageSize / 2, huge, contiguous));
				}
			}
		}
		return builder.build();
	}

}
