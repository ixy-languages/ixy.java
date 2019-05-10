package de.tum.in.net.ixy.pci;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import de.tum.in.net.ixy.pci.BuildConstants;

/**
 * PCI static utils with instantiation support.
 * <p>
 * This class allows direct manipulation of arbitrary PCI devices through a set of static methods that access the
 * pseudo file system {@code sysfs} provided by the Linux kernel itself.
 * <p>
 * To avoid passing around the PCI device to all the static methods and to improve efficiency, this class can be
 * instantiated. This allows the instance to remember an use always the same PCI device and to allocate a direct {@link
 * ByteBuffer} which can be reused in all operations that require the manipulation of the pseudo file system {@code
 * sysfs}. Moreover, the static methods will throw a {@link FileNotFoundException} when trying to access the {@code
 * bind} and {@code unbind} resources if the device is not bound to a driver.
 * <p>
 * The required resources when instantiating this class, which will cause a {@link FileNotFoundException} to be thrown
 * if not found, are the collection of all resources used by the individual static methods. These are:
 * <ul>
 *   <li>The {@code config} resource of the PCI device, usually found under {@code
 *       /sys/bus/pci/devices/<device>/config}.</li>
 *   <li>The {@code resource0} resource of the PCI device, usually available under {@code
 *       /sys/bus/pci/devices/<device>/resource0}.</li>
 *   <li>The {@code bind} resource of the driver of the PCI device, usually available under {@code
 *       /sys/bus/pci/devices/<device>/driver/bind}, but it has been <em>optimized</em> to use {@code
 *       /sys/bus/pci/drivers/(virtio-pci|ixgbe)/bind} because we use only the 10GbE {@code ixgbe} family and the {@code
 *       virtio} devices.</li>
 *   <li>The {@code unbind} resource of the driver of the PCI device, usually available under {@code
 *       /sys/bus/pci/devices/<device>/driver/unbind}, but it has been <em>optimized</em> to use {@code
 *       /sys/bus/pci/drivers/(virtio-pci|ixgbe)/unbind} because we use only the 10GbE {@code ixgbe} family and the
 *       {@code virtio} devices.</li>
 * </ul>
 *
 * @author Esaú García Sánchez-Torija
 * @see ByteBuffer
 */
@Slf4j
public final class Pci {

	/** Path format string used to build the path to a PCI device. */
	private static final String PCI_PATH_FMT = "/sys/bus/pci/devices/%s";

	/** Path format string used to build the path to a resource of a PCI device. */
	private static final String PCI_RES_PATH_FMT = PCI_PATH_FMT + "/%s";

	/** Path format string used to build the path to a Virtio driver resource. */
	private static final String PCI_VIRTIO_PATH_FMT = "/sys/bus/pci/drivers/virtio-pci/%s";

	/** Path format string used to build the path to a ixgbe driver resource. */
	private static final String PCI_IXGBE_PATH_FMT = "/sys/bus/pci/drivers/ixgbe/%s";

	/**
	 * Bit mask used to manipulate the DMA status.
	 * @see <a href="https://en.wikipedia.org/wiki/PCI_configuration_space#/media/File:Pci-config-space.svg">PCI
	 *      Configuration Space</a>
	 */
	private static final byte DMA_BIT = 0b00000100;

	/**
	 * The vendor id used by Intel devices.
	 * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L145">Intel's Vendor ID</a>
	 */
	private static final short VEN_ID_INTEL = (short) 0x8086;

	/**
	 * The vendor id used by Virtio devices.
	 * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L150">RedHat's Vendor ID</a>
	 */
	private static final short VEN_ID_QUMRANET = (short) 0x1AF4;

