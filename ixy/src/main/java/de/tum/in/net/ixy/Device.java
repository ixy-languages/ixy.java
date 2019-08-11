package de.tum.in.net.ixy;

import de.tum.in.net.ixy.memory.MemoryManager;
import de.tum.in.net.ixy.memory.PacketBufferWrapper;
import de.tum.in.net.ixy.memory.SmartUnsafeMemoryManager;
import de.tum.in.net.ixy.utils.Threads;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_ERROR;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

import static java.io.File.separator;

/**
 * Manipulates a NIC using the PCI configuration space.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({
		"ConstantConditions", "IOResourceOpenedButNotSafelyClosed", "resource",
		"PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"
})
public abstract class Device implements Closeable {

	////////////////////////////////////////////////////// PATHS ///////////////////////////////////////////////////////

	/** Path format string used to build the path to a PCI device. */
	private static final @NotNull String PCI_PATH_FMT = separator
			+ String.join(separator, "sys", "bus", "pci", "devices", "%s");

	/** Path format string used to build the path to a resource of a PCI device. */
	private static final @NotNull String PCI_RES_PATH_FMT = String.join(separator, PCI_PATH_FMT, "%s");

	/** Path format string used to build the path to a PCI driver. */
	private static final @NotNull String PCI_DRV_PATH_FMT = separator
			+ String.join(separator, "sys", "bus", "pci", "drivers", "%s");

	/** Path format string used to build the path to a PCI driver resource. */
	private static final @NotNull String PCI_DRV_RES_PATH_FMT = String.join(separator, PCI_DRV_PATH_FMT, "%s");

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
	private static final int BYTES_MIN = Collections.max(List.of(BYTES_VENDOR, BYTES_DEVICE, BYTES_CLASS, BYTES_COMMAND,
			BYTES_DMA, BYTES_BIND, BYTES_UNBIND, BYTES_MAPPABLE));

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
	private static final @NotNull String RW_DATA = "rwd";

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

	/** The memory manager. */
	protected final @NotNull MemoryManager mmanager = SmartUnsafeMemoryManager.getSingleton();

	/** Holds the name of the device. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "name", rank = 2)
	public final @NotNull String name;

	/** Holds the driver of the device. */
	@EqualsAndHashCode.Include
	@ToString.Include(name = "driver", rank = 1)
	private final @NotNull String driver;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a new instance bound to a device and a driver.
	 *
	 * @param name   The device name.
	 * @param driver The device driver.
	 * @throws FileNotFoundException If the device does not exist.
	 */
	protected Device(@NotNull String name, @NotNull String driver) throws FileNotFoundException {
		if (!OPTIMIZED) {
			name = name.trim();
			driver = driver.trim();
			if (name.isEmpty()) throw new IllegalArgumentException("The parameter 'name' MUST NOT be blank or empty.");
			if (driver.isEmpty()) {
				throw new IllegalArgumentException("The parameter 'driver' MUST NOT be blank or empty.");
			}
		}
		if (DEBUG >= LOG_TRACE) log.trace("Creating PCI device instance for '{}' with driver '{}'.", name, driver);
		val min = Math.max(BYTES_MIN, name.length());
		this.name = name;
		this.driver = driver;
		buffer = ByteBuffer.allocateDirect(min).order(ByteOrder.nativeOrder());
		config = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_CONFIG), RW_DATA).getChannel();
		resource = new RandomAccessFile(String.format(PCI_RES_PATH_FMT, name, PCI_RES_MAP), RW_DATA).getChannel();
		bindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_BIND)).getChannel();
		unbindChannel = new FileOutputStream(String.format(PCI_DRV_RES_PATH_FMT, driver, PCI_RES_UNBIND)).getChannel();
	}

	//////////////////////////////////////////////// PCI FUNCTIONALITY /////////////////////////////////////////////////

	/**
	 * Returns the vendor identifier.
	 *
	 * @return The vendor identifier.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	protected short getVendorId() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Reading vendor id of '{}'.", name);
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_VENDOR).read(buffer.clear().limit(BYTES_VENDOR).mark());
			if (bytes < BYTES_VENDOR) {
				log.warn("Could NOT read the exact amount of bytes needed to read the vendor id.");
			}
		} else {
			config.position(POSITION_VENDOR).read(buffer.clear().limit(BYTES_VENDOR).mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Returns the device identifier.
	 *
	 * @return The device identifier.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	protected short getDeviceId() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Reading device id of '{}'.", name);
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_DEVICE).read(buffer.clear().limit(BYTES_DEVICE).mark());
			if (bytes < BYTES_DEVICE) {
				log.warn("Could NOT read the exact amount of bytes needed to read the device id.");
			}
		} else {
			config.position(POSITION_DEVICE).read(buffer.clear().limit(BYTES_DEVICE).mark());
		}
		return buffer.reset().getShort();
	}

	/**
	 * Returns the class identifier.
	 *
	 * @return The class identifier.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	protected byte getClassId() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Reading class id of '{}'.", name);
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_CLASS).read(buffer.clear().limit(BYTES_CLASS));
			if (bytes < BYTES_CLASS) log.warn("Could NOT read the exact amount of bytes needed to read the class id.");
		} else {
			config.position(POSITION_CLASS).read(buffer.clear().limit(BYTES_CLASS));
		}
		return (buffer.order() == ByteOrder.BIG_ENDIAN) ? buffer.get(0) : buffer.get(2);
	}

	/**
	 * Checks whether the <em>direct memory access</em> is enabled or not.
	 *
	 * @return The DMA status.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	public boolean isDmaEnabled() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Checking DMA status of '{}'.", name);
		return (getCommand() & DMA_BIT) == DMA_BIT;
	}

	/**
	 * Enables <em>direct memory access</em>.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void enableDma() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Enabling DMA of '{}'.", name);
		setDma(true);
	}

	/**
	 * Disables <em>direct memory access</em>.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void disableDma() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Disabling DMA of '{}'.", name);
		setDma(false);
	}

	/**
	 * Checks whether the driver is bound or not.
	 *
	 * @return The bind status.
	 */
	@Contract(pure = true)
	public boolean isBound() {
		if (DEBUG >= LOG_DEBUG) log.debug("Checking driver binding of '{}'.", name);
		val dev = String.format(PCI_DRV_RES_PATH_FMT, driver, name);
		return Files.exists(Paths.get(dev));
	}

	/**
	 * Binds the driver.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void bind() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Binding the driver.");
		if (DEBUG >= LOG_WARN) {
			val bytes = bindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
			if (bytes < BYTES_BIND) log.warn("Could NOT write the exact amount of bytes needed to bind the driver.");
		} else {
			bindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
		}
	}

	/**
	 * Unbinds the driver.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	public void unbind() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Unbinding the driver.");
		if (DEBUG >= LOG_WARN) {
			val bytes = unbindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
			if (bytes < BYTES_BIND) log.warn("Could NOT write the exact amount of bytes needed to unbind the driver.");
		} else {
			unbindChannel.write(buffer.clear().put(name.getBytes(StandardCharsets.UTF_8)).flip());
		}
	}

	/**
	 * Checks whether the device can be memory mapped or not.
	 *
	 * @return The mappability.
	 * @throws IOException If an I/O error occurs.
	 */
	@Contract(pure = true)
	public boolean isMappable() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Checking mappability.");
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_MAPPABLE).read(buffer.clear().limit(BYTES_MAPPABLE).mark());
			if (bytes < BYTES_MAPPABLE) log.warn("Could NOT read the exact amount of bytes needed to read a BAR.");
		} else {
			config.position(POSITION_MAPPABLE).read(buffer.clear().limit(BYTES_MAPPABLE).mark());
		}
		return (buffer.reset().getInt() & 0x00000001) == 0;
	}

	/**
	 * Returns the memory of the PCI device mapped.
	 *
	 * @return The mapped memory.
	 */
	@Contract(pure = true)
	public long map() {
		if (DEBUG >= LOG_DEBUG) log.debug("Mapping 'resource0' of '{}'.", name);
		val path = String.format(PCI_RES_PATH_FMT, name, PCI_RES_MAP);
		try {
			return mmanager.mmap(new File(path), false, false);
		} catch (final FileNotFoundException e) {
			if (DEBUG >= LOG_ERROR) log.error("The resource 'resource0' does not exist.", e);
		} catch (final IOException e) {
			if (Objects.equals(e.getMessage(), "Invalid argument")) {
				if (DEBUG >= LOG_ERROR) log.error("The PCI device is a legacy device and cannot be mapped.", e);
			} else {
				if (DEBUG >= LOG_ERROR) log.error("Error while mapping the device memory.", e);
			}
		}
		return 0;
	}

	@Override
	public void close() throws IOException {
		if (DEBUG >= LOG_DEBUG) log.debug("Closing PCI device.");
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
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_COMMAND).read(buffer.clear().limit(BYTES_COMMAND).mark());
			if (bytes < BYTES_COMMAND) log.warn("Could NOT read the exact amount of bytes needed to read the command.");
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
	private void setDma(final boolean status) throws IOException {
		var command = getCommand();
		val pos = buffer.position() - BYTES_COMMAND;
		if (status) command |= DMA_BIT;
		else command &= ~DMA_BIT;
		buffer.position(pos).putShort(command);
		if (DEBUG >= LOG_WARN) {
			val bytes = config.position(POSITION_COMMAND).write(buffer.position(pos).limit(BYTES_COMMAND));
			if (bytes < BYTES_DMA) log.warn("Could NOT write the exact amount of bytes needed to set the DMA status.");
		} else {
			config.position(POSITION_COMMAND).write(buffer.position(pos).limit(BYTES_COMMAND));
		}
	}

	//////////////////////////////////////////////// NIC FUNCTIONALITY /////////////////////////////////////////////////

	/** Configures the network card accordingly. */
	public abstract void configure();

	/**
	 * Checks whether the device being manipulated is supported or not by the driver.
	 *
	 * @return The support status.
	 * @throws IOException If an I/O error occurs.
	 */
	public abstract boolean isSupported() throws IOException;

	/**
	 * Returns the value of an arbitrary register.
	 *
	 * @param offset The offset to start reading from.
	 * @return The value of the register.
	 */
	@Contract(pure = true)
	protected abstract int getRegister(int offset);

	/**
	 * Sets the value of an arbitrary register.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The value of the register.
	 */
	protected abstract void setRegister(int offset, int value);

	/**
	 * Blocks the calling thread until the given {@code value} is written.
	 *
	 * @param offset The offset of the register.
	 * @param value  The value to check for.
	 */
	@Contract(pure = true)
	@SuppressWarnings("PMD.AssignmentInOperand")
	protected void waitAndSetRegister(final int offset, final int value) {
		if (DEBUG >= LOG_TRACE) {
			val xflags = leftPad(Integer.toHexString(value), Integer.BYTES * 2);
			log.trace("Waiting for flags 0b{} to be set on the register @ offset 0x{}.", xflags, leftPad(offset));
			var counter = 0L;
			var current = getRegister(offset);
			while (current != value) {
				setRegister(offset, value);
				current = getRegister(offset);
				if (counter++ == Long.MAX_VALUE) {
					log.warn("The value is not set even after {} cycles.", counter);
				}
			}
			if (counter != 0) log.trace("It took {} cycles for the register to have the desired value.", counter);
		} else {
			var current = getRegister(offset);
			while (current != value) {
				setRegister(offset, value);
				current = getRegister(offset);
			}
		}
	}

	/**
	 * Sets the bits of a flag to {@code 1}.
	 *
	 * @param offset The offset to start writing to.
	 * @param flags  The flags to set.
	 */
	protected void setFlags(final int offset, final int flags) {
		if (DEBUG >= LOG_TRACE) {
			val xflags = leftPad(Integer.toBinaryString(flags), Integer.BYTES * 2);
			log.trace("Setting flags 0b{} of register @ offset 0x{}.", xflags, leftPad(offset));
		}
		setRegister(offset, getRegister(offset) | flags);
	}

	/**
	 * Sets the bits of a flag to {@code 0}.
	 *
	 * @param offset The offset to start writing to.
	 * @param flags  The flags to clear.
	 */
	protected void clearFlags(final int offset, final int flags) {
		if (DEBUG >= LOG_TRACE) {
			val xflags = leftPad(Integer.toBinaryString(flags), Integer.BYTES * 2);
			log.trace("Clearing flags 0b{} of register @ offset 0x{}.", xflags, leftPad(offset));
		}
		setRegister(offset, getRegister(offset) & ~flags);
	}

	/**
	 * Blocks the calling thread until the given {@code flags} are set.
	 *
	 * @param offset The offset to start reading from.
	 * @param flags  The flags to check for.
	 */
	@Contract(pure = true)
	@SuppressWarnings("PMD.AssignmentInOperand")
	protected void waitSetFlags(final int offset, final int flags) {
		if (DEBUG >= LOG_TRACE) {
			val xflags = leftPad(Integer.toBinaryString(flags), Integer.BYTES * 2);
			log.trace("Waiting for flags 0b{} to be set on the register @ offset 0x{}.", xflags, leftPad(offset));
			var counter = 0L;
			var current = getRegister(offset);
			while ((current & flags) != flags) {
				Threads.sleep(10);
				current = getRegister(offset);
				if (counter++ == Long.MAX_VALUE) {
					log.warn("The flags are not set even after {} cycles.", counter);
				}
			}
			if (counter != 0) log.trace("It took {} cycles for the register to have the desired flags.", counter);
		} else {
			var current = getRegister(offset);
			while ((current & flags) != flags) {
				Threads.sleep(10);
				current = getRegister(offset);
			}
		}
	}

	/**
	 * Blocks the calling thread until the given {@code flags} are cleared.
	 *
	 * @param offset The offset to start reading from.
	 * @param flags  The flags to check for.
	 */
	@Contract(pure = true)
	@SuppressWarnings("PMD.AssignmentInOperand")
	protected void waitClearFlags(final int offset, final int flags) {
		if (DEBUG >= LOG_TRACE) {
			val xflags = leftPad(Integer.toBinaryString(flags), Integer.BYTES * 2);
			log.trace("Waiting for flags 0b{} to be cleared on the register @ offset 0x{}.", xflags, leftPad(offset));
			var counter = 0L;
			var current = getRegister(offset);
			while ((current & flags) != 0) {
				Threads.sleep(10);
				current = getRegister(offset);
				if (counter++ == Long.MAX_VALUE) {
					log.warn("The flags are not set even after {} cycles.", counter);
				}
			}
			if (counter != 0) log.trace("It took {} cycles for the register to have the desired flags.", counter);
		} else {
			var current = getRegister(offset);
			while ((current & flags) != 0) {
				Threads.sleep(10);
				current = getRegister(offset);
			}
		}
	}

	/**
	 * Returns the promiscuous status.
	 *
	 * @return The promiscuous status.
	 */
	@Contract(pure = true)
	public abstract boolean isPromiscuousEnabled();

	/** Enables the promiscuous mode. */
	public abstract void enablePromiscuous();

	/** Disables the promiscuous mode. */
	public abstract void disablePromiscuous();

	/**
	 * Returns the link speed.
	 *
	 * @return The link speed.
	 */
	@Contract(pure = true)
	public abstract long getLinkSpeed();

	/**
	 * Reads a batch of packets from a queue.
	 *
	 * @param queue   The queue.
	 * @param buffers The packet list.
	 * @param offset  The offset to start reading from.
	 * @param length  The number of packets to read.
	 * @return The number of packets read.
	 */
	@Contract(mutates = "param2")
	public abstract int rxBatch(int queue, @NotNull PacketBufferWrapper[] buffers, int offset, int length);

	/**
	 * Reads a batch of packets from a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param buffers The packet list.
	 * @param offset  The offset to start reading from.
	 * @param length  The number of packets to read.
	 */
	@Contract(mutates = "param2")
	private void rxBusyWait(final int queue, final @NotNull PacketBufferWrapper[] buffers, int offset, int length) {
		if (!OPTIMIZED) length = Math.min(length, buffers.length - offset);
		if (DEBUG >= LOG_TRACE) log.trace("Synchronously reading a batch of {} packets.", length);
		while (length > 0) {
			val processed = rxBatch(queue, buffers, offset, length);
			offset += processed;
			length -= processed;
		}
	}

	/**
	 * Writes a batch of packets to a queue.
	 *
	 * @param queue   The queue.
	 * @param buffers The packet list.
	 * @param offset  The offset to start writing to.
	 * @param length  The number of packets to write.
	 * @return The number of written packets.
	 */
	@Contract(mutates = "param2")
	public abstract int txBatch(int queue, @NotNull PacketBufferWrapper[] buffers, int offset, int length);

	/**
	 * Reads a batch of packets in a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param buffers The packet list.
	 * @param offset  The offset to start writing to.
	 * @param length  The number of packets to write.
	 */
	@Contract(mutates = "param2")
	public void txBusyWait(final int queue, final @NotNull PacketBufferWrapper[] buffers, int offset, int length) {
		if (!OPTIMIZED) {
			length = Math.min(length, buffers.length - offset);
		}
		if (DEBUG >= LOG_TRACE) log.trace("Synchronously writing a batch of {} packets.", length);
		while (length > 0) {
			val processed = txBatch(queue, buffers, offset, length);
			offset += processed;
			length -= processed;
		}
	}

	/**
	 * Updates an stats instance.
	 *
	 * @param stats The stats.
	 */
	@Contract(mutates = "param1")
	public abstract void readStats(@NotNull Stats stats);

}
