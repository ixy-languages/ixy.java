package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.Pci;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
	@DisplayName("PCI device access for Virtio device using static methods")
	class Virtio {

		/** The expected vendor id. */
		private static final short EXPECTED_VENDOR = (short) 0x1af4;

		/** The expected device id. */
		private static final short EXPECTED_DEVICE = (short) 0x1000;

		/**
		 * Checks that the vendor id is correct.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#getVendorId(String)
		 */
		@ParameterizedTest(name = "[static] Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		@Order(-1)
		void getVendorId(@NotNull final String pciDevice) {
			val vendor = assertDoesNotThrow(() -> Pci.getVendorId(pciDevice), "vendor id retrieval should not fail");
			assertEquals(EXPECTED_VENDOR, vendor, "vendor id should be correct");
		}

		/**
		 * Checks that the device id is correct.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#getDeviceId(String)
		 */
		@ParameterizedTest(name = "[static] Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		@Order(-1)
		void getDeviceId(@NotNull final String pciDevice) {
			val device = assertDoesNotThrow(() -> Pci.getDeviceId(pciDevice), "device id retrieval should not fail");
			assertEquals(EXPECTED_DEVICE, device, "device id should be correct");
		}

		/**
		 * Checks that the class id is correct.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#getClassId(String)
		 */
		@ParameterizedTest(name = "[static] Class id of {0} should be " + EXPECTED_CLASS + " (Network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		@Order(-1)
		void getClassId(@NotNull final String pciDevice) {
			val klass = assertDoesNotThrow(() -> Pci.getClassId(pciDevice), "class id retrieval should not fail");
			assertEquals(EXPECTED_CLASS, klass, "class id should be correct");
		}

		/**
		 * Checks that the driver cannot be bound twice.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#bindDriver(String)
		 */
		@Order(0)
		@ParameterizedTest(name = "[static] PCI device {0} driver cannot be bound twice")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void bindDriverException(@NotNull final String pciDevice) {
			val exception = assertThrows(IOException.class, () -> Pci.bindDriver(pciDevice), "binding should fail");
			assertEquals(exception.getMessage(), "No such device", "the reason should be that the device is not found");
		}

		/**
		 * Checks that the driver can be unbound.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#unbindDriver(String)
		 */
		@Order(1)
		@ParameterizedTest(name = "[static] PCI device {0} driver can be unbound")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void unbindDriver(@NotNull final String pciDevice) {
			assertDoesNotThrow(() -> Pci.unbindDriver(pciDevice), "binding should not fail");
		}

		/**
		 * Checks that the driver cannot be bound back.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#unbindDriver(String)
		 */
		@Order(2)
		@ParameterizedTest(name = "[static] PCI device {0} driver cannot be unbound twice")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void unbindDriverException(@NotNull final String pciDevice) {
			assertThrows(FileNotFoundException.class, () -> Pci.unbindDriver(pciDevice), "unbinding should fail");
		}

		/**
		 * Checks that the DMA can be enabled.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#enableDma(String)
		 */
		@Order(3)
		@ParameterizedTest(name = "[static] PCI device {0} DMA can be enabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void enableDma(@NotNull final String pciDevice) {
			assertDoesNotThrow(() -> Pci.enableDma(pciDevice), "DMA enabling should not fail");
		}

		/**
		 * Checks that the DMA is enabled.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#isDmaEnabled(String)
		 */
		@Order(4)
		@ParameterizedTest(name = "[static] PCI device {0} DMA is enabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void isDmaEnabled(@NotNull final String pciDevice) {
			val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(pciDevice), "DMA status retrieval should not fail");
			assertTrue(status, "DMA should be enabled");
		}

		/**
		 * Checks that the DMA can be disabled.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#disableDma(String)
		 */
		@Order(5)
		@ParameterizedTest(name = "[static] PCI device {0} DMA can be disabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void disableDma(@NotNull final String pciDevice) {
			assertDoesNotThrow(() -> Pci.disableDma(pciDevice), "DMA disabling should not fail");
		}

		/**
		 * Checks that the DMA is enabled.
		 *
		 * @param pciDevice A PCI device.
		 * @see Pci#isDmaEnabled(String)
		 */
		@Order(6)
		@ParameterizedTest(name = "[static] PCI device {0} DMA is disabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioSource")
		void isDmaDisabled(@NotNull final String pciDevice) {
			val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(pciDevice), "DMA status retrieval should not fail");
			assertFalse(status, "DMA should be disabled");
		}

		/** Make sure all the devices are bound again after testing. */
		@Test
		@DisplayName("All devices can be restored")
		@Order(7)
		void cleanUp() {
			PciTest.virtioSource()
					.forEach(pciDevice -> {
						assertDoesNotThrow(() -> new Pci(pciDevice).bindDriver(), "driver binding can be restored");
					});
		}

		/** Checks the non-static methods. */
		@Nested
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		@DisplayName("PCI device access for Virtio device using non-static methods")
		class NonStatic {

			/**
			 * Checks that creating a new instance with an invalid PCI device throws a {@link FileNotFoundException}.
			 */
			@Test
			@DisplayName("Invalid PCI device fails")
			@Order(-2)
			void constructorException() {
				assertThrows(FileNotFoundException.class, () -> new Pci(""), "instantiation should fail");
			}

			/**
			 * Checks that the vendor id is correct.
			 *
			 * @param pci A {@link Pci} instance.
			 * @see Pci#getVendorId()
			 */
			@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			@Order(-1)
			void getVendorId(@NotNull final Pci pci) {
				val vendor = assertDoesNotThrow(() -> pci.getVendorId(), "vendor id retrieval should not fail");
				assertEquals(EXPECTED_VENDOR, vendor, "vendor id should be correct");
			}

			/**
			 * Checks that the device id is correct.
			 *
			 * @param pci A {@link Pci} instance.
			 * @see Pci#getDeviceId()
			 */
			@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			@Order(-1)
			void getDeviceId(@NotNull final Pci pci) {
				val device = assertDoesNotThrow(() -> pci.getDeviceId(), "device id retrieval should not fail");
				assertEquals(EXPECTED_DEVICE, device, "device id should be correct");
			}

			/**
			 * Checks that the class id is correct.
			 *
			 * @param pci A {@link Pci} instance.
			 * @see Pci#getClassId()
			 */
			@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (Virtio network device)")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			@Order(-1)
			void getClassId(@NotNull final Pci pci) {
				val klass = assertDoesNotThrow(() -> pci.getClassId(), "class id retrieval should not fail");
				assertEquals(EXPECTED_CLASS, klass, "class id should be correct");
			}

			/**
			 * Checks that the driver cannot be bound twice.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @throws IOException If an I/O error occurs.
			 * @see Pci#bindDriver()
			 */
			@Order(0)
			@ParameterizedTest(name = "PCI device {0} driver cannot be bound twice")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			@Disabled
			void bindDriverException(@NotNull final Pci pci) {
				val exception = assertThrows(IOException.class, () -> pci.bindDriver(), "binding should fail");
				assertEquals(exception.getMessage(), "No such device",
						"the reason should be that the device is not found");
			}

			/**
			 * Checks that the driver can be unbound.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#unbindDriver()
			 */
			@Order(1)
			@ParameterizedTest(name = "PCI device {0} driver can be unbound")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void unbindDriver(@NotNull final Pci pci) {
				assertDoesNotThrow(() -> pci.unbindDriver());
			}

			/**
			 * Checks that the driver cannot be unbound twice.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#unbindDriver()
			 */
			@Order(2)
			@ParameterizedTest(name = "PCI device {0} driver cannot be unbound twice")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void unbindDriverException(@NotNull final Pci pci) {
				val exception = assertThrows(IOException.class, () -> pci.unbindDriver(), "binding should fail");
				assertEquals(exception.getMessage(), "No such device",
						"the reason should be that the device is not found");
			}

			/**
			 * Checks that the driver cannot be bound twice.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#bindDriver()
			 */
			@Order(3)
			@ParameterizedTest(name = "PCI device {0} driver can be bound")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void bindDriver(@NotNull final Pci pci) {
				assertDoesNotThrow(() -> pci.bindDriver());
			}

			/**
			 * Checks that the DMA can be enabled.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @throws IOException If an I/O error occurs.
			 * @see Pci#enableDma()
			 */
			@Order(4)
			@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void enableDma(@NotNull final Pci pci) {
				assertDoesNotThrow(() -> pci.enableDma(), "DMA enabling should not fail");
			}

			/**
			 * Checks that the DMA is enabled.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#isDmaEnabled()
			 */
			@Order(5)
			@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void isDmaEnabled(@NotNull final Pci pci) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), "DMA status retrieval should not fail");
				assertTrue(status, "DMA should be enabled");
			}

			/**
			 * Checks that the DMA can be enabled.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#disableDma()
			 */
			@Order(6)
			@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void disableDma(@NotNull final Pci pci) {
				assertDoesNotThrow(() -> pci.disableDma(), "DMA disabling should not fail");
			}

			/**
			 * Checks that the DMA is disabled.
			 *
			 * @param pciDevice A {@link Pci} instance.
			 * @see Pci#isDmaEnabled()
			 */
			@Order(7)
			@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
			@MethodSource("de.tum.in.net.ixy.pci.test.PciTest#virtioPciSource")
			void isDmaDisabled(@NotNull final Pci pci) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), "DMA status retrieval should not fail");
				assertFalse(status, "DMA should be disabled");
			}

		}
	}

	/**
	 * Source of {@link Pci} instances.
	 * <p>
	 * This source uses the {@link PciTest#virtioSource()} as input and tries to create a {@link Pci} instance for each
	 * PCI device. If the instantiation throws a {@link FileNotFoundException}, that device is discarded and is not
	 * included in the output stream.
	 *
	 * @return A {@link Stream} of valid {@link Pci} instances.
	 * @see PciTest#virtioSource()
	 * @see PciTest#newPci(String)
	 */
	@NotNull
	private static Stream<@NotNull Pci> virtioPciSource() {
		return virtioSource()
				.map(PciTest::newPci)
				.filter(Objects::nonNull);
	}

	/**
	 * Source of {@code virtio} PCI devices.
	 * <p>
	 * Reads the environment variable {@code IXY_VIRTIO_COUNT} and creates a {@link Stream<String>} containing all the
	 * names.
	 * <p>
	 * This method does not filter the PCI devices by checking the existence of the devices themselves, it only
	 * discards {@code null} or empty devices. Therefore, there is no guarantee that creating an instance of {@link
	 * Pci} will not throw a {@link FileNotFoundException}.
	 *
	 * @return A {@link Stream} of PCI devices.
	 */
	@NotNull
	private static Stream<@NotNull String> virtioSource() {
		val count = System.getenv("IXY_VIRTIO_COUNT");
		return IntStream.range(1, 1 + Integer.parseInt(count))
				.mapToObj(i -> System.getenv("IXY_VIRTIO_ADDR_" + i))
				.filter(Objects::nonNull)
				.filter(it -> !it.isEmpty());
	}

	/**
	 * Source of {@code ixgbe} PCI devices.
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
	private static Stream<@NotNull String> ixgbeSource() {
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
	@Nullable
	private static Pci newPci(@NotNull String pciDevice) {
		try {
			return new Pci(pciDevice);
		} catch (FileNotFoundException e) {
			System.out.println("CAGADA 1 => " + e.getMessage());
			log.error("Could not create PCI instance because the PCI device does not exist", e);
		} catch (IOException e) {
			System.out.println("CAGADA 2");
			log.error("Could not create PCI instance because the PCI device driver could not be guessed", e);
		}
		return null;
	}

}
