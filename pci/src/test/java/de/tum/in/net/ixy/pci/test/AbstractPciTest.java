package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.pci.Device;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

abstract class AbstractPciTest {

	@Contract(pure = true)
	void commonTest_getVendorId(@Nullable Device device, short vendor) {
		assumeThat(device).isNotNull();
		val vendorId = assertDoesNotThrow(device::getVendorId);
		assertThat(vendorId).isEqualTo(vendor);
	}

	@Contract(pure = true)
	void commonTest_getDeviceId(@Nullable Device device, @NotNull Set<Short> devices) {
		assumeThat(device).isNotNull();
		val deviceId = assertDoesNotThrow(device::getDeviceId);
		assertThat(deviceId).isIn(devices);
	}

	@Contract(pure = true)
	void commonTest_getClassId(@Nullable Device device, byte klass) {
		assumeThat(device).isNotNull();
		val classId = assertDoesNotThrow(device::getClassId);
		assertThat(classId).isEqualTo(klass);
	}

	@Contract(pure = true)
	void commonTest_isDmaEnabled(@Nullable Device device, boolean status) {
		assumeThat(device).isNotNull();
		val dmaStatus = assertDoesNotThrow(device::isDmaEnabled);
		assertThat(dmaStatus).isEqualTo(status);
	}

	@Contract(pure = true)
	void commonTest_map(@Nullable Device device) {
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
	void commonTest_close(@Nullable Device device) {
		assumeThat(device).isNotNull();
		assertDoesNotThrow(device::close);
		assertDoesNotThrow(device::close);
	}

	@Contract(pure = true)
	void commonTest_close_exceptions(@NotNull Device device) {
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
	void commonTest_bindunbind(@NotNull Device device) {
		assertThat(device).isNotNull();
		assertThatExceptionOfType(IOException.class).isThrownBy(device::bind);
		assertDoesNotThrow(device::unbind);
		assertThatExceptionOfType(IOException.class).isThrownBy(device::unbind);
		assertDoesNotThrow(device::bind);
	}

}
