package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.UnsafeMemoryManager;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.val;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests the class {@link UnsafeMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("UnsafeMemoryManager")
final class UnsafeMemoryManagerTest {

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
	@DisplayName("Page size can be computed")
	void pageSize() {
		assertThat(mmanager).isNotNull();
		val pagesize = mmanager.pageSize();
		assertThat(pagesize)
				.as("Page size")
				.isGreaterThan(0)
				.withFailMessage("should be a power of two")
				.isEqualTo(pagesize & -pagesize);
	}

	@Test
	@DisplayName("Address size can be computed")
	void addressSize() {
		assertThat(mmanager).isNotNull();
		val addrsize = mmanager.addressSize();
		assertThat(addrsize)
				.as("Address size")
				.isGreaterThan(0)
				.withFailMessage("should be a power of two")
				.isEqualTo(addrsize & -addrsize);
	}

	@Test
	@SuppressWarnings("deprecation")
	@DisplayName("Huge memory page size cannot be computed")
	void hugepageSize() {
		assertThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(mmanager::hugepageSize);
	}

	@ParameterizedTest(name = "Memory can be allocated (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocateSource")
	@EnabledIfRoot
	void allocate(final Long size, final Boolean huge, final Boolean contiguous) {
		assertThat(mmanager).isNotNull();

		// Huge memory pages are not supported
		if (huge) {
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Allocation")
					.isThrownBy(() -> mmanager.allocate(size, huge, contiguous));
			return;
		}

		// Make sure we can use the Unsafe object
		assertThat(unsafe).isNotNull();

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
		unsafe.freeMemory(addr);
	}

	@ParameterizedTest(name = "Memory can be freed (size={0}; huge={1}; contiguous={2})")
	@MethodSource("allocateSource")
	@EnabledIfRoot
	void free(final Long size, final Boolean huge, final Boolean contiguous) {
		assertThat(mmanager).isNotNull();

		// Huge memory pages are not supported
		if (huge) {
			assertThatExceptionOfType(UnsupportedOperationException.class)
					.as("Allocation")
					.isThrownBy(() -> mmanager.allocate(size, huge, contiguous));
			return;
		}

		// Allocate the memory
		val addr = mmanager.allocate(size, huge, contiguous);
		assertThat(addr).as("Address").isNotZero();

		// Free the memory
		assertThat(mmanager.free(addr, size, huge)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read")
	void getputByte() {
		assertThat(mmanager).isNotNull();
		val virt = mmanager.allocate(Byte.BYTES, false, false);
		assertThat(virt).as("Address").isNotZero();
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByte(virt, number);
		assertThat(mmanager.getByte(virt)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(virt, Byte.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary bytes can be written and read (volatile)")
	void getputByteVolatile() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Byte.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = (byte) random.nextInt(Byte.MAX_VALUE + 1);
		mmanager.putByteVolatile(addr, number);
		assertThat(mmanager.getByteVolatile(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Byte.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read")
	void getputShort() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Short.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShort(addr, number);
		assertThat(mmanager.getShort(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Short.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary shorts can be written and read (volatile)")
	void getputShortVolatile() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Short.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = (short) random.nextInt(Short.MAX_VALUE + 1);
		mmanager.putShortVolatile(addr, number);
		assertThat(mmanager.getShortVolatile(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Short.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read")
	void getputInt() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Integer.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = random.nextInt();
		mmanager.putInt(addr, number);
		assertThat(mmanager.getInt(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Integer.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary ints can be written and read (volatile)")
	void getputIntVolatile() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Integer.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = random.nextInt();
		mmanager.putIntVolatile(addr, number);
		assertThat(mmanager.getIntVolatile(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Integer.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary longs can be written and read")
	void getputLong() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Long.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = random.nextLong();
		mmanager.putLong(addr, number);
		assertThat(mmanager.getLong(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Long.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Arbitrary longs can be written and read (volatile)")
	void getputLongVolatile() {
		assertThat(mmanager).isNotNull();
		val addr = mmanager.allocate(Long.BYTES, false, false);
		assertThat(addr).as("Address").isNotZero();
		val number = random.nextLong();
		mmanager.putLongVolatile(addr, number);
		assertThat(mmanager.getLongVolatile(addr)).as("Read").isEqualTo(number);
		assertThat(mmanager.free(addr, Long.BYTES, false)).as("Freeing").isTrue();
	}

	@Test
	@DisplayName("Direct memory can be copied to JVM heap")
	void copy() {
		assertThat(mmanager).isNotNull();
		val bytes = new byte[10];
		random.nextBytes(bytes);
		val addr = mmanager.allocate(bytes.length, false, false);
		assertThat(addr).as("Address").isNotZero();
		for (var i = 0; i < bytes.length; i += 1) {
			mmanager.putByte(addr + i, bytes[i]);
		}
		val copy = new byte[bytes.length];
		mmanager.copy(addr, bytes.length, copy);
		assertThat(mmanager.free(addr, bytes.length, false)).as("Freeing").isTrue();
		assertThat(copy).isEqualTo(bytes);
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	@SuppressWarnings("deprecation")
	@DisplayName("Virtual addresses can be translated to physical addresses")
	void virt2phys() {
		assertThat(mmanager).isNotNull();
		val virt = mmanager.allocate(1, false, false);
		assertThat(virt).as("Address").isNotZero();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> mmanager.virt2phys(virt));
		assertThat(mmanager.free(virt, 1, false)).as("Freeing").isTrue();
	}

	@Test
	@EnabledIfRoot
	@DisplayName("DmaMemory cannot be allocated")
	void allocateDma() {
		assertThat(mmanager).isNotNull();
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> mmanager.dmaAllocate(1, false, false));
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
	 * The source of arguments for {@link #allocate(Long, Boolean, Boolean)}.
	 * <p>
	 * This method will generate all the combinations that could raise exceptions or behave differently.
	 *
	 * @return The {@link Stream} of {@link Arguments}.
	 */
	private static Stream<Arguments> allocateSource() {
		val pagesize = UnsafeMemoryManager.getInstance().pageSize();
		val sizes = new long[]{pagesize / 2, pagesize * 2};
		val hugity = new boolean[]{false, true};
		val contiguousity = new boolean[]{false, true};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val size : sizes) {
			for (val huge : hugity) {
				for (val contiguous : contiguousity) {
					builder.add(Arguments.of(size, huge, contiguous));
				}
			}
		}
		return builder.build();
	}

}
