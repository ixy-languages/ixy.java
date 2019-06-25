package de.tum.in.net.ixy.app;

import de.tum.in.net.ixy.generic.IxyPciDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * A thread whose only purpose is to restore the system as it was before being manipulated by our program.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@RequiredArgsConstructor
public class ShutdownHookThread extends Thread {

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Restores a device to its previous status.
	 *
	 * @param device The device.
	 * @param status The status.
	 */
	private static void restore(@Nullable IxyPciDevice device, @Nullable Boolean status) {
		if (device != null) {
			if (BuildConfig.DEBUG) log.debug("Restoring the device: {}", device);
			if (status == null) {
				if (BuildConfig.DEBUG) log.info("No records of the previous status.");
			} else {
				if (BuildConfig.DEBUG) log.info("Restoring previous status, which was: {}", status);
				if (status) {
					try {
						device.bind();
					} catch (IOException e) {
						if (BuildConfig.DEBUG) log.error("Error while binding card.", e);
					}
				}
			}
		}
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The first device to restore. */
	private final @Nullable IxyPciDevice firstDevice;

	/** A supplier that tells us whether the first device was already bound when the app was first started. */
	private final @NotNull Supplier<Boolean> firstWasDeviceBound;

	/** The second device to restore. */
	private final @Nullable IxyPciDevice secondDevice;

	/** A supplier that tells us whether the second device was already bound when the app was first started. */
	private final @NotNull Supplier<Boolean> secondWasDeviceBound;

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public void run() {
		super.run();
		restore(firstDevice, firstWasDeviceBound.get());
		restore(secondDevice, secondWasDeviceBound.get());
	}

}
