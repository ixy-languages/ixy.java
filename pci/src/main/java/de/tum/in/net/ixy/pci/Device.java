package de.tum.in.net.ixy.pci;

import de.tum.in.net.ixy.generic.IxyDevice;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Simple implementation of an {@link de.tum.in.net.ixy.generic.IxyPciDevice}.
 * <p>
 * It extends the class {@link IxyDevice} to prevent running into multiple-inheritance problems.
 *
 * @author Esau García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true, callSuper = true)
@SuppressWarnings({"ConstantConditions", "IOResourceOpenedButNotSafelyClosed", "PMD.BeanMembersShouldSerialize", "resource"})
public abstract class Device extends IxyDevice {

	////////////////////////////////////////////////////// PATHS ///////////////////////////////////////////////////////

	/** Path format string used to build the path to a PCI device. */
	private static final @NotNull String PCI_PATH_FMT = String.join(File.separator, "", "sys", "bus", "pci", "devices", "%s");

	/** Path format string used to build the path to a resource of a PCI device. */
	private static final @NotNull String PCI_RES_PATH_FMT = String.join(File.separator, PCI_PATH_FMT, "%s");

	/** Path format string used to build the path to a PCI driver. */
	private static final @NotNull String PCI_DRV_PATH_FMT = String.join(File.separator, "", "sys", "bus", "pci", "drivers", "%s");

	/** Path format string used to build the path to a PCI driver resource. */
	private static final @NotNull String PCI_DRV_RES_PATH_FMT = String.join(File.separator, PCI_DRV_PATH_FMT, "%s");

	/** The resource file {@code config}. */
	private static final @NotNull String PCI_RES_CONFIG = "config";

	/** The resource file {@code resource0}. */
	private static final @NotNull String PCI_RES_MAP = "resource0";

	/** The resource file {@code bind}. */
	private static final @NotNull String PCI_RES_BIND = "bind";

	/** The resource file {@code unbind}. */
	private static final @NotNull String PCI_RES_UNBIND = "unbind";

	////////////////////////////////////////////////////// BYTES ///////////////////////////////////////////////////////

	/** The minimum capacity a {@link ByteBuffer} needs to read the vendor id. */
	private static final int BYTES_VENDOR = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to read the device id. */
	private static final int BYTES_DEVICE = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to read the class id. */
	private static final int BYTES_CLASS = 3;

	/** The minimum capacity a {@link ByteBuffer} needs to read the command field. */
	private static final int BYTES_COMMAND = 2;

	/** The minimum capacity a {@link ByteBuffer} needs to set the DMA status. */
	private static final int BYTES_DMA = BYTES_COMMAND;

	/** The minimum capacity a {@link ByteBuffer} needs to read the mappability. */
	private static final int BYTES_MAPPABLE = 4;

	/** The minimum capacity a {@link ByteBuffer} needs to write to bind the driver. */
	private static final int BYTES_BIND = 12;

	/** The minimum capacity a {@link ByteBuffer} needs to write to unbind the driver. */
	private static final int BYTES_UNBIND = 12;

	/** The minimum capacity a {@link ByteBuffer} needs to for any of the previous resources. */
	private static final int BYTES_MIN = IntStream.of(BYTES_VENDOR, BYTES_DEVICE,
			BYTES_CLASS, BYTES_COMMAND,
			BYTES_DMA, BYTES_BIND,
			BYTES_UNBIND, BYTES_MAPPABLE).max().getAsInt();

	//////////////////////////////////////////////////// POSITIONS /////////////////////////////////////////////////////

	/** The position a {@link ByteBuffer} needs to read the vendor identifier. */
	private static final int POSITION_VENDOR = 0;

	/** The position a {@link ByteBuffer} needs to read the device id. */
	private static final int POSITION_DEVICE = 2;

	/** The position a {@link ByteBuffer} needs to read the class id. */
	private static final int POSITION_CLASS = 9;

	/** The position a {@link ByteBuffer} needs to read the command field. */
	private static final int POSITION_COMMAND = 4;

	/** The position a {@link ByteBuffer} needs to read the mappability. */
	private static final int POSITION_MAPPABLE = 0x10;

	////////////////////////////////////////////////////// MODES ///////////////////////////////////////////////////////

