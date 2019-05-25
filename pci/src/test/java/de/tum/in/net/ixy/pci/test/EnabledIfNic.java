package de.tum.in.net.ixy.pci.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/** Enables a JUnit test if there are NICs with the specific driver. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfNicCondition.class)
@interface EnabledIfNic {

	/**
	 * The name of the driver which will be used to read the environment {@code IXY_<DRIVER>_COUNT} and {@code
	 * IXY_<DRIVER>_ADDR_{i}} variables.
	 * @return The name of the driver.
	 */
	String driver();

}
