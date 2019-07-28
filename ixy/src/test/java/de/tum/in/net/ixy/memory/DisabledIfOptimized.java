package de.tum.in.net.ixy.memory;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit annotation that disallows the execution of a test if {@link de.tum.in.net.ixy.BuildConfig#OPTIMIZED} is {@code
 * true}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("CyclicClassDependency")
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(DisabledIfOptimizedCondition.class)
@interface DisabledIfOptimized { }
