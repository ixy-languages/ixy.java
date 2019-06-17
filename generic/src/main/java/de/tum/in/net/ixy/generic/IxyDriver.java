package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Ixy's driver specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDriver extends ExtensionPoint {

	@NotNull IxyDevice getDevice(@NotNull String device, @NotNull String driver);

}
