package de.tum.in.net.ixy.pci.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation that allows the execution of a test case if the user name is {@code root} or the user id {@code 0}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfRootCondition.class)
@interface EnabledIfRoot { }
