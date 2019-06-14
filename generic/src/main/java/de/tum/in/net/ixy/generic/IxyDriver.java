package de.tum.in.net.ixy.generic;

import org.jetbrains.annotations.NotNull;

/**
 * Ixy's driver specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyDriver {

	@NotNull IxyDevice getDevice(@NotNull String device, @NotNull String driver);

}
