package de.tum.in.net.ixy.ixgbe;

/**
 * Definitions file from the {@code ixgbe} C-based driver with utils to compute register offsets.
 * <p>
 * Unused definitions have been removed to improve readability, but the order has been left untouched.
 *
 * @author Esaú García Sanchez-Torija
 * @see <a href="https://github.com/emmericp/ixy/blob/master/src/driver/ixgbe_type.h">Ixy</a>
 */
@SuppressWarnings({"PointlessBitwiseExpression", "checkstyle:AbbreviationAsWordInName", "checkstyle:MethodName"})
final class IxgbeDefs {

	static final short INTEL_VENDOR_ID = (short) 0x8086;
	static final short DEV_ID_82598 = 0x10B6;
	static final short DEV_ID_82598_BX = 0x1508;
	static final short DEV_ID_82598AF_DUAL_PORT = 0x10C6;
	static final short DEV_ID_82598AF_SINGLE_PORT = 0x10C7;
	static final short DEV_ID_82598AT = 0x10C8;
	static final short DEV_ID_82598AT2 = 0x150B;
	static final short DEV_ID_82598EB_SFP_LOM = 0x10DB;
	static final short DEV_ID_82598EB_CX4 = 0x10DD;
	static final short DEV_ID_82598_CX4_DUAL_PORT = 0x10EC;
	static final short DEV_ID_82598_DA_DUAL_PORT = 0x10F1;
	static final short DEV_ID_82598_SR_DUAL_PORT_EM = 0x10E1;
	static final short DEV_ID_82598EB_XF_LR = 0x10F4;
	static final short DEV_ID_82599_KX4 = 0x10F7;
	static final short DEV_ID_82599_KX4_MEZZ = 0x1514;
	static final short DEV_ID_82599_KR = 0x1517;
	static final short DEV_ID_82599_COMBO_BACKPLANE = 0x10F8;
	static final short SUBDEV_ID_82599_KX4_KR_MEZZ = 0x000C;
	static final short DEV_ID_82599_CX4 = 0x10F9;
	static final short DEV_ID_82599_SFP = 0x10FB;
	static final short SUBDEV_ID_82599_SFP = 0x11A9;
	static final short SUBDEV_ID_82599_SFP_WOL0 = 0x1071;
	static final short SUBDEV_ID_82599_RNDC = 0x1F72;
	static final short SUBDEV_ID_82599_560FLR = 0x17D0;
	static final short SUBDEV_ID_82599_ECNA_DP = 0x0470;
	static final short SUBDEV_ID_82599_SP_560FLR = 0x211B;
	static final short SUBDEV_ID_82599_LOM_SNAP6 = 0x2159;
	static final short SUBDEV_ID_82599_SFP_1OCP = 0x000D;
	static final short SUBDEV_ID_82599_SFP_2OCP = 0x0008;
	static final short SUBDEV_ID_82599_SFP_LOM_OEM1 = (short) 0x8976;
	static final short SUBDEV_ID_82599_SFP_LOM_OEM2 = 0x06EE;
	static final short DEV_ID_82599_BACKPLANE_FCOE = 0x152A;
	static final short DEV_ID_82599_SFP_FCOE = 0x1529;
	static final short DEV_ID_82599_SFP_EM = 0x1507;
	static final short DEV_ID_82599_SFP_SF2 = 0x154D;
	static final short DEV_ID_82599_SFP_SF_QP = 0x154A;
	static final short DEV_ID_82599_QSFP_SF_QP = 0x1558;
	static final short DEV_ID_82599EN_SFP = 0x1557;
	static final short SUBDEV_ID_82599EN_SFP_OCP1 = 0x0001;
	static final short DEV_ID_82599_XAUI_LOM = 0x10FC;
	static final short DEV_ID_82599_T3_LOM = 0x151C;
	static final short DEV_ID_82599_VF = 0x10ED;
	static final short DEV_ID_82599_VF_HV = 0x152E;
	static final short DEV_ID_82599_LS = 0x154F;
	static final short DEV_ID_X540T = 0x1528;
	static final short DEV_ID_X540_VF = 0x1515;
	static final short DEV_ID_X540_VF_HV = 0x1530;
	static final short DEV_ID_X540T1 = 0x1560;
	static final short DEV_ID_X550T = 0x1563;
	static final short DEV_ID_X550T1 = 0x15D1;
	static final short DEV_ID_X550EM_A_KR = 0x15C2;
	static final short DEV_ID_X550EM_A_KR_L = 0x15C3;
	static final short DEV_ID_X550EM_A_SFP_N = 0x15C4;
	static final short DEV_ID_X550EM_A_SGMII = 0x15C6;
	static final short DEV_ID_X550EM_A_SGMII_L = 0x15C7;
	static final short DEV_ID_X550EM_A_10G_T = 0x15C8;
	static final short DEV_ID_X550EM_A_QSFP = 0x15CA;
	static final short DEV_ID_X550EM_A_QSFP_N = 0x15CC;
	static final short DEV_ID_X550EM_A_SFP = 0x15CE;
	static final short DEV_ID_X550EM_A_1G_T = 0x15E4;
	static final short DEV_ID_X550EM_A_1G_T_L = 0x15E5;
	static final short DEV_ID_X550EM_X_KX4 = 0x15AA;
	static final short DEV_ID_X550EM_X_KR = 0x15AB;
	static final short DEV_ID_X550EM_X_SFP = 0x15AC;
	static final short DEV_ID_X550EM_X_10G_T = 0x15AD;
	static final short DEV_ID_X550EM_X_1G_T = 0x15AE;
	static final short DEV_ID_X550EM_X_XFI = 0x15B0;
	static final short DEV_ID_X550_VF_HV = 0x1564;
	static final short DEV_ID_X550_VF = 0x1565;
	static final short DEV_ID_X550EM_A_VF = 0x15C5;
	static final short DEV_ID_X550EM_A_VF_HV = 0x15B4;
	static final short DEV_ID_X550EM_X_VF = 0x15A8;
	static final short DEV_ID_X550EM_X_VF_HV = 0x15A9;
	static final int CTRL = 0x00000;
	// ...
	static final int CTRL_EXT = 0x00018;
	// ...
	static final int EEC = 0x10010;
	// ...
	static final int EIMC = 0x00888;
	// ...
	static final int RDRXCTL = 0x02F00;
	static final int RXCTRL = 0x03000;
	// ...
	static final int FCTRL = 0x05080;
	// ...
	static final int DMATXCTL = 0x04A80;
	// ...
	static final int DTXMXSZRQ = 0x08100;
	// ...
	static final int DMATXCTL_TE = 0x1;
	// ...
	static final int RTTDCS = 0x04900;
	static final int RTTDCS_ARBDIS = 0x00000040;
	// ...
	static final int GPRC = 0x04074;
	// ...
	static final int GPTC = 0x04080;
	static final int GORCL = 0x04088;
	static final int GORCH = 0x0408C;
	static final int GOTCL = 0x04090;
	static final int GOTCH = 0x04094;
	// ...
	static final int HLREG0 = 0x04240;
	// ...
	static final int AUTOC = 0x042A0;
	static final int LINKS = 0x042A4;
	// ...
	private static final int AUTOC_LMS_SHIFT = 13;
	static final int AUTOC_LMS_10G_SERIAL = (0x3 << AUTOC_LMS_SHIFT);
	static final int AUTOC_LMS_MASK = (0x7 << AUTOC_LMS_SHIFT);
	private static final int AUTOC_10G_PMA_PMD_SHIFT = 7;
	static final int AUTOC_10G_XAUI = (0x0 << AUTOC_10G_PMA_PMD_SHIFT);
	// ...
	static final int RDRXCTL_CRCSTRIP = 0x00000002;
	// ...
	static final int RDRXCTL_DMAIDONE = 0x00000008;
	// ...
	private static final int CTRL_LNK_RST = 0x00000008;
	private static final int CTRL_RST = 0x04000000;
	static final int CTRL_RST_MASK = (CTRL_LNK_RST | CTRL_RST);
	// ...
	static final int CTRL_EXT_NS_DIS = 0x00010000;
	// ...
	static final int TXPBSIZE_40KB = 0x0000A000;
	// ...
	static final int RXPBSIZE_128KB = 0x00020000;
	// ...
	static final int HLREG0_TXCRCEN = 0x00000001;
	static final int HLREG0_RXCRCSTRP = 0x00000002;
	// ...
	static final int HLREG0_TXPADEN = 0x00000400;
	// ...
	static final int AUTOC_AN_RESTART = 0x00001000;
	// ...
	static final int AUTOC_10G_PMA_PMD_MASK = 0x00000180;
	// ...
	static final int LINKS_UP = 0x40000000;
	// ...
	static final int LINKS_SPEED_82599 = 0x30000000;
	static final int LINKS_SPEED_10G_82599 = 0x30000000;
	static final int LINKS_SPEED_1G_82599 = 0x20000000;
	static final int LINKS_SPEED_100_82599 = 0x10000000;
	// ...
	static final int EEC_ARD = 0x00000200;
	// ...
	static final int TXDCTL_ENABLE = 0x02000000;
	// ...
	static final int RXCTRL_RXEN = 0x00000001;
	// ...
	static final int RXDCTL_ENABLE = 0x02000000;
	// ...
	static final int FCTRL_MPE = 0x00000100;
	static final int FCTRL_UPE = 0x00000200;
	static final int FCTRL_BAM = 0x00000400;
	// ...
	private static final int TXD_CMD_EOP = 0x01000000;
	private static final int TXD_CMD_IFCS = 0x02000000;
	// ...
	private static final int TXD_CMD_RS = 0x08000000;
	private static final int TXD_CMD_DEXT = 0x20000000;
	// ...
	private static final int TXD_STAT_DD = 0x00000001;
	// ...
	private static final int RXD_STAT_DD = 0x01;
	private static final int RXD_STAT_EOP = 0x02;
	// ...
	static final int SRRCTL_DROP_EN = 0x10000000;
	// ...
	static final int SRRCTL_DESCTYPE_ADV_ONEBUF = 0x02000000;
	// ...
	static final int SRRCTL_DESCTYPE_MASK = 0x0E000000;
	// ...
	static final int ADVTXD_DTYP_DATA = 0x00300000;
	// ...
	static final int RXDADV_STAT_DD = RXD_STAT_DD;
	static final int RXDADV_STAT_EOP = RXD_STAT_EOP;
	// ...
	static final int ADVTXD_STAT_DD = TXD_STAT_DD;
	static final int ADVTXD_DCMD_EOP = TXD_CMD_EOP;
	static final int ADVTXD_DCMD_IFCS = TXD_CMD_IFCS;
	static final int ADVTXD_DCMD_RS = TXD_CMD_RS;
	static final int ADVTXD_DCMD_DEXT = TXD_CMD_DEXT;
	static final int ADVTXD_PAYLEN_SHIFT = 14;

