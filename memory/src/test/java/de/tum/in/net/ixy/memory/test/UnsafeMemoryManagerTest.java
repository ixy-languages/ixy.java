package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.memory.UnsafeMemoryManager;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"HardCodedStringLiteral", "DuplicateStringLiteralInspection", "UseOfSunClasses"})
final class UnsafeMemoryManagerTest {

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
		Collection<DynamicTest> exceptions() {
			var expected = 0;
			expected += 2*3*3*3; // get
			expected += 2*3*3*3; // getVolatile
			expected += 2*3*3*3; // put
			expected += 2*3*3*3; // putVolatile
			expected += 3*2*2;   // copy
			expected += 3*2*2;   // copyVolatile
			expected += 3*3*3;   // allocate
			expected += 3*3*2;   // free
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

			return tests;
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getByte(0)")
		void getByte() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByte(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getByte(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getByteVolatile(0)")
		void getByteVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByteVolatile(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getByteVolatile(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putByte(0, byte)")
		void putByte() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByte(0L, (byte) 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putByte(0L, (byte) 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putByteVolatile(0, byte)")
		void putByteVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByteVolatile(0L, (byte) 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putByteVolatile(0L, (byte) 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getShort(0)")
		void getShort() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShort(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getShort(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getShortVolatile(0)")
		void getShortVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShortVolatile(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getShortVolatile(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putShort(0, short)")
		void putShort() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShort(0L, (short) 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putShort(0L, (short) 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putShortVolatile(0, short)")
		void putShortVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShortVolatile(0L, (short) 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putShortVolatile(0L, (short) 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getInt(0)")
		void getInt() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getInt(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getInt(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getIntVolatile(0)")
		void getIntVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getIntVolatile(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getIntVolatile(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putInt(0, int)")
		void putInt() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putInt(0L, 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putInt(0L, 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putIntVolatile(0, int)")
		void putIntVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putIntVolatile(0L, 0));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putIntVolatile(0L, 0));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getLong(0)")
		void getLong() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLong(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getLong(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for getLongVolatile(0)")
		void getLongVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLongVolatile(0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.getLongVolatile(0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putLong(0, long)")
		void putLong() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLong(0L, 0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putLong(0L, 0L));
		}

		@Test
		@DisabledIfOptimized
		@DisplayName("Parameters are checked for putLongVolatile(0, long)")
		void putLongVolatile() {
			assumeManagers();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLongVolatile(0L, 0L));
			assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanagerClone.putLongVolatile(0L, 0L));
		}

	}

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** The memory manager instance to test. */
	private IxyMemoryManager mmanager;

	/** The memory manager cloned instance to test. */
	private IxyMemoryManager mmanagerClone;

	// Creates an "UnsafeMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = UnsafeMemoryManager.getSingleton();
		Field unsafeField = null;
		try {
			unsafeField = UnsafeMemoryManager.class.getDeclaredField("unsafe");
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		}
		if (unsafeField != null) {
			unsafeField.setAccessible(true);
			try {
				val unsafe = (Unsafe) unsafeField.get(mmanager);
				mmanagerClone = (IxyMemoryManager) unsafe.allocateInstance(UnsafeMemoryManager.class);
			} catch (IllegalAccessException | InstantiationException e) {
//				e.printStackTrace();
			} finally {
				unsafeField.setAccessible(false);
			}
		}
	}

	@Test
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		try {
			val constructor = UnsafeMemoryManager.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			val exception = catchThrowable(constructor::newInstance);
			assertThat(exception).isInstanceOf(InvocationTargetException.class);
			val original = ((InvocationTargetException) exception).getTargetException();
			assertThat(original).isInstanceOf(IllegalStateException.class);
		} catch (NoSuchMethodException | SecurityException e) {
//			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("Page size can be computed")
	void pageSize() {
		assumeThat(mmanager).isNotNull();
		val pagesize = mmanager.pageSize();
		assertThat(pagesize)
				.as("Page size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
	}

	@Test
	@DisplayName("Address size can be computed")
	void addressSize() {
		assumeThat(mmanager).isNotNull();
		val addrsize = mmanager.addressSize();
		assertThat(addrsize)
				.as("Address size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(addrsize & -addrsize);
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
	void allocate_free(Long size, AllocationType allocationType, LayoutType layoutType) {
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
		val end = addr + size - 1;
		assertThat(addr).as("Address").isNotZero();

		// Test all memory addresses for every granularity using non-overlapping parallel write and read operations
		for (val bytes : new int[]{Byte.BYTES, Short.BYTES, Integer.BYTES, Long.BYTES}) {
			for (var i = 0; i < bytes; i += 1) {
				// Compute all the addresses that can be used without overlapping
				val alignment = i;
				val aligned = LongStream.range(0, size - 1 - bytes)
						.parallel()
						.filter(x -> x % bytes == alignment)
						.map(x -> addr + x)
						.peek(address -> testValidAddress(addr, end, address, bytes));

				// Compute the most significant bit (excluding the last one which defines the sign) and maximum value
				val msb = 1 << ((bytes << 3) - 1 - 1);
				val max = (msb - 1) | msb;
				val rand = random.nextLong() & max;

				// Test the addresses with the correct data type
				switch (bytes) {
					case Byte.BYTES:
						aligned.forEach(address -> testWriteByte(address, (byte) rand));
						break;
					case Short.BYTES:
						aligned.forEach(address -> testWriteShort(address, (short) rand));
						break;
					case Integer.BYTES:
						aligned.forEach(address -> testWriteInt(address, (int) rand));
						break;
					case Long.BYTES:
						aligned.forEach(address -> testWriteLong(address, rand));
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
		val address = assumeAllocate(Byte.BYTES);
		// Write some data
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByte(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getByte(address);
		mmanager.free(address, Byte.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary volatile bytes can be written and read")
	void getputByteVolatile() {
		val address = assumeAllocate(Byte.BYTES);
		// Write some data
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByteVolatile(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getByteVolatile(address);
		mmanager.free(address, Byte.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		val address = assumeAllocate(Short.BYTES);
		// Write some data
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShort(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getShort(address);
		mmanager.free(address, Short.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary volatile shorts can be written and read")
	void getputShortVolatile() {
		val address = assumeAllocate(Short.BYTES);
		// Write some data
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShortVolatile(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getShortVolatile(address);
		mmanager.free(address, Short.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		val address = assumeAllocate(Integer.BYTES);
		// Write some data
		val number = random.nextInt();
		mmanager.putInt(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getInt(address);
		mmanager.free(address, Integer.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary volatile ints can be written and read")
	void getputIntVolatile() {
		val address = assumeAllocate(Integer.BYTES);
		val number = random.nextInt();
		mmanager.putIntVolatile(address, number);
		val value = mmanager.getIntVolatile(address);
		mmanager.free(address, Integer.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		val address = assumeAllocate(Long.BYTES);
		// Write some data
		val number = random.nextLong();
		mmanager.putLong(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getLong(address);
		mmanager.free(address, Long.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Arbitrary volatile longs can be written and read")
	void getputLongVolatile() {
		val address = assumeAllocate(Long.BYTES);
		// Write some data
		val number = random.nextLong();
		mmanager.putLongVolatile(address, number);
		// Release the memory and verify the contents
		val value = mmanager.getLongVolatile(address);
		mmanager.free(address, Long.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@DisplayName("Direct memory can be copied from|to the JVM heap")
	void getput() {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val address = assumeAllocate(size);
		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.put(address, size, bytes, 0);
		mmanager.put(address, 0, bytes, 0);
		// Recover the data from memory
		val copy = new byte[size];
		mmanager.get(address, size, copy, 0);
		mmanager.get(address, 0, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(address, size, AllocationType.STANDARD);
		assertThat(copy).as("Read|Written data").isEqualTo(bytes);
	}

	@Test
	@DisplayName("Direct memory can be copied from|to the JVM heap (volatile)")
	void getputVolatile() {
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		val address = assumeAllocate(size);
		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.putVolatile(address, size, bytes, 0);
		mmanager.putVolatile(address, 0, bytes, 0);
		// Recover the data from memory
		val copy = new byte[size];
		mmanager.getVolatile(address, size, copy, 0);
		mmanager.getVolatile(address, 0, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(address, size, AllocationType.STANDARD);
		assertThat(copy).as("Read|Written data").isEqualTo(bytes);
	}

	@RepeatedTest(2)
	@DisplayName("Direct memory can be copied to another region")
	void copy(RepetitionInfo repetitionInfo) {
		assumeThat(mmanager).isNotNull();
		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		// Allocate the memory
		val addr1 = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		val addr2 = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(addr1).as("Address").isNotZero();
		assumeThat(addr2).as("Address").isNotZero();
		// Decide the order based on the repetition
		val src = (repetitionInfo.getCurrentRepetition() & 1) == 0 ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		val dest = (repetitionInfo.getCurrentRepetition() & 1) == 1 ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.put(src, bytes.length, bytes, 0);
		// Copy the data to another memory region and recover it from memory
		val copy = new byte[bytes.length];
		mmanager.copy(src, copy.length, dest);
		mmanager.get(dest, copy.length, copy, 0);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
		// Try to copy to the same address just to get 100% coverage
		mmanager.copy(dest, copy.length, dest);
		mmanager.get(dest, copy.length, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(src, bytes.length, AllocationType.STANDARD);
		mmanager.free(dest, copy.length, AllocationType.STANDARD);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
	}

	@RepeatedTest(2)
	@DisplayName("Direct memory can be copied to another region (volatile)")
	void copyVolatile(RepetitionInfo repetitionInfo) {
		assumeThat(mmanager).isNotNull();
		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;
		// Allocate the memory
		val addr1 = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		val addr2 = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(addr1).as("Address").isNotZero();
		assumeThat(addr2).as("Address").isNotZero();
		// Decide the order based on the repetition
		val src = (repetitionInfo.getCurrentRepetition() & 1) == 0 ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		val dest = (repetitionInfo.getCurrentRepetition() & 1) == 1 ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.putVolatile(src, bytes.length, bytes, 0);
		// Copy the data to another memory region and recover it from memory
		val copy = new byte[size];
		mmanager.copyVolatile(src, bytes.length, dest);
		mmanager.getVolatile(dest, bytes.length, copy, 0);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
		// Try to copy to the same address just to get 100% coverage
		mmanager.copyVolatile(dest, copy.length, dest);
		mmanager.getVolatile(dest, copy.length, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(src, bytes.length, AllocationType.STANDARD);
		mmanager.free(dest, bytes.length, AllocationType.STANDARD);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
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
	@DisplayName("The equals(Object) method works as expected")
	void equalsTest() throws InstantiationException {
		assumeManagers();
		// Assert as many different cases as possible
		val softly = new SoftAssertions();
		softly.assertThat(mmanager).isNotEqualTo(null);
		softly.assertThat(mmanager).isEqualTo(mmanager);
		softly.assertThat(mmanager).isNotEqualTo(mmanagerClone);
		softly.assertThat(mmanagerClone).isNotEqualTo(mmanager);
		// Do nasty things to get 100% coverage
		Field unsafeField = null;
		try {
			unsafeField = UnsafeMemoryManager.class.getDeclaredField("unsafe");
		} catch (NoSuchFieldException e) {
//			e.printStackTrace();
		}
		if (unsafeField != null) {
			unsafeField.setAccessible(true);
			try {
				val unsafe = unsafeField.get(mmanager);
				unsafeField.set(mmanagerClone, unsafe);
				assertThat(mmanager).isEqualTo(mmanagerClone);
				assertThat(mmanagerClone).isEqualTo(mmanager);
			} catch (IllegalAccessException e) {
//				e.printStackTrace();
			} finally {
				unsafeField.setAccessible(false);
			}
		}
		softly.assertAll();
	}

	@Test
	@DisplayName("The hashCode() method works as expected")
	void hashCodeTest() {
		assumeManagers();
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
	 * Calls the allocate function from the memory manager and assumes some properties.
	 *
	 * @param size The number of bytes.
	 * @return The base address of the allocated memory region.
	 */
	private long assumeAllocate(long size) {
		assumeThat(mmanager).isNotNull();
		val address = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(address).as("Address").isNotZero();
		return address;
	}

	/**
	 * Asserts that an arbitrary address is within the specified bounds.
	 *
	 * @param start   The start of the memory region.
	 * @param end     The end of the memory region.
	 * @param address The address to manipulate.
	 * @param bytes   The number of bytes to manipulate.
	 */
	private static void testValidAddress(long start, long end, long address, int bytes) {
		val softly = new SoftAssertions();
		for (var i = 0; i < bytes; i += 1) {
			softly.assertThat(address + i)
					.as("Address")
					.withFailMessage("should be inside memory region")
					.isBetween(start, end);
		}
		softly.assertAll();
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 *
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private void testWriteByte(long address, byte value) {
		mmanager.putByte(address, value);
		assertThat(mmanager.getByte(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 *
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private void testWriteShort(long address, short value) {
		mmanager.putShort(address, value);
		assertThat(mmanager.getShort(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 *
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private void testWriteInt(long address, int value) {
		mmanager.putInt(address, value);
		assertThat(mmanager.getInt(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 *
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private void testWriteLong(long address, long value) {
		mmanager.putLong(address, value);
		assertThat(mmanager.getLong(address)).isEqualTo(value);
	}

	/**
	 * The source of arguments for {@link #allocate_free(Long, AllocationType, LayoutType)}.
	 * <p>
	 * This method will generate all the combinations that could raise exceptions or behave differently.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocate_free_Arguments() {
		val size = UnsafeMemoryManager.getSingleton().pageSize();
		AllocationType[] hugity = {AllocationType.STANDARD, AllocationType.HUGE};
		LayoutType[] contigity = {LayoutType.STANDARD, LayoutType.CONTIGUOUS};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val huge : hugity) {
			for (val contiguous : contigity) {
				if (huge == hugity[0] && contiguous == contigity[0]) {
					builder.add(Arguments.of(size << 1, huge, contiguous));
				} else {
					builder.add(Arguments.of(size / 2, huge, contiguous));
				}
			}
		}
		return builder.build();
	}

}