	/**
	 * Set of device ids used by ixgbe devices.
	 * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L369-L409">Physical ixgbe device IDs</a>
	 * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L507-L514">Virtual ixgbe device IDs</a>
	 */
	private static final Set<Short> DEV_ID_IXGBE = Set.of(
		// Physical devices
		(short) 0x10B6, (short) 0x1508, (short) 0x10C6, (short) 0x10C7,
		(short) 0x10C8, (short) 0x150B, (short) 0x10DB, (short) 0x10DD,
		(short) 0x10EC, (short) 0x10F1, (short) 0x10E1, (short) 0x10F4,
		(short) 0x10F7, (short) 0x1514, (short) 0x1517, (short) 0x10F8,
		(short) 0x000C, (short) 0x10F9, (short) 0x10FB, (short) 0x11A9,
		(short) 0x1F72, (short) 0x17D0, (short) 0x0470, (short) 0x152A,
		(short) 0x1529, (short) 0x1507, (short) 0x154D, (short) 0x154A,
		(short) 0x1558, (short) 0x1557, (short) 0x10FC, (short) 0x151C,
		(short) 0x154F, (short) 0x1528, (short) 0x1560, (short) 0x15AC,
		(short) 0x15AD, (short) 0x15AE, (short) 0x1563, (short) 0x15AA,
		(short) 0x15AB,
		// Virtual devices
		(short) 0x10ED, (short) 0x152E, (short) 0x1515, (short) 0x1530,
		(short) 0x1564, (short) 0x1565, (short) 0x15A8, (short) 0x15A9
	);

	/**
	 * Set of device ids used by virtio devices.
	 * @see <a href="https://github.com/Juniper/contrail-dpdk/blob/2e83c81bade9a69197929490c68cb98289cb091e/lib/librte_eal/common/include/rte_pci_dev_ids.h#L535">Virtual virtio device IDs</a>
	 */
	private static final Set<Short> DEV_ID_VIRTIO = Set.of((short) 0x1000);

