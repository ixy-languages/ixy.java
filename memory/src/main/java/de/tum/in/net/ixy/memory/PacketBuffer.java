package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyMemoryManager;
import de.tum.in.net.ixy.generic.IxyMempool;
import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of Ixy's packet buffer specification.
 * <p>
 * The memory layout is depicted below:
 * <pre>
 *                  64 bits
 * /---------------------------------------\
 * |       Physical Address Pointer        |
 * |---------------------------------------|
 * |          Memory Pool Pointer          |
 * |---------------------------------------| 64 bytes
 * | Memory Pool Index |    Packet Size    |
 * |---------------------------------------|
 * |          Headroom (variable)          |
 * \---------------------------------------/
 * </pre>
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals", "PMD.BeanMembersShouldSerialize"})
public final class PacketBuffer implements IxyPacketBuffer, Comparable<IxyPacketBuffer> {

	/**
	 * Builder pattern for packet buffers.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@Slf4j
	@SuppressWarnings({"ConstantConditions", "PMD.AvoidDuplicateLiterals"})
	@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
	public static final class Builder {

		/** The memory manager of the packet. */
		@EqualsAndHashCode.Include
		private @Nullable IxyMemoryManager mmanager;

		/** The virtual memory address of the packet. */
		@EqualsAndHashCode.Include
		private long virtualAddress;

		/** The physical memory address of the packet. */
		@EqualsAndHashCode.Include
		private @Nullable Long physicalAddress;

		/** The size of the packet. */
		@EqualsAndHashCode.Include
		private @Nullable Integer packetSize;

		/** The memory pool identifier of the packet. */
		@EqualsAndHashCode.Include
		private @Nullable Integer memoryPool;

		// Private builder only accessible to the outer class
		private Builder() {}

		/**
		 * Sets the memory manager of the builder.
		 *
		 * @param memoryManager The memory manager.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public @NotNull Builder manager(@NotNull IxyMemoryManager memoryManager) {
			if (BuildConfig.DEBUG) log.debug("Setting memory manager");
			mmanager = memoryManager;
			return this;
		}

		/**
		 * Sets the virtual memory address of the builder.
		 *
		 * @param virtualAddress The virtual memory address.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public @NotNull Builder virtual(long virtualAddress) {
			if (BuildConfig.DEBUG) {
				val xaddress = Long.toHexString(virtualAddress);
				log.debug("Setting virtual memory address 0x{}", xaddress);
			}
			this.virtualAddress = virtualAddress;
			return this;
		}

		/**
		 * Sets the physical memory address of the builder.
		 *
		 * @param physicalAddress The physical memory address.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public @NotNull Builder physical(@Nullable Long physicalAddress) {
			if (BuildConfig.DEBUG) {
				val xaddress = Long.toHexString(physicalAddress == null ? 0L : physicalAddress);
				log.debug("Setting physical memory address 0x{}", xaddress);
			}
			this.physicalAddress = physicalAddress;
			return this;
		}

		/**
		 * Sets the size of the builder.
		 *
		 * @param size The size.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public @NotNull Builder size(@Nullable Integer size) {
			if (BuildConfig.DEBUG) log.debug("Setting size {}", size);
			packetSize = size;
			return this;
		}

		/**
		 * Sets the memory pool identifier of the builder.
		 *
		 * @param pool The memory pool identifier.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public Builder pool(@Nullable Integer pool) {
			if (BuildConfig.DEBUG) log.debug("Setting memory pool identifier {}", pool);
			memoryPool = pool;
			return this;
		}

		/**
		 * Sets the memory pool identifier of the builder.
		 *
		 * @param pool The memory pool identifier.
		 * @return This builder.
		 */
		@Contract("_ -> !null")
		public Builder pool(@Nullable IxyMempool pool) {
			return pool(pool == null ? null : pool.getId());
		}

		/**
		 * Builds a new packet buffer using the properties of the builder.
		 *
		 * @return The packet buffer.
		 */
		@Contract(value = " -> new", pure = true)
		@SuppressFBWarnings("NP_NULL_PARAM_DEREF")
		public @NotNull PacketBuffer build() {
			return new PacketBuffer(mmanager, virtualAddress, physicalAddress, packetSize, memoryPool);
		}

