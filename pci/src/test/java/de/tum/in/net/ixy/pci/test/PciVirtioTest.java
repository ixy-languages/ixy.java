package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.Pci;

import static de.tum.in.net.ixy.pci.test.Messages.*;

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

	/** The driver used by the VirtIO devices. */
	private static final String DRIVER = "virtio-pci";

	/** The expected class id. */
	private static final byte EXPECTED_CLASS = (byte) 0x02;

	/** The expected vendor id. */
	private static final short EXPECTED_VENDOR = (short) 0x1af4;

	/** The expected device id. */
	private static final short EXPECTED_DEVICE = (short) 0x1000;

	/** Checks that all the static methods throw a {@link NullPointerException} right away. */
	@Test
	@Order(-1)
	@DisplayName("All static methods throw a NullPointerException when the PCI device is null")
	void nullPointerException() {
		assertThrows(NullPointerException.class, () -> Pci.getVendorId(null),  MSG_VENDOR_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.getDeviceId(null),  MSG_DEVICE_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.getClassId(null),   MSG_CLASS_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.bind(null),   MSG_BIND_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.unbind(null), MSG_UNBIND_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.enableDma(null),    MSG_DMA_ENABLE_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.isDmaEnabled(null), MSG_DMA_STATUS_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.disableDma(null),   MSG_DMA_DISABLE_METHOD);
		assertThrows(NullPointerException.class, () -> Pci.mapResource(null),  MSG_MAP_METHOD);
	}

	/** Checks that all the static methods throw a {@link FileNotFoundException} as soon as the FS is accessed. */
	@Test
	@Order(-1)
	@DisplayName("All static methods throw a FileNotFoundException when the PCI device does not exist")
	void fileNotFoundException() {
		assertThrows(FileNotFoundException.class, () -> Pci.getVendorId(""),  MSG_VENDOR_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.getDeviceId(""),  MSG_DEVICE_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.getClassId(""),   MSG_CLASS_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.bind(""),   MSG_BIND_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.unbind(""), MSG_UNBIND_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.enableDma(""),    MSG_DMA_ENABLE_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.isDmaEnabled(""), MSG_DMA_STATUS_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.disableDma(""),   MSG_DMA_DISABLE_METHOD);
		assertThrows(FileNotFoundException.class, () -> Pci.mapResource(""),  MSG_MAP_METHOD);
	}

	/**
	 * Checks that the vendor id is correct.
	 *
	 * @param device A PCI device.
	 * @see Pci#getVendorId(String)
	 */
	@ParameterizedTest(name = "Vendor id of {0} should be " + EXPECTED_VENDOR + " (Red Hat, Inc)")
	@MethodSource("virtioSource")
	@Order(-1)
	void getVendorId(@NotNull final String device) {
		val vendor = assertDoesNotThrow(() -> Pci.getVendorId(device), MSG_VENDOR_METHOD_NOT);
		assertEquals(EXPECTED_VENDOR, vendor, MSG_VENDOR_VALUE);
	}

	/**
	 * Checks that the device id is correct.
	 *
	 * @param device A PCI device.
	 * @see Pci#getDeviceId(String)
	 */
	@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (VirtIO network device)")
	@MethodSource("virtioSource")
	@Order(-1)
	void getDeviceId(@NotNull final String device) {
		val id = assertDoesNotThrow(() -> Pci.getDeviceId(device), MSG_DEVICE_METHOD_NOT);
		assertEquals(EXPECTED_DEVICE, id, MSG_DEVICE_VALUE);
	}

	/**
	 * Checks that the class id is correct.
	 *
	 * @param device A PCI device.
	 * @see Pci#getClassId(String)
	 */
	@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (Network device)")
	@MethodSource("virtioSource")
	@Order(-1)
	void getClassId(@NotNull final String device) {
		val klass = assertDoesNotThrow(() -> Pci.getClassId(device), MSG_CLASS_METHOD_NOT);
		assertEquals(EXPECTED_CLASS, klass, MSG_CLASS_VALUE);
	}

	/**
	 * Checks that the driver cannot be bound twice.
	 *
	 * @param device A PCI device.
	 * @see Pci#bind(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver cannot be bound twice")
	@MethodSource("virtioSource")
	@Order(0)
	void bindException(@NotNull final String device) {
		val exception = assertThrows(IOException.class, () -> Pci.bind(device), MSG_BIND_METHOD);
		assertEquals("No such device", exception.getMessage(), MSG_BIND_EXCEPTION);
	}

	/**
	 * Checks that the driver can be unbound.
	 *
	 * @param device A PCI device.
	 * @see Pci#unbind(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver can be unbound")
	@MethodSource("virtioSource")
	@Order(1)
	void unbind(@NotNull final String device) {
		assertDoesNotThrow(() -> Pci.unbind(device), MSG_UNBIND_METHOD_NOT);
	}

	/**
	 * Checks that the driver cannot be unbound again.
	 *
	 * @param device A PCI device.
	 * @see Pci#unbind(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver cannot be unbound twice")
	@MethodSource("virtioSource")
	@Order(2)
	void unbindException(@NotNull final String device) {
		assertThrows(FileNotFoundException.class, () -> Pci.unbind(device), MSG_UNBIND_METHOD);
	}

	/**
	 * Checks that the driver cannot be bound back.
	 *
	 * @param device A PCI device.
	 * @see Pci#bind(String)
	 */
	@ParameterizedTest(name = "PCI device {0} driver cannot be bound again")
	@MethodSource("virtioSource")
	@Order(2)
	void bindException2(@NotNull final String device) {
		assertThrows(FileNotFoundException.class, () -> Pci.bind(device), MSG_BIND_METHOD);
	}

	/**
	 * Checks that the DMA can be enabled.
	 *
	 * @param device A PCI device.
	 * @see Pci#enableDma(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
	@MethodSource("virtioSource")
	@Order(3)
	void enableDma(@NotNull final String device) {
		assertDoesNotThrow(() -> Pci.enableDma(device), MSG_DMA_ENABLE_METHOD_NOT);
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param device A PCI device.
	 * @see Pci#isDmaEnabled(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
	@MethodSource("virtioSource")
	@Order(4)
	void isDmaEnabled(@NotNull final String device) {
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(device), MSG_DMA_ENABLE_METHOD_NOT);
		assertTrue(status, MSG_DMA_STATUS_VALUE_1);
	}

	/**
	 * Checks that the DMA can be disabled.
	 *
	 * @param device A PCI device.
	 * @see Pci#disableDma(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
	@MethodSource("virtioSource")
	@Order(5)
	void disableDma(@NotNull final String device) {
		assertDoesNotThrow(() -> Pci.disableDma(device), MSG_DMA_DISABLE_METHOD_NOT);
	}

	/**
	 * Checks that the DMA is enabled.
	 *
	 * @param device A PCI device.
	 * @see Pci#isDmaEnabled(String)
	 */
	@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
	@MethodSource("virtioSource")
	@Order(6)
	void isDmaDisabled(@NotNull final String device) {
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(device), MSG_DMA_STATUS_METHOD_NOT);
		assertFalse(status, MSG_DMA_STATUS_VALUE_0);
	}

	/** Make sure all the devices are bound again after testing. */
	@Test
	@Order(7)
	@DisplayName("All devices can be restored")
	void cleanUp() {
		PciVirtioTest.virtioSource().forEach(device -> {
			assertDoesNotThrow(() -> new Pci(device, DRIVER).bind(), MSG_BIND_METHOD_NOT);
		});
	}

	/**
	 * Checks that the resource {@code resource0} can be mapped.
	 *
	 * @param device A PCI device.
	 * @see Pci#mapResource(String)
	 */
	@Disabled
	@ParameterizedTest(name = "PCI device {0} resource0 can be mapped")
	@MethodSource("virtioSource")
	@Order(8)
	void mapResource(@NotNull final String device) {
		assertDoesNotThrow(() -> Pci.unbind(device));
		assertDoesNotThrow(() -> Pci.enableDma(device));
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(device));
		assertTrue(status);
		try {
			assertDoesNotThrow(() -> Pci.mapResource(device), MSG_MAP_METHOD_NOT);
		} finally {
			assertDoesNotThrow(() -> new Pci(device, DRIVER).bind(), MSG_BIND_METHOD_NOT);
		}
	}

	/**
	 * Make sure the constructor will fail in certain situations.
	 * 
	 * @param device A PCI device.
	 * @see Pci#Pci(String, String)
	 */
	@ParameterizedTest(name = "PCI device {0} instances throw with invalid parameters")
	@MethodSource("virtioSource")
	@Order(9)
	void constructorException(@NotNull final String device) {
		assertThrows(NullPointerException.class,  () -> new Pci(null, null),     MSG_CONSTRUCTOR_METHOD);
		assertThrows(NullPointerException.class,  () -> new Pci("",   null),     MSG_CONSTRUCTOR_METHOD);
		assertThrows(NullPointerException.class,  () -> new Pci(null, ""),       MSG_CONSTRUCTOR_METHOD);
		assertThrows(FileNotFoundException.class, () -> new Pci("",   ""),       MSG_CONSTRUCTOR_METHOD);
		assertDoesNotThrow(                       () -> new Pci(device, DRIVER), MSG_CONSTRUCTOR_METHOD_NOT);
	}

	/** Checks the non-static methods. */
	@Nested
	@DisplayName("PCI device access for VirtIO device using non-static methods")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class NonStatic {

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
				val vendor = assertDoesNotThrow(() -> pci.getVendorId(), MSG_VENDOR_METHOD_NOT);
				assertEquals(EXPECTED_VENDOR, vendor, MSG_VENDOR_VALUE);
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
		@ParameterizedTest(name = "Device id of {0} should be " + EXPECTED_DEVICE + " (VirtIO network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(-1)
		void getDeviceId(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val device = assertDoesNotThrow(() -> pci.getDeviceId(), MSG_DEVICE_METHOD_NOT);
				assertEquals(EXPECTED_DEVICE, device, MSG_DEVICE_VALUE);
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
		@ParameterizedTest(name = "Class id of {0} should be " + EXPECTED_CLASS + " (VirtIO network device)")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(-1)
		void getClassId(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val klass = assertDoesNotThrow(() -> pci.getClassId(), MSG_CLASS_METHOD_NOT);
				assertEquals(EXPECTED_CLASS, klass, MSG_CLASS_VALUE);
			}
		}

		/**
		 * Checks that the driver cannot be bound twice.
		 *
		 * @param device A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#bind()
		 */
		@Order(0)
		@ParameterizedTest(name = "PCI device {0} driver cannot be bound twice")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		void bindException(@NotNull final Pci pci) {
			val exception = assertThrows(IOException.class, () -> pci.bind(), MSG_BIND_METHOD);
			assertEquals(exception.getMessage(), "No such device", "the reason should be that the device is not found");
		}

		/**
		 * Checks that the driver can be unbound.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#unbind()
		 */
		@ParameterizedTest(name = "PCI device {0} driver can be unbound")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(1)
		void unbind(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.unbind(), MSG_UNBIND_METHOD_NOT);
		}

		/**
		 * Checks that the driver cannot be unbound twice.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#unbind()
		 */
		@ParameterizedTest(name = "PCI device {0} driver cannot be unbound twice")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(2)
		void unbindException(@NotNull final Pci pci) {
			val exception = assertThrows(IOException.class, () -> pci.unbind(), MSG_UNBIND_METHOD);
			assertEquals("No such device", exception.getMessage(), "the reason should be that the device is not found");
		}

		/**
		 * Checks that the driver cannot be bound twice.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#bind()
		 */
		@ParameterizedTest(name = "PCI device {0} driver can be bound")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(3)
		void bindTest(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.bind(), MSG_BIND_METHOD_NOT);
		}

		/**
		 * Checks that the DMA can be enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#enableDma(String)} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 * 
		 * @param device A {@link Pci} instance.
		 * @throws IOException If an I/O error occurs.
		 * @see Pci#enableDma()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA can be enabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(4)
		void enableDma(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				assertDoesNotThrow(() -> pci.enableDma(), MSG_DMA_STATUS_METHOD_NOT);
			}
		}

		/**
		 * Checks that the DMA is enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#isDmaEnabled()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#isDmaEnabled()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA is enabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(5)
		void isDmaEnabled(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), MSG_DMA_STATUS_METHOD_NOT);
				assertTrue(status, MSG_DMA_STATUS_VALUE_1);
			}
		}

		/**
		 * Checks that the DMA can be enabled.
		 * <p>
		 * Internally it tests the method {@link Pci#disableDma()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#disableDma()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA can be disabled")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(6)
		void disableDma(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				assertDoesNotThrow(() -> pci.disableDma(), MSG_DMA_DISABLE_METHOD_NOT);
			}
		}

		/**
		 * Checks that the DMA is disabled.
		 * <p>
		 * Internally it tests the method {@link Pci#isDmaEnabled()} multiple times so that we can be sure everything
		 * works even if the internal buffer's position is at the end and there is not enough room to read the required
		 * amount of bytes.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#isDmaEnabled()
		 */
		@ParameterizedTest(name = "PCI device {0} DMA is disabled check")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(7)
		void isDmaDisabled(@NotNull final Pci pci) {
			for (var i = 0; i < pci.getName().length(); i += 1) {
				val status = assertDoesNotThrow(() -> pci.isDmaEnabled(), MSG_DMA_STATUS_METHOD_NOT);
				assertFalse(status, MSG_DMA_STATUS_VALUE_0);
			}
		}

		/**
		 * Checks that the resource {@code resource0} can be mapped.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#mapResource()
		 */
		@Disabled
		@ParameterizedTest(name = "PCI device {0} resource0 can be mapped")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(8)
		void mapResource(@NotNull final Pci pci) {
			assertDoesNotThrow(() -> pci.unbind());
			assertDoesNotThrow(() -> pci.enableDma());
			val status = assertDoesNotThrow(() -> pci.isDmaEnabled());
			assertTrue(status);
			try {
				assertDoesNotThrow(() -> pci.mapResource(), MSG_MAP_METHOD_NOT);
			} finally {
				assertDoesNotThrow(() -> pci.bind(),  MSG_BIND_METHOD_NOT);
			}
		}

		/**
		 * Checks that the resource {@code resource0} can be mapped.
		 *
		 * @param device A {@link Pci} instance.
		 * @see Pci#mapResource()
		 */
		@ParameterizedTest(name = "PCI device {0} stops working if closed")
		@MethodSource("de.tum.in.net.ixy.pci.test.PciVirtioTest#virtioPciSource")
		@Order(9)
		void close(@NotNull final Pci pci) {
			assertDoesNotThrow(             () -> pci.close(),        MSG_CLOSE_METHOD_NOT);
			assertThrows(IOException.class, () -> pci.getVendorId(),  MSG_VENDOR_METHOD);
			assertThrows(IOException.class, () -> pci.getDeviceId(),  MSG_DEVICE_METHOD);
			assertThrows(IOException.class, () -> pci.getClassId(),   MSG_CLASS_METHOD);
			assertThrows(IOException.class, () -> pci.bind(),   MSG_BIND_METHOD);
			assertThrows(IOException.class, () -> pci.unbind(), MSG_UNBIND_METHOD);
			assertThrows(IOException.class, () -> pci.enableDma(),    MSG_DMA_ENABLE_METHOD);
			assertThrows(IOException.class, () -> pci.isDmaEnabled(), MSG_DMA_STATUS_METHOD);
			assertThrows(IOException.class, () -> pci.disableDma(),   MSG_DMA_DISABLE_METHOD);
			assertThrows(IOException.class, () -> pci.mapResource(),  MSG_MAP_METHOD);
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
	 * @param device The PCI device.
	 * @return The {@link Pci} instance.
	 * @see Pci#Pci(String)
	 */
	@Nullable
	private static Pci newPci(@NotNull String device) {
		try {
			return new Pci(device, DRIVER);
		} catch (FileNotFoundException e) {
			log.error("Could not create PCI instance because the PCI device does not exist", e);
		} catch (IOException e) {
			log.error("Could not create PCI instance because the PCI device driver could not be guessed", e);
		}
		return null;
	}

}
