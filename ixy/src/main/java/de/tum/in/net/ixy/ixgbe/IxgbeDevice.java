package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.Device;
import de.tum.in.net.ixy.Stats;
import de.tum.in.net.ixy.memory.JniMemoryManager;
import de.tum.in.net.ixy.memory.MemoryManager;
import de.tum.in.net.ixy.memory.Mempool;
import de.tum.in.net.ixy.memory.PacketBufferWrapper;
import de.tum.in.net.ixy.memory.SmartJniMemoryManager;
import de.tum.in.net.ixy.memory.SmartUnsafeMemoryManager;
import de.tum.in.net.ixy.utils.Threads;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.tum.in.net.ixy.BuildConfig.DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_DEBUG;
import static de.tum.in.net.ixy.BuildConfig.LOG_INFO;
import static de.tum.in.net.ixy.BuildConfig.LOG_TRACE;
import static de.tum.in.net.ixy.BuildConfig.LOG_WARN;
import static de.tum.in.net.ixy.BuildConfig.MEMORY_MANAGER;
import static de.tum.in.net.ixy.BuildConfig.OPTIMIZED;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI;
import static de.tum.in.net.ixy.BuildConfig.PREFER_JNI_FULL;
import static de.tum.in.net.ixy.utils.Strings.leftPad;