	/** The name of the PCI device being manipulated. */
	@Getter
	private String name;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code config} of PCI device. */
	private SeekableByteChannel config;

	/** The {@link FileChannel} instance used to access the resource {@code resource0} of PCI device. */
	private FileChannel resource;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code unbind} of PCI device driver. */
	private SeekableByteChannel unbind;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code bind} of PCI device driver. */
	private SeekableByteChannel bind;

	/** Direct {@link ByteBuffer} used to read/write from/to the resources of the PCI device. */
	private final ByteBuffer buffer;

	/**
	 * Allocates the resources needed to manipulate the PCI device as fast as possible.
	 * <p>
	 * Basically, stores the PCI device in the member variable {@code name}, creates a {@code rw}-mode {@link
	 * SeekableByteChannel} for the {@code config} resource of the PCI device and a {@code r}-mode {@link
	 * SeekableByteChannel} for the {@code bind} and {@code unbind} resources of the driver of the PCI device.
	 * <p>
	 * To further optimice the creation of {@link Pci} instances, the fields {@link BuildConstants#FORCE_IXGBE} and
	 * {@link BuildConstants#FORCE_VIRTIO} can be set to {@code true} to not try to predict which driver should be used
	 * with the PCI device.
	 * <p>
	 * If no specific driver is being forced, the resource {@code config} will be read to extract the vendor id and the
	 * device id and if there is no match with either {@link #VEN_ID_QUMRANET}, {@link #VEN_ID_INTEL}, {@link
	 * #DEV_ID_IXGBE} or {@link #DEV_ID_VIRTIO}, a {@link RuntimeException} will be thrown stating so.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 * @throws IOException           If reading the {@code config} resource fails while trying to guess the driver.
	 * @see BuildConstants#FORCE_IXGBE
	 * @see BuildConstants#FORCE_VIRTIO
	 */
	public Pci(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		this(pciDevice, BuildConstants.FORCE_IXGBE, BuildConstants.FORCE_VIRTIO);
	}

	/**
	 * Allocates the resources needed to manipulate the PCI device as fast as possible.
	 * <p>
	 * Basically, stores the PCI device in the member variable {@code name}, creates a {@code rw}-mode {@link
	 * SeekableByteChannel} for the {@code config} resource of the PCI device and a {@code r}-mode {@link
	 * SeekableByteChannel} for the {@code bind} and {@code unbind} resources of the driver of the PCI device.
	 * <p>
	 * To further optimice the creation of {@link Pci} instances, the parameters {@code ixgbe} and {@code virtio} can
	 * be set to {@code true} to not try to predict which driver should be used with the PCI device.
	 * <p>
	 * If no specific driver is being forced, the resource {@code config} will be read to extract the vendor id and the
	 * device id and if there is no match with either {@link VEN_ID_QUMRANET}, {@link VEN_ID_INTEL}, {@link
	 * DEV_ID_IXGBE} or {@link DEV_ID_VIRTIO}, a {@link RuntimeException} will be thrown stating so.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @param ixgbe     Whether the PCI device is an {@code ixgbe} device or not.
	 * @param virtio    Whether the PCI device is an {@code virtio} device or not.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 * @throws IOException           If reading the {@code config} resource fails while trying to guess the driver.
	 */
	public Pci(@NonNull final String pciDevice, final boolean ixgbe, final boolean virtio) throws FileNotFoundException, IOException {
		name     = pciDevice;
		buffer   = ByteBuffer.allocateDirect(Math.max(3, name.length())).order(ByteOrder.nativeOrder());
		config   = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, "config"), "rwd").getChannel();
		resource = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, "resource0"), "rwd").getChannel();
		if (ixgbe) {
			unbind = new FileOutputStream(String.format(PCI_IXGBE_PATH_FMT, "unbind")).getChannel();
			bind   = new FileOutputStream(String.format(PCI_IXGBE_PATH_FMT, "bind")).getChannel();
		} else if (virtio) {
			unbind = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "unbind")).getChannel();
			bind   = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "bind")).getChannel();
		} else {
			val vendor = getVendorId(buffer, config);
			boolean isIxgbe = true;
			switch (vendor) {
				case VEN_ID_QUMRANET:
					isIxgbe = false;
				case VEN_ID_INTEL:
					break;
				default:
					throw new RuntimeException(String.format("The PCI device %s is not from a supported vendor", name));
			}
			val device = getDeviceId(buffer, config);
			if (isIxgbe) {
				if (DEV_ID_IXGBE.contains(device)) {
					unbind = new FileOutputStream(String.format(PCI_IXGBE_PATH_FMT, "unbind")).getChannel();
					bind   = new FileOutputStream(String.format(PCI_IXGBE_PATH_FMT, "bind")).getChannel();
				} else {
					throw new RuntimeException(String.format("The Intel PCI device %s is a supported device", name));
				}
			} else {
				if (DEV_ID_VIRTIO.contains(device)) {
					unbind = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "unbind")).getChannel();
					bind   = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "bind")).getChannel();
				} else {
					throw new RuntimeException(String.format("The Qumranet PCI device %s is a supported device", name));
				}
			}
		}
	}

	/**
	 * Reads the vendor id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of
	 * the resource {@code config} of the PCI device.
	 *
	 * @return The vendor id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getVendorId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public short getVendorId() throws IOException {
		log.trace("Reading vendor id of PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 2) {
			buffer.limit(pos + 2);
		} else {
			buffer.clear();
		}
		return getVendorId(buffer, config);
	}

	/**
	 * Reads the device id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of
	 * the resource {@code config} from the PCI device.
	 *
	 * @return The device id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getDeviceId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getDeviceId(ByteBuffer, SeekableByteChannel)
	 */
	public short getDeviceId() throws IOException {
		log.trace("Reading device id of PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 2) {
			buffer.limit(pos + 2);
		} else {
			buffer.clear();
		}
		return getDeviceId(buffer, config);
	}

	/**
	 * Reads the class id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of
	 * the resource {@code config} from the PCI device.
	 *
	 * @return The class id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getClassId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getClassId(ByteBuffer, SeekableByteChannel)
	 */
	public byte getClassId() throws IOException {
		log.trace("Reading class id of PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 3) {
			buffer.limit(pos + 3);
		} else {
			buffer.clear();
		}
		return getClassId(buffer, config);
	}

	/**
	 * Unbinds the driver.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and writes the PCI device
	 * name to the resource {@code unbind} of the PCI device driver.
	 *
	 * @throws IOException If an I/O error occurs when calling {@link #unbindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #unbindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public void unbindDriver() throws IOException {
		log.trace("Unbinding the driver of PCI device {}", name);
		buffer.clear().put(name.getBytes()).flip();
		unbindDriver(buffer, unbind);
	}

	/**
	 * Binds the driver.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and writes the PCI device
	 * name to the resource {@code bind} of the PCI device driver.
	 *
	 * @throws IOException If an I/O error occurs when calling {@link #bindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #bindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public void bindDriver() throws IOException {
		log.trace("Binding driver of PCI device {}", name);
		buffer.clear().put(name.getBytes()).flip();
		bindDriver(buffer, bind);
	}

	/**
	 * Reads the status of the DMA bit.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents 
	 * the resource {@code config} of the PCI device.
	 * 
	 * @return The status of the DMA bit.
	 * @throws IOException If an I/O error occurs when calling {@link #getCommand(ByteBuffer, SeekableByteChannel)}.
	 * @see #getCommand(ByteBuffer, SeekableByteChannel)
	 */
	public boolean isDmaEnabled() throws IOException {
		log.trace("Checking if DMA is enabled on PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 2) {
			buffer.limit(pos + 2);
		} else {
			buffer.clear();
		}
		return (getCommand(buffer, config) & DMA_BIT) != 0;
	}

	/**
	 * Enables the DMA bit.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and overwrites the contents
	 * of the resource {@code config} of the PCI device.
	 * 
	 * @throws IOException If an I/O error occurs when calling {@link #setDma(ByteBuffer, SeekableByteChannel,
	 *                     boolean)}.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	public void enableDma() throws IOException {
		log.trace("Enabling DMA on PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 2) {
			buffer.limit(pos + 2);
		} else {
			buffer.clear();
		}
		setDma(buffer, config, true);
	}

	/**
	 * Disables the DMA bit.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and overwrites the contents
	 * of the resource {@code config} of the PCI device.
	 * 
	 * @throws IOException If an I/O error occurs when calling {@link #setDma(ByteBuffer, SeekableByteChannel,
	 *                     boolean)}.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	public void disableDma() throws IOException {
		log.trace("Disabling DMA on PCI device {}", name);
		val pos = buffer.position();
		if (buffer.capacity() - pos >= 2) {
			buffer.limit(pos + 2);
		} else {
			buffer.clear();
		}
		setDma(buffer, config, false);
	}

	/**
	 * Maps the resource {@code resource0} to a memory region.
	 * 
	 * @return The buffer mapped to the file.
	 * @throws IOException If an I/O error occurs.
	 */
	public MappedByteBuffer mapResource() throws IOException {
		return getMmap(resource);
	}

	/**
	 * Releases all the resources that have been allocated.
	 * <p>
	 * Because Java does not have destructors, the convention is to provide a {@code close} method that releases all
	 * the resources allocated to an instance.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void close() throws IOException {
		config.close();
		resource.close();
		unbind.close();
		bind.close();
	}

	/**
	 * Returns the {@link String} representation of this PCI device.
	 * <p>
	 * The {@link String} representation of this PCI device is nothing more than the path used to access all its
	 * resources. This method has been implemented so that an instance can be used to generate the display text of a
	 * test case.
	 *
	 * @return The path of the PCI device.
	 */
	@Override
	public String toString() {
		return String.format(PCI_PATH_FMT, name);
	}

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Given the name of a PCI device, reads its vendor id.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and reads the contents of the resource {@code
	 * config} from the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @return The vendor id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getVendorId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public static short getVendorId(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Reading vendor id of PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val file = new FileInputStream(resource)) {
			return getVendorId(buffer, file.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, reads its device id.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and reads the contents of the resource {@code
	 * config} from the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @return The device id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getDeviceId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getDeviceId(ByteBuffer, SeekableByteChannel)
	 */
	public static short getDeviceId(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Reading device id of PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val file = new FileInputStream(resource)) {
			return getDeviceId(buffer, file.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, reads its class id.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and reads the contents of the resource {@code
	 * config} from the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @return The class id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getClassId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getClassId(ByteBuffer, SeekableByteChannel)
	 */
	public static byte getClassId(@NonNull String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Reading class id of PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(3).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val stream = new FileInputStream(resource)) {
			return getClassId(buffer, stream.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, unbinds the driver.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} containing the PCI device and writes the contents
	 * to the resource {@code driver/unbind} of the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the driver is already unbound or is bound to another driver.
	 * @throws IOException           If an I/O error occurs when calling {@link #unbindDriver(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #unbindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public static void unbindDriver(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Unbinding driver of PCI device {}", pciDevice);
		val buffer = ByteBuffer.wrap(pciDevice.getBytes());
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "driver/unbind");
		try (val stream = new FileOutputStream(resource, false)) {
			unbindDriver(buffer, stream.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, binds the driver.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} containing the PCI device and writes the contents
	 * to the resource {@code driver/bind} of the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the driver is already unbound or is bound to another driver.
	 * @throws IOException           If an I/O error occurs when calling {@link #bindDriver(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #bindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public static void bindDriver(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Binding driver of PCI device {}", pciDevice);
		val buffer = ByteBuffer.wrap(pciDevice.getBytes());
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "driver/bind");
		try (val stream = new FileOutputStream(resource, false)) {
			bindDriver(buffer, stream.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, reads the status of the DMA bit.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and reads the contents of the resource {@code
	 * config} from the given PCI device.
	 * 
	 * @param pciDevice The name of the PCI device.
	 * @return The status of the DMA bit.
	 * @throws FileNotFoundException If the driver is already unbound or is bound to another driver.
	 * @throws IOException           If an I/O error occurs when calling {@link #getCommand(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getCommand(ByteBuffer, SeekableByteChannel)
	 */
	public static boolean isDmaEnabled(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Checking if DMA is enabled on PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(2);
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val stream = new FileInputStream(resource)) {
			return (getCommand(buffer, stream.getChannel()) & DMA_BIT) != 0;
		}
	}

	/**
	 * Given the name of a PCI device, enables the DMA bit.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and overwrites the contents of the resource {@code
	 * config} from the given PCI device.
	 * 
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the driver is already unbound or is bound to another driver.
	 * @throws IOException           If an I/O error occurs when calling {@link #setDma(ByteBuffer, SeekableByteChannel,
	 *                               boolean)}.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	public static void enableDma(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Enabling DMA on PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(2);
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val raf = new RandomAccessFile(resource, "rwd")) {
			setDma(buffer, raf.getChannel(), true);
		}
	}

	/**
	 * Given the name of a PCI device, disables the DMA bit.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} and overwrites the contents of the resource {@code
	 * config} from the given PCI device.
	 * 
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the driver is already unbound or is bound to another driver.
	 * @throws IOException           If an I/O error occurs when calling {@link #setDma(ByteBuffer, SeekableByteChannel,
	 *                               boolean)}.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	public static void disableDma(@NonNull final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Disabling CMA on PCI device {}", pciDevice);
		val buffer = ByteBuffer.allocate(2);
		val resource = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val raf = new RandomAccessFile(resource, "rwd")) {
			setDma(buffer, raf.getChannel(), false);
		}
	}

	/**
	 * Maps the resource {@code resource0} from the PCI device to a memory region.
	 * 
	 * @param pciDevice The PCI device.
	 * @return The memory mapped file.
	 * @throws FileNotFoundException If the resource {@code resource0} does not exist.
	 * @throws IOException           If an I/O error occurs.
	 */
	public static MappedByteBuffer mapResource(@NonNull final String pciDevice) throws FileNotFoundException,
			IOException {
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "resource0");
		val fileChannel = new RandomAccessFile(path, "rwd").getChannel();
		return getMmap(fileChannel);
	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Reads the required bytes to get the vendor id.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit and updates the {@code
	 * channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The vendor id.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code channel} and writing them to
	 *                     the {@code buffer}.
	 */
	private static short getVendorId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.position(0).read(buffer.mark());
		if (bytes < 2) {
			log.warn("Could't read the exact amount of bytes needed to read the vendor id");
		}
		return buffer.reset().getShort();
	}

	/**
	 * Reads the required bytes to get the device id.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit and updates the {@code
	 * channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The device id.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code channel} and writing them to
	 *                     the {@code buffer}.
	 */
	private static short getDeviceId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.position(2).read(buffer.mark());
		if (bytes < 2) {
			log.warn("Could't read the exact amount of bytes needed to read the device id");
		}
		return buffer.reset().getShort();
	}

	/**
	 * Reads the required bytes to get the class id.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit and updates the {@code
	 * channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The class id.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code channel} and writing them to
	 *                     the {@code buffer}.
	 */
	private static byte getClassId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val pos = buffer.position();
		val bytes = channel.position(9).read(buffer);
		if (bytes < 3) {
			log.warn("Could't read the exact amount of bytes needed to read the class id");
		}
		if (buffer.order() == ByteOrder.BIG_ENDIAN) {
			return buffer.get(pos);
		}
		return buffer.get(pos + 2);
	}

	/**
	 * Writes the required bytes to unbind the driver.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit and that the {@code channel}'s
	 * position is correctly set.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read from.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be written to.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code buffer} and writing them to
	 *                     the {@code channel}.
	 */
	private static void unbindDriver(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.write(buffer);
		if (bytes < 12) {
			log.warn("Couldn't write the exact amount of bytes needed to unbind the driver");
		}
	}

	/**
	 * Writes the required bytes to bind the driver.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit and that the {@code channel}'s
	 * position is correctly set.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read from.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be written to.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code buffer} and writing them to
	 *                     the {@code channel}.
	 */
	private static void bindDriver(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.write(buffer);
		log.trace("Internal call to private implementation of bindDriver");
		if (bytes < 12) {
			log.warn("Couldn't write the exact amount of bytes needed to bind the driver");
		}
	}

	/**
	 * Reads the required bytes to get the command field.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The command field.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code channel} and writing them to
	 *                     the {@code buffer}.
	 */
	private static short getCommand(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.position(4).read(buffer.mark());
		if (bytes < 2) {
			log.warn("Couldn't read the exact amount of bytes needed to read the command");
		}
		return buffer.reset().getShort();
	}

	/**
	 * Writes the required bytes to enable DMA.
	 * <p>
	 * This method assumes that the {@code buffer} has the correct capacity or a limit.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read/written from/to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read/written from/to.
	 * @param status  The status of the DMA bit.
	 * @throws IOException If an I/O error occurs when reading/writing the bytes from/to the {@code buffer} and writing
	 *                     reading them to/from the {@code channel}.
	 */
	private static void setDma(final ByteBuffer buffer, final SeekableByteChannel channel, final boolean status)
			throws IOException {
		val pos = buffer.position();
		var command = getCommand(buffer, channel);
		if (status) {
			command |= DMA_BIT;
		} else {
			command &= ~DMA_BIT;
		}
		buffer.position(pos).putShort(command);
		val octets = channel.position(4).write(buffer.position(pos));
		if (octets < 2) {
			log.warn("Couldn't write the exact amount of bytes needed to set the DMA status");
		}
	}

	/**
	 * Creates a {@link MappedByteBuffer} using a {@link FileChannel} associated with a file.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param channel The channel used to map the file to memory.
	 * @return The file mapped to memory.
	 * @throws IOException If an I/O error occurs.
	 */
	private static MappedByteBuffer getMmap(final FileChannel channel) throws IOException {
		try {
			val mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
			mmap.order(ByteOrder.nativeOrder());
			return mmap;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

}
