package de.tum.in.net.ixy.pci;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

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
 * {@code config}.
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

	/** The name of the PCI device being manipulated. */
	@Getter
	private String name;

	/** The {@link RandomAccessFile} instance used to access the resource {@code config} of PCI device. */
	private RandomAccessFile config;

	/** Direct {@link ByteBuffer} used to read/write from/to the resources of the PCI device. */
	private final ByteBuffer buffer = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder());

	/**
	 * Allocates the resources needed to read/write from/to the PCI device as fast as possible.
	 *
	 * @param pciDevice The name of the PCI device.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 */
	public Pci(@NonNull final String pciDevice) throws FileNotFoundException {
		name = pciDevice;
		config = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, "config"), "r");
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
		return getVendorId(buffer, config.getChannel());
	}

	/**
	 * Reads the device id.
	 * <p>
	 * This method uses the previously allocated direct {@link ByteBuffer} {@link #buffer} and reads the contents of the
	 * resource {@code config} from the PCI device.
	 *
	 * @return The device id of the PCI device.
	 * @throws FileNotFoundException If the given {@code pciDevice} or its resource {@code config} do not exist.
	 * @throws IOException           If an I/O error occurs when calling {@link #getVendorId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public short getDeviceId() throws IOException {
		log.trace("Reading device id of PCI device {}", name);
		return getDeviceId(buffer, config.getChannel());
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
	public static short getVendorId(final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Reading vendor id of PCI device {}", pciDevice);
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		val file = new RandomAccessFile(path, "r");
		val buffer = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder());
		try {
			return getVendorId(buffer, file.getChannel());
		} finally {
			file.close();
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
	 * @throws IOException           If an I/O error occurs when calling {@link #getVendorId(ByteBuffer,
	 *                               SeekableByteChannel)}.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public static short getDeviceId(final String pciDevice) throws FileNotFoundException, IOException {
		log.trace("Reading device id of PCI device {}", pciDevice);
		val path = String.format(PCI_RES_PATH_FMT, pciDevice, "config");
		val file = new RandomAccessFile(path, "r");
		val buffer = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder());
		try {
			return getDeviceId(buffer, file.getChannel());
		} finally {
			file.close();
		}
	}


	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Reads the required bytes to get the vendor id.
	 * <p>
	 * This method updates the {@code buffer} position to the origin, the {@code channel} position where the device id
	 * should be located and performs a read operation without setting a limit.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link ReadableByteChannel} where the bytes will be read from.
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
	 * This method updates the {@code buffer} position to the origin, the {@code channel} position where the device id
	 * should be located and performs a read operation without setting a limit.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link ReadableByteChannel} where the bytes will be read from.
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

}
