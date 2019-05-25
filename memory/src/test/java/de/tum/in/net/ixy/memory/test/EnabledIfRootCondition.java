package de.tum.in.net.ixy.memory.test;

import lombok.val;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/** Evaluates an extension context to decide if a JUnit test should be executed, allowing only {@code root} to do so. */
final class EnabledIfRootCondition implements ExecutionCondition {

	/** Cached evaluation result used when the annotation {@code @EnabledIfRoot} is not present. */
	private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfRoot is not present");

	/** Cached evaluation result used when the user cannot be identified. */
	private static final ConditionEvaluationResult ENABLED_NOT_FOUND = enabled("Cannot identify user id");

	/** Cached evaluation result used when the user is {@code root}. */
	private static final ConditionEvaluationResult ENABLED_ROOT = enabled("The user id is 0");

	/** Cached evaluation result computed once, because the user does not change during the whole execution. */
	private static ConditionEvaluationResult CACHED_RESULT;

	// Compute the value of CACHED_RESULT to use it every time an ExtensionContext needs to be evaluated
	static {
		// Get the first valid user id
		val id = Stream.of("EUID", "SUID", "RUID", "UID")
				.map(System::getenv)
				.filter(Objects::nonNull)
				.filter(Pattern.compile("^\\d+$").asMatchPredicate())
				.mapToInt(Integer::parseInt)
				.findFirst();

		// Do a not-so-strict check using the user name
		if (id.isEmpty()) {
			val name = System.getProperty("user.name");
			if (name == null || name.isBlank()) {
				CACHED_RESULT = ENABLED_NOT_FOUND;
			} else if (!name.equals("root")) {
				CACHED_RESULT = disabled(format("The user id is %s", name));
			} else {
				CACHED_RESULT = ENABLED_ROOT;
			}
		} else {
			// Enable if the user id is root's
			val number = id.getAsInt();
			if (number == 0) {
				CACHED_RESULT = ENABLED_ROOT;
			} else {
				CACHED_RESULT = disabled(format("The user id is %d", number));
			}
		}
	}

	/** {@inheritDoc */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {

		// Search our custom annotation
		val optional = findAnnotation(context.getElement(), EnabledIfRoot.class);

		// If the annotation is not present, ignore the whole evaluation process and enable it
		if (optional.isEmpty()) {
			return ENABLED_BY_DEFAULT;
		}

		// Return the precomputed result
		return CACHED_RESULT;
	}

}
