package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.Pci;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.jetbrains.annotations.Nullable;
import lombok.NonNull;
import lombok.val;

import static de.tum.in.net.ixy.pci.test.Messages.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the class {@link Pci} using only VirtIO-based devices.
 * <p>
 * Because all the methods that manipulate the PCI devices make use of the <a href="http://man7.org/linux/man-pages/man5/sysfs.5.html">sysfs</a>
 * pseudo-filesystem, this test is enabled if and only if the host system that is executing the tests is a Linux-based
 * operative system.
 *
 * @author Esaú García Sánchez-Torija
 * @see Pci
 */
@EnabledOnOs(OS.LINUX)
@DisplayName("Pci (VirtIO)")
@Execution(ExecutionMode.CONCURRENT)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class VirtioPciTest {

	/** The lock identifier for the methods that access the resource {@code config}. */
	private static final String LOCK_CONFIG = "config";

	/** The lock identifier for the methods that access the resource {@code resource0}. */
	private static final String LOCK_MAP = "map";

	/** The lock identifier for the methods that access the resource {@code bind} and {@code unbind}. */
	private static final String LOCK_DRIVER = "driver";

	/** The name of the environment variable that counts how many VirtIO PCI devices exist. */
	private static final String ENV_KEY_NIC_COUNT = "IXY_VIRTIO_COUNT";

	/** The name of the environment variable that holds the address of a VirtIO PCI device. */
	private static final String ENV_KEY_NIC_ADDR = "IXY_VIRTIO_ADDR_";

	/** The name of the driver the PCI devices should use. */
	private static final String DRIVER = "virtio-pci";

	/** The expected class id. */
	private static final byte EXPECTED_CLASS = (byte) 0x02;

	/** The expected class id in hexadecimal format. */
	private static final String EXPECTED_CLASS_HEX = "0x02";

	/** The expected vendor id. */
	private static final short EXPECTED_VENDOR = (short) 0x1af4;

	/** The expected vendor id in hexadecimal format. */
	private static final String EXPECTED_VENDOR_HEX = "0x1af4";

	/** The expected device id. */
	private static final short EXPECTED_DEVICE = (short) 0x1000;

	/** The expected device id in hexadecimal format. */
	private static final String EXPECTED_DEVICE_HEX = "0x1000";

	/**
	 * The expected message of the exception thrown by the binding and unbinding methods.
	 * @see Pci#bind()
	 * @see Pci#bind(String)
	 * @see Pci#unbind()
	 * @see Pci#unbind(String)
	 */
	private static final String EXPECTED_BIND_MESSAGE = "No such device";

	/** The expected message of the exception thrown when the user does not have sufficient permissions. */
	private static final String EXPECTED_SEC_MESSAGE = "Permission denied";

	/** The expected message thrown when a resource cannot be found. */
	private static final String EXPECTED_NOT_FOUND_MESSAGE = "No such file or directory";

	@BeforeAll
	@DisplayName("All NICs can be bound before starting the tests")
	static void setUp() {
		pciSource().forEach(pci -> {
			try {
				if (pci != null) {
					pci.bind();
				}
			} catch (IOException e) {
				val message = e.getMessage();
				if (message == null) {
					throw new RuntimeException(e);
				} else if (!message.equals(EXPECTED_BIND_MESSAGE) && !message.contains(EXPECTED_SEC_MESSAGE)) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	//////////////////////////////////////////////// NON-STATIC METHODS ////////////////////////////////////////////////

	@Test
	@Tag("non-static")
	@DisplayName("Construction fails with wrong parameters")
	void Pci_exceptions() {
		assertThrows(NullPointerException.class,  () -> new Pci(null, null), MSG_CONSTRUCTOR_METHOD);
		assertThrows(NullPointerException.class,  () -> new Pci("",   null), MSG_CONSTRUCTOR_METHOD);
		assertThrows(NullPointerException.class,  () -> new Pci(null, ""),   MSG_CONSTRUCTOR_METHOD);
		assertThrows(FileNotFoundException.class, () -> new Pci("",   ""),   MSG_CONSTRUCTOR_METHOD);
	}

	@ParameterizedTest(name = "[{0}] The vendor id should be " + EXPECTED_VENDOR_HEX + " (RedHat, Inc)")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getVendorId(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val vendor = assertDoesNotThrow((ThrowingSupplier<Short>) pci::getVendorId, MSG_VENDOR_METHOD_NOT);
		assertEquals(EXPECTED_VENDOR, vendor, MSG_VENDOR_VALUE);
	}

	@ParameterizedTest(name = "[{0}] The device id should be " + EXPECTED_DEVICE_HEX + " (VirtIO Network Device)")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getDeviceId(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val device = assertDoesNotThrow((ThrowingSupplier<Short>) pci::getDeviceId, MSG_DEVICE_METHOD_NOT);
		assertEquals(EXPECTED_DEVICE, device, MSG_DEVICE_VALUE);
	}

	@ParameterizedTest(name = "[{0}] The class id should be " + EXPECTED_CLASS_HEX + " (Network Controller)")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getClassId(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val klass = assertDoesNotThrow((ThrowingSupplier<Byte>) pci::getClassId, MSG_CLASS_METHOD_NOT);
		assertEquals(EXPECTED_CLASS, klass, MSG_CLASS_VALUE);
	}

	@ParameterizedTest(name = "[{0}] DMA status can be false")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void isDmaEnabled(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::enableDma, MSG_DMA_ENABLE_METHOD);
		val status = assertDoesNotThrow((ThrowingSupplier<Boolean>) pci::isDmaEnabled, MSG_DMA_STATUS_METHOD_NOT);
		assertTrue(status, MSG_DMA_STATUS_VALUE_1);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be true")
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void isDmaDisabled(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::disableDma, MSG_DMA_DISABLE_METHOD);
		val status = assertDoesNotThrow((ThrowingSupplier<Boolean>) pci::isDmaEnabled, MSG_DMA_STATUS_METHOD_NOT);
		assertFalse(status, MSG_DMA_STATUS_VALUE_0);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA can be enabled")
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void enableDma(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::enableDma, MSG_DMA_ENABLE_METHOD_NOT);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA can be disabled")
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void disableDma(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::disableDma, MSG_DMA_DISABLE_METHOD_NOT);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Can be mapped to memory")
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void map(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val mapable = assertDoesNotThrow((ThrowingSupplier<Boolean>) pci::mapable, MSG_MAPABLE_METHOD_NOT);
		assumeTrue(mapable);
		val map = assertDoesNotThrow((ThrowingSupplier<MappedByteBuffer>) pci::map, MSG_MAP_METHOD_NOT);
		assertNotNull(map, MSG_MAP_NOT_NULL);
	}

	@ParameterizedTest(name = "[{0}] Memory mapping return behaves correctly")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void map_exception(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val mapable = assertDoesNotThrow((ThrowingSupplier<Boolean>) pci::mapable, MSG_MAPABLE_METHOD_NOT);
		val map = assertDoesNotThrow((ThrowingSupplier<MappedByteBuffer>) pci::map, MSG_MAP_METHOD_NOT);
		if (mapable) {
			assertNotNull(map, MSG_MAP_NOT_NULL);
		} else {
			assertNull(map, MSG_MAP_NULL);
		}
	}

	@ParameterizedTest(name = "[{0}] Can be closed")
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void close(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow(pci::close, MSG_CLOSE_METHOD_NOT);
	}

	@ParameterizedTest(name = "[{0}] Manipulating the device after close fails")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_DRIVER, mode = ResourceAccessMode.READ_WRITE)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void close_exceptions(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow(pci::close, MSG_CLOSE_METHOD_NOT);
		assertThrows(IOException.class, pci::getVendorId,  MSG_VENDOR_METHOD);
		assertThrows(IOException.class, pci::getDeviceId,  MSG_DEVICE_METHOD);
		assertThrows(IOException.class, pci::getClassId,   MSG_CLASS_METHOD);
		assertThrows(IOException.class, pci::bind,         MSG_BIND_METHOD);
		assertThrows(IOException.class, pci::unbind,       MSG_UNBIND_METHOD);
		assertThrows(IOException.class, pci::enableDma,    MSG_DMA_ENABLE_METHOD);
		assertThrows(IOException.class, pci::disableDma,   MSG_DMA_DISABLE_METHOD);
		assertThrows(IOException.class, pci::isDmaEnabled, MSG_DMA_STATUS_METHOD);
		assertThrows(IOException.class, pci::mapable,      MSG_MAPABLE_METHOD);
		assertThrows(IOException.class, pci::map,          MSG_MAP_METHOD);
	}

	@ParameterizedTest(name = "[{0}] Binding when already bound fails")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(1)
	void bind_1(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val exception = assertThrows(IOException.class, pci::bind, MSG_BIND_METHOD);
		assertEquals(EXPECTED_BIND_MESSAGE, exception.getMessage(), MSG_BIND_CAUSE);
	}

	@ParameterizedTest(name = "[{0}] Unbinding when already bound succeeds")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(2)
	void bind_2(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::unbind, MSG_UNBIND_METHOD_NOT);
	}

	@ParameterizedTest(name = "[{0}] Unbinding when already unbound fails")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(3)
	void bind_3(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		val exception = assertThrows(IOException.class, pci::unbind, MSG_UNBIND_METHOD_NOT);
		assertEquals(EXPECTED_BIND_MESSAGE, exception.getMessage(), MSG_BIND_CAUSE);
	}

	@ParameterizedTest(name = "[{0}] Binding when already unbound succeeds")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("pciSource")
	@Tag("non-static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(4)
	void bind_4(final Pci pci) {
		assumeTrue(Objects.nonNull(pci));
		assertDoesNotThrow((ThrowingSupplier<Pci>) pci::bind, MSG_BIND_METHOD_NOT);
	}

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	@Test
	@Tag("static")
	@DisplayName("Static methods check for nullity")
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ResourceLock(value = LOCK_DRIVER, mode = ResourceAccessMode.READ_WRITE)
	void static_exceptions() {
		assertThrows(NullPointerException.class,  () -> Pci.getVendorId(null),  MSG_VENDOR_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.getDeviceId(null),         MSG_DEVICE_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.getClassId(null),   MSG_CLASS_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.isDmaEnabled(null), MSG_DMA_STATUS_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.enableDma(null),    MSG_DMA_ENABLE_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.disableDma(null),   MSG_DMA_DISABLE_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.bind(null),         MSG_BIND_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.unbind(null),       MSG_UNBIND_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.mapable(null),      MSG_MAP_METHOD);
		assertThrows(NullPointerException.class,  () -> Pci.map(null),          MSG_MAP_METHOD);
	}

	@ParameterizedTest(name = "[{0}] The vendor id should be " + EXPECTED_VENDOR_HEX + " (RedHat, Inc) [static]")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getVendorId_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val vendor = assertDoesNotThrow(() -> Pci.getVendorId(device), MSG_VENDOR_METHOD_NOT);
		assertEquals(EXPECTED_VENDOR, vendor, MSG_VENDOR_VALUE);
	}

	@ParameterizedTest(name = "[{0}] The device id should be " + EXPECTED_DEVICE_HEX + " (VirtIO Network Device) [static]")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getDeviceId_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val dev = assertDoesNotThrow(() -> Pci.getDeviceId(device), MSG_DEVICE_METHOD_NOT);
		assertEquals(EXPECTED_DEVICE, dev, MSG_DEVICE_VALUE);
	}

	@ParameterizedTest(name = "[{0}] The class id should be " + EXPECTED_CLASS_HEX + " (Network Controller) [static]")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void getClassId_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val klass = assertDoesNotThrow(() -> Pci.getClassId(device), MSG_CLASS_METHOD_NOT);
		assertEquals(EXPECTED_CLASS, klass, MSG_CLASS_VALUE);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be false [static]")
	@MethodSource("addressSource")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Tag("static")
	void isDmaEnabled_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		assertDoesNotThrow(() -> Pci.enableDma(device), MSG_DMA_ENABLE_METHOD);
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(device), MSG_DMA_STATUS_METHOD_NOT);
		assertTrue(status, MSG_DMA_STATUS_VALUE_1);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA status can be true [static]")
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void isDmaDisabled_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		assertDoesNotThrow(() -> Pci.disableDma(device), MSG_DMA_DISABLE_METHOD);
		val status = assertDoesNotThrow(() -> Pci.isDmaEnabled(device), MSG_DMA_STATUS_METHOD_NOT);
		assertFalse(status, MSG_DMA_STATUS_VALUE_0);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA can be enabled [static]")
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void enableDma_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		assertDoesNotThrow(() -> Pci.enableDma(device), MSG_DMA_ENABLE_METHOD_NOT);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ_WRITE)
	@ParameterizedTest(name = "[{0}] DMA can be disabled [static]")
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void disableDma_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		assertDoesNotThrow(() -> Pci.disableDma(device), MSG_DMA_DISABLE_METHOD_NOT);
	}

	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@ParameterizedTest(name = "[{0}] Can be mapped to memory [static]")
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void map_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val mapable = assertDoesNotThrow(() -> Pci.mapable(device), MSG_MAPABLE_METHOD_NOT);
		assumeTrue(mapable);
		val map = assertDoesNotThrow(() -> Pci.map(device), MSG_MAP_METHOD_NOT);
		assertNotNull(map, MSG_MAP_NOT_NULL);
	}

	@ParameterizedTest(name = "[{0}] Memory mapping return behaves correctly [static]")
	@ResourceLock(value = LOCK_CONFIG, mode = ResourceAccessMode.READ)
	@ResourceLock(value = LOCK_MAP,    mode = ResourceAccessMode.READ)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	void map_static_exception(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val mapable = assertDoesNotThrow(() -> Pci.mapable(device), MSG_MAPABLE_METHOD_NOT);
		val map = assertDoesNotThrow(() -> Pci.map(device), MSG_MAP_METHOD_NOT);
		if (mapable) {
			assertNotNull(map, MSG_MAP_NOT_NULL);
		} else {
			assertNull(map, MSG_MAP_NULL);
		}
	}

	@ResourceLock(value = LOCK_DRIVER, mode = ResourceAccessMode.READ_WRITE)
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(5)
	void bind_1_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val exception = assertThrows(IOException.class, () -> Pci.bind(device), MSG_BIND_METHOD);
		assertEquals(EXPECTED_BIND_MESSAGE, exception.getMessage(), MSG_BIND_CAUSE);
	}

	@ParameterizedTest(name = "[{0}] Unbinding when already bound succeeds [static]")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(6)
	void bind_2_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		assertDoesNotThrow(() -> Pci.unbind(device), MSG_UNBIND_METHOD_NOT);
	}

	@ParameterizedTest(name = "[{0}] Unbinding when already unbound fails [static]")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(7)
	void bind_3_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val exception = assertThrows(FileNotFoundException.class, () -> Pci.unbind(device), MSG_UNBIND_METHOD_NOT);
		assertTrue(exception.getMessage().contains(EXPECTED_NOT_FOUND_MESSAGE), MSG_BIND_CAUSE);
	}

	@ParameterizedTest(name = "[{0}] Binding when already unbound fails [static]")
	@Execution(ExecutionMode.SAME_THREAD)
	@MethodSource("addressSource")
	@Tag("static")
	@EnabledIfVirtio
	@EnabledIfRoot
	@Order(8)
	void bind_4_static(final String device) {
		assumeTrue(Objects.nonNull(device));
		assumeFalse(device.isBlank());
		val exception = assertThrows(FileNotFoundException.class, () -> Pci.bind(device), MSG_BIND_METHOD);
		assertTrue(exception.getMessage().contains(EXPECTED_NOT_FOUND_MESSAGE), MSG_BIND_CAUSE);
	}

	@AfterAll
	@DisplayName("All NICs can be bound after finishing the tests")
	static void tearDown() {
		pciSource().forEach(pci -> {
			try {
				if (pci != null) {
					pci.bind();
				}
			} catch (IOException e) {
				val message = e.getMessage();
				if (message == null) {
					throw new RuntimeException(e);
				} else if (!message.equals(EXPECTED_BIND_MESSAGE) && !message.contains(EXPECTED_SEC_MESSAGE)) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	/**
	 * Source of {@link Pci} instances.
	 * <p>
	 * This source uses the {@link #addressSource()} as input and tries to create a {@link Pci} instance using {@link
	 * #newPci(String)}. If the instantiation throws a {@link FileNotFoundException}, the result returned value will be
	 * {@code null} and that device is discarded from the stream.
	 * <p>
	 * To prevent initialization errors, a {@link Stream<Pci>} with one {@code null} element is returned in case there
	 * are no VirtIO devices available.
	 *
	 * @return A {@link Stream<Pci>} of valid {@link Pci} instances.
	 * @see #ixyCount()
	 * @see #addressSource()
	 * @see #newPci(String)
	 */
	@NonNull
	private static Stream<Pci> pciSource() {
		if (ixyCount() > 0) {
			val original = addressSource().map(VirtioPciTest::newPci).filter(Objects::nonNull);
			val count = addressSource().map(VirtioPciTest::newPci).filter(Objects::nonNull).count();
			if (count > 0) {
				return original;
			}
			return Stream.concat(original, Stream.of((Pci) null));
		}
		return Stream.of((Pci) null);
	}

	/**
	 * Source of {@code virtio} PCI devices.
     * <p>
	 * This method does not check if the PCI addresses exist but only checks for nullity and its format, therefore,
	 * there is no guarantee that creating an instance of {@link Pci} will not throw a {@link FileNotFoundException}.
	 *
	 * @return The PCI devices.
	 * @see #ixyCount()
	 */
	@NonNull
	private static Stream<String> addressSource() {
		if (ixyCount() > 0) {
			return IntStream.range(1, 1 + ixyCount())
					.mapToObj(i -> System.getenv(ENV_KEY_NIC_ADDR + i))
					.filter(Objects::nonNull)
					.filter(it -> !it.isEmpty())
					.filter(it -> it.matches("^\\d{4}:\\d{2}:\\d{2}\\.\\d$"));
		}
		return Stream.of((String) null);
	}

	/**
	 * Creates a {@link Pci} instance and handles any checked exception.
	 * <p>
	 * If the given PCI device does not exist or there is an error, {@code null} is returned.
	 *
	 * @param device The PCI device.
	 * @return The {@link Pci} instance.
	 * @see Pci#Pci(String, String)
	 */
	@Nullable
	private static Pci newPci(@NonNull final String device) {
		try {
			return new Pci(device, DRIVER);
		} catch (FileNotFoundException e) {
			val message = e.getMessage();
			if (message == null) {
				throw new RuntimeException(e);
			} else if (!message.contains(EXPECTED_SEC_MESSAGE)) {
				e.printStackTrace();
			}
			return null;
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