/**
 * The device driver for Intel's 10-Gigabit NICs ({@code ixgbe} family).
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
public final class IxgbeDevice extends Device {

	///////////////////////////////////////////////// STATIC VARIABLES /////////////////////////////////////////////////

	/** The maximum number of queues supported. */
	private static final int MAX_QUEUES = 64;

	/** The minimum number of entries of the memory pool. */
	private static final int MIN_MEMPOOL_ENTRIES = 4096;

	/** The maximum number of entries per RX queue. */
	private static final short RX_MAX_ENTRIES = 4096;

	/** The maximum number of entries per TX queue. */
	private static final short TX_MAX_ENTRIES = 4096;

	/** The number of entries per RX queue. */
	private static final short RX_ENTRIES = 512;

	/** The number of entries per TX queue. */
	private static final short TX_ENTRIES = 512;

	/** The size of a descriptor of the RX queue. */
	private static final int RX_DESCRIPTOR_SIZE = 16;

	/** The size of a descriptor of the TX queue. */
	private static final int TX_DESCRIPTOR_SIZE = 16;

	/** The amount of packets to clean per batch. */
	private static final short TX_CLEAN_BATCH = 32;

	/** The factor used to transform Mbps to bps. */
	private static final int CLASS_NIC = 0x02;

	/** The number of milliseconds to wait for the link to come up. */
	private static final int WAIT_LINK_MS = 10_000;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	static {
		if (RX_ENTRIES > RX_MAX_ENTRIES) throw new IllegalStateException("The number of RX entries is too big.");
		if (TX_ENTRIES > TX_MAX_ENTRIES) throw new IllegalStateException("The number of TX entries is too big.");
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** The read queues. */
	private final @NotNull IxgbeRxQueue[] rxQueues;

	/** The write queues. */
	private final @NotNull IxgbeTxQueue[] txQueues;

	/**
	 * Keeps track of the {@link PacketBufferWrapper} instances passed to {@link
	 * #txBatch(int, PacketBufferWrapper[], int, int)} to return them to the correct memory pool.
	 */
	@SuppressWarnings("FieldNotUsedInToString")
	private final @NotNull PacketBufferWrapper[][] cleanablePool;

	/** The memory manager. */
	@SuppressWarnings({"FieldNotUsedInToString", "NestedConditionalExpression"})
	private final @NotNull MemoryManager mmanager = MEMORY_MANAGER == PREFER_JNI_FULL
			? JniMemoryManager.getSingleton()
			: MEMORY_MANAGER == PREFER_JNI
			? SmartJniMemoryManager.getSingleton()
			: SmartUnsafeMemoryManager.getSingleton();

	private final @NotNull MemoryManager _mmanager = SmartJniMemoryManager.getSingleton();

	/** The memory mapping of the PCI resource. */
	private long mapResource;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	/**
	 * Creates a device driver for Intel Ixgbe devices.
	 *
	 * @param name     The device name.
	 * @param rxQueues The number of read queues.
	 * @param txQueues The number of write queues.
	 * @throws FileNotFoundException If the device does not exist.
	 */
	public IxgbeDevice(final @NotNull String name, int rxQueues, int txQueues) throws FileNotFoundException {
		super(name, "ixgbe");
		if (!OPTIMIZED) {
			if (rxQueues < 0 || txQueues < 0) {
				throw new NegativeArraySizeException("The parameter 'rxQueues' and 'txQueues' MUST be positive.");
			}
			if (rxQueues > MAX_QUEUES) {
				if (DEBUG > LOG_WARN) log.warn("The amount of RX queues is over the allowed limit. Adapting queues.");
				rxQueues = MAX_QUEUES;
			}
			if (txQueues > MAX_QUEUES) {
				if (DEBUG > LOG_WARN) log.warn("The amount of TX queues is over the allowed limit. Adapting queues.");
				txQueues = MAX_QUEUES;
			}
		}
		this.rxQueues = new IxgbeRxQueue[rxQueues];
		this.txQueues = new IxgbeTxQueue[txQueues];
		this.cleanablePool = new PacketBufferWrapper[txQueues][TX_ENTRIES];
		mapResource = super.map();
	}

	/** Does all the appropriate calls to reset and initialize the link properly. */
	private void resetAndInitAll() {
		if (DEBUG >= LOG_INFO) log.info("Resetting and initializing device: {}", this);
		resetLink();
		initLink();

		if (DEBUG >= LOG_DEBUG) log.debug("Resetting stats.");
		val stats = new Stats();
		readStats(stats);

		// Initialize the structures of the queues
		initRx();
		initTx();

		// Start all Rx/Tx queues
		for (var i = 0; i < rxQueues.length; i += 1) {
			startRxQueue(i);
		}
		for (var i = 0; i < txQueues.length; i += 1) {
			startTxQueue(i);
		}

		enablePromiscuous();
		waitLink();
	}

	/** Resets the link. */
	private void resetLink() {
		if (DEBUG >= LOG_DEBUG) log.debug("Resetting link: {}", this);

		if (DEBUG >= LOG_TRACE) log.trace("Disabling all interrupts.");
		setRegister(IxgbeDefs.EIMC, Integer.MAX_VALUE);

		if (DEBUG >= LOG_TRACE) log.trace("Issuing global reset.");
		setRegister(IxgbeDefs.CTRL, IxgbeDefs.CTRL_RST_MASK);
		waitClearFlags(IxgbeDefs.CTRL, IxgbeDefs.CTRL_RST_MASK);

		if (DEBUG >= LOG_TRACE) log.trace("Sleeping for 10 milliseconds.");
		Threads.sleep(10);

		if (DEBUG >= LOG_TRACE) log.trace("Disabling all interrupts again.");
		setRegister(IxgbeDefs.EIMC, Integer.MAX_VALUE);
	}

	/** Initializes the link. */
	@SuppressFBWarnings("INT_VACUOUS_BIT_OPERATION")
	private void initLink() {
		if (DEBUG >= LOG_DEBUG) log.debug("Initializing link: {}", this);

		if (DEBUG >= LOG_TRACE) log.trace("Waiting for EEPROM auto read completion.");
		waitSetFlags(IxgbeDefs.EEC, IxgbeDefs.EEC_ARD);

		if (DEBUG >= LOG_TRACE) log.trace("Waiting for DMA initialization to complete.");
		waitSetFlags(IxgbeDefs.RDRXCTL, IxgbeDefs.RDRXCTL_DMAIDONE);

		if (DEBUG >= LOG_TRACE) log.trace("Configuring EEPROM again.");
		setRegister(IxgbeDefs.AUTOC, (getRegister(IxgbeDefs.AUTOC) & ~IxgbeDefs.AUTOC_LMS_MASK)
				| IxgbeDefs.AUTOC_LMS_10G_SERIAL);
		setRegister(IxgbeDefs.AUTOC, (getRegister(IxgbeDefs.AUTOC) & ~IxgbeDefs.AUTOC_10G_PMA_PMD_MASK)
				| IxgbeDefs.AUTOC_10G_XAUI);

		if (DEBUG >= LOG_TRACE) log.trace("Negotiating link automatically.");
		setFlags(IxgbeDefs.AUTOC, IxgbeDefs.AUTOC_AN_RESTART);
	}

	/** Initializes the RX queues. */
	@SuppressWarnings({"Duplicates", "HardcodedFileSeparator", "LawOfDemeter"})
	private void initRx() {
		if (DEBUG >= LOG_DEBUG) log.debug("Initializing RX queues.");

		if (DEBUG >= LOG_TRACE) log.trace("Disabling RX.");
		clearFlags(IxgbeDefs.RXCTRL, IxgbeDefs.RXCTRL_RXEN);

		if (DEBUG >= LOG_TRACE) log.trace("Enabling 128kb packet buffer.");
		setRegister(IxgbeDefs.RXPBSIZE(0), IxgbeDefs.RXPBSIZE_128KB);
		for (var i = 1; i < 8; i += 1) {
			setRegister(IxgbeDefs.RXPBSIZE(i), 0);
		}

		if (DEBUG >= LOG_TRACE) log.trace("Enabling CRC offloading.");
		setFlags(IxgbeDefs.HLREG0, IxgbeDefs.HLREG0_RXCRCSTRP);
		setFlags(IxgbeDefs.RDRXCTL, IxgbeDefs.RDRXCTL_CRCSTRIP);

		if (DEBUG >= LOG_TRACE) log.trace("Accepting broadcast packets.");
		setFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_BAM);

		if (DEBUG >= LOG_DEBUG) log.trace("Configuring all RX queues:");
		for (var i = 0; i < rxQueues.length; i += 1) {
			if (DEBUG >= LOG_DEBUG) log.debug(">>> Initializing RX queue #{}.", i);

			if (DEBUG >= LOG_TRACE) log.trace("Enabling advanced RX descriptors.");
			setRegister(IxgbeDefs.SRRCTL(i), ((getRegister(IxgbeDefs.SRRCTL(i)) & ~IxgbeDefs.SRRCTL_DESCTYPE_MASK))
					| IxgbeDefs.SRRCTL_DESCTYPE_ADV_ONEBUF);

			if (DEBUG >= LOG_TRACE) log.trace("Dropping packets if not descriptors available.");
			setFlags(IxgbeDefs.SRRCTL(i), IxgbeDefs.SRRCTL_DROP_EN);

			if (DEBUG >= LOG_TRACE) log.trace("Enabling descriptor ring.");
			val ringSizeBytes = RX_ENTRIES * RX_DESCRIPTOR_SIZE;
			val dma = mmanager.dmaAllocate(ringSizeBytes, true, true);

			if (DEBUG >= LOG_TRACE) log.trace("Setting everything to -1.");
			var addr = dma.getVirtual();
			val max = addr + ringSizeBytes;
			while (addr < max) {
				mmanager.putLongVolatile(addr, -1L);
				addr += Long.BYTES;
			}

			val buff = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.nativeOrder()).putLong(0, dma.getPhysical())
					.asIntBuffer();
			waitAndSetRegister(IxgbeDefs.RDBAL(i), buff.get(0));
			waitAndSetRegister(IxgbeDefs.RDBAH(i), buff.get(1));
			waitAndSetRegister(IxgbeDefs.RDLEN(i), ringSizeBytes);
			if (DEBUG >= LOG_INFO) {
				log.info("RX ring {} virtual address 0x{}.", i, leftPad(dma.getVirtual()));
				log.info("RX ring {} physical address 0x{}.", i, leftPad(dma.getPhysical()));
			}

			if (DEBUG >= LOG_TRACE) log.trace("Emptying the ring.");
			waitAndSetRegister(IxgbeDefs.RDH(i), 0);
			waitAndSetRegister(IxgbeDefs.RDT(i), 0);

			val queue = new IxgbeRxQueue(dma.getVirtual(), RX_ENTRIES);
			queue.index = 0;
			rxQueues[i] = queue;
		}

		if (DEBUG >= LOG_TRACE) log.trace("Enabling magic bits.");
		setFlags(IxgbeDefs.CTRL_EXT, IxgbeDefs.CTRL_EXT_NS_DIS);

		if (DEBUG >= LOG_TRACE) log.trace("Disabling broken features/flags.");
		for (var i = 0; i < rxQueues.length; i += 1) {
			clearFlags(IxgbeDefs.DCA_RXCTRL(i), 1 << 12);
		}

		if (DEBUG >= LOG_TRACE) log.trace("Enabling RX.");
		setFlags(IxgbeDefs.RXCTRL, IxgbeDefs.RXCTRL_RXEN);
	}

	/** Initializes the TX queues. */
	@SuppressWarnings({"Duplicates", "LawOfDemeter", "MagicNumber"})
	private void initTx() {
		if (DEBUG >= LOG_DEBUG) log.debug("Initializing TX queues.");

		if (DEBUG >= LOG_TRACE) log.trace("Enable CRC offloading and small packet padding.");
		setFlags(IxgbeDefs.HLREG0, IxgbeDefs.HLREG0_TXCRCEN | IxgbeDefs.HLREG0_TXPADEN);

		if (DEBUG >= LOG_TRACE) log.trace("Setting default buffer size allocations.");
		setRegister(IxgbeDefs.TXPBSIZE(0), IxgbeDefs.TXPBSIZE_40KB);
		for (var i = 1; i < 8; i += 1) {
			setRegister(IxgbeDefs.TXPBSIZE(i), 0);
		}

		if (DEBUG >= LOG_TRACE) log.trace("Configuring for non-DCB and non-VTd.");
		setRegister(IxgbeDefs.DTXMXSZRQ, 0xFFFF);
		clearFlags(IxgbeDefs.RTTDCS, IxgbeDefs.RTTDCS_ARBDIS);

		for (var i = 0; i < txQueues.length; i += 1) {
			if (DEBUG >= LOG_DEBUG) log.debug(">>> Initializing TX queue #{}.", i);

			if (DEBUG >= LOG_TRACE) log.trace("Allocating memory for descriptor ring.");
			val ringSizeBytes = TX_ENTRIES * TX_DESCRIPTOR_SIZE;
			var dma = mmanager.dmaAllocate(ringSizeBytes, true, true);

			if (DEBUG >= LOG_TRACE) log.trace("Setting everything to -1.");
			var addr = dma.getVirtual();
			val max = addr + ringSizeBytes;
			while (addr < max) {
				mmanager.putLongVolatile(addr, -1L);
				addr += Long.BYTES;
			}

			if (DEBUG >= LOG_TRACE) log.trace("Enabling descriptor ring.");
			val buff = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.nativeOrder()).putLong(0, dma.getPhysical()).asIntBuffer();
			waitAndSetRegister(IxgbeDefs.TDBAL(i), buff.get(0));
			waitAndSetRegister(IxgbeDefs.TDBAH(i), buff.get(1));
			waitAndSetRegister(IxgbeDefs.TDLEN(i), ringSizeBytes);
			if (DEBUG >= LOG_INFO) {
				log.info("TX ring {} virtual address 0x{}.", i, leftPad(dma.getVirtual()));
				log.info("TX ring {} physical address 0x{}.", i, leftPad(dma.getPhysical()));
			}

			if (DEBUG >= LOG_TRACE) log.trace("Enabling writeback magic values.");
			var txdctl = getRegister(IxgbeDefs.TXDCTL(i));
			txdctl &= ~((0x3F << 16) | (0x3F << 8) | 0x3F);
			txdctl |= ((4 << 16) | (8 << 8) | 36);
			setRegister(IxgbeDefs.TXDCTL(i), txdctl);

			var queue = new IxgbeTxQueue(dma.getVirtual(), TX_ENTRIES);
			queue.index = 0;
			txQueues[i] = queue;
		}

		if (DEBUG >= LOG_TRACE) log.trace("Enabling DMA bit.");
		setRegister(IxgbeDefs.DMATXCTL, IxgbeDefs.DMATXCTL_TE);
	}

	/**
	 * Starts the given RX queue.
	 *
	 * @param queueId The queue id.
	 */
	@SuppressWarnings("LawOfDemeter")
	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
	private void startRxQueue(final int queueId) {
		if (DEBUG >= LOG_DEBUG) log.debug("Starting RX queue #{}.", queueId);
		val queue = rxQueues[queueId];

		if (DEBUG >= LOG_TRACE) log.trace("Allocating memory pool.");
		val  mempoolSize = RX_ENTRIES + TX_ENTRIES;
		queue.mempool = allocateMempool(Math.max(MIN_MEMPOOL_ENTRIES, mempoolSize), 2048);

		if (DEBUG >= LOG_DEBUG) log.debug("Setting descriptor addresses:");
		for (var i = 0; i < queue.capacity; i += 1) {
			if (DEBUG >= LOG_DEBUG) log.debug(">>> Setting descriptor address #{}.", i);

			if (DEBUG >= LOG_TRACE) log.trace("Extracting free packet buffer wrapper.");
			val packetBufferWrapper = queue.mempool.poll();
			if (packetBufferWrapper == null) throw new IllegalStateException("Could not allocate packet buffer.");

			if (DEBUG >= LOG_TRACE) {
				log.trace("Configuring queue with packet buffer wrapper data: {}.", packetBufferWrapper);
			}
			val descrAddr = queue.getDescriptorAddress(i);
			queue.setPacketBufferAddress(descrAddr,
					packetBufferWrapper.getPhysicalAddress() + PacketBufferWrapper.DATA_OFFSET);
			queue.setPacketBufferHeaderAddress(descrAddr, 0);
			queue.buffers[i] = packetBufferWrapper.getVirtual();

			if (DEBUG >= LOG_TRACE) log.trace("Adding packet buffer back.");
			queue.mempool.offer(packetBufferWrapper);
		}

		if (DEBUG >= LOG_TRACE) log.trace("Enabling and waiting for RX queue #{}.", queueId);
		setFlags(IxgbeDefs.RXDCTL(queueId), IxgbeDefs.RXDCTL_ENABLE);
		waitSetFlags(IxgbeDefs.RXDCTL(queueId), IxgbeDefs.RXDCTL_ENABLE);

		// Rx queue starts out full
		waitAndSetRegister(IxgbeDefs.RDH(queueId), 0);
		waitAndSetRegister(IxgbeDefs.RDT(queueId), (queue.capacity - 1));
	}

	/**
	 * Starts the given TX queue.
	 *
	 * @param queueId The queue id.
	 */
	private void startTxQueue(final int queueId) {
		if (DEBUG >= LOG_DEBUG) log.debug("Starting TX queue #{}.", queueId);

		// TX queue starts out empty
		waitAndSetRegister(IxgbeDefs.TDH(queueId), 0);
		waitAndSetRegister(IxgbeDefs.TDT(queueId), 0);

		if (DEBUG >= LOG_TRACE) log.trace("Enabling and waiting for TX queue #{}.", queueId);
		setFlags(IxgbeDefs.TXDCTL(queueId), IxgbeDefs.TXDCTL_ENABLE);
		waitSetFlags(IxgbeDefs.TXDCTL(queueId), IxgbeDefs.TXDCTL_ENABLE);
	}

	/**
	 * Waits for the link to start up.
	 * <p>
	 * After 10 seconds a warning is issued and it is assumed it is ready.
	 */
	@SuppressWarnings({"HardcodedFileSeparator", "PMD.AvoidLiteralsInIfCondition"})
	private void waitLink() {
		if (DEBUG >= LOG_INFO) log.info("Waiting for link.");

		// Active block until 10.000 iterations or the link speed is available
		var waited = 0;
		var speed = getLinkSpeed();
		while (speed == 0 && waited < WAIT_LINK_MS) {
			Threads.sleep(10);
			waited += 10;
			speed = getLinkSpeed();
		}

		if (DEBUG >= LOG_INFO) {
			if (speed != 0L) {
				log.info("Link speed working @ {} Mbit/s", speed);
			}
		} else if (DEBUG >= LOG_WARN) {
			if (speed == 0L) {
				log.warn("Timed out while waiting for the link to go up.");
			} else {
				log.info("Link speed working @ {} Mbit/s", speed);
			}
		}
	}

	/**
	 * Creates a memory pool of the given capacity and packet buffer wrapper size.
	 *
	 * @param capacity  The capacity of the queue.
	 * @param entrySize The number of entries this queue has.
	 * @return The memory pool.
	 */
	@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
	private @NotNull Mempool allocateMempool(final int capacity, final int entrySize) {
		if (mmanager.getHugepageSize() % entrySize != 0) {
			throw new IllegalArgumentException("The buffer size of a packet buffer wrapper MUST be"
					+ " a divisor of the size of a huge memory page.");
		}
		val dma = mmanager.dmaAllocate((long) capacity * entrySize, true, true);
		val mempool = new Mempool(capacity);
		mempool.allocate(entrySize, dma);
		return mempool;
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
	public long map() {
		return (mapResource != 0L)  ? mapResource : (mapResource = super.map());
	}

	/** {@inheritDoc} */
	@Override
	public void configure() {
		if (DEBUG >= LOG_INFO) log.info("Mapping device memory.");
		resetAndInitAll();
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("SwitchStatementWithTooManyBranches")
	public boolean isSupported() throws IOException {
		if (getClassId() != CLASS_NIC) return false;
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

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
	public int getRegister(final int offset) {
		if (!OPTIMIZED) {
			if (mapResource == 0L) throw new IllegalStateException("the memory MUST be mapped.");
			if (offset < 0) throw new IllegalArgumentException("The parameter 'offset' MUST NOT be negative.");
		}
		if (DEBUG >= LOG_TRACE) log.trace("Reading register @ 0x{} + 0x{}.", leftPad(mapResource), leftPad(offset));
		return mmanager.getIntVolatile(mapResource + offset);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
	protected void setRegister(final int offset, final int value) {
		if (!OPTIMIZED) {
			if (mapResource == 0L) throw new IllegalStateException("the memory MUST be mapped.");
			if (offset < 0) throw new IllegalArgumentException("The parameter 'offset' MUST NOT be negative.");
		}
		if (DEBUG >= LOG_TRACE) {
			log.trace("Writing value 0x{} to register @ 0x{} + 0x{}.",
					leftPad(value), leftPad(mapResource), leftPad(offset));
		}
		mmanager.putIntVolatile(mapResource + offset, value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isPromiscuousEnabled() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking promiscuous mode.");
		val reg = getRegister(IxgbeDefs.FCTRL);
		val mask = IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE;
		return (reg & mask) == mask;
	}

	/** {@inheritDoc} */
	@Override
	public void enablePromiscuous() {
		if (DEBUG >= LOG_DEBUG) log.debug("Enabling promiscuous mode.");
		setFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE);
	}

	/** {@inheritDoc} */
	@Override
	public void disablePromiscuous() {
		if (DEBUG >= LOG_DEBUG) log.debug("Disabling promiscuous mode.");
		clearFlags(IxgbeDefs.FCTRL, IxgbeDefs.FCTRL_MPE | IxgbeDefs.FCTRL_UPE);
	}

	/** {@inheritDoc} */
	@Override
	public long getLinkSpeed() {
		if (DEBUG >= LOG_TRACE) log.trace("Checking link speed.");

		// Check if the link is up and ready
		val links = getRegister(IxgbeDefs.LINKS);
		if ((links & IxgbeDefs.LINKS_UP) == 0) {
			if (DEBUG >= LOG_TRACE) log.trace("The link is not up.");
			return 0;
		}

		// Make sure the link speed is valid
		switch (links & IxgbeDefs.LINKS_SPEED_82599) {
			case IxgbeDefs.LINKS_SPEED_100_82599:
				return 100;
			case IxgbeDefs.LINKS_SPEED_1G_82599:
				return 1_000;
			case IxgbeDefs.LINKS_SPEED_10G_82599:
				return 10_000;
			default:
				if (DEBUG >= LOG_WARN) log.warn("Unknown link speed.");
				return 0;
		}
	}

	/** {@inheritDoc} */
	@Override
	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
	@SuppressWarnings({"Duplicates", "ForLoopWithMissingComponent", "LawOfDemeter", "PMD.DataflowAnomalyAnalysis"})
	public int rxBatch(final int queueId, final @NotNull PacketBufferWrapper[] buffers, final int offset, int length) {
		if (!OPTIMIZED) {
			if (queueId < 0 || queueId >= rxQueues.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'queueId' MUST be in the range [0, rxQueues).");
			}
			if (buffers == null) throw new NullPointerException("The parameter 'packets' MUST NOT be null.");
			if (offset < 0 || offset >= buffers.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be inside [0, buffers.length).");
			}
			if (length < 0) throw new IllegalArgumentException("The parameter 'length' MUST be positive.");
			val diff = buffers.length - offset;
			if (diff < length) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to receive {} PacketBufferWrappers but the buffer can only store up to {}."
							+ "Adapting length.", length, diff);
				}
				length = diff;
			}
			if (length == 0) return 0;
		}

		// Prepare for the loop
		val queue = rxQueues[queueId];
		var rxIndex = queue.index;
		var lastRxIndex = rxIndex;
		var bufInd = offset;
		val max = bufInd + length;

		// Try to read as many packets as the user wants
		for (; bufInd < max; bufInd += 1) {

			// Get a descriptor and its status
			val descAddr = queue.getDescriptorAddress(rxIndex);
			val status = queue.getWritebackErrorStatus(descAddr);

			// If we don't have packets, stop processing
			if ((status & IxgbeDefs.RXDADV_STAT_DD) == 0) break;

			// If there is no End of Packet, we need to stop, because we haven't implemented multi-segment packets
			if ((status & IxgbeDefs.RXDADV_STAT_EOP) == 0) {
				throw new UnsupportedOperationException("Multisegment pkts. NOT supported; incr. buffer or decr. MTU.");
			}

			// There is a packet, read and copy the whole descriptor
			val packetBuffer = new PacketBufferWrapper(queue.buffers[rxIndex]);
			packetBuffer.setSize(queue.getWritebackLength(descAddr));

			// This would be the place to implement RX offloading by translating the device-specific
			// flags to an independent representation in that buffer (similar to how DPDK works)
			val newBuf = queue.mempool.poll();
			if (newBuf == null) {
				throw new OutOfMemoryError("Failed to allocate buffer for RX; memory leaking or small memory pool.");
			}

			// Register the packet in the RX queue
			queue.setPacketBufferAddress(descAddr, newBuf.getPhysicalAddress() + PacketBufferWrapper.DATA_OFFSET);
			queue.setPacketBufferHeaderAddress(descAddr, 0);
			queue.buffers[rxIndex] = newBuf.getVirtual();
			buffers[bufInd] = packetBuffer;

			// Want to read the next one in the next iteration but we still need the current one to update RDT later
			lastRxIndex = rxIndex;
			rxIndex = wrapRing(rxIndex, queue.capacity);
		}

		// Notify the hardware that we are done
		if (rxIndex != lastRxIndex) {
			waitAndSetRegister(IxgbeDefs.RDT(queueId), lastRxIndex);
			queue.index = rxIndex;
		}

		// Return the number of processed packets
		return bufInd - offset;
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings({"Duplicates", "ForLoopWithMissingComponent", "LawOfDemeter", "PMD.DataflowAnomalyAnalysis"})
	public int txBatch(final int queueId, final @NotNull PacketBufferWrapper[] buffers, final int offset, int length) {
		if (!OPTIMIZED) {
			if (queueId < 0 || queueId >= txQueues.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'queueId' MUST be in the range [0, rxQueues).");
			}
			if (buffers == null) throw new NullPointerException("The parameter 'packets' MUST NOT be null.");
			if (offset < 0 || offset >= buffers.length) {
				throw new ArrayIndexOutOfBoundsException("The parameter 'offset' MUST be inside [0, buffers.length).");
			}
			if (length < 0) throw new IllegalArgumentException("The parameter 'length' MUST be positive.");
			val diff = buffers.length - offset;
			if (diff < length) {
				if (DEBUG >= LOG_WARN) {
					log.warn("You are trying to send {} PacketBufferWrappers but the buffer can only store up to {}."
							+ "Adapting length.", length, diff);
				}
				length = diff;
			}
			if (length == 0) return 0;
		}

		// Prepare for the loop
		val queue = txQueues[queueId];
		var cleanIndex = queue.cleanIndex;
		var currentIndex = queue.index;
		val cmdTypeFlags = IxgbeDefs.ADVTXD_DCMD_EOP | IxgbeDefs.ADVTXD_DCMD_RS | IxgbeDefs.ADVTXD_DCMD_IFCS
				| IxgbeDefs.ADVTXD_DCMD_DEXT | IxgbeDefs.ADVTXD_DTYP_DATA;

		// All packet buffers that will be handled here will belong to the same mempool
		Mempool pool = null;

		// Step 1: Clean up descriptors that were sent out by the hardware and return them to the mempool
		// Start by reading step 2 which is done first for each packet
		// Cleaning up must be done in batches for performance reasons, so this is unfortunately somewhat complicated
		while (true) {

			// Invariant: currentIndex is always ahead of clean, therefore we can calculate how many packets to clean
			var cleanable = currentIndex - cleanIndex;
			if (cleanable < 0) cleanable = queue.capacity + cleanable;
			if (cleanable < TX_CLEAN_BATCH) break;

			// Calculate the index of the last descriptor in the clean batch
			// We can't check all descriptors for performance reasons
			var cleanupTo = cleanIndex + TX_CLEAN_BATCH - 1;
			if (cleanupTo >= queue.capacity) cleanupTo -= queue.capacity;

			// Get the descriptor and its status
			val descAddr = queue.getDescriptorAddress(cleanupTo);
			val status = queue.getOffloadInfoStatus(descAddr);

			// If this flag is off, it means it we can't clean up the whole batch (according to te hardware)
			if ((status & IxgbeDefs.ADVTXD_STAT_DD) == 0) break;

			var i = cleanIndex;
			while (true) {
				val packetBuffer = cleanablePool[queueId][i];
//				val packetBuffer = new PacketBufferWrapper(queue.buffers[i]);
				if (pool == null) {
					pool = Mempool.find(packetBuffer);
					if (pool == null) throw new IllegalStateException("Could NOT find mempool with the given id.");
				}
				pool.offer(packetBuffer);
//				cleanablePool[queueId][i] = null;
				if (i == cleanupTo) break;
				i = wrapRing(i, queue.capacity);
			}

			// Next descriptor to be cleaned up is one after the one we just cleaned
			cleanIndex = wrapRing((short) cleanupTo, queue.capacity);
		}

		// Update the clean index
		queue.cleanIndex = cleanIndex;

		// Step 2: Send out as many of our packets as possible
		var sent = offset;
		val max = offset + length;
		for (; sent < max; sent += 1) {
			// Get next descriptor index
			val nextIndex = wrapRing(currentIndex, queue.capacity);

			// We are full if the next index is the one we are trying to reclaim
			if (cleanIndex == nextIndex) break;

			// Get the packet buffer, remove it from the original array and cache it for cleaning purposes
			var buffer = buffers[sent];
//			buffers[sent] = null;
			cleanablePool[queueId][currentIndex] = buffer;

			// Remember the virtual address to clean it up later
			queue.buffers[currentIndex] = buffer.getVirtual();
			queue.index = wrapRing(queue.index, queue.capacity);

			// NIC will read the data from here
			val descAddr = queue.getDescriptorAddress(currentIndex);
			queue.setPacketBufferAddress(descAddr, buffer.getPhysicalAddress() + PacketBufferWrapper.DATA_OFFSET);

			// Always the same flags: One buffer (EOP), advanced data descriptor, CRC offload, data length
			var bufSize = buffer.getSize();
			queue.setCmdTypeLength(descAddr, cmdTypeFlags | bufSize);

			// No fancy offloading - only the total payload length
			// implement offloading flags here:
			// - IP checksum offloading is trivial: just set the offset
			// - TCP/UDP checksum offloading is more annoying, you have to pre-calculate the pseudo-header checksum
			queue.setOffloadInfoStatus(descAddr, bufSize << IxgbeDefs.ADVTXD_PAYLEN_SHIFT);
			currentIndex = nextIndex;
		}

		// Send out by advancing tail, i.e. pass control of the bus to the NIC
		setRegister(IxgbeDefs.TDT(queueId), queue.index);
		return sent - offset;
	}

	/** {@inheritDoc} */
	@Override
	public void readStats(@NotNull Stats stats) {
		val rxPackets = getRegister(IxgbeDefs.GPRC);
		val txPackets = getRegister(IxgbeDefs.GPTC);
		val rxBytes = getRegister(IxgbeDefs.GORCL) + (((long) getRegister(IxgbeDefs.GORCH)) << Integer.SIZE);
		val txBytes = getRegister(IxgbeDefs.GOTCL) + (((long) getRegister(IxgbeDefs.GOTCH)) << Integer.SIZE);
		stats.addRxPackets(rxPackets);
		stats.addTxPackets(txPackets);
		stats.addRxBytes(rxBytes);
		stats.addTxBytes(txBytes);
	}

	/** {@inheritDoc} */
	@Override
	public @NotNull String toString() {
		return "IxgbeDevice"
				+ "("
				+ "name=" + name
				+ ", driver=ixgbe"
				+ ", address=" + mapResource
				+ ", rx_queues=" + rxQueues.length
				+ ", tx_queues=" + txQueues.length
				+ ")";
	}

	/**
	 * Computes the next index of a ring buffer.
	 *
	 * @param index    The current index.
	 * @param ringSize The ring buffer size.
	 * @return The next index.
	 */
	private static short wrapRing(final short index, final short ringSize) {
		return (short) ((index + 1) & (ringSize - 1));
	}

}