	/** The read and write mode used to access the resources {@code bind} and {@code unbind}. */
	private static final @NotNull String READ_WRITE_DATA = "rwd";

	//////////////////////////////////////////////////// BIT MASKS /////////////////////////////////////////////////////

	/**
	 * Bit mask used to manipulate the DMA status.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/PCI_configuration_space#/media/File:Pci-config-space.svg">PCI
	 * Configuration Space</a>
	 */
	private static final byte DMA_BIT = 0b00000100;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The {@link SeekableByteChannel} used to access the resource {@code config} of the PCI device. */
	private final @NotNull SeekableByteChannel config;

	/** The {@link FileChannel} used to access the resource {@code resource0} of the PCI device. */
	private final @NotNull FileChannel resource;

	/** The {@link SeekableByteChannel} used to access the resource {@code bind} of the PCI device's driver. */
	private final @NotNull SeekableByteChannel bindChannel;

	/** The {@link SeekableByteChannel} used to access the resource {@code unbind} of the PCI device's driver. */
	private final @NotNull SeekableByteChannel unbindChannel;

	/**
	 * The direct {@link ByteBuffer} used to read/write from/to {@link #config}, {@link #resource}, {@link #bindChannel}
	 * and {@link #unbindChannel}.
	 */
	private final @NotNull ByteBuffer buffer;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/** Holds the name of the device. */
	@Getter(onMethod_ = {@Contract(value = "-> !null", pure = true)})
	@ToString.Include(name = "name", rank = 2)
	@EqualsAndHashCode.Include
	private final @NotNull String name;

	/** Holds the driver of the device. */
	@Getter(onMethod_ = {@Contract(value = "-> !null", pure = true)})
	@ToString.Include(name = "driver", rank = 1)
	@EqualsAndHashCode.Include
	private final @NotNull String driver;