	/**
	 * Returns the offset of the register <em>Split Receive Control Registers</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int SRRCTL(final int queue) {
		if (Integer.compareUnsigned(queue, 15) <= 0) {
			return 0x02100 + queue * 4;
		} else if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01014 + queue * 0x40;
		}
		return 0x0D014 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>RX DCA Control Register</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int DCA_RXCTRL(final int queue) {
		if (Integer.compareUnsigned(queue, 15) <= 0) {
			return 0x02200 + queue * 4;
		} else if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x0100C + queue * 0x40;
		}
		return 0x0D00C + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Packet Buffer Size</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RXPBSIZE(final int queue) {
		return 0x03C00 + queue * 4;
	}

	/**
	 * Returns the offset of the register <em>Transmit Packet Buffer Size</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TXPBSIZE(final int queue) {
		return 0x0CC00 + queue * 4;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Base Address Low</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RDBAL(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01000 + queue * 0x40;
		}
		return 0x0D000 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Base Address Low</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TDBAL(final int queue) {
		return 0x06000 + queue * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Base Address High</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RDBAH(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01004 + queue * 0x40;
		}
		return 0x0D004 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Base Address High</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TDBAH(final int queue) {
		return 0x06004 + queue * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Length</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RDLEN(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01008 + queue * 0x40;
		}
		return 0x0D008 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Length</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TDLEN(final int queue) {
		return 0x06008 + queue * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Control</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TXDCTL(final int queue) {
		return 0x06028 + queue * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Control</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RXDCTL(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01028 + queue * 0x40;
		}
		return 0x0D028 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Head</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RDH(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01010 + queue * 0x40;
		}
		return 0x0D010 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Head</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TDH(final int queue) {
		return 0x06010 + queue * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Receive Descriptor Tail</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int RDT(final int queue) {
		if (Integer.compareUnsigned(queue, 64) < 0) {
			return 0x01018 + queue * 0x40;
		}
		return 0x0D018 + (queue - 64) * 0x40;
	}

	/**
	 * Returns the offset of the register <em>Transmit Descriptor Tail</em> for the given {@code queue}.
	 *
	 * @param queue The queue id.
	 * @return The register offset.
	 */
	static int TDT(final int queue) {
		return 0x06018 + queue * 0x40;
	}
	
}
