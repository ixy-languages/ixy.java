package de.tum.in.net.ixy.pci;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * PCI static utils with instantiation support.
 * <p>
 * This class allows reading and writing to arbitrary PCI name's resources and also instantiation, so that all the
 * reading/writing targets the same PCI name. This is useful to reduce the verbosity of the code that only needs to
 * read/write from/to a specific PCI name.
 * <p>
 * If the static methods are used, a disposable non-direct {@link ByteBuffer} is allocated to read/write from/to the
 * file, otherwise a direct {@link ByteBuffer} big enough to read all the different PCI properties is allocated.
 * <p>
 * The required resources when instantiating are the collection of all resources used by the static methods, which are
 * {@code config}, {@code driver/unbind} and {@code driver/bind}.
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

	/** The name of the PCI device being manipulated. */
	@Getter
	private String name;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code config} of PCI device. */
	private SeekableByteChannel config;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code driver/unbind} of PCI device. */
	private SeekableByteChannel unbind;

	/** The {@link SeekableByteChannel} instance used to access the resource {@code driver/bind} of PCI device. */
	private SeekableByteChannel bind;

	/** Direct {@link ByteBuffer} used to read/write from/to the resources of the PCI device. */
	private final ByteBuffer buffer;

	/**
	 * Allocates the resources needed to read/write from/to the PCI device as fast as possible.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 */
	public Pci(@NonNull final String pciDevice) throws FileNotFoundException {
		name   = pciDevice;
		buffer = ByteBuffer.allocateDirect(Math.max(4, name.length())).order(ByteOrder.nativeOrder());
		config = new FileInputStream(String.format(PCI_RES_PATH_FMT, name, "config")).getChannel();
		unbind = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "unbind")).getChannel();
		bind   = new FileOutputStream(String.format(PCI_VIRTIO_PATH_FMT, "bind")).getChannel();
	}

	/**
	 * Reads the vendor id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of the
	 * resource {@code config} from the PCI device.
	 *
	 * @return The vendor id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getVendorId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public short getVendorId() throws IOException {
		log.trace("Reading vendor id of PCI device {}", name);
		return getVendorId(buffer, config);
	}

	/**
	 * Reads the device id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of the
	 * resource {@code config} from the PCI device.
	 *
	 * @return The device id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getDeviceId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getDeviceId(ByteBuffer, SeekableByteChannel)
	 */
	public short getDeviceId() throws IOException {
		log.trace("Reading device id of PCI device {}", name);
		return getDeviceId(buffer, config);
	}

	/**
	 * Reads the class id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of the
	 * resource {@code config} from the PCI device.
	 *
	 * @return The class id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getClassId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getClassId(ByteBuffer, SeekableByteChannel)
	 */
	public byte getClassId() throws IOException {
		log.trace("Reading class id of PCI device {}", name);
		return getClassId(buffer, config);
	}

	/**
	 * Unbinds the driver.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and writes the PCI device
	 * name to the resource {@code driver/unbind} of the PCI device.
	 *
	 * @throws IOException If an I/O error occurs when calling {@link #unbindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #unbindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public void unbindDriver() throws IOException {
		buffer.position(0).put(name.getBytes());
		unbindDriver(buffer, unbind);
	}

	/**
	 * Binds the driver.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and writes the PCI device
	 * name to the resource {@code driver/bind} of the PCI device.
	 *
	 * @throws IOException If an I/O error occurs when calling {@link #bindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #bindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public void bindDriver() throws IOException {
		buffer.position(0).put(name.getBytes());
		bindDriver(buffer, bind);
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
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val file = new FileInputStream(path)) {
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
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val file = new FileInputStream(path)) {
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
		val buffer = ByteBuffer.allocate(1);
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		try (val file = new FileInputStream(path)) {
			return getClassId(buffer, file.getChannel());
		}
	}

	/**
	 * Given the name of a PCI device, unbinds the driver.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} containing the PCI device and writes the contents
	 * to the resource {@code driver/unbind} of the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws IOException If an I/O error occurs when calling {@link #unbindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #unbindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public static void unbindDriver(@NonNull final String pciDevice) throws IOException {
		val buffer = ByteBuffer.wrap(pciDevice.getBytes());
		val resource = String.format(PCI_VIRTIO_PATH_FMT, "unbind");
		try (val stream = new FileOutputStream(resource, false)) {
			unbindDriver(buffer, stream.getChannel());
		} catch (FileNotFoundException e) {
			log.warn("Driver not loaded", e);
		}
	}

	/**
	 * Given the name of a PCI device, binds the driver.
	 * <p>
	 * This method creates a disposable non-direct {@link ByteBuffer} containing the PCI device and writes the contents
	 * to the resource {@code driver/bind} of the given PCI device.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws IOException If an I/O error occurs when calling {@link #bindDriver(ByteBuffer, SeekableByteChannel)}.
	 * @see #bindDriver(ByteBuffer, SeekableByteChannel)
	 */
	public static void bindDriver(@NonNull final String pciDevice) throws IOException {
		val buffer = ByteBuffer.wrap(pciDevice.getBytes());
		val resource = String.format(PCI_VIRTIO_PATH_FMT, "bind");
		try (val stream = new FileOutputStream(resource, false)) {
			bindDriver(buffer, stream.getChannel());
		} catch (FileNotFoundException e) {
			log.warn("Driver not loaded", e);
		}
	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Reads the required bytes to get the vendor id.
	 * <p>
	 * This method sets the {@code buffer} position to the origin, the {@code channel} position where the device id
	 * should be located and performs a read operation without setting a limit.
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
		val bytes = channel.position(0).read(buffer.position(0));
		if (bytes != 2) {
			log.warn("Could't read the exact amount of bytes needed to read the vendor id");
		}
		return buffer.getShort(0);
	}

	/**
	 * Reads the required bytes to get the device id.
	 * <p>
	 * This method sets the {@code buffer} position to the origin, the {@code channel} position where the device id
	 * should be located and performs a read operation without setting a limit.
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
		val bytes = channel.position(2).read(buffer.position(0));
		if (bytes != 2) {
			log.warn("Could't read the exact amount of bytes needed to read the device id");
		}
		return buffer.getShort(0);
	}

	/**
	 * Reads the required bytes to get the class id.
	 * <p>
	 * This method sets the {@code buffer} position to the origin, the {@code channel} position where the device id
	 * should be located and performs a read operation without setting a limit.
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
	private static byte getClassId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.position(11).read(buffer.position(0));
		if (bytes != 4) {
			log.warn("Could't read the exact amount of bytes needed to read the class id");
		}
		return buffer.get(0);
	}

	/**
	 * Writes the required bytes to unbind the driver.
	 * <p>
	 * This method sets the {@code buffer} position to the origin, the {@code channel} position to the origin and
	 * performs a write operation without setting a limit.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 * 
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read from.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be written to.
	 * @return The device id.
	 * @throws IOException If an I/O error occurs when reading the bytes from the {@code buffer} and writing them to
	 *                     the {@code channel}.
	 */
	private static void unbindDriver(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val bytes = channel.position(0).write(buffer.position(0));
		if (bytes != 12) {
			log.warn("Couldn't write the exact amount of bytes needed to unbind the driver");
		}
	}

	/**
	 * Writes the required bytes to bind the driver.
	 * <p>
	 * This method sets the {@code buffer} position to the origin, the {@code channel} position to the origin and
	 * performs a write operation without setting a limit.
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
		val bytes = channel.position(0).write(buffer.position(0));
		if (bytes != 12) {
			log.warn("Couldn't write the exact amount of bytes needed to bind the driver");
		}
	}

}
