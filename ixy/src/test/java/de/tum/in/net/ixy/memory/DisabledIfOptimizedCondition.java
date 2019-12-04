package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.BuildConfig;

import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * JUnit annotation that disallows the execution of a test if {@link BuildConfig#OPTIMIZED} is {@code true}.
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings("CyclicClassDependency")
final class DisabledIfOptimizedCondition implements ExecutionCondition {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** Cached evaluation result used when the annotation {@code @DisabledIfOptimized} is not present. */
	private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfRoot is not present");

	/** Cached evaluation result. */
	private static final ConditionEvaluationResult CACHED_RESULT = BuildConfig.OPTIMIZED
			? disabled("The OPTIMIZED flag is true")
			: enabled("The OPTIMIZED flag is false");

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@Contract(value = "null -> fail", pure = true)
	public @NotNull ConditionEvaluationResult evaluateExecutionCondition(final @NotNull ExtensionContext context) {
		val optional = findAnnotation(context.getElement(), DisabledIfOptimized.class);
		return optional.isEmpty() ? ENABLED_BY_DEFAULT : CACHED_RESULT;
	}

}
