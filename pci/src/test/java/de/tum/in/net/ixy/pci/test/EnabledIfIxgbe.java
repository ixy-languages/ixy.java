package de.tum.in.net.ixy.pci.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Enables a JUnit test if there are 10-Gigabit NICs from Intel Corporation (ixgbe). */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfNic(driver = "IXGBE")
@interface EnabledIfIxgbe { }
