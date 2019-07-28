package de.tum.in.net.ixy.memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import lombok.val;

import org.assertj.core.api.SoftAssertions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import org.mockito.internal.matchers.Null;
import sun.misc.Unsafe;

import static de.tum.in.net.ixy.BuildConfig.DEFAULT_HUGEPAGE_PATH;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the class {@link SmartUnsafeMemoryManager}.
 *
 * @author Esaú García Sánchez-Torija
 */
@DisplayName("SmartUnsafeMemoryManager")
@Execution(ExecutionMode.CONCURRENT)
final class SmartUnsafeMemoryManagerTest {

	/** A cached instance of a pseudo-random number generator. */
	private static final Random random = new SecureRandom();

	/** A cached instance of the Unsafe-based smart memory manager. */
	private static MemoryManager mmanager;

	@BeforeAll
	static void setUp() {
		mmanager = SmartUnsafeMemoryManager.getSingleton();
	}

	@Test
	@DisplayName("isValid()")
	void isValid() {
		assumeTrue(mmanager != null);
		assertThat(mmanager.isValid()).isTrue();
	}

	@Test
	@DisplayName("getAddressSize()")
	void getAddressSize() {
		assumeTrue(mmanager != null);
		val addrSize = mmanager.getAddressSize();
		assertThat(addrSize).isPositive().withFailMessage("should be a power of two").isEqualTo(addrSize & -addrSize);
	}

	@Test
	@DisplayName("getPageSize()")
	void getPageSize() {
		assumeTrue(mmanager != null);
		val pageSize = mmanager.getPageSize();
		assertThat(pageSize).isPositive().withFailMessage("should be a power of two").isEqualTo(pageSize & -pageSize);
	}

	@Test
	@DisplayName("getHugepageSize()")
	void getHugepageSize() {
		assumeTrue(mmanager != null);
		val pageSize = mmanager.getHugepageSize();
		assertThat(pageSize).isPositive().withFailMessage("should be a power of two").isEqualTo(pageSize & -pageSize);
	}

	@Test
	@DisplayName("allocate(long, boolean, boolean) && free(long, long, boolean, boolean)")
	void allocate_free() {
		assumeTrue(mmanager != null);
		val softly = new SoftAssertions();
		for (val huge : Arrays.asList(false, true)) {
			for (val lock : Arrays.asList(false, true)) {
				val bytes = createSize(0xFFFF);
				val address = mmanager.allocate(bytes, huge, lock);
				softly.assertThat(address).isNotZero();
				mmanager.free(address, bytes, huge, lock);
			}
		}
		softly.assertAll();
	}

	@Test
	@DisplayName("mmap(File, boolean, boolean)")
	void mmap_munmap(final @TempDir Path dir) throws IOException {
		assumeTrue(mmanager != null);
		for (val huge : Arrays.asList(false)) {
			for (val lock : Arrays.asList(false, true)) {
				File file;
				if (huge) {
					file = Paths.get(DEFAULT_HUGEPAGE_PATH, "mmap.txt").toFile();
					val raf = new RandomAccessFile(file, "rwd");
					raf.setLength(30);
				} else {
					file = dir.resolve("mmap.txt").toFile();
					Files.write(file.toPath(), new byte[1]);
				}
				assertThat(file.length()).as("check size").isNotZero();
				val map = mmanager.mmap(file, huge, lock);
				val softly = new SoftAssertions();
				softly.assertThat(map).isNotZero();
				if (huge) softly.assertThat(map % mmanager.getHugepageSize()).isZero();
				if (map != 0) mmanager.munmap(map, file, huge, lock);
				file.delete();
				softly.assertAll();
			}
		}
	}

