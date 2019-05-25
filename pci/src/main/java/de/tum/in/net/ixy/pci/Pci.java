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
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * PCI static utils with instantiation support.
 * <p>
 * This class allows direct manipulation of arbitrary PCI devices through a set of static methods that access the
 * pseudo file system {@code sysfs} provided by the Linux kernel itself.
 * <p>
 * To avoid passing around the PCI device to all the static methods, this class can be instantiated. This allows the
 * instance to remember an use always the same PCI device and to allocate a direct {@link ByteBuffer} which can be
 * reused in all operations that require the manipulation of the pseudo file system {@code sysfs}. Moreover, the static
 * methods will throw a {@link FileNotFoundException} when trying to access any resource they need ({@code config},
 * {@code resource0},
 * {@code bind} and {@code unbind}, whereas the non-static methods never throw it, being {@link Pci#Pci(String, String)}
 * the exception.
 * <p>
 * The required resources when instantiating this class, which will cause a {@link FileNotFoundException} to be thrown
 * if not found, are the collection of all resources used by the individual static methods. These are:
 * <ul>
 *   <li>The {@code config} resource of the PCI device, usually found under {@code
 *       /sys/bus/pci/devices/<device>/config}.</li>
 *   <li>The {@code resource0} resource of the PCI device, usually available under {@code
 *       /sys/bus/pci/devices/<device>/resource0}.</li>
 *   <li>The {@code bind} resource of the driver of the PCI device, usually available under {@code
 *       /sys/bus/pci/drivers/<driver>/bindChannel}.</li>
 *   <li>The {@code unbind} resource of the driver of the PCI device, usually available under {@code
 *       /sys/bus/pci/drivers/<driver>/unbindChannel}.</li>
 * </ul>
 *
 * @author Esaú García Sánchez-Torija
 * @see ByteBuffer
 */
@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class Pci {

	////////////////////////////////////////////////////// PATHS ///////////////////////////////////////////////////////

	/** Path format string used to build the path to a PCI device. */
	private static final String PCI_PATH_FMT = "/sys/bus/pci/devices/%s";

	/** Path format string used to build the path to a resource of a PCI device. */
	private static final String PCI_RES_PATH_FMT = PCI_PATH_FMT + "/%s";

	/** Path format string used to build the path to a PCI driver. */
	private static final String PCI_DRV_PATH_FMT = "/sys/bus/pci/drivers/%s";

	/** Path format string used to build the path to a PCI driver resource. */
	private static final String PCI_DRV_RES_PATH_FMT = PCI_DRV_PATH_FMT + "/%s";

	/** The resource file {@code config}. */
	private static final String PCI_RES_CONFIG = "config";

	/** The resource file {@code resource0}. */
	private static final String PCI_RES_MAP = "resource0";

	/** The resource file {@code bind}. */
	private static final String PCI_RES_BIND = "bind";

	/** The resource file {@code bind} for the static methods. */
	private static final String PCI_RES_BIND_STATIC = "driver/bind";

	/** The resource file {@code unbind}. */
	private static final String PCI_RES_UNBIND = "unbind";

	/** The resource file {@code unbind} for the static methods. */
	private static final String PCI_RES_UNBIND_STATIC = "driver/unbind";

	////////////////////////////////////////////////////// BYTES ///////////////////////////////////////////////////////

	/** The minimum capacity a {@link ByteBuffer} needs to read the vendor id. */
	private static final int BYTES_VENDOR = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to read the device id. */
	private static final int BYTES_DEVICE = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to read the class id. */
	private static final int BYTES_CLASS = 3;

	/** The minimum capacity a {@link ByteBuffer} needs to read the command. */
	private static final int BYTES_COMMAND = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to set the DMA status. */
	private static final int BYTES_DMA = BYTES_COMMAND;

	/** The minimum capacity a {@link ByteBuffer} needs to read the mapability. */
	private static final int BYTES_MAPABLE = 4;

	/** The minimum capacity a {@link ByteBuffer} needs to write to manipulate the driver binding. */
	private static final int BYTES_BIND = 12;

	/** The minimum capacity a {@link ByteBuffer} needs to write to manipulate the driver binding. */
	private static final int BYTES_UNBIND = 12;

	/** The minimum capacity a {@link ByteBuffer} needs to for any of the previous resources. */
	private static final int BYTES_MIN = IntStream.of(BYTES_VENDOR, BYTES_DEVICE,
			                                          BYTES_CLASS,  BYTES_COMMAND,
			                                          BYTES_DMA,    BYTES_BIND,
			                                          BYTES_UNBIND, BYTES_MAPABLE).max().getAsInt();

	////////////////////////////////////////////////////// MODES ///////////////////////////////////////////////////////

	/** The read and write mode used to access the resources {@code bind} and {@code unbind}. */
	private static final String READ_WRITE_DATA = "rwd";

	//////////////////////////////////////////////////// BIT MASKS /////////////////////////////////////////////////////

	/**
	 * Bit mask used to manipulate the DMA status.
	 * @see <a href="https://en.wikipedia.org/wiki/PCI_configuration_space#/media/File:Pci-config-space.svg">PCI Configuration Space</a>
	 */
	private static final byte DMA_BIT = 0b00000100;

	//////////////////////////////////////////////// EXCEPTION MESSAGES  ///////////////////////////////////////////////

	/** The message of the exception thrown when the device being mapped is a legacy device. */
	private static final String LEGACY_MESSAGE = "Invalid argument";

	/**
	 * The name of the PCI device being manipulated.
	 * ----------------------- GETTER -----------------------
	 * Returns the name of the PCI device being manipulated.
	 *
	 * @return The name of the PCI device.
	 */
	@Getter
	private String name;

	/** The {@link SeekableByteChannel} used to access the resource {@code config} of the PCI device. */
	private transient SeekableByteChannel config;

	/** The {@link FileChannel} used to access the resource {@code resource0} of the PCI device. */
	private transient FileChannel resource;

	/** The {@link SeekableByteChannel} used to access the resource {@code bind} of the PCI device's driver. */
	private transient SeekableByteChannel bindChannel;

	/** The {@link SeekableByteChannel} used to access the resource {@code unbind} of the PCI device's driver. */
	private transient SeekableByteChannel unbindChannel;

	/** The direct {@link ByteBuffer} used to read/write from/to all the {@link SeekableByteChannel}. */
	private transient final ByteBuffer buffer;

	/**
	 * Allocates the resources needed to make any future PCI device manipulation as fast as possible.
	 * <p>
	 * Basically, stores the PCI device in the member variable {@code name}, creates a {@code rw}-mode {@link
	 * SeekableByteChannel} for the resource {@code config} and a {@code w}-mode {@link SeekableByteChannel} for the
	 * resources {@code bind} and {@code unbind}.
	 * <p>
	 * To further optimise the creation of {@link Pci} instances, the parameter {@code driver} can be set to assume what
	 * driver needs to be used. If no specific driver is being enforced, the instantiation will fail and an {@link
	 * IllegalArgumentException} will be thrown.
	 *
	 * @param device The name of the PCI device.
	 * @param driver The driver used by the device.
	 * @throws FileNotFoundException If the specified PCI device does not exist or any of its required resources.
	 */
	@SuppressWarnings("resource")
	public Pci(@NonNull String device, @NonNull final String driver) throws FileNotFoundException {
		if (BuildConfig.DEBUG) log.trace("Creating a PCI instance to manipulate the device {}", device);
		val min = Math.min(BYTES_MIN, device.length());
		name = device;
		buffer = ByteBuffer.allocateDirect(min).order(ByteOrder.nativeOrder());
		config = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_CONFIG), READ_WRITE_DATA).getChannel();
		resource = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_MAP), READ_WRITE_DATA).getChannel();
		bindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_BIND)).getChannel();
		unbindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_UNBIND)).getChannel();
	}

	/**
	 * Reads the vendor id.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return The vendor id.
	 * @throws IOException If an I/O error occurs.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	public short getVendorId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading vendor id of PCI device {}", name);
		return getVendorId(buffer.clear().limit(BYTES_VENDOR), config);
	}

	/**
	 * Reads the device id.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return The device id.
	 * @throws IOException If an I/O error occurs.
	 * @see #getDeviceId(ByteBuffer, SeekableByteChannel)
	 */
	public short getDeviceId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading device id of PCI device {}", name);
		return getDeviceId(buffer.clear().limit(BYTES_DEVICE), config);
	}

	/**
	 * Reads the class id.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return The class id.
	 * @throws IOException If an I/O error occurs.
	 * @see #getClassId(ByteBuffer, SeekableByteChannel)
	 */
	public byte getClassId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading class id of PCI device {}", name);
		return getClassId(buffer.clear().limit(BYTES_CLASS), config);
	}

	/**
	 * Reads the status of the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return The status of the DMA bit.
	 * @throws IOException If an I/O error occurs.
	 * @see #getCommand(ByteBuffer, SeekableByteChannel)
	 */
	public boolean isDmaEnabled() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Checking if DMA is enabled on PCI device {}", name);
		return (getCommand(buffer.clear().limit(2), config) & DMA_BIT) != 0;
	}

	/**
	 * Enables the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return This PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	@NonNull
	public Pci enableDma() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Enabling DMA on PCI device {}", name);
		setDma(buffer.clear().limit(BYTES_DMA), config, true);
		return this;
	}

	/**
	 * Disables the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return This PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	@NonNull
	public Pci disableDma() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Disabling DMA on PCI device {}", name);
		setDma(buffer.clear().limit(BYTES_DMA), config, false);
		return this;
	}

	/**
	 * Checks if the PCI device can be mapped into memory.
	 * <p>
	 * This method uses resource {@code config} and the internal direct {@link #buffer}.
	 *
	 * @return The PCI device mapability.
	 * @throws IOException If an I/O error occurs.
	 * @see #mapable(ByteBuffer, SeekableByteChannel)
	 */
	public boolean mapable() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Checking mapability of PCI device {}", name);
		return mapable(buffer.clear().limit(BYTES_MAPABLE), config);
	}

	/**
	 * Returns the memory mapping of the memory of the PCI device.
	 * <p>
	 * If the device is a legacy device, the mapping will fail and null will be returned.
	 *
	 * @return The memory mapped region.
	 * @throws IOException If an I/O error occurs.
	 * @see #getMmap(FileChannel)
	 */
	@Nullable
	public MappedByteBuffer map() throws IOException {
		try {
			return getMmap(resource);
		} catch (IOException e) {
			if (LEGACY_MESSAGE.equals(e.getMessage())) {
				return null;
			}
			throw e;
		}
	}

	/**
	 * Releases all the resources that have been allocated.
	 * <p>
	 * Because Java does not have destructors, the convention is to provide a {@code close} method that releases all the
	 * resources allocated.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void close() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Closing resources of PCI device {}", name);
		config.close();
		resource.close();
		unbindChannel.close();
		bindChannel.close();
	}

	/**
	 * Binds the driver.
	 * <p>
	 * This method uses resource {@code bind} and the internal direct {@link #buffer}.
	 *
	 * @return This PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see #bind(ByteBuffer, SeekableByteChannel)
	 */
	@NonNull
	public Pci bind() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Binding driver of PCI device {}", name);
		buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip();
		bind(buffer, bindChannel);
		return this;
	}

	/**
	 * Unbinds the driver.
	 * <p>
	 * This method uses resource {@code unbind} and the internal direct {@link #buffer}.
	 *
	 * @return This PCI device.
	 * @throws IOException If an I/O error occurs.
	 * @see #unbind(ByteBuffer, SeekableByteChannel)
	 */
	@NonNull
	public Pci unbind() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Unbinding the driver of PCI device {}", name);
		buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip();
		unbind(buffer, unbindChannel);
		return this;
	}

	/**
	 * Returns the {@link String} representation of this PCI device.
	 * <p>
	 * The {@link String} representation of this PCI device is nothing but the PCI device name that was used to
	 * instantiate this class. This method has been implemented so that an instance can be used to generate the display
	 * text of a parametrized test case.
	 *
	 * @return The PCI device.
	 */
	@Override
	@NonNull
	public String toString() {
		return name;
	}

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Reads the vendor id.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @return The vendor id.
	 * @throws FileNotFoundException If the given {@code device} or the resource file {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #getVendorId(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static short getVendorId(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Reading vendor id of PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_VENDOR).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val file = new FileInputStream(resource)) {
			return getVendorId(buffer, file.getChannel());
		}
	}

	/**
	 * Reads the device id.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @return The device id.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #getDeviceId(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static short getDeviceId(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Reading device id of PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_DEVICE).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val file = new FileInputStream(resource)) {
			return getDeviceId(buffer, file.getChannel());
		}
	}

	/**
	 * Reads the class id.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @return The class id.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #getClassId(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static byte getClassId(@NonNull String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Reading class id of PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_CLASS).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val stream = new FileInputStream(resource)) {
			return getClassId(buffer, stream.getChannel());
		}
	}

	/**
	 * Reads the status of the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @return The status of the DMA bit.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #getCommand(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static boolean isDmaEnabled(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Checking if DMA is enabled on PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_DMA);
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val stream = new FileInputStream(resource)) {
			return (getCommand(buffer, stream.getChannel()) & DMA_BIT) != 0;
		}
	}

	/**
	 * Enables the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static void enableDma(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Enabling DMA on PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_DMA);
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val raf = new RandomAccessFile(resource, READ_WRITE_DATA)) {
			setDma(buffer, raf.getChannel(), true);
		}
	}

	/**
	 * Disables the DMA bit.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #setDma(ByteBuffer, SeekableByteChannel, boolean)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static void disableDma(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Disabling DMA on PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_DMA);
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val raf = new RandomAccessFile(resource, READ_WRITE_DATA)) {
			setDma(buffer, raf.getChannel(), false);
		}
	}

	/**
	 * Checks if the PCI device can be mapped into memory.
	 * <p>
	 * This method uses resource {@code config} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @return The PCI device mapability.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code config} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #mapable(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static boolean mapable(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Checking mapability of PCI device {}", device);
		val buffer = ByteBuffer.allocate(BYTES_MAPABLE).order(ByteOrder.nativeOrder());
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_CONFIG);
		try (val stream = new FileInputStream(resource)) {
			return mapable(buffer, stream.getChannel());
		}
	}

	/**
	 * Returns the memory mapping of the memory of the PCI device.
	 * <p>
	 * If the device is a legacy device, the mapping will fail and null will be returned.
	 *
	 * @param device The PCI device.
	 * @return The memory mapped region.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code resource0} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #getMmap(FileChannel)
	 */
	@Nullable
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static MappedByteBuffer map(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.trace("Mapping resource0 of PCI device {}", device);
		val path = String.format(PCI_RES_PATH_FMT, device, PCI_RES_MAP);
		try (val file = new RandomAccessFile(path, READ_WRITE_DATA)) {
			return getMmap(file.getChannel());
		} catch (IOException e) {
			if (LEGACY_MESSAGE.equals(e.getMessage())) {
				return null;
			}
			throw e;
		}
	}

	/**
	 * Binds the driver.
	 * <p>
	 * This method uses resource {@code bind} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code driver/bindChannel} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #bind(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static void bind(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Binding driver of PCI device {}", device);
		val buffer = ByteBuffer.wrap(device.getBytes(StandardCharsets.UTF_8));
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_BIND_STATIC);
		try (val stream = new FileOutputStream(resource, false)) {
			bind(buffer, stream.getChannel());
		}
	}

	/**
	 * Unbinds the driver.
	 * <p>
	 * This method uses resource {@code unbind} and a temporal non-direct {@link ByteBuffer}.
	 *
	 * @param device The PCI device.
	 * @throws FileNotFoundException If the given {@code device} or its resource {@code driver/unbindChannel} do not exist.
	 * @throws IOException If an I/O error occurs.
	 * @see #unbind(ByteBuffer, SeekableByteChannel)
	 */
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public static void unbind(@NonNull final String device) throws FileNotFoundException, IOException {
		if (BuildConfig.DEBUG) log.debug("Unbinding driver of PCI device {}", device);
		val buffer = ByteBuffer.wrap(device.getBytes(StandardCharsets.UTF_8));
		val resource = String.format(PCI_RES_PATH_FMT, device, PCI_RES_UNBIND_STATIC);
		try (val stream = new FileOutputStream(resource, false)) {
			unbind(buffer, stream.getChannel());
		}
	}

	///////////////////////////////////////////////// INTERNAL METHODS /////////////////////////////////////////////////

	/**
	 * Reads the required bytes to get the vendor id.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 2, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The vendor id.
	 * @throws IOException If an I/O error occurs.
	 */
	private static short getVendorId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(0).read(buffer.mark());
			if (bytes < BYTES_VENDOR) log.warn("Could't read the exact amount of bytes needed to read the vendor id");
		} else {
			channel.position(0).read(buffer.mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Reads the required bytes to get the device id.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 2, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The device id.
	 * @throws IOException If an I/O error occurs.
	 */
	private static short getDeviceId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(2).read(buffer.mark());
			if (bytes < BYTES_DEVICE) log.warn("Could't read the exact amount of bytes needed to read the device id");
		} else {
			channel.position(2).read(buffer.mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Reads the required bytes to get the class id.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 3, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The class id.
	 * @throws IOException If an I/O error occurs.
	 */
	private static byte getClassId(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		val pos = buffer.position();
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(9).read(buffer);
			if (bytes < BYTES_CLASS) log.warn("Could't read the exact amount of bytes needed to read the class id");
		} else {
			channel.position(9).read(buffer);
		}
		return (buffer.order() == ByteOrder.BIG_ENDIAN) ? buffer.get(pos) : buffer.get(pos + 2);
	}

	/**
	 * Reads the required bytes to get the command field.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 2, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be written to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read from.
	 * @return The command field.
	 * @throws IOException If an I/O error occurs.
	 */
	private static short getCommand(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(4).read(buffer.mark());
			if (bytes < BYTES_COMMAND) log.warn("Couldn't read the exact amount of bytes needed to read the command");
		} else {
			channel.position(4).read(buffer.mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Writes the required bytes to enable DMA.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 2, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read/written from/to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read/written from/to.
	 * @param status  The status of the DMA bit.
	 * @throws IOException If an I/O error occurs.
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
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(4).write(buffer.position(pos));
			if (bytes < BYTES_DMA) log.warn("Couldn't write the exact amount of bytes needed to set the DMA status");
		} else {
			channel.position(4).write(buffer.position(pos));
		}
	}

	/**
	 * Reads the required bytes to know if the device can be mapped to memory.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 4, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read/written from/to.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be read/written from/to.
	 * @throws IOException If an I/O error occurs.
	 */
	private static boolean mapable(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = channel.position(0x10).read(buffer.mark());
			if (bytes < BYTES_MAPABLE) log.warn("Couldn't read the exact amount of bytes needed to read a BAR");
		} else {
			channel.position(0x10).read(buffer.mark());
		}
		return (buffer.reset().getInt() & 0x00000001) == 0;
	}

	/**
	 * Creates a {@link MappedByteBuffer} using a {@link FileChannel} associated with a file.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param channel The channel used to map the file to memory.
	 * @return The memory mapped file.
	 * @throws IOException If an I/O error occurs.
	 */
	@NonNull
	private static MappedByteBuffer getMmap(final FileChannel channel) throws IOException {
		val mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
		return (MappedByteBuffer) mmap.order(ByteOrder.nativeOrder());
	}

	/**
	 * Writes the required bytes to bindChannel the driver.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 12, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read from.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be written to.
	 * @throws IOException If an I/O error occurs.
	 */
	private static void bind(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG ) {
			val bytes = channel.write(buffer);
			if (bytes < BYTES_BIND) log.warn("Couldn't write the exact amount of bytes needed to bindChannel the driver");
		} else {
			channel.write(buffer);
		}
	}

	/**
	 * Writes the required bytes to unbindChannel the driver.
	 * <p>
	 * This method assumes that the {@code buffer} has a capacity or a limit big enough to read the required amount of
	 * bytes, which is 12, and updates the {@code channel}'s position.
	 * <p>
	 * This method exists only to reduce the amount of code used in the rest of publicly available methods. This method
	 * should be inline, but Java does not support such specifier.
	 *
	 * @param buffer  The {@link ByteBuffer} where the bytes will be read from.
	 * @param channel The {@link SeekableByteChannel} where the bytes will be written to.
	 * @throws IOException If an I/O error occurs.
	 */
	private static void unbind(final ByteBuffer buffer, final SeekableByteChannel channel) throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = channel.write(buffer);
			if (bytes < BYTES_UNBIND) log.warn("Couldn't write the exact amount of bytes needed to unbindChannel the driver");
		} else {
			channel.write(buffer);
		}
	}

}
