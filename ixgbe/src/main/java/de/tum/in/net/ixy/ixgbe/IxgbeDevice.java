package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import de.tum.in.net.ixy.generic.IxyStats;
import de.tum.in.net.ixy.pci.BuildConfig;
import de.tum.in.net.ixy.pci.Device;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * The device driver for Intel's 10-Gigabit NICs ({@code ixgbe} family).
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
final class IxgbeDevice extends Device {

	/** The factor used to transform Mbps to bps. */
	private static final long MBPS_TO_BPS = 1_000_000;

	/** The memory of the PCI device. */
	private @Nullable MappedByteBuffer memory;

	/**
	 * Creates a device driver for Intel Ixgbe devices.
	 *
	 * @param name     The device name.
	 * @param rxQueues The number of read queues.
	 * @param txQueues The number of write queues.
	 * @throws FileNotFoundException If the device does not exist.
	 */
	IxgbeDevice(@NotNull String name, int rxQueues, int txQueues) throws FileNotFoundException {
		super(name, "ixgbe");
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public void allocate() {
		memory = map().orElse(null);
	}

	@Override
	public boolean isSupported() throws IOException {
		if (getVendorId() != IxgbeDefs.INTEL_VENDOR_ID) return false;
		switch (getDeviceId()) {
			case IxgbeDefs.DEV_ID_82598:
			case IxgbeDefs.DEV_ID_82598_BX:
			case IxgbeDefs.DEV_ID_82598AF_DUAL_PORT:
			case IxgbeDefs.DEV_ID_82598AF_SINGLE_PORT:
			case IxgbeDefs.DEV_ID_82598AT:
			case IxgbeDefs.DEV_ID_82598AT2:
			case IxgbeDefs.DEV_ID_82598EB_SFP_LOM:
			case IxgbeDefs.DEV_ID_82598EB_CX4:
			case IxgbeDefs.DEV_ID_82598_CX4_DUAL_PORT:
			case IxgbeDefs.DEV_ID_82598_DA_DUAL_PORT:
			case IxgbeDefs.DEV_ID_82598_SR_DUAL_PORT_EM:
			case IxgbeDefs.DEV_ID_82598EB_XF_LR:
			case IxgbeDefs.DEV_ID_82599_KX4:
			case IxgbeDefs.DEV_ID_82599_KX4_MEZZ:
			case IxgbeDefs.DEV_ID_82599_KR:
			case IxgbeDefs.DEV_ID_82599_COMBO_BACKPLANE:
			case IxgbeDefs.SUBDEV_ID_82599_KX4_KR_MEZZ:
			case IxgbeDefs.DEV_ID_82599_CX4:
			case IxgbeDefs.DEV_ID_82599_SFP:
			case IxgbeDefs.SUBDEV_ID_82599_SFP:
			case IxgbeDefs.SUBDEV_ID_82599_SFP_WOL0:
			case IxgbeDefs.SUBDEV_ID_82599_RNDC:
			case IxgbeDefs.SUBDEV_ID_82599_560FLR:
			case IxgbeDefs.SUBDEV_ID_82599_ECNA_DP:
			case IxgbeDefs.SUBDEV_ID_82599_SP_560FLR:
			case IxgbeDefs.SUBDEV_ID_82599_LOM_SNAP6:
			case IxgbeDefs.SUBDEV_ID_82599_SFP_1OCP:
			case IxgbeDefs.SUBDEV_ID_82599_SFP_2OCP:
			case IxgbeDefs.SUBDEV_ID_82599_SFP_LOM_OEM1:
			case IxgbeDefs.SUBDEV_ID_82599_SFP_LOM_OEM2:
			case IxgbeDefs.DEV_ID_82599_BACKPLANE_FCOE:
			case IxgbeDefs.DEV_ID_82599_SFP_FCOE:
			case IxgbeDefs.DEV_ID_82599_SFP_EM:
			case IxgbeDefs.DEV_ID_82599_SFP_SF2:
			case IxgbeDefs.DEV_ID_82599_SFP_SF_QP:
			case IxgbeDefs.DEV_ID_82599_QSFP_SF_QP:
			case IxgbeDefs.DEV_ID_82599EN_SFP:
			case IxgbeDefs.SUBDEV_ID_82599EN_SFP_OCP1:
			case IxgbeDefs.DEV_ID_82599_XAUI_LOM:
			case IxgbeDefs.DEV_ID_82599_T3_LOM:
			case IxgbeDefs.DEV_ID_82599_VF:
			case IxgbeDefs.DEV_ID_82599_VF_HV:
			case IxgbeDefs.DEV_ID_82599_LS:
			case IxgbeDefs.DEV_ID_X540T:
			case IxgbeDefs.DEV_ID_X540_VF:
			case IxgbeDefs.DEV_ID_X540_VF_HV:
			case IxgbeDefs.DEV_ID_X540T1:
			case IxgbeDefs.DEV_ID_X550T:
			case IxgbeDefs.DEV_ID_X550T1:
			case IxgbeDefs.DEV_ID_X550EM_A_KR:
			case IxgbeDefs.DEV_ID_X550EM_A_KR_L:
			case IxgbeDefs.DEV_ID_X550EM_A_SFP_N:
			case IxgbeDefs.DEV_ID_X550EM_A_SGMII:
			case IxgbeDefs.DEV_ID_X550EM_A_SGMII_L:
			case IxgbeDefs.DEV_ID_X550EM_A_10G_T:
			case IxgbeDefs.DEV_ID_X550EM_A_QSFP:
			case IxgbeDefs.DEV_ID_X550EM_A_QSFP_N:
			case IxgbeDefs.DEV_ID_X550EM_A_SFP:
			case IxgbeDefs.DEV_ID_X550EM_A_1G_T:
			case IxgbeDefs.DEV_ID_X550EM_A_1G_T_L:
			case IxgbeDefs.DEV_ID_X550EM_X_KX4:
			case IxgbeDefs.DEV_ID_X550EM_X_KR:
			case IxgbeDefs.DEV_ID_X550EM_X_SFP:
			case IxgbeDefs.DEV_ID_X550EM_X_10G_T:
			case IxgbeDefs.DEV_ID_X550EM_X_1G_T:
			case IxgbeDefs.DEV_ID_X550EM_X_XFI:
			case IxgbeDefs.DEV_ID_X550_VF_HV:
			case IxgbeDefs.DEV_ID_X550_VF:
			case IxgbeDefs.DEV_ID_X550EM_A_VF:
			case IxgbeDefs.DEV_ID_X550EM_A_VF_HV:
			case IxgbeDefs.DEV_ID_X550EM_X_VF:
			case IxgbeDefs.DEV_ID_X550EM_X_VF_HV:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected int getRegister(int offset) {
		if (!BuildConfig.OPTIMIZED) {
			if (memory == null) {
				throw new IllegalStateException("This instance has been already closed and can no longer be used");
			}
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		return memory.getInt(offset);
	}

	@Override
	protected void setRegister(int offset, int value) {
		if (!BuildConfig.OPTIMIZED) {
			if (memory == null) {
				throw new IllegalStateException("This instance has been already closed and can no longer be used");
			}
			if (offset < 0) throw new InvalidOffsetException("offset");
		}
		memory.putInt(offset, value);
	}

	@Override
	public boolean isPromiscuous() {
		if (BuildConfig.DEBUG) log.debug("Checking promiscuous mode.");
		val reg = getRegister(IxgbeDefs.FCTRL);
		val mask = IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE;
		return (reg & mask) == mask;
	}

	@Override
	public void enablePromiscuous() {
		if (BuildConfig.DEBUG) log.debug("Enabling promiscuous mode.");
		setFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE);
	}

	@Override
	public void disablePromiscuous() {
		if (BuildConfig.DEBUG) log.debug("Disabling promiscuous mode.");
		clearFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE);
	}

	@Override
	public long getLinkSpeed() {
		if (BuildConfig.DEBUG) log.debug("Extracting link speed.");
		val links = getRegister(IxgbeDefs.LINKS);
		if ((links & IxgbeDefs.LINKS_UP) == 0) {
			if (BuildConfig.DEBUG) log.warn("The link is not up.");
			return 0;
		}
		switch (links & IxgbeDefs.LINKS_SPEED_82599) {
			case IxgbeDefs.LINKS_SPEED_100_82599:
				return 100 * MBPS_TO_BPS;
			case IxgbeDefs.LINKS_SPEED_1G_82599:
				return 1000 * MBPS_TO_BPS;
			case IxgbeDefs.LINKS_SPEED_10G_82599:
				return 10_000 * MBPS_TO_BPS;
			default:
				if (BuildConfig.DEBUG) log.warn("Unknown link speed.");
				return 0;
		}
	}

	@Override
	public int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		return 0;
	}

	@Override
	public int txBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		return 0;
	}

	@Override
	public void readStats(@NotNull IxyStats stats) {

	}

}
