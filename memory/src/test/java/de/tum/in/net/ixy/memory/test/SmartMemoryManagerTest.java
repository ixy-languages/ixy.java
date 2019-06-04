package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.JniMemoryManager;
import de.tum.in.net.ixy.memory.SmartMemoryManager;

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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.assertj.core.api.SoftAssertions;

import lombok.val;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests the class {@link SmartMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("SmartMemoryManager")
@Execution(ExecutionMode.SAME_THREAD)
final class SmartMemoryManagerTest {

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
	private transient SmartMemoryManager mmanager;

	// Creates a "SmartMemoryManager" instance
	@BeforeEach
	void setUp() {
		mmanager = SmartMemoryManager.getInstance();
	}

	@Test
	@DisplayName("Instantiation is not supported")
	void constructorException() {
		try {
			val constructor = SmartMemoryManager.class.getDeclaredConstructor();
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
	@DisplayName("Huge memory page size can be computed")
	void hugepageSize() {
		assumeThat(mmanager).isNotNull();
		val hpsz = mmanager.hugepageSize();
		assertThat(hpsz)
				.as("Huge memory page size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(hpsz & -hpsz);
	}

	@ParameterizedTest(name = "Memory can be allocated and freed (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocateSource")
	@EnabledIfRoot
	void allocate_free(final Long size, final Boolean huge, final Boolean contiguous) {
		assumeThat(unsafe).isNotNull();
		assumeThat(mmanager).isNotNull();

		// Allocate the memory and make sure it's valid
		val addr = mmanager.allocate(size, huge, contiguous);
		val end = addr + size - 1;
		if (huge && contiguous && size > mmanager.hugepageSize()) {
			assertThat(addr).as("Address").isZero();
			return;
		} else {
			assertThat(addr).as("Address").isNotZero();
		}

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
		val value = mmanager.getLongVolatile(address);
		mmanager.free(address, Long.BYTES, false);
		assertThat(value).as("Read").isEqualTo(number);
	}

	@Test
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
	@DisplayName("Direct memory can be copied from/to the JVM heap (volatile)")
	void getputVolatile() {
		assumeThat(mmanager).isNotNull();

		// Define the amount of data to write randomly
		val size = random.nextInt(Short.MAX_VALUE - Byte.MAX_VALUE) + Byte.MAX_VALUE;

		// Allocate the memory
		val address = mmanager.allocate(size, false, false);
		assumeThat(address).as("Address").isNotZero();

		// Generate and write some random data
		val bytes = new byte[size];
		random.nextBytes(bytes);
		mmanager.putVolatile(address, size, bytes, 0);

		// Recover the data from memory
		val copy = new byte[size];
		mmanager.getVolatile(address, size, copy, 0);

		// Release the memory and verify the contents
		mmanager.free(address, size, false);
		assertThat(copy).as("Read/Written data").isEqualTo(bytes);
	}

	@Test
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
		mmanager.copy(src, size, dest);
		mmanager.get(dest, size, copy, 0);

		// Release the memory and verify the contents
		mmanager.free(src, size, false);
		mmanager.free(dest, size, false);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
	}

	@Test
	@DisplayName("Direct memory can be copied to another region (volatile)")
	void copyVolatile() {
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
		mmanager.copyVolatile(src, size, dest);
		mmanager.get(dest, size, copy, 0);

		// Release the memory and verify the contents
		mmanager.free(src, size, false);
		mmanager.free(dest, size, false);
		assertThat(copy).as("Copied data").isEqualTo(bytes);
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		assumeThat(mmanager).isNotNull();

		// Allocate the memory
		val virt = mmanager.allocate(1, false, false);
		assumeThat(virt).as("Address").isNotZero();

		// Translate it, get the page size and compute the mask
		val phys = mmanager.virt2phys(virt);
		val pagesize = mmanager.pageSize();
		val mask = pagesize - 1;

		// Free up the memory and verify the memory addresses
		mmanager.free(virt, 1, false);
		val softly = new SoftAssertions();
		softly.assertThat(phys).as("Physical address").isNotZero();
		softly.assertThat(pagesize)
				.as("Page size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(phys & mask).as("Offset").isEqualTo(virt & mask);
		softly.assertAll();
	}

	@Test
	@EnabledIfRoot
	@DisplayName("DmaMemory can be allocated")
	void dmaAllocate() {
		assumeThat(mmanager).isNotNull();

		// Allocate some memory
		val dma = mmanager.dmaAllocate(1, false, false);
		assumeThat(dma).isNotNull();

		// Get the page size and compute the mask
		val pagesize = mmanager.pageSize();
		val mask = pagesize - 1;

		// Free up the memory and verify the memory addresses
		mmanager.free(dma.getVirtualAddress(), 1, false);
		val softly = new SoftAssertions();
		softly.assertThat(dma.getPhysicalAddress()).as("Physical address").isNotZero();
		softly.assertThat(pagesize)
				.as("Page size").isGreaterThan(0)
				.withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(dma.getPhysicalAddress() & mask).as("Offset").isEqualTo(dma.getVirtualAddress() & mask);
		softly.assertAll();
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
		val mmanager = JniMemoryManager.getInstance();
		val hugity = new boolean[]{false, true};
		val contiguousity = new boolean[]{false, true};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val huge : hugity) {
			for (val contiguous : contiguousity) {
				val size = huge && contiguous ? mmanager.hugepageSize() + Long.BYTES : mmanager.pageSize() * 2;
				builder.add(Arguments.of(size, huge, contiguous));
			}
		}
		return builder.build();
	}

}
