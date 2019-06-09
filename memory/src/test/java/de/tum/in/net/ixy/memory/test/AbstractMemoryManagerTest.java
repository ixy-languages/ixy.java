package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.params.provider.Arguments;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static de.tum.in.net.ixy.generic.IxyMemoryManager.AllocationType;
import static de.tum.in.net.ixy.generic.IxyMemoryManager.LayoutType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Common functionality shared amongst {@link de.tum.in.net.ixy.memory.test.UnsafeMemoryManagerTest}, {@link
 * de.tum.in.net.ixy.memory.test.JniMemoryManagerTest} and {@link de.tum.in.net.ixy.memory.test.SmartMemoryManagerTest}.
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
abstract class AbstractMemoryManagerTest {

	/** The memory manager to test. */
	IxyMemoryManager mmanager;

	/**
	 * Creates dynamic tests that make sure the API checks the parameters.
	 *
	 * @param mmanager The memory manager.
	 * @return The dynamic tests.
	 */
	@Contract(value = "null -> fail; !null -> new", pure = true)
	static @NotNull Collection<DynamicTest> commonTest_parameters(@NotNull IxyMemoryManager mmanager) {
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
								assumeThat(mmanager).isNotNull();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.get(address, size, buffer, offset));
							}));
							name = String.format("Arguments are checked for getVolatile(%d, %d, %s, %d)", address, size, buff, offset);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeThat(mmanager).isNotNull();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getVolatile(address, size, buffer, offset));
							}));
							name = String.format("Arguments are checked for put(%d, %d, %s, %d)", address, size, buff, offset);
							tests.add(DynamicTest.dynamicTest(name, () -> {
								assumeThat(mmanager).isNotNull();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.put(address, size, buffer, offset));
							}));
							name = String.format("Arguments are checked for putVolatile(%d, %d, %s, %d)", address, size, buff, offset);
							tests.add(DynamicTest.dynamicTest(name, () -> {
//								assumeThat(mmanager).isNotNull();
								assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putVolatile(address, size, buffer, offset));
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
							assumeThat(mmanager).isNotNull();
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copy(src, size, dest));
						}));
						name = String.format("Parameters are checked for copyVolatile(%d, %d, %d)", src, size, dest);
						tests.add(DynamicTest.dynamicTest(name, () -> {
							assumeThat(mmanager).isNotNull();
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.copyVolatile(src, size, dest));
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
							assumeThat(mmanager).isNotNull();
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.allocate(size, allocationType, layoutType));
						}));
					}
				}
				for (val address : addresses) {
					if (address == 0 || size == 0 || allocationType == null) {
						val name = String.format("Parameters are checked for free(%d, %d, %s)", address, size, allocationType);
						tests.add(DynamicTest.dynamicTest(name, () -> {
							assumeThat(mmanager).isNotNull();
							assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.free(address, size, allocationType));
						}));
					}
				}
			}
		}

		/// Add other tests that cannot be added with a loop
		// For bytes
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getByte(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByte(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getByteVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByteVolatile(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putByte(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putByteVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByteVolatile(0L, (byte) 0));
		}));
		// For shorts
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getShort(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShort(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getShortVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShortVolatile(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putShort(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putShortVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShortVolatile(0L, (short) 0));
		}));
		// For ints
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getInt(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getInt(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getIntVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getIntVolatile(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putInt(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putIntVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putIntVolatile(0L, 0));
		}));
		// For longs
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getLong(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLong(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getLongVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLongVolatile(0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putLong(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for putLongVolatile(0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLongVolatile(0L, 0L));
		}));

		return tests;
	}

	/** Tests the method {@link IxyMemoryManager#pageSize()}. */
	@Contract(pure = true)
	final void commonTest_pageSize() {
		assumeThat(mmanager).isNotNull();
		val pagesize = mmanager.pageSize();
		assertThat(pagesize)
				.as("Page size").isPositive()
				.withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
	}

	/** Tests the method {@link IxyMemoryManager#addressSize()}. */
	@Contract(pure = true)
	final void commonTest_addressSize() {
		assumeThat(mmanager).isNotNull();
		val addrsize = mmanager.addressSize();
		assertThat(addrsize)
				.as("Address size").isPositive()
				.withFailMessage("should be a power of two").isEqualTo(addrsize & -addrsize);
	}

	/** Tests the method {@link IxyMemoryManager#hugepageSize()}. */
	@Contract(pure = true)
	final void commonTest_hugepageSize() {
		assumeThat(mmanager).isNotNull();
		val hpsz = mmanager.hugepageSize();
		assertThat(hpsz)
				.as("Huge memory page size").isPositive()
				.withFailMessage("should be a power of two").isEqualTo(hpsz & -hpsz);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getByte(long)} and {@link IxyMemoryManager#putByte(long, byte)}, or
	 * {@link IxyMemoryManager#getByteVolatile(long)}} and {@link IxyMemoryManager#putByteVolatile(long, byte)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputByte(byte number, boolean useVolatile) {
		val address = assumeAllocate(Byte.BYTES);
		// Write the data
		if (useVolatile) mmanager.putByte(address, number);
		else mmanager.putByteVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getByteVolatile(address) : mmanager.getByte(address);
		mmanager.free(address, Byte.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getShort(long)} and {@link IxyMemoryManager#putShort(long, short)}, or
	 * {@link IxyMemoryManager#getShortVolatile(long)}} and {@link IxyMemoryManager#putShortVolatile(long, short)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputShort(short number, boolean useVolatile) {
		val address = assumeAllocate(Short.BYTES);
		// Write the data
		if (useVolatile) mmanager.putShort(address, number);
		else mmanager.putShortVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getShortVolatile(address) : mmanager.getShort(address);
		mmanager.free(address, Short.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getInt(long)} and {@link IxyMemoryManager#putInt(long, int)}, or {@link
	 * IxyMemoryManager#getIntVolatile(long)}} and {@link IxyMemoryManager#putIntVolatile(long, int)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputInt(int number, boolean useVolatile) {
		val address = assumeAllocate(Integer.BYTES);
		// Write the data
		if (useVolatile) mmanager.putInt(address, number);
		else mmanager.putIntVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getIntVolatile(address) : mmanager.getInt(address);
		mmanager.free(address, Integer.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getLong(long)} and {@link IxyMemoryManager#putLong(long, long)}, or
	 * {@link IxyMemoryManager#getLongVolatile(long)}} and {@link IxyMemoryManager#putLongVolatile(long, long)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputLong(long number, boolean useVolatile) {
		val address = assumeAllocate(Long.BYTES);
		// Write the data
		if (useVolatile) mmanager.putLong(address, number);
		else mmanager.putLongVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getLongVolatile(address) : mmanager.getLong(address);
		mmanager.free(address, Long.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the methods {@link IxyMemoryManager#get(long, int, byte[], int)} and {@link IxyMemoryManager#put(long, int,
	 * byte[], int)}.
	 *
	 * @param data The data sample.
	 */
	@Contract(value = "null -> fail", pure = true)
	final void commonTest_getput(@NotNull byte[] data) {
		val address = assumeAllocate(data.length);
		// Write the data
		mmanager.put(address, data.length, data, 0);
		mmanager.put(address, 0, data, 0);
		// Recover the data from memory
		val copy = new byte[data.length];
		mmanager.get(address, copy.length, copy, 0);
		mmanager.get(address, 0, copy, 0); // Just for the coverage
		// Release the memory and verify the contents
		mmanager.free(address, copy.length, AllocationType.STANDARD);
		assertThat(copy).as("Read|Written data").isEqualTo(data);
	}

	/**
	 * Tests the methods {@link IxyMemoryManager#getVolatile(long, int, byte[], int)} and {@link
	 * IxyMemoryManager#putVolatile(long, int, byte[], int)}.
	 *
	 * @param data The data sample.
	 */
	@Contract(value = "null -> fail", pure = true)
	final void commonTest_getputVolatile(@NotNull byte[] data) {
		val address = assumeAllocate(data.length);
		// Write the data
		mmanager.putVolatile(address, data.length, data, 0);
		mmanager.putVolatile(address, 0, data, 0);
		// Recover the data from memory
		val copy = new byte[data.length];
		mmanager.getVolatile(address, copy.length, copy, 0);
		mmanager.getVolatile(address, 0, copy, 0); // Just for the coverage
		// Release the memory and verify the contents
		mmanager.free(address, copy.length, AllocationType.STANDARD);
		assertThat(copy).as("Read|Written data").isEqualTo(data);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#copy(long, int, long)}.
	 *
	 * @param data     The data sample.
	 * @param reversed Whether the addresses should be reversed.
	 */
	@Contract(value = "null, _ -> fail", pure = true)
	final void commonTest_copy(@NotNull byte[] data, boolean reversed) {
		assumeThat(mmanager).isNotNull();
		// Allocate the memory
		val addr1 = mmanager.allocate(data.length, AllocationType.STANDARD, LayoutType.STANDARD);
		val addr2 = mmanager.allocate(data.length, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(addr1).as("Address").isNotZero();
		assumeThat(addr2).as("Address").isNotZero();
		// Decide the order based on the repetition
		val src = reversed ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		val dest = reversed ? Math.max(addr1, addr2) : Math.min(addr1, addr2);
		// Write the data
		mmanager.put(src, data.length, data, 0);
		// Copy the data to another memory region and recover it from memory
		val copy = new byte[data.length];
		mmanager.copy(src, copy.length, dest);
		mmanager.get(dest, copy.length, copy, 0);
		assertThat(copy).as("Copied data").isEqualTo(data);
		// Try to copy to the same address just to get 100% coverage
		mmanager.copy(dest, copy.length, dest);
		mmanager.get(dest, copy.length, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(src, data.length, AllocationType.STANDARD);
		mmanager.free(dest, copy.length, AllocationType.STANDARD);
		assertThat(copy).as("Copied data").isEqualTo(data);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#copyVolatile(long, int, long)}.
	 *
	 * @param data     The data sample.
	 * @param reversed Whether the addresses should be reversed.
	 */
	@Contract(value = "null, _ -> fail", pure = true)
	final void commonTest_copyVolatile(@NotNull byte[] data, boolean reversed) {
		assumeThat(mmanager).isNotNull();
		// Allocate the memory
		val addr1 = mmanager.allocate(data.length, AllocationType.STANDARD, LayoutType.STANDARD);
		val addr2 = mmanager.allocate(data.length, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(addr1).as("Address").isNotZero();
		assumeThat(addr2).as("Address").isNotZero();
		// Decide the order based on the repetition
		val src = reversed ? Math.min(addr1, addr2) : Math.max(addr1, addr2);
		val dest = reversed ? Math.max(addr1, addr2) : Math.min(addr1, addr2);
		// Write the data
		mmanager.putVolatile(src, data.length, data, 0);
		// Copy the data to another memory region and recover it from memory
		val copy = new byte[data.length];
		mmanager.copyVolatile(src, copy.length, dest);
		mmanager.getVolatile(dest, copy.length, copy, 0);
		assertThat(copy).as("Copied data").isEqualTo(data);
		// Try to copy to the same address just to get 100% coverage
		mmanager.copyVolatile(dest, copy.length, dest);
		mmanager.copyVolatile(dest, 0, dest);
		mmanager.copyVolatile(src, 0, dest);
		mmanager.getVolatile(dest, copy.length, copy, 0);
		// Release the memory and verify the contents
		mmanager.free(src, data.length, AllocationType.STANDARD);
		mmanager.free(dest, copy.length, AllocationType.STANDARD);
		assertThat(copy).as("Copied data").isEqualTo(data);
	}

	/**
	 * Calls the allocate function from the memory manager and assumes some properties.
	 *
	 * @param size The data sample.
	 * @return The base address of the allocated memory region.
	 */
	@Contract(pure = true)
	final long assumeAllocate(long size) {
		assumeThat(mmanager).isNotNull();
		val address = mmanager.allocate(size, AllocationType.STANDARD, LayoutType.STANDARD);
		assumeThat(address).as("Address").isNotZero();
		return address;
	}

	/**
	 * Asserts that an arbitrary address can be written and read using {@code byte}s.
	 *
	 * @param address     The address to manipulate.
	 * @param value       The value to use.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void testWrite(long address, byte value, boolean useVolatile) {
		if (useVolatile) {
			mmanager.putByteVolatile(address, value);
			assertThat(mmanager.getByteVolatile(address)).isEqualTo(value);
		} else {
			mmanager.putByte(address, value);
			assertThat(mmanager.getByte(address)).isEqualTo(value);
		}
	}

	/**
	 * Asserts that an arbitrary address can be written and read using {@code short}s.
	 *
	 * @param address     The address to manipulate.
	 * @param value       The value to use.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void testWrite(long address, short value, boolean useVolatile) {
		if (useVolatile) {
			mmanager.putShortVolatile(address, value);
			assertThat(mmanager.getShortVolatile(address)).isEqualTo(value);
		} else {
			mmanager.putShort(address, value);
			assertThat(mmanager.getShort(address)).isEqualTo(value);
		}
	}

	/**
	 * Asserts that an arbitrary address can be written and read using {@code int}s.
	 *
	 * @param address     The address to manipulate.
	 * @param value       The value to use.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void testWrite(long address, int value, boolean useVolatile) {
		if (useVolatile) {
			mmanager.putIntVolatile(address, value);
			assertThat(mmanager.getIntVolatile(address)).isEqualTo(value);
		} else {
			mmanager.putInt(address, value);
			assertThat(mmanager.getInt(address)).isEqualTo(value);
		}
	}

	/**
	 * Asserts that an arbitrary address can be written and read using {@code long}s.
	 *
	 * @param address     The address to manipulate.
	 * @param value       The value to use.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void testWrite(long address, long value, boolean useVolatile) {
		if (useVolatile) {
			mmanager.putLongVolatile(address, value);
			assertThat(mmanager.getLongVolatile(address)).isEqualTo(value);
		} else {
			mmanager.putLong(address, value);
			assertThat(mmanager.getLong(address)).isEqualTo(value);
		}
	}

	/**
	 * Creates different combinations of arguments for the method {@link IxyMemoryManager#allocate(long, AllocationType,
	 * LayoutType)}.
	 *
	 * @param pageSize     The size of a memory page.
	 * @param hugepageSize The size of a huge memory page.
	 * @return The combination of arguments.
	 */
	@Contract(value = "_, _ -> new", pure = true)
	static @NotNull Stream<@NotNull Arguments> commonMethodSource_allocate(long pageSize, long hugepageSize) {
		AllocationType[] hugity = {AllocationType.STANDARD, AllocationType.HUGE};
		LayoutType[] contigity = {LayoutType.STANDARD, LayoutType.CONTIGUOUS};
		Stream.Builder<Arguments> builder = Stream.builder();
		for (val huge : hugity) {
			for (val contiguous : contigity) {
				val size = huge == AllocationType.HUGE && contiguous == LayoutType.CONTIGUOUS
						? hugepageSize + Long.BYTES
						: pageSize << 1;
				builder.add(Arguments.of(size, huge, contiguous));
			}
		}
		return builder.build();
	}

	/**
	 * Returns the field of a class catching any exception thrown during the process.
	 *
	 * @param cls  The class.
	 * @param name The field name.
	 * @return The field.
	 */
	@Contract(value = "null, _ -> fail; _, null -> fail; !null, !null -> new", pure = true)
	static @Nullable Field getDeclaredField(@NotNull Class<?> cls, @NotNull String name) {
		try {
			return cls.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the value of a field catching any exception thrown during the process.
	 *
	 * @param field The field.
	 * @param obj   The object.
	 * @return The field value.
	 */
	@Contract(value = "null, _ -> fail; _, null -> fail", pure = true)
	static @Nullable Object fieldGet(@NotNull Field field, @NotNull Object obj) {
		try {
			return field.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Sets the value of a field catching any exception thrown during the process.
	 *
	 * @param field The field.
	 * @param obj   The object.
	 * @param value The value.
	 */
	@Contract(value = "null, _, _ -> fail; _, null, _ -> fail; _, _, null -> fail", mutates = "param2")
	static void fieldSet(@NotNull Field field, @NotNull Object obj, @NotNull Object value) {
		try {
			field.set(obj, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns an instance of a class catching any exceptions thrown during the process.
	 *
	 * @param cls The class.
	 * @return The instance.
	 */
	@SuppressWarnings("UseOfSunClasses")
	@Contract(value = "null -> fail", pure = true)
	static @Nullable Object allocateInstance(@NotNull Class<?> cls) {
		val unsafeField = getDeclaredField(Unsafe.class, "theUnsafe");
		if (unsafeField == null) return null;
		unsafeField.setAccessible(true);
		val unsafe = (Unsafe) fieldGet(unsafeField, null);
		if (unsafe == null) return null;
		try {
			val instance = unsafe.allocateInstance(cls);
			unsafeField.setAccessible(false);
			return instance;
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}

}