		@Override
		@Contract(pure = true)
		public String toString() {
			var string = PacketBuffer.class.getSimpleName();
			string += ".";
			string += Builder.class.getSimpleName();
			string += "(manager=";
			string += mmanager;
			string += ", virtual=";
			string += virtualAddress;
			string += ", physical=";
			string += physicalAddress;
			string += ", size=";
			string += packetSize;
			string += ", pool=";
			string += memoryPool;
			string += ")";
			return string;
		}

	}

	/**
	 * Returns a new {@link Builder} instance.
	 *
	 * @return A builder.
	 */
	@Contract(value = " -> new", pure = true)
	public static Builder builder() {
		return new Builder();
	}

	/////////////////////////////////////////////////////// SIZES //////////////////////////////////////////////////////

	/** The size of the physical address pointer field. */
	private static final int PAP_SIZE = Long.SIZE;

	/** The size of the memory pool pointer field. */
	private static final int MPP_SIZE = Long.SIZE;

	/** The size of the memory pool index field. */
	private static final int MPI_SIZE = Integer.SIZE;

	/** The size of the packet size field. */
	private static final int PKT_SIZE = Integer.SIZE;

	/** The size of the packet buffer header. */
	private static final int HEADER_SIZE = 64 * Byte.SIZE;

	/////////////////////////////////////////////////////// BYTES //////////////////////////////////////////////////////

	/** The bytes of the physical address pointer field. */
	private static final int PAP_BYTES = PAP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool pointer field. */
	private static final int MPP_BYTES = MPP_SIZE / Byte.SIZE;

	/** The bytes of the memory pool index field. */
	private static final int MPI_BYTES = MPI_SIZE / Byte.SIZE;

	/** The bytes of the packet size field. */
	private static final int PKT_BYTES = PKT_SIZE / Byte.SIZE;

	/** The bytes of the packet buffer header. */
	public static final int HEADER_BYTES = HEADER_SIZE / Byte.SIZE;

	///////////////////////////////////////////////////// OFFSETS //////////////////////////////////////////////////////

	/** The offset of the header. */
	public static final int HEADER_OFFSET = 0;

	/** The offset of the physical address pointer field. */
	public static final int PAP_OFFSET = HEADER_OFFSET;

	/** The offset of the memory pool pointer field. */
	private static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The offset of the memory pool index field. */
	public static final int MPI_OFFSET = MPP_OFFSET + MPP_BYTES;

	/** The offset of the packet size field. */
	public static final int PKT_OFFSET = MPI_OFFSET + MPI_BYTES;

	/** The offset of the data of the buffer. */
	public static final int DATA_OFFSET = HEADER_OFFSET + HEADER_BYTES;

	////////////////////////////////////////////////// STATIC METHODS //////////////////////////////////////////////////

	/**
	 * Common checks performed by {@link #get(int, int, byte[])} and {@link #getVolatile(int, int, byte[])}.
	 * <p>
	 * If one the parameters is not formatted correctly, an {@link IllegalArgumentException} will be thrown.
	 *
	 * @param offset The offset to start copying from.
	 * @param bytes  The number of bytes to copy.
	 * @param buffer The buffer to copy the data to.
	 * @return Whether the operation should be stopped.
	 */
	@Contract(value = "_, _, null -> fail", pure = true)
	private static boolean check(int offset, int bytes, @Nullable byte[] buffer) {
		if (offset < 0) throw new InvalidOffsetException("offset");
		else if (bytes < 0) throw new InvalidSizeException("length");
		else if (buffer == null) throw new InvalidBufferException("buffer");
		return (bytes == 0 || buffer.length == 0);
	}

	///////////////////////////////////////////////// MEMBER VARIABLES /////////////////////////////////////////////////

	/** Holds a reference to a memory manager. */
	private final @NotNull IxyMemoryManager mmanager;

	////////////////////////////////////////////////// MEMBER METHODS //////////////////////////////////////////////////

	@Getter(onMethod_ = {@Contract(pure = true)})
	@ToString.Include(name = "virtual", rank = 1)
	@EqualsAndHashCode.Include
	private final long virtualAddress;

	@Contract("null, _, _, _, _ -> fail")
	private PacketBuffer(@NotNull IxyMemoryManager memoryManager, long virtualAddress, @Nullable Long physicalAddress, @Nullable Integer size, @Nullable Integer pool) {
		if (BuildConfig.DEBUG)
			log.trace("Instantiating packet buffer with address 0x{}", Long.toHexString(virtualAddress));
		if (!BuildConfig.OPTIMIZED) {
			if (memoryManager == null) throw new InvalidNullParameterException("mmanager");
			if (virtualAddress == 0) throw new InvalidMemoryAddressException("address");
		}
		mmanager = memoryManager;
		this.virtualAddress = virtualAddress;
		if (physicalAddress != null) setPhysicalAddress(physicalAddress);
		if (size != null) setSize(size);
		if (pool != null) setMemoryPoolId(pool);
	}

	/**
	 * Sets the physical memory address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of physical memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @param physicalAddress The physical memory address.
	 */
	private void setPhysicalAddress(long physicalAddress) {
		if (BuildConfig.DEBUG) log.trace("Writing physical address pointer field");
		mmanager.putLong(virtualAddress + PAP_OFFSET, physicalAddress);
	}

	/**
	 * Sets the memory pool identifier of the underlying packet buffer.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param memoryPoolId The memory pool identifier.
	 */
	private void setMemoryPoolId(int memoryPoolId) {
		if (BuildConfig.DEBUG) log.trace("Writing memory pool index field");
		mmanager.putInt(virtualAddress + PAP_OFFSET, memoryPoolId);
	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	@Contract(pure = true)
	public long getPhysicalAddress() {
		if (BuildConfig.DEBUG) log.trace("Reading physical address pointer field");
		return mmanager.getLong(virtualAddress + PAP_OFFSET);
	}

	@Override
	@Contract(pure = true)
	public int getMemoryPoolId() {
		if (BuildConfig.DEBUG) log.trace("Reading memory pool index field");
		return mmanager.getInt(virtualAddress + MPI_OFFSET);
	}

	@Override
	@Contract(pure = true)
	public int getSize() {
		if (BuildConfig.DEBUG) log.trace("Reading packet size field");
		return mmanager.getInt(virtualAddress + PKT_OFFSET);
	}

	@Override
	@Contract(pure = true)
	public void setSize(int size) {
		val address = virtualAddress + PKT_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Writing packet size field @ 0x{} with value {}", xaddress, size);
		}
		mmanager.putInt(address, size);
	}

	@Override
	@Contract(pure = true)
	public byte getByte(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data byte @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getByte(address + offset);
	}

	@Override
	@Contract(pure = true)
	public byte getByteVolatile(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data byte @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getByteVolatile(address + offset);
	}

	@Override
	@Contract(pure = true)
	public void putByte(int offset, byte value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data byte 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putByte(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public void putByteVolatile(int offset, byte value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Byte.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data byte 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putByteVolatile(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public short getShort(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data short @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getShort(address + offset);
	}

	@Override
	@Contract(pure = true)
	public short getShortVolatile(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data short @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getShortVolatile(address + offset);
	}

	@Override
	@Contract(pure = true)
	public void putShort(int offset, short value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data short 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putShort(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public void putShortVolatile(int offset, short value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(Short.toUnsignedInt(value));
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data short 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putShortVolatile(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public int getInt(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data int @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getInt(address + offset);
	}

	@Override
	@Contract(pure = true)
	public int getIntVolatile(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data int @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getIntVolatile(address + offset);
	}

	@Override
	@Contract(pure = true)
	public void putInt(int offset, int value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data int 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putInt(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public void putIntVolatile(int offset, int value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Integer.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data int 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putIntVolatile(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public long getLong(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading data long @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getLong(address + offset);
	}

	@Override
	@Contract(pure = true)
	public long getLongVolatile(int offset) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Reading volatile data long @ 0x{} with offset 0x{}", xaddress, xoffset);
		}
		return mmanager.getLongVolatile(address + offset);
	}

	@Override
	@Contract(pure = true)
	public void putLong(int offset, long value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing data long 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putLong(address + offset, value);
	}

	@Override
	@Contract(pure = true)
	public void putLongVolatile(int offset, long value) {
		val address = virtualAddress + DATA_OFFSET;
		if (BuildConfig.DEBUG) {
			val xvalue = Long.toHexString(value);
			val xaddress = Long.toHexString(address);
			val xoffset = Integer.toHexString(offset);
			log.trace("Writing volatile data long 0x{} @ 0x{} with offset 0x{}", xvalue, xaddress, xoffset);
		}
		mmanager.putLongVolatile(address + offset, value);
	}

	@Override
	@Contract(value = "_, _, null -> fail", mutates = "param3")
	public void get(int offset, int bytes, @NotNull byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Reading {} bytes starting from offset 0x{}", bytes, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (check(offset, bytes, buffer)) return;
			bytes = Math.min(bytes, buffer.length);
		}
		mmanager.get(virtualAddress + offset, bytes, buffer, 0);
	}

	@Override
	@Contract(value = "_, _, null -> fail", mutates = "param3")
	public void getVolatile(int offset, int bytes, @NotNull byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Reading {} volatile bytes starting from offset 0x{}", bytes, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (check(offset, bytes, buffer)) return;
			bytes = Math.min(bytes, buffer.length);
		}
		mmanager.getVolatile(virtualAddress + offset, bytes, buffer, 0);
	}

	@Override
	@Contract(value = "_, _, null -> fail", pure = true)
	public void put(int offset, int bytes, @NotNull byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Writing {} bytes starting from offset 0x{}", bytes, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (check(offset, bytes, buffer)) return;
			bytes = Math.min(bytes, buffer.length);
		}
		mmanager.put(virtualAddress + offset, bytes, buffer, 0);
	}

	@Override
	@Contract(value = "_, _, null -> fail", pure = true)
	public void putVolatile(int offset, int bytes, @NotNull byte[] buffer) {
		if (BuildConfig.DEBUG) {
			val xoffset = Integer.toHexString(offset);
			log.debug("Writing {} volatile bytes starting from offset 0x{}", bytes, xoffset);
		}
		if (!BuildConfig.OPTIMIZED) {
			if (check(offset, bytes, buffer)) return;
			bytes = Math.min(bytes, buffer.length);
		}
		mmanager.putVolatile(virtualAddress + offset, bytes, buffer, 0);
	}

	@Override
	@Contract(value = "null -> fail", pure = true)
	public int compareTo(@NotNull IxyPacketBuffer o) {
		if (BuildConfig.DEBUG) log.debug("Comparing with another packet");
		if (!BuildConfig.OPTIMIZED && o == null) throw new InvalidNullParameterException("o");
		return Long.compare(virtualAddress, o.getVirtualAddress());
	}

}
