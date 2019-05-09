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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
class PciTest {

	/** The expected class id. */
	private static final byte EXPECTED_CLASS = (byte) 0x02;

	/** Tests the Pci implementation with Virtio devices. */
	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class Virtio {

		/** The expected vendor id. */
		private static final short EXPECTED_VENDOR = (short) 0x1af4;

		/** The expected device id. */
		private static final short EXPECTED_DEVICE = (short) 0x1000;

		/**
		 * Checks that the vendor id is correct.
		 *
		 * @param pciDevice A PCI device.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#getVendorId(String)
		 */
		@ParameterizedTest(name = "[static] Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
		@MethodSource("pciSource")
		void getVendorId(@NonNull final String pciDevice) throws IOException {
			try {
				assertEquals(EXPECTED_VENDOR, Pci.getVendorId(pciDevice), "vendor id should be correct");
			} catch (FileNotFoundException e) {
				log.warn("The PCI device does not exist", e);
			}
		}

		/**
		 * Checks that the device id is correct.
		 *
		 * @param pciDevice A PCI device.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#getDeviceId(String)
		 */
		@ParameterizedTest(name = "[static] Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
		@MethodSource("pciSource")
		void getDeviceId(@NonNull final String pciDevice) throws IOException {
			try {
				assertEquals(EXPECTED_DEVICE, Pci.getDeviceId(pciDevice), "device id should be correct");
			} catch (FileNotFoundException e) {
				log.warn("The PCI device does not exist", e);
			}
		}

		/**
		 * Checks that the class id is correct.
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

		/** Checks the non-static methods. */
		@Nested
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		class NonStatic {

			/**
			 * Checks that creating a new instance with an invalid PCI device throws a {@link FileNotFoundException}.
			 */
			@Test
			@DisplayName("Invalid PCI device throws FileNotFoundException")
			void constructorException() throws IOException {
				val exception = assertThrows(FileNotFoundException.class, () -> new Pci(""));
				// assertThat(exception.getMessage(), matchesRegex("^.*config \\(No such file or directory\\)$"));
			}

			/**
			 * Checks that the vendor id is correct.
			 *
			 * @param pci A {@link Pci} instance.
			 * @throws IOException If an I/O error occurs.
			 * @see Pci#getVendorId()
			 */
			@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
			@MethodSource("pciSource")
			void getVendorId(@NonNull final Pci pci) throws IOException {
				assertEquals(EXPECTED_VENDOR, pci.getVendorId(), "vendor id should be correct");
			}

			/**
			 * Checks that the device id is correct.
			 *
			 * @param pci A {@link Pci} instance.
			 * @throws IOException If an I/O error occurs.
			 * @see Pci#getDeviceId()
			 */
			@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
			@MethodSource("pciSource")
			void getDeviceId(@NonNull final Pci pci) throws IOException {
				assertEquals(EXPECTED_DEVICE, pci.getDeviceId(), "device id should be correct");
			}

			/**
			 * Checks that the class id is correct.
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
			@Order(0)
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
			@Order(1)
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
			@Order(2)
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
			@Order(3)
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
			@Order(4)
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
			@Order(5)
			@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
			@MethodSource("pciSource")
			@Disabled
			void isDmaDisabled(@NonNull final Pci pci) throws IOException {
				assertFalse(pci.isDmaEnabled(), "DMA should be disabled");
			}

			/**
			 * Source of {@link Pci} instances.
			 * <p>
			 * This source uses the {@link PciTest#virtioSource()} as input and tries to create a {@link Pci} instance
			 * for each PCI device. If the instantiation throws a {@link FileNotFoundException}, that device is
			 * discarded and is not included in the output stream.
			 *
			 * @return A {@link Stream} of valid {@link Pci} instances.
			 * @see PciTest#virtioSource()
			 * @see PciTest#newPci(String)
			 */
			@NotNull
			private Stream<Pci> pciSource() {
				return PciTest.Virtio.this.pciSource()
						.map(PciTest::newPci)
						.filter(Objects::nonNull);
			}

		}
		
		/**
		 * Source of {@code virtio} PCI devices.
		 * <p>
		 * Reads the environment variable {@code IXY_VIRTIO_COUNT} and creates a {@link Stream<String>} containing all
		 * the names.
		 * <p>
		 * This method does not filter the PCI devices by checking the existence of the devices themselves, it only
		 * discards {@code null} or empty devices. Therefore, there is no guarantee that creating an instance of {@link
		 * Pci} will not throw a {@link FileNotFoundException}.
		 *
		 * @return A {@link Stream} of PCI devices.
		 */
		@NotNull
		private Stream<String> pciSource() {
			val count = System.getenv("IXY_VIRTIO_COUNT");
			return IntStream.range(1, 1 + Integer.parseInt(count))
					.mapToObj(i -> System.getenv("IXY_VIRTIO_ADDR_" + i))
					.filter(Objects::nonNull)
					.filter(it -> !it.isEmpty());
		}

	}

	/**
	 * Source of {{@code ixgbe} PCI devices.
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
	private static Stream<String> ixgbeSource() {
		val count = System.getenv("IXY_IXGBE_COUNT");
		return IntStream.range(1, 1 + Integer.parseInt(count))
				.mapToObj(i -> System.getenv("IXY_IXGBE_ADDR_" + i))
				.filter(Objects::nonNull)
				.filter(it -> !it.isEmpty());
	}

	/**
	 * Creates a {@link Pci} instance and handles any checked exception.
	 * <p>
	 * If the given PCI device does not exist or there is an error, {@code null} is returned.
	 *
	 * @param pciDevice The PCI device.
	 * @return The {@link Pci} instance.
	 * @see Pci#Pci(String)
	 */
	private static Pci newPci(final String pciDevice) {
		try {
			return new Pci(pciDevice);
		} catch (FileNotFoundException e) {
			log.error("Could not create PCI instance because the PCI device does not exist", e);
		} catch (IOException e) {
			log.error("Could not create PCI instance because the PCI device driver could not be guessed", e);
		}
		return null;
	}

}
