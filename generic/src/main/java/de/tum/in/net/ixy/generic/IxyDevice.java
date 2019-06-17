package de.tum.in.net.ixy.generic;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Ixy's device specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public abstract class IxyDevice implements IxyPciDevice {

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
	 * Sets the bits of a flag to {@code 1}.
	 *
	 * @param offset The offset to start writing to.
	 * @param flags  The flags to set.
	 */
	protected void setFlags(int offset, int flags) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			val xflags = Integer.toBinaryString(flags);
			log.debug("Setting register at offset 0x{} with flags 0b{}", xoffset, xflags);
		}
		setRegister(offset, getRegister(offset) | flags);
	}

	/**
	 * Sets the bits of a flag to {@code 0}.
	 *
	 * @param offset The offset to start writing to.
	 * @param flags  The flags to clear.
	 */
	protected void clearFlags(int offset, int flags) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			val xflags = Integer.toBinaryString(flags);
			log.debug("Clearing register at offset 0x{} with flags 0b{}", xoffset, xflags);
		}
		setRegister(offset, getRegister(offset) | ~flags);
	}

	/**
	 * Blocks the calling thread until the given {@code flags} are cleared.
	 *
	 * @param offset The offset to start reading from.
	 * @param flags  The flags to check for.
	 */
	@Contract(pure = true)
	protected void waitClearRegister(int offset, int flags) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			val xflags = Integer.toBinaryString(flags);
			log.debug("Waiting register at offset 0x{} for flags 0b{} to be cleared", xoffset, xflags);
		}
		var current = getRegister(offset);
		while ((current & flags) != 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				if (BuildConfig.DEBUG) {
					log.warn("Could not sleep for 10 milliseconds", e);
				}
			}
			current = getRegister(offset);
		}
	}

	/**
	 * Blocks the calling thread until the given {@code flags} are set.
	 *
	 * @param offset The offset to start reading from.
	 * @param flags  The flags to check for.
	 */
	@Contract(pure = true)
	protected void waitSetRegister(int offset, int flags) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			val xflags = Integer.toBinaryString(flags);
			log.debug("Waiting register at offset 0x{} for flags 0b{} to be cleared", xoffset, xflags);
		}
		var current = getRegister(offset);
		while ((current & flags) != flags) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				if (BuildConfig.DEBUG) {
					log.warn("Could not sleep for 10 milliseconds", e);
				}
			}
			current = getRegister(offset);
		}
	}

	/**
	 * Returns the promiscuous status.
	 *
	 * @return The promiscuous status.
	 */
	@Contract(pure = true)
	public abstract boolean isPromiscuous();

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
	 * @param packets The packet list.
	 * @param offset  The offset to start reading from.
	 * @param length  The number of packets to read.
	 * @return The number of packets read.
	 */
	@Contract(mutates = "param2")
	public abstract int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length);

	/**
	 * Wrapper for {@link #rxBatch(int, IxyPacketBuffer[], int, int)} that computes the size automatically based on the
	 * parameter {@code offset}.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start reading from.
	 * @return The number of packets read.
	 */
	@Contract(mutates = "param2")
	public int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val length = packets.length - offset;
		if (length <= 0) return 0;
		if (BuildConfig.DEBUG) log.debug("Delegate reading a batch of {} packets", length);
		return rxBatch(queue, packets, offset, length);
	}

	/**
	 * Wrapper for {@link #rxBatch(int, IxyPacketBuffer[], int, int)} that reads as many packets as the buffer can
	 * hold.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @return The number of read packets.
	 */
	@Contract(mutates = "param2")
	public int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (packets.length == 0) return 0;
		}
		if (BuildConfig.DEBUG) log.debug("Delegate reading a whole batch of packets");
		return rxBatch(queue, packets, 0, packets.length);
	}

	/**
	 * Reads a batch of packets in a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start reading from.
	 * @param length  The number of packets to read.
	 */
	@Contract(mutates = "param2")
	public void rxBusyWait(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
			length = Math.min(length, packets.length - offset);
		}
		if (BuildConfig.DEBUG) log.debug("Synchronously reading a batch of {} packets", length);
		var received = 0;
		while (received < length) {
			val processed = rxBatch(queue, packets, offset, length);
			received += processed;
			offset += processed;
			length -= processed;
		}
	}

	/**
	 * Wrapper for {@link #rxBusyWait(int, IxyPacketBuffer[], int, int)} that computes the size automatically based on
	 * the parameter {@code offset}.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start reading from.
	 */
	@Contract(mutates = "param2")
	public void rxBusyWait(int queue, @NotNull IxyPacketBuffer[] packets, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val length = packets.length - offset;
		if (length <= 0) return;
		if (BuildConfig.DEBUG) log.debug("Delegate synchronously reading a batch of {} packets", length);
		rxBusyWait(queue, packets, offset, length);
	}

	/**
	 * Wrapper for {@link #rxBusyWait(int, IxyPacketBuffer[], int, int)} that reads as many packets as the buffer can
	 * hold.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 */
	@Contract(mutates = "param2")
	public void rxBusyWait(int queue, @NotNull IxyPacketBuffer[] packets) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (packets.length == 0) return;
		}
		if (BuildConfig.DEBUG) log.debug("Delegate synchronously reading a whole batch of packets");
		rxBusyWait(queue, packets, 0, packets.length);
	}

	/**
	 * Writes a batch of packets in a queue.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start writing to.
	 * @param length  The number of packets to write.
	 * @return The number of written packets.
	 */
	@Contract(mutates = "param2")
	public abstract int txBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length);

	/**
	 * Wrapper for {@link #txBatch(int, IxyPacketBuffer[], int, int)} that computes the size automatically based on the
	 * parameter {@code offset}.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start writing to.
	 * @return The number of written packets.
	 */
	@Contract(mutates = "param2")
	public int txBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val length = packets.length - offset;
		if (length <= 0) return 0;
		if (BuildConfig.DEBUG) log.debug("Delegate reading a batch of {} packets", length);
		return txBatch(queue, packets, offset, length);
	}

	/**
	 * Wrapper for {@link #txBatch(int, IxyPacketBuffer[], int, int)} that reads as many packets as the buffer can
	 * hold.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @return The number of written packets.
	 */
	@Contract(mutates = "param2")
	public int txBatch(int queue, @NotNull IxyPacketBuffer[] packets) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (packets.length == 0) return 0;
		}
		if (BuildConfig.DEBUG) log.debug("Delegate writing a whole batch of packets");
		return txBatch(queue, packets, 0, packets.length);
	}

	/**
	 * Reads a batch of packets in a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start writing to.
	 * @param length  The number of packets to write.
	 */
	@Contract(mutates = "param2")
	public void txBusyWait(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
			length = Math.min(length, packets.length - offset);
		}
		if (BuildConfig.DEBUG) log.debug("Synchronously writing a batch of {} packets", length);
		var received = 0;
		while (received < length) {
			val processed = txBatch(queue, packets, offset, length);
			received += processed;
			offset += processed;
			length -= processed;
		}
	}

	/**
	 * Wrapper for {@link #txBusyWait(int, IxyPacketBuffer[], int, int)} that computes the size automatically based on
	 * the parameter {@code offset}.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @param offset  The offset to start writing to.
	 */
	@Contract(mutates = "param2")
	public void txBusyWait(int queue, @NotNull IxyPacketBuffer[] packets, int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		val length = packets.length - offset;
		if (length <= 0) return;
		if (BuildConfig.DEBUG) log.debug("Delegate synchronously writing a batch of {} packets", length);
		txBusyWait(queue, packets, offset, length);
	}

	/**
	 * Wrapper for {@link #txBusyWait(int, IxyPacketBuffer[], int, int)} that writes as many packets as the buffer can
	 * hold.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 */
	@Contract(mutates = "param2")
	public void txBusyWait(int queue, @NotNull IxyPacketBuffer[] packets) {
		if (!BuildConfig.OPTIMIZED) {
			if (packets == null) throw new InvalidNullParameterException("packets");
			if (packets.length == 0) return;
		}
		if (BuildConfig.DEBUG) log.debug("Delegate synchronously writing a whole batch of packets");
		txBusyWait(queue, packets, 0, packets.length);
	}

	/**
	 * Updates an stats instance.
	 *
	 * @param stats The stats.
	 */
	@Contract(value = "null -> fail", mutates = "param1")
	public abstract void readStats(@NotNull IxyStats stats);

}
