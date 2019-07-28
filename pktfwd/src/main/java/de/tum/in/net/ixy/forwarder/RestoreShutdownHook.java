package de.tum.in.net.ixy.forwarder;

import de.tum.in.net.ixy.Device;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.tum.in.net.ixy.forwarder.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.forwarder.BuildConfig.LOG_INFO;

/**
 * Restores a devices to its old status.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("ConstantConditions")
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
final class RestoreShutdownHook extends Thread {

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The collection of devices to restore. */
	@ToString.Include
	@EqualsAndHashCode.Include
	private final Device[] devices;

	/** The collection of devices binding statuses. */
	@ToString.Include
	@EqualsAndHashCode.Include
	private final @NotNull boolean[] bindings;

	/** The collection of devices DMA statuses. */
	@ToString.Include
	@EqualsAndHashCode.Include
	private final @NotNull boolean[] dmas;

	/** The collection of devices promiscuous statuses. */
	@ToString.Include
	@EqualsAndHashCode.Include
	private final @NotNull boolean[] promiscuity;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Given a list of devices extracts information about them to restore it once the JVM is shut down.
	 *
	 * @param devices The devices to restore.
	 */
	RestoreShutdownHook(final @NotNull Device... devices) {
		super("Device Restoring Thread");
		this.devices = devices;
		bindings = new boolean[devices.length];
		dmas = new boolean[devices.length];
		promiscuity = new boolean[devices.length];
		var i = 0;
		for (val device : devices) {
			bindings[i] = device.isBound();
			try {
				if (bindings[i]) {
					device.unbind();
				}
			} catch (final IOException e) {
				log.error("Error when unbinding the driver.");
			} finally {
				if (device.map() == 0L) {
					log.warn("The mapping address is 0; an error will happen sooner or later.");
				}
			}
			try {
				dmas[i] = device.isDmaEnabled();
			} catch (final IOException e) {
				if (DEBUG >= LOG_ERROR) log.error("Error while getting the DMA status of {}.", device);
				dmas[i] = false;
			}
			promiscuity[i++] = device.isPromiscuousEnabled();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void run() {
		var i = 0;
		for (val device : devices) {
			if (DEBUG >= LOG_INFO) log.info(">>> Restoring device {}.", device);

			val promiscuous = promiscuity[i];
			if (DEBUG >= LOG_DEBUG) log.debug("    Restoring promiscuity ({}).", promiscuous);
			if (!promiscuous) {
				if (device.isPromiscuousEnabled()) device.disablePromiscuous();
			} else if (promiscuous) {
				if (!device.isPromiscuousEnabled()) device.enablePromiscuous();
			}

			val dma = dmas[i];
			if (DEBUG >= LOG_DEBUG) log.debug("    Restoring DMA status ({}).", dma);
			try {
				if (!dma) {
					if (device.isDmaEnabled()) device.disableDma();
				} else if (dma) {
					if (!device.isDmaEnabled()) device.enableDma();
				}
			} catch (final IOException e) {
				if (DEBUG >= LOG_ERROR) log.error("Error while checking or restoring the DMA status.", e);
			}

			val bound = bindings[i];
			if (DEBUG >= LOG_DEBUG) log.debug("    Restoring driver binding ({}).", bound);
			try {
				if (!bound) {
					if (device.isBound()) device.unbind();
				} else if (dma) {
					if (!device.isBound()) device.bind();
				}
			} catch (final IOException e) {
				if (DEBUG >= LOG_ERROR) log.error("Error while checking or restoring the driver status.", e);
			}

			if (DEBUG >= LOG_DEBUG) log.debug("    Closing device.");
			try {
				device.close();
			} catch (final IOException e) {
				if (DEBUG >= LOG_ERROR) log.error("Error while closing the device.", e);
			}
		}
	}

}
