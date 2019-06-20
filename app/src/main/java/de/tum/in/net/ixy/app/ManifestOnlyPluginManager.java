package de.tum.in.net.ixy.app;

import org.pf4j.DefaultPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;

/**
 * An overridden {@link DefaultPluginManager} that only uses the {@code MANIFEST.MF} to find plugins.
 *
 * @author Esaú García Sánchez-Torija
 */
public class ManifestOnlyPluginManager extends DefaultPluginManager {

	@Override
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new ManifestPluginDescriptorFinder();
	}

}
