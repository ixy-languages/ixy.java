package de.tum.in.net.ixy.pci.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import de.tum.in.net.ixy.pci.Pci;

/**
 * Checks the class {@link Pci}.
 * <p>
 * All the tests of this test suite can be executed randomly or concurrently, except for the {@code dma} tests. Where a
 * {@code get} has to be tested after each {@code set}.
 */
@Slf4j
@DisplayName("PCI device access (read & write)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PciTest {

	/** The expected vendor id of the ixgbe devices. */
	private static final short EXPECTED_VENDOR_IXGBE = (short) 0x8086;

	/** The expected vendor id of the Virtio devices. */
	private static final short EXPECTED_VENDOR_VIRTIO = (short) 0x1af4;

	/** The expected device id of the Virtio devices. */
	private static final short EXPECTED_DEVICE_VIRTIO = (short) 0x1000;

	/** The expected class id, doesn't matter which network card we check. */
	private static final byte EXPECTED_CLASS = (byte) 0x02;

	/**
	 * Checks that the vendor id of the Virtio devices is correct.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#getVendorId(String)
	 */
	@ParameterizedTest(name = "[static] Vendor id of {0} should be " + EXPECTED_VENDOR_VIRTIO + " (Red Hat, Inc)")
	@MethodSource("pciSource")
	void getVendorId(@NonNull final String pciDevice) throws IOException {
		try {
			assertEquals(EXPECTED_VENDOR_VIRTIO, Pci.getVendorId(pciDevice), "vendor id should be correct");
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the device id of the Virtio devices is correct.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#getDeviceId(String)
	 */
	@ParameterizedTest(name = "[static] Device id of {0} should be " + EXPECTED_DEVICE_VIRTIO + " (Virtio network device)")
	@MethodSource("pciSource")
	void getDeviceId(@NonNull final String pciDevice) throws IOException {
		try {
			assertEquals(EXPECTED_DEVICE_VIRTIO, Pci.getDeviceId(pciDevice), "device id should be correct");
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the class id of the Virtio devices is correct.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#getClassId(String)
	 */
	@ParameterizedTest(name = "[static] Class id of {0} should be " + EXPECTED_CLASS + " (Virtio network device)")
	@MethodSource("pciSource")
	void getClassId(@NonNull final String pciDevice) throws IOException {
		try {
			assertEquals(EXPECTED_CLASS, Pci.getClassId(pciDevice), "class id should be correct");
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the driver can be unbound.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#unbindDriver(String)
	 */
	@Order(0)
	@ParameterizedTest(name = "[static] PCI device {0} driver can be unbound")
	@MethodSource("pciSource")
	void unbindDriver(@NonNull final String pciDevice) throws IOException {
		try {
			Pci.unbindDriver(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the driver can be bound.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#bindDriver(String)
	 */
	@Order(1)
	@ParameterizedTest(name = "[static] PCI device {0} driver can be bound")
	@MethodSource("pciSource")
	void bindDriver(@NonNull final String pciDevice) throws IOException {
		try {
			Pci.bindDriver(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the DMA can be enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#enableDma(String)
	 */
	@Order(2)
	@ParameterizedTest(name = "[static] PCI device {0} DMA can be enabled")
	@MethodSource("pciSource")
	void enableDma(@NonNull final String pciDevice) throws IOException {
		try {
			Pci.enableDma(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#isDmaEnabled(String)
	 */
	@Order(3)
	@ParameterizedTest(name = "[static] PCI device {0} DMA is enabled check")
	@MethodSource("pciSource")
	void isDmaEnabled(@NonNull final String pciDevice) throws IOException {
		try {
			assertTrue(Pci.isDmaEnabled(pciDevice), "DMA should be enabled");
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the DMA can be disabled.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#disableDma(String)
	 */
	@Order(4)
	@ParameterizedTest(name = "[static] PCI device {0} DMA can be disabled")
	@MethodSource("pciSource")
	void disableDma(@NonNull final String pciDevice) throws IOException {
		try {
			Pci.disableDma(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see Pci#isDmaEnabled(String)
	 */
	@Order(5)
	@ParameterizedTest(name = "[static] PCI device {0} DMA is disabled check")
	@MethodSource("pciSource")
	void isDmaDisabled(@NonNull final String pciDevice) throws IOException {
		try {
			assertFalse(Pci.isDmaEnabled(pciDevice), "DMA should be disabled");
		} catch (FileNotFoundException e) {
			log.warn("The PCI device does not exist", e);
		}
	}

	/** Checks the non-static methods of the class {@link Pci}. */
	static class NonStatic {

		/** Checks that creating a new instance with an invalid PCI device throws a {@link FileNotFoundException}. */
		@Test
		@DisplayName("Invalid PCI device throws FileNotFoundException")
		void constructorException() throws IOException {
			val exception = assertThrows(FileNotFoundException.class, () -> new Pci(""));
			// assertThat(exception.getMessage(), matchesRegex("^.*config \\(No such file or directory\\)$"));
		}

		/**
		 * Checks that the vendor id of the Virtio devices is correct.
		 *
		 * @param pci A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#getVendorId()
		 */
		@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR_VIRTIO + " (Red Hat, Inc)")
		@MethodSource("pciSource")
		void getVendorId(@NonNull final Pci pci) throws IOException {
			assertEquals(EXPECTED_VENDOR_VIRTIO, pci.getVendorId(), "vendor id should be correct");
		}

		/**
		 * Checks that the device id of the Virtio devices is correct.
		 *
		 * @param pci A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#getDeviceId()
		 */
		@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE_VIRTIO + " (Virtio network device)")
		@MethodSource("pciSource")
		void getDeviceId(@NonNull final Pci pci) throws IOException {
			assertEquals(EXPECTED_DEVICE_VIRTIO, pci.getDeviceId(), "device id should be correct");
		}

		/**
		 * Checks that the class id of the Virtio devices is correct.
		 *
		 * @param pci A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#getClassId()
		 */
		@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (Virtio network device)")
		@MethodSource("pciSource")
		void getClassId(@NonNull final Pci pci) throws IOException {
			assertEquals(EXPECTED_CLASS, pci.getClassId(), "class id should be correct");
		}

		/**
		 * Checks that the driver can be unbound.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#unbindDriver()
		 */
		@Order(6)
		@ParameterizedTest(name = "PCI device {0} driver can be unbound")
		@MethodSource("pciSource")
		void unbindDriver(@NonNull final Pci pci) throws IOException {
			pci.unbindDriver();
		}

		/**
		 * Checks that the driver can be bound.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#bindDriver()
		 */
		@Order(7)
		@ParameterizedTest(name = "PCI device {0} driver can be bound")
		@MethodSource("pciSource")
		void bindDriver(@NonNull final Pci pci) throws IOException {
			pci.bindDriver();
		}

		/**
		 * Checks that the DMA can be enabled.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#enableDma()
		 */
		@Order(8)
		@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
		@MethodSource("pciSource")
		void enableDma(@NonNull final Pci pci) throws IOException {
			pci.enableDma();
		}

		/**
		 * Checks that the DMA is enabled.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#isDmaEnabled()
		 */
		@Order(9)
		@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
		@MethodSource("pciSource")
		@Disabled
		void isDmaEnabled(@NonNull final Pci pci) throws IOException {
			assertTrue(pci.isDmaEnabled(), "DMA should be enabled");
		}

		/**
		 * Checks that the DMA can be enabled.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#disableDma()
		 */
		@Order(10)
		@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
		@MethodSource("pciSource")
		@Disabled
		void disableDma(@NonNull final Pci pci) throws IOException {
			pci.disableDma();
		}

		/**
		 * Checks that the DMA is disabled.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#isDmaEnabled()
		 */
		@Order(11)
		@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
		@MethodSource("pciSource")
		@Disabled
		void isDmaDisabled(@NonNull final Pci pci) throws IOException {
			assertFalse(pci.isDmaEnabled(), "DMA should be disabled");
		}

		/**
		 * Source of {@link Pci} instances.
		 * <p>
		 * This source uses the {@link PciTest#pciSource()} as input and tries to create a {@link Pci} instance for each
		 * PCI device. If the instantiation throws a {@link FileNotFoundException}, that device is discarded and is not
		 * included in the output stream.
		 *
		 * @return A {@link Stream} of valid {@link Pci} instances.
		 * @see PciTest#pciSource()
		 * @see PciTest#newPci(String)
		 */
		@NotNull
		private static Stream<Pci> pciSource() {
			return PciTest.pciSource()
					.map(PciTest::newPci)
					.filter(Objects::nonNull);
		}

	}

	/////////////////////////////////////////////////// STATIC UTILS ///////////////////////////////////////////////////

	/**
	 * Source of PCI devices.
	 * <p>
	 * Reads the environment variable {@code IXY_VIRTIO_COUNT} and creates a {@link Stream<String>} containing all the
	 * names.
	 * <p>
	 * This method does not filter the PCI devices by checking the existence of the devices themselves, it only discards
	 * {@code null} or empty devices. Therefore, there is no guarantee that creating an instance of {@link Pci} will not
	 * throw a {@link FileNotFoundException}.
	 *
	 * @return A {@link Stream} of PCI devices.
	 */
	@NotNull
	private static Stream<String> pciSource() {
		val count = System.getenv("IXY_VIRTIO_COUNT");
		return IntStream.range(1, 1 + Integer.parseInt(count))
				.mapToObj(i -> System.getenv("IXY_VIRTIO_ADDR_" + i))
				.filter(Objects::nonNull)
				.filter(it -> !it.isEmpty());
	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Creates a {@link Pci} instance and handles any checked exception.
	 * <p>
	 * If the given PCI device does not exist, {@code null} is returned.
	 *
	 * @param pciDevice The PCI device.
	 * @return The {@link Pci} instance.
	 * @throws IOException If there is an I/O error while guessing the driver of the PCI device.
	 * @see Pci#Pci(String)
	 */
	private static Pci newPci(final String pciDevice) throws IOException {
		try {
			return new Pci(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("Could not create PCI instance", e);
		}
		return null;
	}

}
