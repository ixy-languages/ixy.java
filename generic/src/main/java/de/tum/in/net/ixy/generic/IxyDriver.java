package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Ixy's driver specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDriver extends ExtensionPoint {

	/**
	 * Returns the name of the driver.
	 *
	 * @return The name.
	 */
	@Contract(value = " -> new", pure = true)
	@NotNull String getName();

	/**
	 * Returns the implementation of an Ixy device.
	 *
	 * @param device The device name.
	 * @param driver The driver name.
	 * @return The device.
	 */
	@Contract(value = "null, _ -> fail; _, null -> fail; !null, !null -> new", pure = true)
	@NotNull IxyDevice getDevice(@NotNull String device, @NotNull String driver);

}
