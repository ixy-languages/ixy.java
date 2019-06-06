package de.tum.in.net.ixy.memory.test;

import de.tum.in.net.ixy.memory.BuildConfig;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import lombok.val;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * JUnit annotation that disallows the execution of a test case if the member {@link BuildConfig#OPTIMIZED} is {@code
 * true}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class DisabledIfOptimizedCondition implements ExecutionCondition {

	/** Cached evaluation result used when the annotation {@code @DisabledIfOptimized} is not present. */
	private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfRoot is not present");

	/** Cached evaluation result. */
	private static final ConditionEvaluationResult CACHED_RESULT = !BuildConfig.OPTIMIZED
			? enabled("The OPTIMIZED flag is false")
			: disabled("The OPTIMIZED flag is true");

	/** {@inheritDoc */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
		val optional = findAnnotation(context.getElement(), EnabledIfRoot.class);
		return optional.isEmpty() ? ENABLED_BY_DEFAULT : CACHED_RESULT;
	}

}