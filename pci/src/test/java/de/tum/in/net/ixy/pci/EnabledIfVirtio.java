package de.tum.in.net.ixy.pci;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a JUnit tests if there are VirtIO cards available.
 *
 * @author Esaú García Sánchez-Torija
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfNic(driver = "VIRTIO")
@interface EnabledIfVirtio { }
