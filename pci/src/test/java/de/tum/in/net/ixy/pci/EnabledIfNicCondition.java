package de.tum.in.net.ixy.pci;

import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * Evaluates an extension context to decide if a JUnit test should be executed, allowing only hosts with available
 * devices using the driver {@link EnabledIfNic#driver()} to do so.
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings("CyclicClassDependency")
final class EnabledIfNicCondition implements ExecutionCondition {

	/** Cached evaluation result used when the annotation {@code @EnabledIfNic} is not present. */
	private static final @NotNull ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledIfNic is not present");

	/** Cached evaluation result used when at least one NIC is counted and at least one PCI address can be found. */
	private static final @NotNull ConditionEvaluationResult ENABLED_AT_LEAST = enabled("At least one NIC address was found");

	/** Cached evaluation result used when at least one NIC is counted but not a single PCI address can be found. */
	private static final @NotNull ConditionEvaluationResult DISABLED_NOT_FOUND = disabled("Not a single NIC address was found");

	/** The {@link Pattern} used to validate a PCI address. */
	private static final @NotNull Pattern PCI_ADDRESS_REGEX = Pattern.compile("^\\d{4}:\\d{2}\\d{2}.\\d$");

	@Override
	@Contract(pure = true)
	public @NotNull ConditionEvaluationResult evaluateExecutionCondition(@NotNull ExtensionContext context) {
		// Search our custom annotation
		val optional = findAnnotation(context.getElement(), EnabledIfNic.class);
		// If the annotation is not present, ignore the whole evaluation process and enable it
		if (optional.isEmpty()) return ENABLED_BY_DEFAULT;
		// Retrieve the attributes
		val annotation = optional.get();
		val driver = annotation.driver().toUpperCase(Locale.getDefault()).trim();
		Preconditions.notBlank(driver, () -> "The 'driver' attribute must not be blank in " + annotation);
		// Get the NIC count
		val countEnv = System.getenv("IXY_" + driver + "_COUNT");
		val count = parseCount(countEnv);
		// If no NICs are available, disable the test
		if (count == 0) return disabled("No NICs with driver '" + driver + "' are available");
		// Know if there is at least one valid PCI address
		val valid = IntStream.range(1, 1 + count)
				.mapToObj(i -> "IXY_" + driver + "_ADDR_" + i)
				.map(System::getenv)
				.filter(EnabledIfNicCondition::isAddressValid)
				.map(dev -> String.join(File.separator, "", "sys", "bus", "pci", "devices", dev))
				.map(Paths::get)
				.anyMatch(Files::exists);
		// If there were no correctly-formatted and existing addresses, disable the test
		return valid ? DISABLED_NOT_FOUND : ENABLED_AT_LEAST;
	}

	/**
	 * Checks if a PCI address is valid.
	 *
	 * @param address The PCI address.
	 * @return The check result.
	 * @see #PCI_ADDRESS_REGEX
	 */
	private static boolean isAddressValid(@Nullable String address) {
		return address != null && !address.isBlank() && PCI_ADDRESS_REGEX.matcher(address).matches();
	}

	/**
	 * Parses a {@link String} into an {@code int} safely.
	 *
	 * @param number The number to parse.
	 * @return The parsed number.
	 */
	private static int parseCount(@Nullable String number) {
		if (number == null || number.isBlank()) return 0;
		try {
			val num = Integer.parseInt(number);
			return num > 0 ? num : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
