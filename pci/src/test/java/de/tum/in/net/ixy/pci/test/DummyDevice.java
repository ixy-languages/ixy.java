package de.tum.in.net.ixy.pci.test;

import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import de.tum.in.net.ixy.pci.Device;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

/** Dummy class extending {@link Device}. */
final class DummyDevice extends Device {

	DummyDevice(@NotNull String name, @NotNull String driver) throws FileNotFoundException {
		super(name, driver);
	}

	@Override
	public boolean isPromiscuous() {
		return false;
	}

	@Override
	public void enablePromiscuous() {}

	@Override
	public void disablePromiscuous() {}

	@Override
	public long getLinkSpeed() {
		return 0;
	}

	@Override
	public int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		return 0;
	}

	@Override
	public int txBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int size) {
		return 0;
	}

}