package de.tum.in.net.ixy.pci;

import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Collection of common tests.
 *
 * @author Esaú García Sanchez-Torija
 */
enum CommonPciTest {

	// Finish enumerating
	;

	@Contract(pure = true)
	static void getVendorId(@Nullable Device device, short vendor) {
		assumeThat(device).isNotNull();
		val vendorId = assertDoesNotThrow(device::getVendorId);
		assertThat(vendorId).isEqualTo(vendor);
	}

	@Contract(value = "_, null -> fail", pure = true)
	static void getDeviceId(@Nullable Device device, @NotNull Iterable<Short> devices) {
		assumeThat(device).isNotNull();
		val deviceId = assertDoesNotThrow(device::getDeviceId);
		assertThat(deviceId).isIn(devices);
	}

	@Contract(pure = true)
	static void getClassId(@Nullable Device device, byte klass) {
		assumeThat(device).isNotNull();
		val classId = assertDoesNotThrow(device::getClassId);
		assertThat(classId).isEqualTo(klass);
	}

	@Contract(pure = true)
	static void isDmaEnabled(@Nullable Device device, boolean status) {
		assumeThat(device).isNotNull();
		val dmaStatus = assertDoesNotThrow(device::isDmaEnabled);
		assertThat(dmaStatus).isEqualTo(status);
	}

	@Contract(pure = true)
	static void map(@Nullable Device device) {
		assumeThat(device).isNotNull();
		val mappable = assertDoesNotThrow(device::isMappable);
		val map = device.map();
		if (mappable) {
			assertThat(map).isPresent();
		} else {
			assertThat(map).isNotPresent();
		}
	}

	@Contract(pure = true)
	static void close(@Nullable Device device) {
		assumeThat(device).isNotNull();
		assertDoesNotThrow(device::close);
		assertDoesNotThrow(device::close);
	}

	@Contract(pure = true)
	static void close_exceptions(@Nullable Device device) {
		assertThat(device).isNotNull();
		assertDoesNotThrow(device::close);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::getVendorId);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::getDeviceId);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::getClassId);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::bind);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::unbind);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::isDmaEnabled);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::enableDma);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::disableDma);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::isMappable);
		val map = assertDoesNotThrow(device::map);
		assertThat(map).isNotPresent();
	}

	@Contract(pure = true)
	static void bindunbind(@Nullable Device device) {
		assertThat(device).isNotNull();
		val status = device.isBound();
		if (status) {
			assertThatExceptionOfType(IOException.class).isThrownBy(device::bind);
			assertDoesNotThrow(device::unbind);
			assertThat(device.isBound()).isFalse();
			assertThatExceptionOfType(IOException.class).isThrownBy(device::unbind);
			assertDoesNotThrow(device::bind);
			assertThat(device.isBound()).isTrue();
		} else {
			assertThatExceptionOfType(IOException.class).isThrownBy(device::unbind);
			assertDoesNotThrow(device::bind);
			assertThat(device.isBound()).isTrue();
			assertThatExceptionOfType(IOException.class).isThrownBy(device::bind);
			assertDoesNotThrow(device::unbind);
			assertThat(device.isBound()).isFalse();
		}
	}

}
