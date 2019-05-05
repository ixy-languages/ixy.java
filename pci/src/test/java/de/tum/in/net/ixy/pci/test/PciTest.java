package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.Pci;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;

/** Checks the class {@link Pci}. */
@Slf4j
@DisplayName("PCI device access (read & write)")
class PciTest {

	/** The expected vendor id of the Virtio devices. */
	private static final short EXPECTED_VENDOR = 0x1af4;

	/** The expected device id of the Virtio devices. */
	private static final short EXPECTED_DEVICE = 0x1000;

	/** The expected class id of the Virtio devices. */
	private static final byte EXPECTED_CLASS = 0x02;

	/**
	 * Checks that the vendor id of the Virtio devices is correct.
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
	 * Checks that the device id of the Virtio devices is correct.
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

	/** Checks the non-static methods of the class {@link Pci}. */
	static class NonStatic {

		/** Checks that creating a new instance with an invalid PCI device throws a {@link FileNotFoundException}. */
		@Test
		@DisplayName("Invalid PCI device throws FileNotFoundException")
		void constructorException() {
			val exception = assertThrows(FileNotFoundException.class, () -> new Pci(""));
			assertThat(exception.getMessage(), matchesRegex("^.*config \\(No such file or directory\\)$"));
		}

		/**
		 * Checks that the vendor id of the Virtio devices is correct.
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
		 * Checks that the device id of the Virtio devices is correct.
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
	 * @see Pci#Pci(String)
	 */
	private static Pci newPci(final String pciDevice) {
		try {
			return new Pci(pciDevice);
		} catch (FileNotFoundException e) {
			log.warn("Could not create PCI instance", e);
		}
		return null;
	}

}
