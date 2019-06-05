package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.UnsafeMemoryManager;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.val;

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
final class UnsafeMemoryManagerTest {

	/** A constant expression used to resource lock the unsafe object of the memory manager. */
	private static final String UNSAFE_LOCK = "UNSAFE";

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new Random();

	/** A cached instance of the {@link Unsafe} object. */
	private static Unsafe unsafe;

	// Load the Unsafe object
	static {
		try {
			val theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = (Unsafe) theUnsafeField.get(null);
		} catch (final NoSuchFieldException | IllegalAccessException e) {
//			e.printStackTrace();
		}
	}

	/** The memory manager instance to test. */
	private transient UnsafeMemoryManager mmanager;

	// Creates an "UnsafeMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = UnsafeMemoryManager.getInstance();
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
		} catch (final NoSuchMethodException | SecurityException e) {
//			e.printStackTrace();
		}
	}

	@Test
	@DisabledIfOptimized
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Wrong arguments produce exceptions")
	void exceptions() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.allocate(-1, false, false));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.free(0, 0, false));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.free(1, -1, false));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByte(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByteVolatile(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByte(0, (byte) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByteVolatile(0, (byte) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShort(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShortVolatile(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShort(0, (short) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShortVolatile(0, (short) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getInt(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getIntVolatile(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putInt(0, (int) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putIntVolatile(0, (int) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLong(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLongVolatile(0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLong(0, (long) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLongVolatile(0, (long) 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(0, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(1, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(1, 1, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(1, 1, new byte[0], 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(1, 1, new byte[1], -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(1, 1, new byte[1], 3));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(0, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(1, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(1, 1, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(1, 1, new byte[0], 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(1, 1, new byte[1], -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(1, 1, new byte[1], 3));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(0, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(1, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(1, 1, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(1, 1, new byte[0], 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(1, 1, new byte[1], -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(1, 1, new byte[1], 3));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(0, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(1, 0, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(1, 1, null, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(1, 1, new byte[0], 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(1, 1, new byte[1], -1));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(1, 1, new byte[1], 3));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copy(0, 0, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copy(1, 0, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copy(1, 1, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copyVolatile(0, 0, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copyVolatile(1, 0, 0));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copyVolatile(1, 1, 0));
	}

	@Test
	@DisabledIfOptimized
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Missing unsafe object produces exceptions")
	void stateException() {
		try {
			val unsafeField = UnsafeMemoryManager.class.getDeclaredField("unsafe");
			unsafeField.setAccessible(true);
			val _unsafe = unsafeField.get(mmanager);
			synchronized (mmanager) {
				unsafeField.set(mmanager, null);
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getByte(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getByteVolatile(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putByte(0, (byte) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putByteVolatile(0, (byte) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getShort(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getShortVolatile(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putShort(0, (short) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putShortVolatile(0, (short) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getInt(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getIntVolatile(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putInt(0, (int) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putIntVolatile(0, (int) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getLong(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getLongVolatile(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putLong(0, (long) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putLongVolatile(0, (long) 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.get(0, 0, null, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.getVolatile(0, 0, null, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.put(0, 0, null, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.putVolatile(0, 0, null, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.copy(0, 0, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.copyVolatile(0, 0, 0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.virt2phys(0));
				assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mmanager.dmaAllocate(0, false, false));
				unsafeField.set(mmanager, _unsafe);
			}
		} catch (final NoSuchFieldException | IllegalAccessException e) {
//			e.printStackTrace();
		}
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Page size can be computed")
	void pageSize() {
		assumeThat(mmanager).isNotNull();
		val pagesize = mmanager.pageSize();
		assertThat(pagesize)
				.as("Page size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Address size can be computed")
	void addressSize() {
		assumeThat(mmanager).isNotNull();
		val addrsize = mmanager.addressSize();
		assertThat(addrsize)
				.as("Address size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(addrsize & -addrsize);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@SuppressWarnings("deprecation")
	@DisplayName("Huge memory page size cannot be computed")
	void hugepageSize() {
		assumeThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(mmanager::hugepageSize);
	}

	@ParameterizedTest(name = "Memory can be allocated and freed (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocateSource")
	@ResourceLock(UNSAFE_LOCK)
	@EnabledIfRoot
	void allocate_free(final Long size, final Boolean huge, final Boolean contiguous) {
		assumeThat(unsafe).isNotNull();
		assumeThat(mmanager).isNotNull();

		// Huge memory pages are not supported
		if (huge) {
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Allocation").isThrownBy(() -> mmanager.allocate(size, huge, contiguous));
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Freeing").isThrownBy(() -> mmanager.free(1, size, huge));
			return;
		}

		// Allocate the memory and make sure it's valid
		val addr = mmanager.allocate(size, huge, contiguous);
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
				val msb = 1 << (bytes * 8 - 1 - 1);
				val max = (msb - 1) | msb;
				val rand = random.nextLong() & max;

				// Test the addresses with the correct data type
				switch (bytes) {
					case Byte.BYTES:
						aligned.forEach(address -> testWriteByte(unsafe, address, (byte) rand));
						break;
					case Short.BYTES:
						aligned.forEach(address -> testWriteShort(unsafe, address, (short) rand));
						break;
					case Integer.BYTES:
						aligned.forEach(address -> testWriteInt(unsafe, address, (int) rand));
						break;
					case Long.BYTES:
						aligned.forEach(address -> testWriteLong(unsafe, address, rand));
						break;
					default:
						fail("the number of bytes makes no sense");
				}
			}
		}

		// Free the memory
		assertThat(mmanager.free(addr, size, huge)).as("Freeing").isTrue();
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Byte.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByte(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getByte(address);
		mmanager.free(address, Byte.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary bytes can be written and read (volatile)")
	void getputByteVolatile() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Byte.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByteVolatile(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getByteVolatile(address);
		mmanager.free(address, Byte.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Short.BYTES, false, false);
		assertThat(address).as("Address").isNotZero();

		// Write some data
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShort(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getShort(address);
		mmanager.free(address, Short.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary shorts can be written and read (volatile)")
	void getputShortVolatile() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Short.BYTES, false, false);
		assertThat(address).as("Address").isNotZero();

		// Write some data
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShortVolatile(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getShortVolatile(address);
		mmanager.free(address, Short.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Integer.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = random.nextInt();
		mmanager.putInt(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getInt(address);
		mmanager.free(address, Integer.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary ints can be written and read (volatile)")
	void getputIntVolatile() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Integer.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = random.nextInt();
		mmanager.putIntVolatile(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getIntVolatile(address);
		mmanager.free(address, Integer.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Long.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = random.nextLong();
		mmanager.putLong(address, number);

		// Release the memory and verify the contents
		val value = mmanager.getLong(address);
		mmanager.free(address, Long.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Arbitrary longs can be written and read (volatile)")
	void getputLongVolatile() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val address = mmanager.allocate(Long.BYTES, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Write some data
		val number = random.nextLong();
		mmanager.putLongVolatile(address, number);

		// Verify it's correct and release the memory
		assertThat(mmanager.getLongVolatile(address)).as("Read").isEqualTo(number);
		mmanager.free(address, Long.BYTES, false);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Direct memory can be copied from/to the JVM heap")
	void getput() {
		assumeThat(mmanager).isNotNull();

		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;

		// Allocate the memory
		val address = mmanager.allocate(size, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.put(address, size, bytes, 0);

		// Recover the data from memory
		val copy = new byte[size];
		mmanager.get(address, size, copy, 0);

		// Release the memory and verify the contents
		mmanager.free(address, size, false);
		assertThat(copy).as("Read/Written data").isEqualTo(bytes);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Direct memory can be copied from/to the JVM heap (volatile)")
	void getputVolatile() {
		assumeThat(mmanager).isNotNull();

		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;

		// Allocate the memory
		val address = mmanager.allocate(size, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Generate some data and catch the exceptions
		val bytes = new byte[size];
		val copy = new byte[size];
		random.nextBytes(bytes);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Written data").isThrownBy(() -> mmanager.putVolatile(address, size, bytes, 0));
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Written data").isThrownBy(() -> mmanager.getVolatile(address, size, copy, 0));

		// Release the memory
		mmanager.free(address, size, false);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Direct memory can be copied to another region")
	void copy() {
		assumeThat(mmanager).isNotNull();

		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;

		// Allocate the memory
		val src = mmanager.allocate(size, false, false);
		val dest = mmanager.allocate(size, false, false);
		assumeThat(src).as("Address").isNotZero();
		assumeThat(dest).as("Address").isNotZero();

		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.put(src, size, bytes, 0);

		// Copy the data to another memory region and recover it from memory
		val copy = new byte[size];
		mmanager.copy(src, bytes.length, dest);
		mmanager.get(dest, bytes.length, copy, 0);

		// Release the memory and verify the contents
		mmanager.free(src, bytes.length, false);
		mmanager.free(dest, bytes.length, false);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
	}

	@Test
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("Direct memory can be copied to another region (volatile)")
	void copyVolatile() {
		assumeThat(mmanager).isNotNull();

		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;

		// Allocate the memory and catch the exception
		val src = mmanager.allocate(size, false, false);
		val dest = mmanager.allocate(size, false, false);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Copying").isThrownBy(() -> mmanager.copyVolatile(src, size, dest));

		// Release the memory
		mmanager.free(src, size, false);
		mmanager.free(dest, size, false);
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@ResourceLock(UNSAFE_LOCK)
	@SuppressWarnings("deprecation")
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val virt = mmanager.allocate(1, false, false);
		assumeThat(virt).as("Address").isNotZero();

		// Catch the exception
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Translation").isThrownBy(() -> mmanager.virt2phys(virt));

		// Free up the memory and verify the memory addresses
		mmanager.free(virt, 1, false);
	}

	@Test
	@EnabledIfRoot
	@ResourceLock(UNSAFE_LOCK)
	@DisplayName("DmaMemory cannot be allocated")
	void dmaAllocate() {
		assumeThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Translation").isThrownBy(() -> mmanager.dmaAllocate(1, false, false));
	}

	/**
	 * Asserts that an arbitrary address is within the specified bounds.
	 *
	 * @param start   The start of the memory region.
	 * @param end     The end of the memory region.
	 * @param address The address to manipulate.
	 * @param bytes   The number of bytes to manipulate.
	 */
	private static void testValidAddress(final long start, final long end, final long address, final int bytes) {
		for (var i = 0; i < bytes; i += 1) {
			assertThat(address + i)
					.as("Address")
					.withFailMessage("should be inside memory region")
					.isBetween(start, end);
		}
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 * <p>
	 * This method assumes that the {@code unsafe} parameter is not null and tries to manipulate the {@code address}
	 * using {@code byte}s.
	 *
	 * @param unsafe  The unsafe object that allows manipulating arbitrary memory addresses.
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private static void testWriteByte(final Unsafe unsafe, final long address, final byte value) {
		unsafe.putByte(address, value);
		assertThat(unsafe.getByte(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 * <p>
	 * This method assumes that the {@code unsafe} parameter is not null and tries to manipulate the {@code address}
	 * using {@code short}s.
	 *
	 * @param unsafe  The unsafe object that allows manipulating arbitrary memory addresses.
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private static void testWriteShort(final Unsafe unsafe, final long address, final short value) {
		unsafe.putShort(address, value);
		assertThat(unsafe.getShort(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 * <p>
	 * This method assumes that the {@code unsafe} parameter is not null and tries to manipulate the {@code address}
	 * using {@code int}s.
	 *
	 * @param unsafe  The unsafe object that allows manipulating arbitrary memory addresses.
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private static void testWriteInt(final Unsafe unsafe, final long address, final int value) {
		unsafe.putInt(address, value);
		assertThat(unsafe.getInt(address)).isEqualTo(value);
	}

	/**
	 * Asserts that an arbitrary address can be written and read.
	 * <p>
	 * This method assumes that the {@code unsafe} parameter is not null and tries to manipulate the {@code address}
	 * using {@code long}s.
	 *
	 * @param unsafe  The unsafe object that allows manipulating arbitrary memory addresses.
	 * @param address The address to manipulate.
	 * @param value   The value to use.
	 */
	private static void testWriteLong(final Unsafe unsafe, final long address, final long value) {
		unsafe.putLong(address, value);
		assertThat(unsafe.getLong(address)).isEqualTo(value);
	}

	/**
	 * The source of arguments for {@link #allocate_free(Long, Boolean, Boolean)}.
	 * <p>
	 * This method will generate all the combinations that could raise exceptions or behave differently.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocateSource() {
		val size = UnsafeMemoryManager.getInstance().pageSize();
		val hugity = new boolean[]{false, true};
		val contiguousity = new boolean[]{false, true};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val huge : hugity) {
			for (val contiguous : contiguousity) {
				builder.add(Arguments.of(huge && contiguous ? size * 2: size / 2, huge, contiguous));
			}
		}
		return builder.build();
	}

}
