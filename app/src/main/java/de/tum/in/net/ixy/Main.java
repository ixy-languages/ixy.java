package de.tum.in.net.ixy;

import de.tum.in.net.ixy.generic.IxyDriver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;

/**
 * The main entry point of the Ixy program.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class Main {

	/**
	 * Entry point of the application.
	 *
	 * @param args The command line parameters.
	 */
	public static void main(@NotNull String[] args) {
		// Create the plugin manager
		val pluginManager = new DefaultPluginManager() {
			@Override
			protected CompoundPluginDescriptorFinder createPluginDescriptorFinder() {
				return new CompoundPluginDescriptorFinder().add(new ManifestPluginDescriptorFinder());
			}
		};
		// Load the plugins and activate the plugins
		pluginManager.loadPlugins();
		pluginManager.startPlugins();
		// Retrieve the list of plugins
		val plugins = pluginManager.getStartedPlugins();
		var count = plugins.size();
		if (count <= 0) {
			log.warn("No plugins have been found.");
		} else {
			log.info("{} plugins have been found:", count);
			for (val plugin : plugins) {
				val id = plugin.getDescriptor().getPluginId();
				log.info(">>> {}", id);
			}
		}
		// Retrieve the list of device drivers
		val deviceDrivers = pluginManager.getExtensions(IxyDriver.class);
		count = deviceDrivers.size();
		if (count <= 0) {
			log.warn("No device drivers have been found.");
		} else {
			log.info("{} device drivers have been found:", count);
			for (val deviceDriver : deviceDrivers) {
				log.info(">>> {}", deviceDriver.getName());
			}
		}
		// Stop the plugins and unload the plugins
		pluginManager.stopPlugins();
	}

}
