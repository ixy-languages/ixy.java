package de.tum.in.net.ixy.pci;

import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests the class {@link Device}.
 *
 * @author Esaú García Sánchez-Torija
 */
@EnabledOnOs(OS.LINUX)
@DisplayName("Device (Ixgbe)")
@Execution(ExecutionMode.SAME_THREAD)
@SuppressWarnings("JUnitTestMethodWithNoAssertions")
final class IxgbePciTest {

	/** The name of the environment variable that counts how many Ixgbe PCI devices exist. */
	private static final @NotNull String ENV_KEY_NIC_COUNT = "IXY_IXGBE_COUNT";

	/** The name of the environment variable that holds the address of a Ixgbe PCI device. */
	private static final @NotNull String ENV_KEY_NIC_ADDR = "IXY_IXGBE_ADDR_";

	/** The name of the driver the PCI devices should use. */
	private static final @NotNull String DRIVER = "ixgbe";

	/** The pattern a PCI device must match. */
	private static final @NotNull Pattern PCI_NAME_PATTERN = Pattern.compile("^\\d{4}:\\d{2}:\\d{2}\\.\\d$");

	/** The expected vendor identifier. */
	private static final short EXPECTED_VENDOR = (short) 0x8086;

	/** The expected device identifiers. */
	private static final @NotNull Set<Short> EXPECTED_DEVICES = Set.of(
			// Physical devices
			(short) 0x10B6, (short) 0x1508, (short) 0x10C6, (short) 0x10C7,
			(short) 0x10C8, (short) 0x150B, (short) 0x10DB, (short) 0x10DD,
			(short) 0x10EC, (short) 0x10F1, (short) 0x10E1, (short) 0x10F4,
			(short) 0x10F7, (short) 0x1514, (short) 0x1517, (short) 0x10F8,
			(short) 0x000C, (short) 0x10F9, (short) 0x10FB, (short) 0x11A9,
			(short) 0x1F72, (short) 0x17D0, (short) 0x0470, (short) 0x152A,
			(short) 0x1529, (short) 0x1507, (short) 0x154D, (short) 0x154A,
			(short) 0x1558, (short) 0x1557, (short) 0x10FC, (short) 0x151C,
			(short) 0x154F, (short) 0x1528, (short) 0x1560, (short) 0x15AC,
			(short) 0x15AD, (short) 0x15AE, (short) 0x1563, (short) 0x15AA,
			(short) 0x15AB,
			// Virtual devices
			(short) 0x10ED, (short) 0x152E, (short) 0x1515, (short) 0x1530,
			(short) 0x1564, (short) 0x1565, (short) 0x15A8, (short) 0x15A9
	);

	/** The expected class. */
	private static final byte EXPECTED_CLASS = 0x02;

	/** The expected message of the exception thrown when the user does not have sufficient permissions. */
	private static final @NotNull String EXPECTED_SEC_MESSAGE = "Permission denied";

	@Nested
	@DisabledIfOptimized
	@SuppressWarnings("InnerClassMayBeStatic")
	@DisplayName("Device (Ixgbe) (Parameters)")
	final class Parameters {

		@Test
		@DisplayName("Construction fails with wrong parameters")
		void Pci_exceptions() {
			String[] wrong = {null, "", " ", "-"};
			for (val device : wrong) {
				for (val driver : wrong) {
					// Compute the exception class to be thrown, if any
					Class<? extends Exception> exceptionClass;
					if (device == null || driver == null) {
						exceptionClass = NullPointerException.class;
					} else if (BuildConfig.OPTIMIZED) {
						exceptionClass = FileNotFoundException.class;
					} else if (!Objects.equals(device, "-") || !Objects.equals(driver, "-")) {
						exceptionClass = IllegalArgumentException.class;
					} else {
						exceptionClass = FileNotFoundException.class;
					}
					assertThatExceptionOfType(exceptionClass).isThrownBy(() -> new DummyDevice(device, driver));
				}
			}
		}

	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The vendor id should be correct")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void getVendorId(Device device) {
		CommonPciTest.getVendorId(device, EXPECTED_VENDOR);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The device id should be correct")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void getDeviceId(Device device) {
		CommonPciTest.getDeviceId(device, EXPECTED_DEVICES);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The class id should be correct")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void getClassId(Device device) {
		CommonPciTest.getClassId(device, EXPECTED_CLASS);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be true")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void isDmaEnabled(Device device) {
		assertThat(device).isNotNull();
		assertDoesNotThrow(device::enableDma);
		CommonPciTest.isDmaEnabled(device, true);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be false")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void isDmaDisabled(Device device) {
		assertThat(device).isNotNull();
		assertDoesNotThrow(device::disableDma);
		CommonPciTest.isDmaEnabled(device, false);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Can be mapped to memory")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void map(Device device) {
		CommonPciTest.map(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Can be closed")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void close(Device device) {
		CommonPciTest.close(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Manipulating the device after close fails")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void close_exceptions(Device device) {
		CommonPciTest.close_exceptions(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Binding when already bound fails")
	@MethodSource("pciSource")
	@EnabledIfIxgbe
	@EnabledIfRoot
	void bindunbind(Device device) {
		CommonPciTest.bindunbind(device);
	}

	/**
	 * Source of {@link Device} instances.
	 *
	 * @return A {@link Stream<Device>} of valid {@link Device} instances.
	 */
	private static @NotNull Stream<Device> pciSource() {
		if (ixyCount() > 0) {
			val original = addressSource().map(IxgbePciTest::newPci).filter(Optional::isPresent).map(Optional::get);
			val count = addressSource().map(IxgbePciTest::newPci).filter(Optional::isPresent).count();
			return (count > 0) ? original : Stream.concat(original, Stream.of((Device) null));
		}
		return Stream.of((Device) null);
	}

	/**
	 * Source of {@code Ixgbe} PCI devices.
	 *
	 * @return The PCI devices.
	 */
	private static @NotNull Stream<String> addressSource() {
		if (ixyCount() > 0) {
			return IntStream.range(1, 1 + ixyCount())
					.mapToObj(i -> System.getenv(ENV_KEY_NIC_ADDR + i))
					.filter(Objects::nonNull)
					.filter(it -> !it.isEmpty())
					.filter(it -> PCI_NAME_PATTERN.matcher(it).matches());
		}
		return Stream.of((String) null);
	}

	/**
	 * Creates a {@link Device} instance and handles any checked exception.
	 *
	 * @param device The PCI device.
	 * @return The {@link Device} instance.
	 */
	private static @NotNull Optional<@Nullable Device> newPci(@NonNull String device) {
		try {
			return Optional.of(new DummyDevice(device, DRIVER));
		} catch (FileNotFoundException e) {
			val message = e.getMessage();
			if (message == null) {
				throw new RuntimeException(e);
			} else if (!Objects.equals(message, EXPECTED_SEC_MESSAGE)) {
				e.printStackTrace();
			}
			return Optional.empty();
		}
	}

	/**
	 * Parses the environment variable {@link #ENV_KEY_NIC_COUNT} into an {@code int}.
	 *
	 * @return The number of Ixgbe devices available.
	 */
	private static int ixyCount() {
		val count = System.getenv(ENV_KEY_NIC_COUNT);
		try {
			return Integer.parseInt(count);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
