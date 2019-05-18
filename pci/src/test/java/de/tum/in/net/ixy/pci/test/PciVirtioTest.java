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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
 * Checks the class {@link Pci} using {@code virtio} devices.
 * <p>
 * All the tests of this test suite can be executed randomly or concurrently, except for the {@code dma} tests. Where a
 * {@code get} has to be tested after each {@code set}.
 */
@Slf4j
@DisplayName("PCI device manipulation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledOnOs(OS.LINUX)
class PciVirtioTest {

	/** The expected class id. */
	private static final byte EXPECTED_CLASS = (byte) 0x02;

	/** The expected vendor id. */
	private static final short EXPECTED_VENDOR = (short) 0x1af4;

	/** The expected device id. */
	private static final short EXPECTED_DEVICE = (short) 0x1000;

	/** Checks that all the static methods throw a {@link NullPointerException} right away. */
	@Test
	@DisplayName("All static methods throw a NullPointerException when the PCI device is null")
	@Order(-1)
	void nullPointerException() {
		assertThrows(NullPointerException.class, () -> Pci.getVendorId(null), "vendor id retrieval should fail");
		assertThrows(NullPointerException.class, () -> Pci.getDeviceId(null), "device id retrieval should fail");
		assertThrows(NullPointerException.class, () -> Pci.getClassId(null), "class id retrieval should fail");
		assertThrows(NullPointerException.class, () -> Pci.bindDriver(null), "binding should fail");
		assertThrows(NullPointerException.class, () -> Pci.unbindDriver(null), "unbinding should fail");
		assertThrows(NullPointerException.class, () -> Pci.enableDma(null), "DMA enabling should fail");
		assertThrows(NullPointerException.class, () -> Pci.isDmaEnabled(null), "DMA status checking should fail");
		assertThrows(NullPointerException.class, () -> Pci.disableDma(null), "DMA disabling should fail");
	}

	/** Checks that all the static methods throw a {@link FileNotFoundException} as soon as the FS is accessed. */
	@Test
	@DisplayName("All static methods throw a FileNotFoundException when the PCI device does not exist")
	@Order(-1)
	void fileNotFoundException() {
		assertThrows(FileNotFoundException.class, () -> Pci.getVendorId(""), "vendor id retrieval should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.getDeviceId(""), "device id retrieval should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.getClassId(""), "class id retrieval should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.bindDriver(""), "binding should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.unbindDriver(""), "unbinding should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.enableDma(""), "DMA enabling should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.isDmaEnabled(""), "DMA status checking should fail");
		assertThrows(FileNotFoundException.class, () -> Pci.disableDma(""), "DMA disabling should fail");
	}

	/**
	 * Checks that the vendor id is correct.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#getVendorId(String)
	 */
	@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
	@MethodSource("virtioSource")
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
	@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
	@MethodSource("virtioSource")
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
	@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (Network device)")
	@MethodSource("virtioSource")
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
	@ParameterizedTest(name = "PCI device {0} driver cannot be bound twice")
	@MethodSource("virtioSource")
	@Order(0)
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
	@ParameterizedTest(name = "PCI device {0} driver can be unbound")
	@MethodSource("virtioSource")
	@Order(1)
	void unbindDriver(@NotNull final String pciDevice) {
		assertDoesNotThrow(() -> Pci.unbindDriver(pciDevice), "unbinding should not fail");
	}

	/**
	 * Checks that the driver cannot be unbound again.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#unbindDriver(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver cannot be unbound twice")
	@MethodSource("virtioSource")
	@Order(2)
	void unbindDriverException(@NotNull final String pciDevice) {
		assertThrows(FileNotFoundException.class, () -> Pci.unbindDriver(pciDevice), "unbinding should fail");
	}

	/**
	 * Checks that the driver cannot be bound back.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#bindDriver(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver cannot be bound again")
	@MethodSource("virtioSource")
	@Order(2)
	void bindDriverException2(@NotNull final String pciDevice) {
		assertThrows(FileNotFoundException.class, () -> Pci.bindDriver(pciDevice), "binding should fail");
	}

	/**
	 * Checks that the DMA can be enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#enableDma(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
	@MethodSource("virtioSource")
	@Order(3)
	void enableDma(@NotNull final String pciDevice) {
		assertDoesNotThrow(() -> Pci.enableDma(pciDevice), "DMA enabling should not fail");
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#isDmaEnabled(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
	@MethodSource("virtioSource")
	@Order(4)
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
	@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
	@MethodSource("virtioSource")
	@Order(5)
	void disableDma(@NotNull final String pciDevice) {
		assertDoesNotThrow(() -> Pci.disableDma(pciDevice), "DMA disabling should not fail");
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#isDmaEnabled(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
	@MethodSource("virtioSource")
	@Order(6)
	void isDmaDisabled(@NotNull final String pciDevice) {
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(pciDevice), "DMA status retrieval should not fail");
		assertFalse(status, "DMA should be disabled");
	}

	/** Make sure all the devices are bound again after testing. */
	@Test
	@DisplayName("All devices can be restored")
	@Order(7)
	void cleanUp() {
		PciVirtioTest.virtioSource().forEach(pciDevice -> {
			assertDoesNotThrow(() -> new Pci(pciDevice).bindDriver(), "driver binding can be restored");
		});
	}

	/**
	 * Checks that the resource {@code resource0} can be mapped.
	 *
	 * @param pciDevice A PCI device.
	 * @see Pci#mapResource(String)
	 */
	@ParameterizedTest(name = "PCI device {0} resource0 can be mapped")
	@MethodSource("virtioSource")
	@Order(8)
	@Disabled
	void mapResource(@NotNull final String pci) {
		assertDoesNotThrow(() -> Pci.mapResource(pci), "resource0 mapping should not fail");
	}

	/**
	 * Make sure the constructor will fail in certain situations.
	 * 
	 * @param pciDevice A PCI device.
	 * @see Pci#Pci(String)
	 * @see Pci#Pci(String, boolean, boolean)
	 */
	@ParameterizedTest(name = "PCI device {0} cannot be used Ixgbe")
	@MethodSource("virtioSource")
	@Order(9)
	void constructorException(@NotNull final String pciDevice) {
		assertThrows(NullPointerException.class, () -> new Pci(null), "instantiation should fail");
		assertThrows(FileNotFoundException.class, () -> new Pci(pciDevice, true, false), "instantiation should fail");
		assertDoesNotThrow(() -> new Pci(pciDevice, false, true), "instantiation should not fail");
	}

	/** Checks the non-static methods. */
	@Nested
	@DisplayName("PCI device access for Virtio device using non-static methods")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class NonStatic {

		/**
		 * Checks that creating a new instance with an invalid PCI device throws a
		 * {@link FileNotFoundException}.
		 */
		@Test
		@DisplayName("Invalid PCI device fails")
		@Order(-2)
		void constructorException() {
			assertThrows(NullPointerException.class, () -> new Pci(null), "instantiation should fail");
			assertThrows(FileNotFoundException.class, () -> new Pci(""), "instantiation should fail");
		}

		/**
		 * Checks that the vendor id is correct.
		 * <p>
		 * Internally it tests the method {@link Pci#getVendorId()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pci A {@link Pci} instance.
		 * @see Pci#getVendorId()
		 */
		@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(-1)
		void getVendorId(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val vendor = assertDoesNotThrow(() -> pci.getVendorId(), "vendor id retrieval should not fail");
				assertEquals(EXPECTED_VENDOR, vendor, "vendor id should be correct");
			}
		}

		/**
		 * Checks that the device id is correct.
		 * <p>
		 * Internally it tests the method {@link Pci#getDeviceId()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pci A {@link Pci} instance.
		 * @see Pci#getDeviceId()
		 */
		@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (Virtio network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(-1)
		void getDeviceId(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val device = assertDoesNotThrow(() -> pci.getDeviceId(), "device id retrieval should not fail");
				assertEquals(EXPECTED_DEVICE, device, "device id should be correct");
			}
		}

		/**
		 * Checks that the class id is correct.
		 * <p>
		 * Internally it tests the method {@link Pci#getClassId()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pci A {@link Pci} instance.
		 * @see Pci#getClassId()
		 */
		@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (Virtio network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(-1)
		void getClassId(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val klass = assertDoesNotThrow(() -> pci.getClassId(), "class id retrieval should not fail");
				assertEquals(EXPECTED_CLASS, klass, "class id should be correct");
			}
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
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		void bindDriverException(@NotNull final Pci pci) {
			val exception = assertThrows(IOException.class, () -> pci.bindDriver(), "binding should fail");
			assertEquals(exception.getMessage(), "No such device", "the reason should be that the device is not found");
		}

		/**
		 * Checks that the driver can be unbound.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#unbindDriver()
		 */
		@ParameterizedTest(name = "PCI device {0} driver can be unbound")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(1)
		void unbindDriver(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.unbindDriver());
		}

		/**
		 * Checks that the driver cannot be unbound twice.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#unbindDriver()
		 */
		@ParameterizedTest(name = "PCI device {0} driver cannot be unbound twice")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(2)
		void unbindDriverException(@NotNull final Pci pci) {
			val exception = assertThrows(IOException.class, () -> pci.unbindDriver(), "unbinding should fail");
			assertEquals(exception.getMessage(), "No such device", "the reason should be that the device is not found");
		}

		/**
		 * Checks that the driver cannot be bound twice.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#bindDriver()
		 */
		@ParameterizedTest(name = "PCI device {0} driver can be bound")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(3)
		void bindDriver(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.bindDriver(), "binding should not fail");
		}

		/**
		 * Checks that the DMA can be enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#enableDma(String)} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 * 
		 * @param pciDevice A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#enableDma()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(4)
		void enableDma(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				assertDoesNotThrow(() -> pci.enableDma(), "DMA enabling should not fail");
			}
		}

		/**
		 * Checks that the DMA is enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#isDmaEnabled()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#isDmaEnabled()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(5)
		void isDmaEnabled(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), "DMA status retrieval should not fail");
				assertTrue(status, "DMA should be enabled");
			}
		}

		/**
		 * Checks that the DMA can be enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#disableDma()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#disableDma()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(6)
		void disableDma(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				assertDoesNotThrow(() -> pci.disableDma(), "DMA disabling should not fail");
			}
		}

		/**
		 * Checks that the DMA is disabled.
		 * <p>
		 * Internally it tests the method {@link Pci#isDmaEnabled()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#isDmaEnabled()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(7)
		void isDmaDisabled(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), "DMA status retrieval should not fail");
				assertFalse(status, "DMA should be disabled");
			}
		}

		/**
		 * Checks that the resource {@code resource0} can be mapped.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#mapResource()
		 */
		@ParameterizedTest(name = "PCI device {0} resource0 can be mapped")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(8)
		@Disabled
		void mapResource(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.mapResource(), "resource0 mapping should not fail");
		}

		/**
		 * Checks that the resource {@code resource0} can be mapped.
		 *
		 * @param pciDevice A {@link Pci} instance.
		 * @see Pci#mapResource()
		 */
		@ParameterizedTest(name = "PCI device {0} stops working if closed")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(9)
		void close(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.close(), "closing should not fail");
			assertThrows(IOException.class, () -> pci.getVendorId(),  "vendor id retrieval should fail");
			assertThrows(IOException.class, () -> pci.getDeviceId(),  "device id retrieval should fail");
			assertThrows(IOException.class, () -> pci.getClassId(),   "class id retrieval should fail");
			assertThrows(IOException.class, () -> pci.bindDriver(),   "binding should fail");
			assertThrows(IOException.class, () -> pci.unbindDriver(), "unbinding should fail");
			assertThrows(IOException.class, () -> pci.enableDma(),    "DMA enabling should fail");
			assertThrows(IOException.class, () -> pci.isDmaEnabled(), "DMA status checking should fail");
			assertThrows(IOException.class, () -> pci.disableDma(),   "DMA disabling should fail");
		}

	}

	/**
	 * Source of {@link Pci} instances.
	 * <p>
	 * This source uses the {@link PciVirtioTest#virtioSource()} as input and tries to create a {@link Pci} instance for each
	 * PCI device. If the instantiation throws a {@link FileNotFoundException}, that device is discarded and is not
	 * included in the output stream.
	 *
	 * @return A {@link Stream} of valid {@link Pci} instances.
	 * @see PciVirtioTest#virtioSource()
	 * @see PciVirtioTest#newPci(String)
	 */
	@NotNull
	private static Stream<@NotNull Pci> virtioPciSource() {
		return virtioSource()
				.map(PciVirtioTest::newPci)
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
			log.error("Could not create PCI instance because the PCI device does not exist", e);
		} catch (IOException e) {
			log.error("Could not create PCI instance because the PCI device driver could not be guessed", e);
		}
		return null;
	}

}
