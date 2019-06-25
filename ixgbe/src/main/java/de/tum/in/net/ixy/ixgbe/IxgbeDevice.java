package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import de.tum.in.net.ixy.generic.IxyStats;
import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.PacketBuffer;
import de.tum.in.net.ixy.memory.SmartMemoryManager;
import de.tum.in.net.ixy.pci.Device;
import de.tum.in.net.ixy.stats.Stats;
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

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The maximum number of queues supported. */
	private static final int MAX_QUEUES = 64;

	/** The maximum number of entries per RX queue. */
	private static final int RX_MAX_ENTRIES = 4096;

	/** The maximum number of entries per TX queue. */
	private static final int TX_MAX_ENTRIES = 4096;

	/** The number of entries per RX queue. */
	private static final int RX_ENTRIES = 512;

	/** The number of entries per TX queue. */
	private static final int TX_ENTRIES = 512;

	/** The size of a descriptor of the RX queue. */
	private static final int RX_DESCRIPTOR_SIZE = 16;

	/** The size of a descriptor of the TX queue. */
	private static final int TX_DESCRIPTOR_SIZE = 16;

	/** The amount of packets to clean per batch. */
	private static final int TX_CLEAN_BATCH = 32;

	/** The factor used to transform Mbps to bps. */
	private static final long MBPS_TO_BPS = 1_000_000;

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The read queues. */
	private final @NotNull IxgbeRxQueue[] rxQueues;

	/** The write queues. */
	private final @NotNull IxgbeTxQueue[] txQueues;

	/** The memory manager. */
	private final @NotNull IxyMemoryManager mmanager;

	/** The memory of the PCI device. */
	private @Nullable MappedByteBuffer memory;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a device driver for Intel Ixgbe devices.
	 *
	 * @param name          The device name.
	 * @param rxQueues      The number of read queues.
	 * @param txQueues      The number of write queues.
	 * @param memoryManager The memory manager.
	 * @throws FileNotFoundException If the device does not exist.
	 */
	IxgbeDevice(@NotNull String name, int rxQueues, int txQueues, @NotNull IxyMemoryManager memoryManager) throws FileNotFoundException {
		super(name, "ixgbe");
		if (rxQueues < 0 || txQueues < 0) {
			throw new UnsupportedOperationException("Negative queues are not supported");
		} else if (rxQueues > MAX_QUEUES || txQueues > MAX_QUEUES) {
			throw new UnsupportedOperationException(String.format("The maximum amount of queues is %d", MAX_QUEUES));
		}
		this.rxQueues = new IxgbeRxQueue[rxQueues];
		this.txQueues = new IxgbeTxQueue[txQueues];
		mmanager = memoryManager;
	}

	/** Does all the appropriate calls to reset and initialize the link properly. */
	private void resetAndInitAll() {
		if (BuildConfig.DEBUG) log.info("Resetting and initializing device: {}", this);
		resetLink();
		initLink();

		if (BuildConfig.DEBUG) log.debug("Resetting stats.");
		val stats = Stats.of(this);
		readStats(stats);

		initRx();
		initTx();

		// Start each Rx/Tx queue
		for (int i = 0; i < rxQueues.length; i += 1) {
			startRxQueue(i);
		}
		for (int i= 0; i < txQueues.length; i += 1) {
			startTxQueue(i);
		}

		enablePromiscuous();
		waitLink();
	}

	/** Resets the link. */
	private void resetLink() {
		if (BuildConfig.DEBUG) log.debug("Resetting link: {}", this);

		if (BuildConfig.DEBUG) log.trace("Disabling all interrupts.");
		setRegister(IxgbeDefs.EIMC, Integer.MAX_VALUE);

		if (BuildConfig.DEBUG) log.trace("Global reset.");
		setRegister(IxgbeDefs.CTRL, IxgbeDefs.CTRL_RST_MASK);
		waitClearFlags(IxgbeDefs.CTRL, IxgbeDefs.CTRL_RST_MASK);

		if (BuildConfig.DEBUG) log.trace("Sleeping for 10 milliseconds.");
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			log.error("Cannot sleep thread", e);
		}

		if (BuildConfig.DEBUG) log.trace("Disabling all interrupts again.");
		setRegister(IxgbeDefs.EIMC, Integer.MAX_VALUE);
	}

	/** Initializes the link. */
	private void initLink() {
		if (BuildConfig.DEBUG) {
			log.debug("Initializing link: {}", this);
			log.trace("Waiting for EEPROM auto read completion.");
		}
		waitSetFlags(IxgbeDefs.EEC, IxgbeDefs.EEC_ARD);

		if (BuildConfig.DEBUG) log.debug("Waiting for DMA initialization to complete.");
		waitSetFlags(IxgbeDefs.RDRXCTL, IxgbeDefs.RDRXCTL_DMAIDONE);

		if (BuildConfig.DEBUG) log.trace("Configuring EEPROM again.");
		setRegister(IxgbeDefs.AUTOC, (getRegister(IxgbeDefs.AUTOC) & ~IxgbeDefs.AUTOC_LMS_MASK) | IxgbeDefs.AUTOC_LMS_10G_SERIAL);
		setRegister(IxgbeDefs.AUTOC, getRegister(IxgbeDefs.AUTOC) & ~IxgbeDefs.AUTOC_10G_PMA_PMD_MASK | IxgbeDefs.AUTOC_10G_XAUI);

		if (BuildConfig.DEBUG) log.trace("Negotiating link automatically.");
		setFlags(IxgbeDefs.AUTOC, IxgbeDefs.AUTOC_AN_RESTART);
	}

	/** Initializes the RX queues. */
	private void initRx() {
		if (BuildConfig.DEBUG) {
			log.debug("Initializing RX queues.");
			log.trace("Disabling RX.");
		}
		clearFlags(IxgbeDefs.RXCTRL, IxgbeDefs.RXCTRL_RXEN);

		if (BuildConfig.DEBUG) log.trace("Enabling 128kb packet buffer.");
		setRegister(IxgbeDefs.RXPBSIZE(0), IxgbeDefs.RXPBSIZE_128KB);
		for (var i = 1; i < 8; i += 1) {
			setRegister(IxgbeDefs.RXPBSIZE(i), 0);
		}

		if (BuildConfig.DEBUG) log.trace("Enabling CRC offloading.");
		setFlags(IxgbeDefs.HLREG0, IxgbeDefs.HLREG0_RXCRCSTRP);
		setFlags(IxgbeDefs.RDRXCTL, IxgbeDefs.RDRXCTL_CRCSTRIP);

		if (BuildConfig.DEBUG) log.trace("Accepting broadcast packets.");
		setFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_BAM);

		if (BuildConfig.DEBUG) log.trace("Configuring all RX queues:");
		for(var i = 0; i < rxQueues.length; i += 1) {
			if (BuildConfig.DEBUG) {
				log.debug(">>> Initializing RX queue {}", i);
				log.trace("    Enabling advanced RX descriptors.");
			}
			setRegister(IxgbeDefs.SRRCTL(i), (getRegister(IxgbeDefs.SRRCTL(i)) & ~IxgbeDefs.SRRCTL_DESCTYPE_MASK) | IxgbeDefs.SRRCTL_DESCTYPE_ADV_ONEBUF);

			if (BuildConfig.DEBUG) log.trace("    Dropping packets if not descriptors available.");
			setFlags(IxgbeDefs.SRRCTL(i), IxgbeDefs.SRRCTL_DROP_EN);

			if (BuildConfig.DEBUG) log.trace("    Enabling descriptor ring.");
			int ringSizeBytes = RX_ENTRIES * RX_DESCRIPTOR_SIZE;
			var dma = mmanager.dmaAllocate(ringSizeBytes, IxyMemoryManager.AllocationType.HUGE, IxyMemoryManager.LayoutType.CONTIGUOUS);
			setRegister(IxgbeDefs.RDBAL(i), (int) (dma.getPhysicalAddress() & 0xFFFFFFFFL));
			setRegister(IxgbeDefs.RDBAH(i), (int) (dma.getPhysicalAddress() >> Integer.SIZE));
			setRegister(IxgbeDefs.RDLEN(i), ringSizeBytes);
			if (BuildConfig.DEBUG) {
				log.debug("    RX ring {} virtual address 0x{}.", i, Long.toHexString(dma.getVirtualAddress()));
				log.debug("    RX ring {} physical address 0x{}.", i, Long.toHexString(dma.getPhysicalAddress()));
			}

			if (BuildConfig.DEBUG) log.trace("    Emptying the ring.");
			setRegister(IxgbeDefs.RDH(i), 0);
			setRegister(IxgbeDefs.RDT(i), 0);

			val queue = new IxgbeRxQueue(mmanager, RX_ENTRIES);
			queue.setIndex(0);
			queue.setBaseDescriptorAddress(dma.getVirtualAddress());
			rxQueues[i] = queue;
		}

		if (BuildConfig.DEBUG) log.trace("Enabling magic bits.");
		setFlags(IxgbeDefs.CTRL_EXT, IxgbeDefs.CTRL_EXT_NS_DIS);

		if (BuildConfig.DEBUG) log.trace("Disabling broken features/flags.");
		for (var i = 0; i < rxQueues.length; i += 1) {
			clearFlags(IxgbeDefs.DCA_RXCTRL(i), 1 << 12);
		}

		if (BuildConfig.DEBUG) log.trace("Enabling RX.");
		setRegister(IxgbeDefs.RXCTRL, IxgbeDefs.RXCTRL_RXEN);
	}

	/** Initializes the TX queues. */
	private void initTx() {
		if (BuildConfig.DEBUG) {
			log.debug("Initializing TX queues.");
			log.trace("Enable CRC offloading and small packet padding.");
		}
		setFlags(IxgbeDefs.HLREG0, IxgbeDefs.HLREG0_TXCRCEN | IxgbeDefs.HLREG0_TXPADEN);

		//Set default buffer size allocations (section 4.6.11.3.4)
		if (BuildConfig.DEBUG) log.debug("Setting default buffer size allocations.");
		setRegister(IxgbeDefs.TXPBSIZE(0), IxgbeDefs.TXPBSIZE_40KB);
		for (var i = 1; i < 8; i += 1) {
			setRegister(IxgbeDefs.TXPBSIZE(i), 0);
		}

		if (BuildConfig.DEBUG) log.debug("Configuring for non-DCB and non-VTd.");
		setRegister(IxgbeDefs.DTXMXSZRQ, 0xFFFF);
		clearFlags(IxgbeDefs.RTTDCS, IxgbeDefs.RTTDCS_ARBDIS);

		for(var i = 0; i < txQueues.length; i += 1) {
			if (BuildConfig.DEBUG) {
				log.debug(">>> Initializing TX queue {}", i);
				log.trace("    Allocating memory for descriptor ring.");
			}

			val ringSizeBytes = RX_ENTRIES * TX_DESCRIPTOR_SIZE;
			var dmaMem = mmanager.dmaAllocate(ringSizeBytes, IxyMemoryManager.AllocationType.HUGE, IxyMemoryManager.LayoutType.CONTIGUOUS);
			//TODO : The C version sets the allocated memory to -1 here

			if (BuildConfig.DEBUG) log.trace("    Enabling descriptor ring.");
			setRegister(IxgbeDefs.TDBAL(i), (int) (dmaMem.getPhysicalAddress() & 0xFFFFFFFFL));
			setRegister(IxgbeDefs.TDBAH(i), (int) (dmaMem.getPhysicalAddress() >> Integer.SIZE));
			setRegister(IxgbeDefs.TDLEN(i), ringSizeBytes);
			if (BuildConfig.DEBUG) {
				log.debug("TX ring {} virtual address 0x{}", i, Long.toHexString(dmaMem.getVirtualAddress()));
				log.debug("TX ring {} physical address 0x{}", i, Long.toHexString(dmaMem.getPhysicalAddress()));
			}

			if (BuildConfig.DEBUG) log.trace("Enabling writeback magic values.");
			var txdctl = getRegister(IxgbeDefs.TXDCTL(i));
			txdctl &= ~((0x3F << 16) | (0x3F << 8) | 0x3F);
			txdctl |= ((4 << 16) | (8 << 8) | 36);
			setRegister(IxgbeDefs.TXDCTL(i), txdctl);

			var queue = new IxgbeTxQueue(mmanager, TX_ENTRIES);
			queue.setIndex(0);
			queue.setBaseDescriptorAddress(dmaMem.getVirtualAddress());
			txQueues[i] = queue;
		}

		if (BuildConfig.DEBUG) log.trace("Enabling DMA bit.");
		setRegister(IxgbeDefs.DMATXCTL, IxgbeDefs.DMATXCTL_TE);
	}

	/**
	 * Starts the given RX queue.
	 *
	 * @param id The queue id.
	 */
	private void startRxQueue(int id) {
		if (BuildConfig.DEBUG) log.debug("Starting RX queue {}", id);
		val queue = rxQueues[id];
		val entries = queue.getCapacity();
		if ((entries & (entries - 1)) != 0) {
			throw new IllegalStateException("The number of entries of the queues must be a power of 2");
		}

		if (BuildConfig.DEBUG) log.trace("Allocating memory pool.");
		val  mempoolSize = RX_ENTRIES + TX_ENTRIES;
		queue.setMempool(allocateMempool(Math.max(4096, mempoolSize), 2048));

		for (var i = 0; i < entries; i += 1) {
			var descrAddr = queue.getDescriptorAddress(i);
			if (BuildConfig.DEBUG) log.trace("Setting descriptor address at index {}.", i);

			// Allocate a packet buffer
			val packetBuffer = queue.getMempool().get();
			if (packetBuffer == null) {
				throw new IllegalStateException("Could not allocate packet buffer.");
			}
			queue.setBufferAddress(descrAddr, packetBuffer.getPhysicalAddress() + PacketBuffer.DATA_OFFSET);
			queue.setHeaderBufferAddress(descrAddr, 0);
			queue.addresses[i] = packetBuffer.getVirtualAddress();
		}

		if (BuildConfig.DEBUG) log.trace("Enabling and waiting for RX queue {}.", id);
		setFlags(IxgbeDefs.RXDCTL(id), IxgbeDefs.RXDCTL_ENABLE);
		waitSetFlags(IxgbeDefs.RXDCTL(id), IxgbeDefs.RXDCTL_ENABLE);

		// Rx queue starts out full
		setRegister(IxgbeDefs.RDH(id), 0);

		// Was set to 0 before in the init function
		setRegister(IxgbeDefs.RDT(id), (entries - 1));
	}

	/**
	 * Starts the given TX queue.
	 *
	 * @param id The queue id.
	 */
	private void startTxQueue(int id) {
		if (BuildConfig.DEBUG) log.debug("Starting TX queue {}.", id);
		val queue = txQueues[id];
		val entries = queue.getCapacity();
		if((entries & (entries - 1)) != 0) {
			throw new IllegalStateException("The number of entries of the queues must be a power of 2");
		}

		// TX queue starts out empty
		setRegister(IxgbeDefs.TDH(id), 0);
		setFlags(IxgbeDefs.TDT(id), 0);

		if (BuildConfig.DEBUG) log.trace("Enabling and waiting for TX queue {}.", id);
		setFlags(IxgbeDefs.TXDCTL(id), IxgbeDefs.TXDCTL_ENABLE);
		waitSetFlags(IxgbeDefs.TXDCTL(id), IxgbeDefs.TXDCTL_ENABLE);
	}

	/**
	 * Waits for the link to start up.
	 * <p>
	 * After 10 seconds a warning is issued and it is assumed it is ready.
	 */
	private void waitLink() {
		if (BuildConfig.DEBUG) log.debug("Waiting for link.");
		var waited = 0;
		var speed = getLinkSpeed();
		while (speed == 0 && waited < 10_000) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				log.error("Could not sleep for 10 milliseconds.", e);
			}
			waited += 10;
			speed = getLinkSpeed();
		}
		if (BuildConfig.DEBUG) {
			if (speed == 0L) {
				log.warn("Timed out while waiting for the link.");
			} else {
				log.info("Link established @ {} Mbit/s", speed / (double) MBPS_TO_BPS);
			}
		}
	}

	private IxyMempool allocateMempool(int capacity, int entrySize) {
		if (mmanager.hugepageSize() % entrySize != 0) {
			throw new InvalidSizeException("entrySize");
		}
		val dma = mmanager.dmaAllocate(capacity * entrySize, IxyMemoryManager.AllocationType.STANDARD, IxyMemoryManager.LayoutType.STANDARD);
		val mempool = new IxgbeMempool(capacity);
		mempool.setPacketSize(entrySize);
		mempool.allocate(mmanager, dma);
		return mempool;
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public void allocate() {
		if (BuildConfig.DEBUG) log.info("Mapping device memory.");
		memory = map().orElse(null);
		resetAndInitAll();
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
		val rxPackets = getRegister(IxgbeDefs.GPRC);
		val txPackets = getRegister(IxgbeDefs.GPTC);
		val rxBytes = getRegister(IxgbeDefs.GORCL) + (((long) getRegister(IxgbeDefs.GORCH)) << Integer.SIZE);
		val txBytes = getRegister(IxgbeDefs.GOTCL) + (((long) getRegister(IxgbeDefs.GOTCH)) << Integer.SIZE);
		stats.addRxPackets(rxPackets);;
		stats.addTxPackets(txPackets);
		stats.addRxBytes(rxBytes);
		stats.addTxBytes(txBytes);
	}

}
