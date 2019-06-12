package de.tum.in.net.ixy.generic;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Arrays;

/**
 * Ixy's device specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public abstract class IxyDevice {

	/**
	 * Returns the value of an arbitrary register.
	 *
	 * @param offset The offset to start reading from.
	 * @return The value of the register.
	 */
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
	public abstract long getLinkSpeed();

	/**
	 * Reads a batch of packets from a queue.
	 *
	 * @param queue   The queue to use.
	 * @param packets The packet list.
	 * @return The number of packets read.
	 */
	public abstract int rxBatch(int queue, IxyPacketBuffer[] packets);

	/**
	 * Reads a batch of packets in a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 */
	public void rxBusyWait(int queue, IxyPacketBuffer[] packets) {
		var received = 0;
		val len = packets.length;
		while (received < len) {
			received += rxBatch(queue, Arrays.copyOfRange(packets, received, packets.length));
		}
	}

	/**
	 * Writes a batch of packets in a queue.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 * @return The number of packets written.
	 */
	public abstract int txBatch(int queue, IxyPacketBuffer[] packets);

	/**
	 * Writes a batch of packets in a queue synchronously.
	 *
	 * @param queue   The queue.
	 * @param packets The packet list.
	 */
	public void txBusyWait(int queue, IxyPacketBuffer[] packets) {
		var sent = 0;
		val len = packets.length;
		while (sent < len) {
			sent += txBatch(queue, Arrays.copyOfRange(packets, sent, packets.length));
		}
	}

}
