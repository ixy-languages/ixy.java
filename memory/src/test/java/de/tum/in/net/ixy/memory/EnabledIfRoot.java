package de.tum.in.net.ixy.memory;

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
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfRootCondition.class)
@SuppressWarnings("CyclicClassDependency")
@Target({ElementType.TYPE, ElementType.METHOD})
@interface EnabledIfRoot { }
