package de.tum.in.net.ixy.pci;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a JUnit test if there are NICs with a specific driver available.
 *
 * @author Esaú García Sánchez-Torija
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfNicCondition.class)
@interface EnabledIfNic {

	/**
	 * The name of the driver which will be used to read the environment variables {@code IXY_<DRIVER>_COUNT} and {@code
	 * IXY_<DRIVER>_ADDR_{i}}.
	 *
	 * @return The name of the driver.
	 */
	@NotNull String driver();

}