	@Test
	@DisplayName("getByte(long) && putByte(long, byte)")
	void getByte_putByte() {
		val address = assumeAllocate(Byte.BYTES);
		val delta = (byte) (1 + random.nextInt(Byte.MAX_VALUE));

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getByte(address);
		mmanager.putByte(address, delta);
		softly.assertThat(mmanager.getByte(address)).isEqualTo(delta);
		mmanager.putByte(address, (byte) (value + delta));
		softly.assertThat(mmanager.getByte(address)).isEqualTo((byte) (value + delta));
		mmanager.putByte(address, value);
		softly.assertThat(mmanager.getByte(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Byte.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getByteVolatile(long) && putByteVolatile(long, byte)")
	void getByteVolatile_putByteVolatile() {
		val address = assumeAllocate(Byte.BYTES);
		val delta = (byte) (1 + random.nextInt(Byte.MAX_VALUE));

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getByteVolatile(address);
		mmanager.putByteVolatile(address, delta);
		softly.assertThat(mmanager.getByteVolatile(address)).isEqualTo(delta);
		mmanager.putByteVolatile(address, (byte) (value + delta));
		softly.assertThat(mmanager.getByteVolatile(address)).isEqualTo((byte) (value + delta));
		mmanager.putByteVolatile(address, value);
		softly.assertThat(mmanager.getByteVolatile(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Byte.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getShort(long) && putShort(long, short)")
	void getShort_putShort() {
		val address = assumeAllocate(Short.BYTES);
		val delta = (short) (1 + random.nextInt(Short.MAX_VALUE));

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getShort(address);
		mmanager.putShort(address, delta);
		softly.assertThat(mmanager.getShort(address)).isEqualTo(delta);
		mmanager.putShort(address, (short) (value + delta));
		softly.assertThat(mmanager.getShort(address)).isEqualTo((short) (value + delta));
		mmanager.putShort(address, value);
		softly.assertThat(mmanager.getShort(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Short.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getShortVolatile(long) && putShortVolatile(long, short)")
	void getShortVolatile_putShortVolatile() {
		val address = assumeAllocate(Short.BYTES);
		val delta = (short) (1 + random.nextInt(Short.MAX_VALUE));

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getShortVolatile(address);
		mmanager.putShortVolatile(address, delta);
		softly.assertThat(mmanager.getShortVolatile(address)).isEqualTo(delta);
		mmanager.putShortVolatile(address, (short) (value + delta));
		softly.assertThat(mmanager.getShortVolatile(address)).isEqualTo((short) (value + delta));
		mmanager.putShortVolatile(address, value);
		softly.assertThat(mmanager.getShortVolatile(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Short.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getInt(long) && putInt(long, int)")
	void getInt_putInt() {
		val address = assumeAllocate(Integer.BYTES);
		val delta = 1 + random.nextInt(Integer.MAX_VALUE);

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getInt(address);
		mmanager.putInt(address, delta);
		softly.assertThat(mmanager.getInt(address)).isEqualTo(delta);
		mmanager.putInt(address, value + delta);
		softly.assertThat(mmanager.getInt(address)).isEqualTo(value + delta);
		mmanager.putInt(address, value);
		softly.assertThat(mmanager.getInt(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Integer.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getIntVolatile(long) && putIntVolatile(long, int)")
	void getIntVolatile_putIntVolatile() {
		val address = assumeAllocate(Integer.BYTES);
		val delta = 1 + random.nextInt(Integer.MAX_VALUE);

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getIntVolatile(address);
		mmanager.putIntVolatile(address, delta);
		softly.assertThat(mmanager.getIntVolatile(address)).isEqualTo(delta);
		mmanager.putIntVolatile(address, value + delta);
		softly.assertThat(mmanager.getIntVolatile(address)).isEqualTo(value + delta);
		mmanager.putIntVolatile(address, value);
		softly.assertThat(mmanager.getIntVolatile(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Integer.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getLong(long) && putLong(long, long)")
	void getLong_putLong() {
		val address = assumeAllocate(Long.BYTES);
		val delta = 1 + (random.nextLong() & Long.MAX_VALUE);

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getLong(address);
		mmanager.putLong(address, delta);
		softly.assertThat(mmanager.getLong(address)).isEqualTo(delta);
		mmanager.putLong(address, value + delta);
		softly.assertThat(mmanager.getLong(address)).isEqualTo(value + delta);
		mmanager.putLong(address, value);
		softly.assertThat(mmanager.getLong(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Long.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("getLongVolatile(long) && putLongVolatile(long, long)")
	void getLongVolatile_putLongVolatile() {
		val address = assumeAllocate(Long.BYTES);
		val delta = 1 + (random.nextLong() & Long.MAX_VALUE);

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		val value = mmanager.getLongVolatile(address);
		mmanager.putLongVolatile(address, delta);
		softly.assertThat(mmanager.getLongVolatile(address)).isEqualTo(delta);
		mmanager.putLongVolatile(address, value + delta);
		softly.assertThat(mmanager.getLongVolatile(address)).isEqualTo(value + delta);
		mmanager.putLongVolatile(address, value);
		softly.assertThat(mmanager.getLongVolatile(address)).isEqualTo(value);

		// Free the memory and test
		mmanager.free(address, Long.BYTES, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("get(long, int, byte[], int) && put(long, int, byte[], int)")
	void get_put() {
		val size = (int) createSize(0xFF);
		val buffer = new byte[size];
		val address = assumeAllocate(size);

		// Overwrite the data several times and test the value
		val softly = new SoftAssertions();
		mmanager.get(address, buffer.length, buffer, 0);

		for (var i = 0; i < buffer.length; i += 1) buffer[i] += 1;
		mmanager.put(address, buffer.length, buffer, 0);
		val copy = buffer.clone();
		mmanager.get(address, buffer.length, buffer, 0);
		softly.assertThat(buffer).isEqualTo(copy);

		for (var i = 0; i < buffer.length; i += 1) buffer[i] = 0;
		mmanager.get(address, 0, buffer, 0);
		softly.assertThat(buffer).isEqualTo(new byte[buffer.length]);

		if (!OPTIMIZED) {
			mmanager.get(address, 2, buffer, buffer.length - 1);
			val one = new byte[buffer.length];
			one[one.length - 1] = copy[0];
			softly.assertThat(buffer).isEqualTo(one);
		}

		mmanager.put(address, 0, new byte[buffer.length], 0);
		mmanager.get(address, buffer.length, buffer, 0);
		softly.assertThat(buffer).isEqualTo(copy);

		if (!OPTIMIZED) {
			mmanager.put(address, 2, new byte[buffer.length], buffer.length - 1);
			mmanager.get(address, buffer.length, buffer, 0);
			buffer[0] = copy[0];
			softly.assertThat(buffer).isEqualTo(copy);
		}

		// Free the memory and test
		mmanager.free(address, size, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("virt2phys(long)")
	void virt2phys() {
		assumeTrue(mmanager != null);
		val bytes = createSize(0xFFFF);
		val virt = mmanager.allocate(bytes, false, false);
		assertThat(virt).isNotZero();
		val phys = mmanager.virt2phys(virt);
		val softly = new SoftAssertions();
		softly.assertThat(phys).isNotZero();
		softly.assertThat(phys & (mmanager.getPageSize() - 1)).isEqualTo(virt & (mmanager.getPageSize() - 1));
		mmanager.free(virt, bytes, false, false);
		softly.assertAll();
	}

	@Test
	@DisplayName("dmaAllocate(long, boolean, boolean)")
	@SuppressWarnings({"CodeBlock2Expr", "ResultOfMethodCallIgnored"})
	void dmaAllocate() {
		assumeTrue(mmanager != null);
		for (val huge : Arrays.asList(false, true)) {
			for (val lock : Arrays.asList(false, true)) {
				val bytes = createSize(0xFFFF);
				val dma = mmanager.dmaAllocate(bytes, huge, lock);
				val softly = new SoftAssertions();
				softly.assertThat(dma.getPhysical()).isNotZero();
				softly.assertThat(dma.getPhysical() & (mmanager.getPageSize() - 1))
						.isEqualTo(dma.getVirtual() & (mmanager.getPageSize() - 1));
				mmanager.free(dma.getVirtual(), bytes, huge, lock);
				softly.assertAll();
			}
		}
	}

	//////////////////////////////////////////////// PARAMETER CHECKING ////////////////////////////////////////////////

	/**
	 * Tests the parameter checking of the class {@link UnsafeMemoryManager}.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@Nested
	@DisabledIfOptimized
	@DisplayName("SmartUnsafeMemoryManager (Parameters)")
	final class Parameters {

		/** An instance of the memory manager without {@link de.tum.in.net.ixy.utils.Unsafe the Unsafe object}. */
		private final MemoryManager invalid;

		/** Create the instance without {@link de.tum.in.net.ixy.utils.Unsafe the Unsafe object}. */
		Parameters() {
			invalid = (SmartUnsafeMemoryManager) allocateInstance(SmartUnsafeMemoryManager.class);
			if (invalid != null) {
				Field pageSizeField = null, hugepageSizeField = null;
				try {
					pageSizeField = invalid.getClass().getDeclaredField("pageSize");
					pageSizeField.setAccessible(true);
					pageSizeField.setLong(invalid, mmanager.getPageSize());
					hugepageSizeField = invalid.getClass().getDeclaredField("hugepageSize");
					hugepageSizeField.setAccessible(true);
					hugepageSizeField.setLong(invalid, mmanager.getHugepageSize());
				} catch (final IllegalAccessException | NoSuchFieldException e) {
					e.printStackTrace();
				} finally {
					if (pageSizeField != null) pageSizeField.setAccessible(false);
					if (hugepageSizeField != null) hugepageSizeField.setAccessible(false);
				}
			}
		}

		@Test
		@DisplayName("isValid()")
		void isValid() {
			assumeTrue(invalid != null);
			assertThat(invalid.isValid()).isFalse();
		}

		@Test
		@DisplayName("getPageSize()")
		@SuppressWarnings("ResultOfMethodCallIgnored")
		void getPageSize() {
			assumeTrue(invalid != null);
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(invalid::getPageSize);
		}

		@Test
		@DisplayName("getAddressSize()")
		@SuppressWarnings("ResultOfMethodCallIgnored")
		void getAddressSize() {
			assumeTrue(invalid != null);
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(invalid::getAddressSize);
		}

		@Test
		@DisplayName("getHugepageSize()")
		@SuppressWarnings("ResultOfMethodCallIgnored")
		void getHugepageSize() {
			assumeTrue(invalid != null);
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(invalid::getHugepageSize);
		}

		@Test
		@DisplayName("allocate(long, boolean, boolean)")
		@SuppressWarnings({"CodeBlock2Expr", "ResultOfMethodCallIgnored"})
		void allocate() {
			assumeTrue(mmanager != null);
			for (val huge : Arrays.asList(false, true)) {
				for (val lock : Arrays.asList(false, true)) {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.allocate(0, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.allocate(Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.allocate(0, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.allocate(Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.allocate(createSize(Long.MAX_VALUE), huge, lock);
					});
				}
			}
		}

		@Test
		@DisplayName("free(long, long, boolean, boolean)")
		@SuppressWarnings("CodeBlock2Expr")
		void free() {
			assumeTrue(mmanager != null);
			for (val huge : Arrays.asList(false, true)) {
				for (val lock : Arrays.asList(false, true)) {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.free(0, 0, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.free(0, Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.free(0, createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.free(createAddress(), 0, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.free(createAddress(), Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(0, 0, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(0, Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(0, createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(createAddress(), 0, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(createAddress(), Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.free(createAddress(), createSize(Long.MAX_VALUE), huge, lock);
					});
				}
			}
		}

		@Test
		@DisplayName("mmap(File, boolean, boolean)")
		@SuppressWarnings({"CodeBlock2Expr", "ConstantConditions", "ResultOfMethodCallIgnored"})
		void mmap(final @TempDir Path dir) throws IOException {
			assumeTrue(mmanager != null);
			val file = dir.resolve("mmap.txt").toFile();
			for (val huge : Arrays.asList(false, true)) {
				for (val lock : Arrays.asList(false, true)) {
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						mmanager.mmap(null, huge, lock);
					});
					assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> {
						mmanager.mmap(file, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.mmap(null, huge, lock);
					});
				}
			}
			assumeTrue(file.createNewFile());
			assumeTrue(file.setWritable(false));
			if (!file.canWrite()) {
				for (val huge : Arrays.asList(false, true)) {
					for (val lock : Arrays.asList(false, true)) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.mmap(file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							invalid.mmap(file, huge, lock);
						});
					}
				}
				assumeTrue(file.setWritable(true));
			}
			assumeTrue(file.setReadable(false));
			if (!file.canRead()) {
				for (val huge : Arrays.asList(false, true)) {
					for (val lock : Arrays.asList(false, true)) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.mmap(file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							invalid.mmap(file, huge, lock);
						});
					}
				}
				assumeTrue(file.setReadable(true));
			}
		}

		@Test
		@SuppressWarnings({"CodeBlock2Expr", "ConstantConditions"})
		@DisplayName("munmap(long, File, boolean, boolean)")
		void munmap(final @TempDir Path dir) throws IOException {
			assumeTrue(mmanager != null);
			val address = createAddress();
			val file = dir.resolve("munmap.txt").toFile();
			for (val huge : Arrays.asList(false, true)) {
				for (val lock : Arrays.asList(false, true)) {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.munmap(0, null, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.munmap(0, file, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						mmanager.munmap(address, null, huge, lock);
					});
					assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> {
						mmanager.munmap(address, file, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						invalid.munmap(0, null, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						invalid.munmap(0, file, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.munmap(address, null, huge, lock);
					});
					assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> {
						invalid.munmap(address, file, huge, lock);
					});
				}
			}
			assumeTrue(file.createNewFile());
			assumeTrue(file.setWritable(false));
			if (!file.canWrite()) {
				for (val huge : Arrays.asList(false, true)) {
					for (val lock : Arrays.asList(false, true)) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.munmap(0, file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.munmap(address, file, huge, lock);
						});
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
							invalid.munmap(0, file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							invalid.munmap(address, file, huge, lock);
						});
					}
				}
				assumeTrue(file.setWritable(true));
			}
			assumeTrue(file.setReadable(false));
			if (!file.canRead()) {
				for (val huge : Arrays.asList(false, true)) {
					for (val lock : Arrays.asList(false, true)) {
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.munmap(0, file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							mmanager.munmap(address, file, huge, lock);
						});
						assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
							invalid.munmap(0, file, huge, lock);
						});
						assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
							invalid.munmap(address, file, huge, lock);
						});
					}
				}
				assumeTrue(file.setReadable(true));
			}
		}

		@Test
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@DisplayName("getByte(0) && getByteVolatile(0)")
		void getByte_getByteVolatile() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByte(0));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getByteVolatile(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getByte(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getByteVolatile(0));
		}

		@Test
		@DisplayName("putByte(0, byte) && putByteVolatile(0, byte)")
		void putByte_putByteVolatile() {
			val value = (byte) random.nextInt(0xFF);
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByte(0, value));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putByteVolatile(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putByte(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putByteVolatile(0, value));
		}

		@Test
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@DisplayName("getShort(0) && getShortVolatile(0)")
		void getShort_getShortVolatile() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShort(0));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getShortVolatile(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getShort(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getShortVolatile(0));
		}

		@Test
		@DisplayName("putShort(0, short) && putShortVolatile(0, short)")
		void putShort_putShortVolatile() {
			val value = (short) random.nextInt(0xFFFF);
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShort(0, value));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putShortVolatile(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putShort(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putShortVolatile(0, value));
		}

		@Test
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@DisplayName("getInt(0) && getIntVolatile(0)")
		void getInt_getIntVolatile() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getInt(0));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getIntVolatile(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getInt(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getIntVolatile(0));
		}

		@Test
		@DisplayName("putInt(0, int) && putIntVolatile(0, int)")
		void putInt_putIntVolatile() {
			val value = random.nextInt();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putInt(0, value));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putIntVolatile(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putInt(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putIntVolatile(0, value));
		}

		@Test
		@SuppressWarnings("ResultOfMethodCallIgnored")
		@DisplayName("getLong(0) && getLongVolatile(0)")
		void getLong_getLongVolatile() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLong(0));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.getLongVolatile(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getLong(0));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.getLongVolatile(0));
		}

		@Test
		@DisplayName("putLong(0, long) && putLongVolatile(0, long)")
		void putLong_putLongVolatile() {
			val value = random.nextLong();
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLong(0, value));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.putLongVolatile(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putLong(0, value));
			assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> invalid.putLongVolatile(0, value));
		}

		@Test
		@DisplayName("get(long, int, byte[], int)")
		@SuppressWarnings({"CodeBlock2Expr", "ConstantConditions"})
		void get() {
			val bytes = 2 + (int) createSize(0xFF);
			val buffer = new byte[bytes];

			// Prepare the arguments
			final long[] args1 = {createAddress(), 0};
			final int[] args2 = {random.nextInt(buffer.length), (int) -createSize(Integer.MAX_VALUE)};
			final byte[][] args3 = {buffer, null};
			final int[] args4 = {
					random.nextInt(buffer.length),
					-random.nextInt(Integer.MAX_VALUE) - 1,
					buffer.length
			};

			// Try all combinations
			for (var i = 0; i < args1.length; i += 1) {
				for (var j = 0; j < args2.length; j += 1) {
					for (var k = 0; k < args3.length; k += 1) {
						for (var l = 0; l < args4.length; l += 1) {
							val fi = i;
							val fj = j;
							val fk = k;
							val fl = l;
							assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
								invalid.get(args1[fi], args2[fj], args3[fk], args4[fl]);
							});
							var klass = (Class<? extends RuntimeException>) null;
							if (i + j + k + l == 0) continue;
							else if (i + j != 0) klass = IllegalArgumentException.class;
							else if (k != 0) klass = NullPointerException.class;
							else klass = ArrayIndexOutOfBoundsException.class;
							assertThatExceptionOfType(klass).isThrownBy(() -> {
								mmanager.get(args1[fi], args2[fj], args3[fk], args4[fl]);
							});
						}
					}
				}
			}
		}

		@Test
		@DisplayName("put(long, int, byte[], int)")
		@SuppressWarnings({"CodeBlock2Expr", "ConstantConditions"})
		void put() {
			val bytes = 2 + (int) createSize(0xFF);
			val buffer = new byte[bytes];

			// Prepare the arguments
			final long[] args1 = {createAddress(), 0};
			final int[] args2 = {random.nextInt(buffer.length), (int) -createSize(Integer.MAX_VALUE)};
			final byte[][] args3 = {buffer, null};
			final int[] args4 = {
					random.nextInt(buffer.length),
					-random.nextInt(Integer.MAX_VALUE) - 1,
					buffer.length
			};

			// Try all combinations
			for (var i = 0; i < args1.length; i += 1) {
				for (var j = 0; j < args2.length; j += 1) {
					for (var k = 0; k < args3.length; k += 1) {
						for (var l = 0; l < args4.length; l += 1) {
							val fi = i;
							val fj = j;
							val fk = k;
							val fl = l;
							assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
								invalid.put(args1[fi], args2[fj], args3[fk], args4[fl]);
							});
							var klass = (Class<? extends RuntimeException>) null;
							if (i + j + k + l == 0) continue;
							else if (i + j != 0) klass = IllegalArgumentException.class;
							else if (k != 0) klass = NullPointerException.class;
							else klass = ArrayIndexOutOfBoundsException.class;
							assertThatExceptionOfType(klass).isThrownBy(() -> {
								mmanager.put(args1[fi], args2[fj], args3[fk], args4[fl]);
							});
						}
					}
				}
			}
		}

		@Test
		@DisplayName("virt2phys(long)")
		@SuppressWarnings("ResultOfMethodCallIgnored")
		void virt2phys() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mmanager.virt2phys(0));
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> invalid.virt2phys(0));
		}

		@Test
		@DisplayName("dmaAllocate(long, boolean, boolean)")
		@SuppressWarnings({"CodeBlock2Expr", "ResultOfMethodCallIgnored"})
		void dmaAllocate() {
			assumeTrue(mmanager != null);
			for (val huge : Arrays.asList(false, true)) {
				for (val lock : Arrays.asList(false, true)) {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.dmaAllocate(0, huge, lock);
					});
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
						mmanager.dmaAllocate(Long.MIN_VALUE + createSize(Long.MAX_VALUE), huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.dmaAllocate(0, huge, lock);
					});
					assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
						invalid.dmaAllocate(Long.MIN_VALUE - createSize(Long.MAX_VALUE), huge, lock);
					});
				}
			}
		}

	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Calls the allocate function from the memory manager and assumes some properties.
	 *
	 * @param size The data sample.
	 * @return The base address of the allocated memory region.
	 */
	@Contract(pure = true)
	private long assumeAllocate(final @Range(from = 0, to = Long.MAX_VALUE) long size) {
		assumeTrue(mmanager != null);
		val address = mmanager.allocate(size, false, false);
		assumeTrue(address != 0);
		return address;
	}

	/**
	 * Creates a valid size given a mask to apply.
	 *
	 * @param mask The mask.
	 * @return A valid size.
	 */
	@Contract(pure = true)
	private long createSize(final long mask) {
		var bytes = 0L;
		while (bytes == 0) {
			while (bytes <= 0) {
				bytes = random.nextLong();
			}
			bytes &= mask;
		}
		return bytes;
	}

	/**
	 * Creates a valid address.
	 *
	 * @return A valid address.
	 */
	@Contract(pure = true)
	private long createAddress() {
		var address = 0L;
		while (address == 0) {
			address = random.nextLong();
		}
		return address;
	}

	/**
	 * Returns the field of a class catching any exception thrown during the process.
	 *
	 * @param cls  The class.
	 * @param name The field name.
	 * @return The field.
	 */
	@Contract(pure = true)
	@SuppressWarnings("SameParameterValue")
	private static @Nullable Field getDeclaredField(final @NotNull Class<?> cls, final @NotNull String name) {
		try {
			return cls.getDeclaredField(name);
		} catch (final NoSuchFieldException e) {
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
	@Contract(pure = true)
	@SuppressWarnings("SameParameterValue")
	private static @Nullable Object fieldGet(final @NotNull Field field, final @NotNull Object obj) {
		try {
			return field.get(obj);
		} catch (final IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns an instance of a class catching any exceptions thrown during the process.
	 *
	 * @param cls The class.
	 * @return The instance.
	 */
	@SuppressWarnings({"ConstantConditions", "SameParameterValue", "UseOfSunClasses"})
	@Contract(value = "null -> fail", pure = true)
	private static @Nullable Object allocateInstance(@NotNull Class<?> cls) {
		val unsafeField = getDeclaredField(Unsafe.class, "theUnsafe");
		if (unsafeField == null) return null;
		unsafeField.setAccessible(true);
		val unsafe = (Unsafe) fieldGet(unsafeField, null);
		if (unsafe == null) return null;
		try {
			val instance = unsafe.allocateInstance(cls);
			unsafeField.setAccessible(false);
			return instance;
		} catch (final InstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}

}
