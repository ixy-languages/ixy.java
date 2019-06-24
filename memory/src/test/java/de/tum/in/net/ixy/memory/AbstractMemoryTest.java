package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.BuildConfig;
import de.tum.in.net.ixy.generic.IxyMemoryManager;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
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
 * Common functionality shared amongst {@link UnsafeMemoryManagerTest}, {@link
 * JniMemoryManagerTest} and {@link SmartMemoryManagerTest}.
 *
 * @author Esaú García Sánchez-Torija
 */
abstract class AbstractMemoryTest {

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
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutByte(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutByteVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutByteVolatile(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addByte(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addByteVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addByteVolatile(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddByte(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddByteVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddByteVolatile(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddByte(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddByteVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddByteVolatile(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetByte(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetByte(0L, (byte) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetByteVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetByteVolatile(0L, (byte) 0));
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
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutShort(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutShortVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutShortVolatile(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addShort(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addShortVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addShortVolatile(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddShort(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddShortVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddShortVolatile(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddShort(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddShortVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddShortVolatile(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetShort(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetShort(0L, (short) 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetShortVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetShortVolatile(0L, (short) 0));
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
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutInt(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutIntVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutIntVolatile(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addInt(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addIntVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addIntVolatile(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddInt(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddIntVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddIntVolatile(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddInt(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddIntVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddIntVolatile(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetInt(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetInt(0L, 0));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetIntVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetIntVolatile(0L, 0));
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
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutLong(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndPutLongVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndPutLongVolatile(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addLong(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addLongVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addLongVolatile(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddLong(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddLongVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddLongVolatile(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddLong(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for getAndAddLongVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getAndAddLongVolatile(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetLong(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetLong(0L, 0L));
		}));
		tests.add(DynamicTest.dynamicTest("Parameters are checked for addAndGetLongVolatile(0, 0)", () -> {
			assumeThat(mmanager).isNotNull();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.addAndGetLongVolatile(0L, 0L));
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
		val addrsize = mmanager.addressSize() * Byte.SIZE;
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
	 * Tests the method {@link IxyMemoryManager#getByte(long)}, {@link IxyMemoryManager#putByte(long, byte)}, {@link
	 * IxyMemoryManager#getAndPutByte(long, byte)}, {@link IxyMemoryManager#addByte(long, byte)}, {@link
	 * IxyMemoryManager#getAndAddByte(long, byte)} and {@link IxyMemoryManager#addAndGetByte(long, byte)}, or
	 * {@link IxyMemoryManager#getByteVolatile(long)}, {@link IxyMemoryManager#putByteVolatile(long, byte)}, {@link
	 * IxyMemoryManager#getAndPutByteVolatile(long, byte)}, {@link IxyMemoryManager#addByteVolatile(long, byte)}, {@link
	 * IxyMemoryManager#getAndAddByteVolatile(long, byte)} and {@link
	 * IxyMemoryManager#addAndGetByteVolatile(long, byte)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputaddByte(byte number, boolean useVolatile) {
		val address = assumeAllocate(Byte.BYTES);
		// Write the data
		if (useVolatile) mmanager.putByte(address, number);
		else mmanager.putByteVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getByteVolatile(address) : mmanager.getByte(address);
		if (useVolatile) mmanager.addByteVolatile(address, (byte) 1);
		else mmanager.addByte(address, (byte) 1);
		val added = useVolatile ? mmanager.getByteVolatile(address) : mmanager.getByte(address);
		val postadded = useVolatile ? mmanager.getAndAddByteVolatile(address, (byte) 1) : mmanager.getAndAddByte(address, (byte) 1);
		val preadded = useVolatile ? mmanager.addAndGetByteVolatile(address, (byte) 1) : mmanager.addAndGetByte(address, (byte) 1);
		val replaced = useVolatile ? mmanager.getAndPutByteVolatile(address, value) : mmanager.getAndPutByte(address, value);
		val lastValue = useVolatile ? mmanager.getByteVolatile(address) : mmanager.getByte(address);
		mmanager.free(address, Byte.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
		assertThat(added).as("Read").isEqualTo((byte) (value + 1));
		assertThat(postadded).as("Read").isEqualTo(added);
		assertThat(preadded).as("Read").isEqualTo((byte) (postadded + 2));
		assertThat(replaced).as("Read").isEqualTo(preadded);
		assertThat(lastValue).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getShort(long)}, {@link IxyMemoryManager#putShort(long, short)}, {@link
	 * IxyMemoryManager#getAndPutShort(long, short)}, {@link IxyMemoryManager#addShort(long, short)}, {@link
	 * IxyMemoryManager#getAndAddShort(long, short)} and {@link IxyMemoryManager#addAndGetShort(long, short)}, or
	 * {@link IxyMemoryManager#getShortVolatile(long)}, {@link IxyMemoryManager#putShortVolatile(long, short)}, {@link
	 * IxyMemoryManager#getAndPutShortVolatile(long, short)}, {@link IxyMemoryManager#addShortVolatile(long, short)}, {@link
	 * IxyMemoryManager#getAndAddShortVolatile(long, short)} and {@link
	 * IxyMemoryManager#addAndGetShortVolatile(long, short)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputaddShort(short number, boolean useVolatile) {
		val address = assumeAllocate(Short.BYTES);
		// Write the data
		if (useVolatile) mmanager.putShort(address, number);
		else mmanager.putShortVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getShortVolatile(address) : mmanager.getShort(address);
		if (useVolatile) mmanager.addShortVolatile(address, (short) 1);
		else mmanager.addShort(address, (short) 1);
		val added = useVolatile ? mmanager.getShortVolatile(address) : mmanager.getShort(address);
		val postadded = useVolatile ? mmanager.getAndAddShortVolatile(address, (short) 1) : mmanager.getAndAddShort(address, (short) 1);
		val preadded = useVolatile ? mmanager.addAndGetShortVolatile(address, (short) 1) : mmanager.addAndGetShort(address, (short) 1);
		val replaced = useVolatile ? mmanager.getAndPutShortVolatile(address, value) : mmanager.getAndPutShort(address, value);
		val lastValue = useVolatile ? mmanager.getShortVolatile(address) : mmanager.getShort(address);
		mmanager.free(address, Short.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
		assertThat(added).as("Read").isEqualTo((short) (value + 1));
		assertThat(postadded).as("Read").isEqualTo(added);
		assertThat(preadded).as("Read").isEqualTo((short) (postadded + 2));
		assertThat(replaced).as("Read").isEqualTo(preadded);
		assertThat(lastValue).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getInt(long)}, {@link IxyMemoryManager#putInt(long, int)}, {@link
	 * IxyMemoryManager#getAndPutInt(long, int)}, {@link IxyMemoryManager#addInt(long, int)}, {@link
	 * IxyMemoryManager#getAndAddInt(long, int)} and {@link IxyMemoryManager#addAndGetInt(long, int)}, or
	 * {@link IxyMemoryManager#getIntVolatile(long)}, {@link IxyMemoryManager#putIntVolatile(long, int)}, {@link
	 * IxyMemoryManager#getAndPutIntVolatile(long, int)}, {@link IxyMemoryManager#addIntVolatile(long, int)}, {@link
	 * IxyMemoryManager#getAndAddIntVolatile(long, int)} and {@link
	 * IxyMemoryManager#addAndGetIntVolatile(long, int)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputaddInt(int number, boolean useVolatile) {
		val address = assumeAllocate(Integer.BYTES);
		// Write the data
		if (useVolatile) mmanager.putInt(address, number);
		else mmanager.putIntVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getIntVolatile(address) : mmanager.getInt(address);
		if (useVolatile) mmanager.addIntVolatile(address, 1);
		else mmanager.addInt(address, 1);
		val added = useVolatile ? mmanager.getIntVolatile(address) : mmanager.getInt(address);
		val postadded = useVolatile ? mmanager.getAndAddIntVolatile(address, 1) : mmanager.getAndAddInt(address, 1);
		val preadded = useVolatile ? mmanager.addAndGetIntVolatile(address, 1) : mmanager.addAndGetInt(address, 1);
		val replaced = useVolatile ? mmanager.getAndPutIntVolatile(address, value) : mmanager.getAndPutInt(address, value);
		val lastValue = useVolatile ? mmanager.getIntVolatile(address) : mmanager.getInt(address);
		mmanager.free(address, Integer.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
		assertThat(added).as("Read").isEqualTo(value + 1);
		assertThat(postadded).as("Read").isEqualTo(added);
		assertThat(preadded).as("Read").isEqualTo((int) (postadded + 2));
		assertThat(replaced).as("Read").isEqualTo(preadded);
		assertThat(lastValue).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the method {@link IxyMemoryManager#getLong(long)}, {@link IxyMemoryManager#putLong(long, long)}, {@link
	 * IxyMemoryManager#getAndPutLong(long, long)}, {@link IxyMemoryManager#addLong(long, long)}, {@link
	 * IxyMemoryManager#getAndAddLong(long, long)} and {@link IxyMemoryManager#addAndGetLong(long, long)}, or
	 * {@link IxyMemoryManager#getLongVolatile(long)}, {@link IxyMemoryManager#putLongVolatile(long, long)}, {@link
	 * IxyMemoryManager#getAndPutLongVolatile(long, long)}, {@link IxyMemoryManager#addLongVolatile(long, long)}, {@link
	 * IxyMemoryManager#getAndAddLongVolatile(long, long)} and {@link
	 * IxyMemoryManager#addAndGetLongVolatile(long, long)}.
	 *
	 * @param number      The data sample.
	 * @param useVolatile Whether to use volatile methods or not.
	 */
	@Contract(pure = true)
	final void commonTest_getputaddLong(long number, boolean useVolatile) {
		val address = assumeAllocate(Long.BYTES);
		// Write the data
		if (useVolatile) mmanager.putLong(address, number);
		else mmanager.putLongVolatile(address, number);
		// Release the memory and verify the contents
		val value = useVolatile ? mmanager.getLongVolatile(address) : mmanager.getLong(address);
		if (useVolatile) mmanager.addLongVolatile(address, 1);
		else mmanager.addLong(address, 1);
		val added = useVolatile ? mmanager.getLongVolatile(address) : mmanager.getLong(address);
		val postadded = useVolatile ? mmanager.getAndAddLongVolatile(address, 1) : mmanager.getAndAddLong(address, 1);
		val preadded = useVolatile ? mmanager.addAndGetLongVolatile(address, 1) : mmanager.addAndGetLong(address, 1);
		val replaced = useVolatile ? mmanager.getAndPutLongVolatile(address, value) : mmanager.getAndPutLong(address, value);
		val lastValue = useVolatile ? mmanager.getLongVolatile(address) : mmanager.getLong(address);
		mmanager.free(address, Long.BYTES, AllocationType.STANDARD);
		assertThat(value).as("Read").isEqualTo(number);
		assertThat(added).as("Read").isEqualTo(value + 1);
		assertThat(postadded).as("Read").isEqualTo(added);
		assertThat(preadded).as("Read").isEqualTo((long) (postadded + 2));
		assertThat(replaced).as("Read").isEqualTo(preadded);
		assertThat(lastValue).as("Read").isEqualTo(number);
	}

	/**
	 * Tests the methods {@link IxyMemoryManager#get(long, int, byte[], int)} and {@link IxyMemoryManager#put(long, int,
	 * byte[], int)}.
	 *
	 * @param data The data sample.
	 */
	@Contract(pure = true)
	final void commonTest_getput(@NotNull byte[] data) {
		val address = assumeAllocate(data.length);
		// Write the data
		mmanager.put(address, data.length, data, 0);
		if (!BuildConfig.OPTIMIZED) mmanager.put(address, 0, data, 0);
		// Recover the data from memory
		val copy = new byte[data.length];
		mmanager.get(address, copy.length, copy, 0);
		if (!BuildConfig.OPTIMIZED) mmanager.get(address, 0, copy, 0); // Just for the coverage
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
	@Contract(pure = true)
	final void commonTest_getputVolatile(@NotNull byte[] data) {
		val address = assumeAllocate(data.length);
		// Write the data
		mmanager.putVolatile(address, data.length, data, 0);
		if (!BuildConfig.OPTIMIZED) mmanager.putVolatile(address, 0, data, 0);
		// Recover the data from memory
		val copy = new byte[data.length];
		mmanager.getVolatile(address, copy.length, copy, 0);
		if (!BuildConfig.OPTIMIZED) mmanager.getVolatile(address, 0, copy, 0); // Just for the coverage
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
	@Contract(pure = true)
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
	@Contract(pure = true)
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
	 * Tests the method {@link IxyMemoryManager#obj2virt(Object)}.
	 *
	 * @param object The object sample.
	 */
	@Contract(pure = true)
	final void commonTest_obj2virt(@NotNull Object object) {
		assumeThat(object).isNotNull();
		assumeThat(mmanager).isNotNull();
		assertThat(mmanager.obj2virt(object)).isNotZero();
	}

	/** Tests the method {@link IxyMemoryManager#virt2phys(long)}. */
	@Contract(pure = true)
	final void commonTest_virt2phys() {
		val virt = assumeAllocate(1);
		// Translate it, get the page size and compute the mask
		val phys = mmanager.virt2phys(virt);
		val pagesize = mmanager.pageSize();
		val mask = pagesize - 1;
		// Free up the memory and verify the memory addresses
		val softly = new SoftAssertions();
		softly.assertThat(mmanager.free(virt, 1, AllocationType.STANDARD)).as("Freeing").isTrue();
		softly.assertThat(phys).as("Physical address").isNotZero();
		softly.assertThat(pagesize).as("Page size").isPositive().withFailMessage("should be a power of two").isEqualTo(pagesize & -pagesize);
		softly.assertThat(phys & mask).as("Offset").isEqualTo(virt & mask);
		softly.assertAll();
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