	/**
	 * Creates a new instance bound to a device and a driver.
	 *
	 * @param name   The device.
	 * @param driver The driver.
	 * @throws FileNotFoundException If the device does not exist.
	 */
	@Contract("null, _ -> fail; _, null -> fail")
	protected Device(@NotNull String name, @NotNull String driver) throws FileNotFoundException {
		if (!BuildConfig.OPTIMIZED) {
			if (name == null) throw new InvalidNullParameterException("name");
			if (driver == null) throw new InvalidNullParameterException("name");
			if (name.isBlank()) throw new IllegalArgumentException("The parameter 'name' is blank");
			if (driver.isBlank()) throw new IllegalArgumentException("The parameter 'driver' is blank");
			name = name.trim();
			driver = driver.trim();
		}
		if (BuildConfig.DEBUG) log.debug("Creating PCI device instance for {} with driver {}", name, driver);
		val min = Math.min(BYTES_MIN, name.length());
		this.name = name;
		this.driver = driver;
		buffer = ByteBuffer.allocateDirect(min).order(ByteOrder.nativeOrder());
		config = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_CONFIG), READ_WRITE_DATA).getChannel();
		resource = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_MAP), READ_WRITE_DATA).getChannel();
		bindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_BIND)).getChannel();
		unbindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_UNBIND)).getChannel();
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public short getVendorId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading vendor id of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_VENDOR).read(buffer.clear().limit(BYTES_VENDOR).mark());
			if (bytes < BYTES_VENDOR) log.warn("Could't read the exact amount of bytes needed to read the vendor id");
		} else {
			config.position(POSITION_VENDOR).read(buffer.clear().limit(BYTES_VENDOR).mark());
		}
		return buffer.reset().getShort();
	}

	@Override
	@Contract(pure = true)
	public short getDeviceId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading device id of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_DEVICE).read(buffer.clear().limit(BYTES_DEVICE).mark());
			if (bytes < BYTES_DEVICE) log.warn("Could't read the exact amount of bytes needed to read the device id");
		} else {
			config.position(POSITION_DEVICE).read(buffer.clear().limit(BYTES_DEVICE).mark());
		}
		return buffer.reset().getShort();
	}

	@Override
	@Contract(pure = true)
	public byte getClassId() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Reading class id of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_CLASS).read(buffer.clear().limit(BYTES_CLASS));
			if (bytes < BYTES_CLASS) log.warn("Could't read the exact amount of bytes needed to read the class id");
		} else {
			config.position(POSITION_CLASS).read(buffer.clear().limit(BYTES_CLASS));
		}
		return (buffer.order() == ByteOrder.BIG_ENDIAN) ? buffer.get(0) : buffer.get(2);
	}

	@Override
	@Contract(pure = true)
	public boolean isDmaEnabled() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Checking if DMA is enabled on PCI device {}", name);
		return (getCommand() & DMA_BIT) != 0;
	}

	@Override
	public void enableDma() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Enabling DMA on PCI device {}", name);
		setDma(true);
	}

	@Override
	public void disableDma() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Disabling DMA on PCI device {}", name);
		setDma(false);
	}

	@Override
	@Contract(pure = true)
	public boolean isBound() {
		val dev = String.format(PCI_DRV_RES_PATH_FMT, driver, name);
		return Files.exists(Path.of(dev));
	}

	@Override
	public void bind() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Binding driver of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = bindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
			if (bytes < BYTES_BIND) log.warn("Couldn't write the exact amount of bytes needed to bind the driver");
		} else {
			bindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
		}
	}

	@Override
	public void unbind() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Unbinding the driver of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = unbindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
			if (bytes < BYTES_BIND) log.warn("Couldn't write the exact amount of bytes needed to unbind the driver");
		} else {
			unbindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
		}
	}

	@Override
	@Contract(pure = true)
	public boolean isMappable() throws IOException {
		if (BuildConfig.DEBUG) log.debug("Checking mapability of PCI device {}", name);
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_MAPPABLE).read(buffer.clear().limit(BYTES_MAPPABLE).mark());
			if (bytes < BYTES_MAPPABLE) log.warn("Couldn't read the exact amount of bytes needed to read a BAR");
		} else {
			config.position(POSITION_MAPPABLE).read(buffer.clear().limit(BYTES_MAPPABLE).mark());
		}
		return (buffer.reset().getInt() & 0x00000001) == 0;
	}

	@Override
	@Contract(pure = true)
	@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
	public @NotNull Optional<MappedByteBuffer> map() {
		if (BuildConfig.DEBUG) log.trace("Mapping 'resource0' of PCI device {}", name);
		val path = String.format(PCI_RES_PATH_FMT, name, PCI_RES_MAP);
		try (
				val file = new RandomAccessFile(path, READ_WRITE_DATA);
				val channel = file.getChannel()
		) {
			val mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
			return Optional.of((MappedByteBuffer) mmap.order(ByteOrder.nativeOrder()));
		} catch (FileNotFoundException e) {
			log.debug("The resource 'resource0' does not exist.", e);
		} catch (IOException e) {
			if (Objects.equals(e.getMessage(), "Invalid argument")) {
				log.error("The PCI device is a legacy device and cannot be mapped.", e);
			} else {
				log.error("Error while mapping the device memory.", e);
			}
		}
		return Optional.empty();
	}

	@Override
	public void close() throws IOException {
		config.close();
		resource.close();
		bindChannel.close();
		unbindChannel.close();
	}

	/**
	 * Returns the value of the field {@code command}.
	 *
	 * @return The value.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	private short getCommand() throws IOException {
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_COMMAND).read(buffer.clear().limit(BYTES_COMMAND).mark());
			if (bytes < BYTES_COMMAND) log.warn("Couldn't read the exact amount of bytes needed to read the command");
		} else {
			config.position(POSITION_COMMAND).read(buffer.clear().limit(BYTES_COMMAND).mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Sets the DMA status.
	 *
	 * @param status The DMA status.
	 * @throws IOException If an I/O error occurs.
	 */
	private void setDma(boolean status) throws IOException {
		var command = getCommand();
		val pos = buffer.position() - BYTES_COMMAND;
		if (status) {
			command |= DMA_BIT;
		} else {
			command &= ~DMA_BIT;
		}
		buffer.position(pos).putShort(command);
		if (BuildConfig.DEBUG) {
			val bytes = config.position(POSITION_COMMAND).write(buffer.position(pos).limit(BYTES_COMMAND));
			if (bytes < BYTES_DMA) log.warn("Couldn't write the exact amount of bytes needed to set the DMA status");
		} else {
			config.position(POSITION_COMMAND).write(buffer.position(pos).limit(BYTES_COMMAND));
		}
	}

}
