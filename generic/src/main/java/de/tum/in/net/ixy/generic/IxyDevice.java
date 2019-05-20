package de.tum.in.net.ixy.generic;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import de.tum.in.net.ixy.pci.Pci;
import lombok.NonNull;

/**
 * Extension of a {@link Pci} device that defines a common interface for the different specific drivers.
 * 
 * @author Esaú García Sánchez-Torija
 */
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

	// /**
	//  * The vendor id used by Intel devices.
	//  * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L145">Intel's Vendor ID</a>
	//  */
	// private static final short VEN_ID_INTEL = (short) 0x8086;

	// /**
	//  * The vendor id used by Virtio devices.
	//  * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L150">RedHat's Vendor ID</a>
	//  */
	// private static final short VEN_ID_QUMRANET = (short) 0x1AF4;

	// /**
	//  * Set of device ids used by ixgbe devices.
	//  * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L369-L409">Physical ixgbe device IDs</a>
	//  * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L507-L514">Virtual ixgbe device IDs</a>
	//  */
	// private static final Set<Short> DEV_ID_IXGBE = Set.of(
	// 	// Physical devices
	// 	(short) 0x10B6, (short) 0x1508, (short) 0x10C6, (short) 0x10C7,
	// 	(short) 0x10C8, (short) 0x150B, (short) 0x10DB, (short) 0x10DD,
	// 	(short) 0x10EC, (short) 0x10F1, (short) 0x10E1, (short) 0x10F4,
	// 	(short) 0x10F7, (short) 0x1514, (short) 0x1517, (short) 0x10F8,
	// 	(short) 0x000C, (short) 0x10F9, (short) 0x10FB, (short) 0x11A9,
	// 	(short) 0x1F72, (short) 0x17D0, (short) 0x0470, (short) 0x152A,
	// 	(short) 0x1529, (short) 0x1507, (short) 0x154D, (short) 0x154A,
	// 	(short) 0x1558, (short) 0x1557, (short) 0x10FC, (short) 0x151C,
	// 	(short) 0x154F, (short) 0x1528, (short) 0x1560, (short) 0x15AC,
	// 	(short) 0x15AD, (short) 0x15AE, (short) 0x1563, (short) 0x15AA,
	// 	(short) 0x15AB,
	// 	// Virtual devices
	// 	(short) 0x10ED, (short) 0x152E, (short) 0x1515, (short) 0x1530,
	// 	(short) 0x1564, (short) 0x1565, (short) 0x15A8, (short) 0x15A9
	// );

	// /**
	//  * Set of device ids used by virtio devices.
	//  * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L535">Virtual virtio device IDs</a>
	//  */
	// private static final Set<Short> DEV_ID_VIRTIO = Set.of((short) 0x1000);

}
