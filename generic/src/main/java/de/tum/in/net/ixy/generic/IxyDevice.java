package de.tum.in.net.ixy.generic;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import de.tum.in.net.ixy.pci.Pci;
import lombok.NonNull;

/** Extension of a {@link Pci} device that defines a common interface for the different specific drivers. */
public abstract class IxyDevice extends Pci {

	/**
	 * Adds a constraint to the original {@link Pci} constructor, that is, the device must be a network device (0x02).
	 * 
	 * @param device The name of the PCI device.
	 * @param driver The driver of the PCI device.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 * @throws IOException           If reading the {@code config} resource fails while trying to guess the driver.
	 */
	public IxyDevice(final String device, final String driver) throws FileNotFoundException, IOException {
		super(device, driver);
		if (getClassId() != 0x02) {
			throw new IllegalArgumentException("IxyDevice instances only work with network devices");
		}
	}

	/**
	 * Returns the driver used by the device implementation, if any.
	 * 
	 * @return The driver.
	 */
	public abstract String getDriver();

}
