package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.BuildConfig;
import de.tum.in.net.ixy.pci.Device;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
@DisplayName("Device (VirtIO)")
final class VirtioPciTest {

	/** The name of the environment variable that counts how many VirtIO PCI devices exist. */
	private static final @NotNull String ENV_KEY_NIC_COUNT = "IXY_VIRTIO_COUNT";

	/** The name of the environment variable that holds the address of a VirtIO PCI device. */
	private static final @NotNull String ENV_KEY_NIC_ADDR = "IXY_VIRTIO_ADDR_";

	/** The name of the driver the PCI devices should use. */
	private static final @NotNull String DRIVER = "virtio-pci";

	/** The pattern a PCI device must match. */
	private static final @NotNull Pattern PCI_NAME_PATTERN = Pattern.compile("^\\d{4}:\\d{2}:\\d{2}\\.\\d$");

	/** The expected vendor identifier. */
	private static final short EXPECTED_VENDOR = 0x1af4;

	/** The expected device identifiers. */
	private static final Set<Short> EXPECTED_DEVICES = Set.of((short) 0x1000);

	/** The expected class. */
	private static final byte EXPECTED_CLASS = 0x02;

	/** The expected message of the exception thrown by the binding and unbinding methods. */
	private static final String EXPECTED_BIND_MESSAGE = "No such device";

	/** The expected message of the exception thrown when the user does not have sufficient permissions. */
	private static final @NotNull String EXPECTED_SEC_MESSAGE = "Permission denied";

	@BeforeAll
	@DisplayName("All NICs can be bound before starting the tests")
	static void setUp() {
		pciSource().forEach(pci -> {
			try {
				if (pci != null) pci.bind();
			} catch (IOException e) {
				val message = e.getMessage();
				if (message == null) {
					throw new RuntimeException(e);
				} else if (!Objects.equals(message, EXPECTED_BIND_MESSAGE) && !Objects.equals(message, EXPECTED_SEC_MESSAGE)) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Test
	@DisplayName("Construction fails with wrong parameters")
	void Pci_exceptions() {
		String[] wrong = {null, "", " ", File.separator};
		for (val device : wrong) {
			for (val driver : wrong) {
				if (Objects.equals(device, File.separator) && Objects.equals(driver, File.separator)) {
					assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> new DummyDevice(device, driver));
				} else {
					assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new DummyDevice(device, driver));
				}
			}
		}
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The vendor id should be correct")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getVendorId(Device device) {
		CommonPciTest.getVendorId(device, EXPECTED_VENDOR);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The device id should be correct")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getDeviceId(Device device) {
		CommonPciTest.getDeviceId(device, EXPECTED_DEVICES);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] The class id should be correct")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getClassId(Device device) {
		CommonPciTest.getClassId(device, EXPECTED_CLASS);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be true")
	@MethodSource("pciSource")
	@EnabledIfVirtio
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
	@EnabledIfVirtio
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
	@EnabledIfVirtio
	@EnabledIfRoot
	void map(Device device) {
		CommonPciTest.map(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Can be closed")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void close(Device device) {
		CommonPciTest.close(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Manipulating the device after close fails")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void close_exceptions(Device device) {
		CommonPciTest.close_exceptions(device);
	}

	@ResourceLock(value = BuildConfig.LOCK_NIC, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = BuildConfig.LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Binding when already bound fails")
	@MethodSource("pciSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	void bindunbind(Device device) {
		CommonPciTest.bindunbind(device);
	}

	@AfterAll
	@DisplayName("All NICs can be bound after finishing the tests")
	static void tearDown() {
		pciSource().forEach(pci -> {
			try {
				if (pci != null) pci.bind();
			} catch (IOException e) {
				val message = e.getMessage();
				if (message == null) {
					throw new RuntimeException(e);
				} else if (!Objects.equals(message, EXPECTED_BIND_MESSAGE) && !Objects.equals(message, EXPECTED_SEC_MESSAGE)) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * Source of {@link Device} instances.
	 *
	 * @return A {@link Stream<Device>} of valid {@link Device} instances.
	 */
	private static @NotNull Stream<Device> pciSource() {
		if (ixyCount() > 0) {
			val original = addressSource().map(VirtioPciTest::newPci).filter(Optional::isPresent).map(Optional::get);
			val count = addressSource().map(VirtioPciTest::newPci).filter(Optional::isPresent).count();
			return (count > 0) ? original : Stream.concat(original, Stream.of((Device) null));
		}
		return Stream.of((Device) null);
	}

	/**
	 * Source of {@code VirtIO} PCI devices.
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
	 * @return The number of VirtIO devices available.
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
