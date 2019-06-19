package de.tum.in.net.ixy;

import com.sun.source.tree.Tree;
import de.tum.in.net.ixy.generic.IxyDriver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The main entry point of the Ixy program.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class Main {

	/** The list of positional arguments. */
	private static @NotNull List<String> argumentsList = new ArrayList<>();

	/** The list of key-value arguments. */
	private static @NotNull Map<String, String> argumentsKeyValue = new TreeMap<>();

	/** The list of optional arguments. */
	private static @NotNull Set<String> argumentsOpt = new TreeSet<>();

	/**
	 * Entry point of the application.
	 *
	 * @param argv The command line arguments.
	 */
	public static void main(@NotNull String[] argv) {
		// Parse the parameters and read the required ones
		parseArguments(argv);
		val argvDriver = argumentsKeyValue.getOrDefault("--driver", argumentsKeyValue.getOrDefault("-d", null));
		val argvPlugin = argumentsKeyValue.getOrDefault("--plugin", argumentsKeyValue.getOrDefault("-p", null));
		val argvApp = argumentsList.isEmpty() ? "" : argumentsList.remove(0);

		// Detect if the pluginsDir is defined and set a default value
		val pluginManager = getPluginManager();
		val deviceDriver = getPluginDriver(IxyDriver.class, pluginManager, argvPlugin, argvDriver);
		if (deviceDriver == null) {
			log.error("The device driver could not be extracted.");
			return;
		}

		// Detect the application we want to use
		switch (argvApp) {
			case "forward":
			case "forwarder":
				forward(deviceDriver);
				break;
			case "generate":
			case "generator":
				generate(deviceDriver);
				break;
			default:
				log.error("Unknown application. The only supported applications are 'generate' and 'forward'.");
		}
	}

	/**
	 * The method that starts the generator application.
	 *
	 * @param driver The driver instance.
	 */
	private static void generate(@NotNull IxyDriver driver) {
		log.info("Preparing generator application.");
	}

	/**
	 * The method that starts the forwarder application.
	 *
	 * @param driver The driver instance.
	 */
	private static void forward(@NotNull IxyDriver driver) {
		log.info("Preparing forwarder application.");
	}

	/**
	 * Parses a list of arguments and stores them in {@link #argumentsList}, {@link #argumentsKeyValue} and {@link
	 * #argumentsOpt}.
	 *
	 * @param argv The list of arguments.
	 */
	@Contract("null -> fail")
	private static void parseArguments(@NotNull String[] argv) {
		log.debug("Parsing parameters:");
		val argc = argv.length;
		for (var i = 0; i < argc; i += 1) {
			val param = argv[i].trim();
			if (param.isEmpty()) {
				log.warn(">>> Skipping empty parameter at array position {}", i);
			} else if (param.charAt(0) != '-') {
				log.debug(">>> Found list argument: {}", param);
				argumentsList.add(param);
			} else if (i + 1 < argc && argv[i + 1].charAt(0) != '-') {
				val value = argv[++i];
				log.debug(">>> Found key value argument: {} => {}", param, value);
				argumentsKeyValue.put(param, value);
			} else {
				log.debug(">>> Found option argument: {}", param);
				argumentsOpt.add(param);
			}
		}
	}

	/**
	 * Constructs an appropriate plugin manager.
	 *
	 * @return A plugin manager.
	 */
	@Contract(value = " -> new", pure = true)
	private static PluginManager getPluginManager() {
		var pluginsDir = System.getProperty("pf4j.pluginsDir");
		if (pluginsDir == null || Files.notExists(Path.of(pluginsDir))) {
			log.warn("No pluginsDir defined or it does not exist. An optimal value will be computed.");
			var failed = false;
			if (Files.exists(Path.of("plugins"))) {
				pluginsDir = "plugins";
			} else if (Files.exists(Paths.get("build", "plugins"))) {
				pluginsDir = String.join(File.separator, "build", "plugins");
			} else {
				log.error("The plugins folder cannot be deduced. Exiting.");
				failed = true;
			}
			if (!failed) {
				log.info("Optimal value found. Setting pf4j.pluginsDir to: {}", pluginsDir);
				System.setProperty("pf4j.pluginsDir", pluginsDir);
			}
		}
		// Use a plugin manager that only reads the manifest
		return new ManifestOnlyPluginManager();
	}

	/**
	 * Finds the specified plugin and device driver.
	 * <p>
	 * Depending on the values of the parameters this method behaves differently:
	 * <ul>
	 *     <li>If no plugin name is specified, it won't be taken into account. This means that all drivers with the
	 *     matching name will be considered candidates and then the conflict must be resolved by the user.</li>
	 *     <li>If no driver name is specified, it won't be taken into account either. This means that all drivers will
	 *     be considered drivers, either of all plugins (if the plugin name is {@code null}) or from the specified
	 *     plugin (if the plugin name is not {@code null}.</li>
	 *     <li>If there are collisions, this method will prompt the user to select the correct driver and resolve the
	 *     conflict.</li>
	 * </ul>
	 * <p>
	 * When no matches are found this method will return {@code null}.
	 *
	 * @param cls           The class that defines the extension point.
	 * @param pluginManager The plugin manager.
	 * @param pluginName    The name of the plugin.
	 * @param driverName    The name of the driver.
	 * @return The plugin that meets all the conditions.
	 */
	private static <T> @Nullable T getPluginDriver(@NotNull Class<T> cls, @NotNull PluginManager pluginManager, @Nullable String pluginName, @Nullable String driverName) {
		log.debug("Loading all plugins.");
		pluginManager.loadPlugins();

		// Prepare the data structures
		val plugins = pluginManager.getResolvedPlugins();
		val count = plugins.size();
		val matchingPlugins = new TreeMap<String, List<String[]>>();    // Plugins that contain a matching device driver
		val nonMatchingPlugins = new TreeMap<String, Boolean>(); // Plugins that do not; will be unloaded

		// If no plugins are found, exit
		if (count <= 0) {
			log.warn("No plugins have been found. Exiting.");
			return null;
		}

		// If plugins are found, list them to find matches
		else {
			log.info("{} plugin(s) have been found:", count);
			for (val plugin : plugins) {
				val id = plugin.getDescriptor().getPluginId();
				log.info(">>> {}", id);

				// If we want a specific plugin, filter out those that do not match
				if (pluginName != null && !Objects.equals(pluginName, id)) {
					nonMatchingPlugins.put(id, false);
					continue;
				}

				// Start the plugin so that we can get the device drivers it exposes
				pluginManager.startPlugin(id);
				val deviceDrivers = pluginManager.getExtensions(IxyDriver.class, id);

				// List all the drivers and save all the matches
				val size = deviceDrivers.size();
				if (size > 0) {
					var i = 0;
					for (val deviceDriver : deviceDrivers) {
						val name = deviceDriver.getName();
						log.info(" {}- {}", ++i < size ? "|" : "\\", name);
						if (driverName == null || Objects.equals(driverName, name)) {
							if (matchingPlugins.containsKey(id)) {
								matchingPlugins.get(id).add(new String[]{name, deviceDriver.getClass().getCanonicalName()});
							} else {
								val list = new ArrayList<String[]>(1);
								list.add(new String[]{name, deviceDriver.getClass().getCanonicalName()});
								matchingPlugins.put(id, list);
							}
						}
					}
				}

				// If no match was found, the plugin will be stopped right away
				if (!matchingPlugins.containsKey(id)) {
					nonMatchingPlugins.put(id, true);
				}
			}
		}

		// If no match is found, exit
		if (matchingPlugins.isEmpty()) {
			log.warn("No matching plugins found.");
			stopAndUnload(pluginManager, nonMatchingPlugins);
			return null;
		}

		// Resolve any conflicts
		val pluginsSize = matchingPlugins.size();
		var canonicalName = (String) null;
		if (pluginsSize == 1) {
			val entry = matchingPlugins.firstEntry();
			val key = matchingPlugins.firstEntry().getKey();
			val value = matchingPlugins.firstEntry().getValue().get(0);
			canonicalName = value[1];
			driverName = value[0];
			pluginName = key;
		} else if (pluginsSize > 1) {
			log.warn("Multiple plugins are conflicting.");
			System.out.println("Multiple plugins are implementing the device driver you want:");

			// List and save in an array the possible options
			val options = new ArrayList<String[]>(pluginsSize);
			var i = 0;
			for (val option : matchingPlugins.entrySet()) {
				val plugin = option.getKey();
				System.out.println(String.format(">>> %s", plugin));
				System.out.println(" | ");
				val drivers = option.getValue();
				val size = drivers.size();
				var j = 0;
				for (val driver : drivers) {
					val name = driver[0];
					val klass = driver[1];
					System.out.println(String.format(" %s- [%d] %s (%s)", ++j < size ? "|" : "\\", i++, name, klass));
					options.add(new String[]{plugin, name, klass});
				}
				System.out.println();
			}

			// Ask until a valid answer is given
			var selection = -1;
			val max = options.size();
			try (val scanner = new Scanner(System.in)) {
				do {
					System.out.print("Select the appropriate: ");
					try {
						selection = scanner.nextInt();
					} catch (InputMismatchException e) {
						scanner.next();
//						e.printStackTrace();
					}
				} while (selection < 0 || selection >= max);
			} catch (RuntimeException e) {
				log.error("Error while reading from the standard input stream.", e);
				log.warn("Using the first available device driver.");
				selection = 0;
			}

			// Discard all the options that are not the selection
			val selected = options.get(selection);
			canonicalName = selected[2];
			driverName = selected[1];
			pluginName = selected[0];
			for (val oldCandidate : matchingPlugins.entrySet()) {
				val plugin = oldCandidate.getKey();
				if (!Objects.equals(plugin, pluginName)) nonMatchingPlugins.put(plugin, true);
			}
			for (val invalid : nonMatchingPlugins.entrySet()) {
				matchingPlugins.remove(invalid.getKey());
			}
		}

		// Stop and unload the plugins that are not needed
		stopAndUnload(pluginManager, nonMatchingPlugins);

		// Return the plugin instance, if more than one
		val extensions = pluginManager.getExtensions(cls, pluginName);
		if (canonicalName == null) {
			return extensions.get(0);
		}
		for (val extension : extensions) {
			if (Objects.equals(extension.getClass().getCanonicalName(), canonicalName)) {
				return extension;
			}
		}
		log.error("Unexpectedly, there was no matching extension point on the list of extensions");
		return null;
	}

	/**
	 * Stops a list of plugins.
	 *
	 * @param pluginManager The plugin manager.
	 * @param plugins       The plugins to stop.
	 */
	private static void stopAndUnload(@NotNull PluginManager pluginManager, @NotNull Map<String, Boolean> plugins) {
		if (!plugins.isEmpty()) log.info("Stopping and unloading plugins:");
		for (val plugin : plugins.entrySet()) {
			val id = plugin.getKey();
			val stop = plugin.getValue();
			log.info(">>> {}", id);
			if (stop) pluginManager.stopPlugin(id);
			pluginManager.unloadPlugin(id);
		}
	}

}
