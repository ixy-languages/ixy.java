package de.tum.in.net.ixy.generic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ixy's packet specification.
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings({"HardCodedStringLiteral", "DuplicateStringLiteralInspection"})
public interface IxyPacketBuffer {

	/**
	 * Packet builder pattern.
	 * <p>
	 * This pattern is very useful when several instances need to be created with similar properties.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
	@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
	abstract class Builder {

		/**
		 * The virtual address of the packet.
		 * ----------------- GETTER -----------------
		 * Returns the virtual address of the packet.
		 *
		 * @return The virtual address of the packet.
		 * ----------------- SETTER -----------------
		 * Sets the virtual address of the packet.
		 *
		 * @param virtualAddress The virtual address of the packet.
		 */
		@Setter
		@SuppressWarnings("JavaDoc")
		@Getter(value = AccessLevel.PROTECTED, onMethod_ = {@Contract(pure = true)})
		private long virtualAddress;

		/**
		 * The physical address of the packet.
		 * ------------------ GETTER ------------------
		 * Returns the physical address of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @return The physical address of the packet.
		 * ---------------- SETTER ----------------
		 * Sets the physical address of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @param virtualAddress The physical address of the packet.
		 */
		@Setter
		@SuppressWarnings("JavaDoc")
		@Getter(value = AccessLevel.PROTECTED, onMethod_ = {@Contract(pure = true)})
		private @Nullable Long physicalAddress;

		/**
		 * The memory pool id of the packet.
		 * ------------------ GETTER ------------------
		 * Returns the memory pool id of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @return The memory pool id of the packet.
		 * ---------------- SETTER ----------------
		 * Sets the memory pool id of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @param memoryPoolId The memory pool id of the packet.
		 */
		@Setter
		@SuppressWarnings("JavaDoc")
		@Getter(value = AccessLevel.PROTECTED, onMethod_ = {@Contract(pure = true)})
		private @Nullable Integer memoryPoolId;

		/**
		 * The size of the packet.
		 * ------------------ GETTER ------------------
		 * Returns the size of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @return The size of the packet.
		 * ---------------- SETTER ----------------
		 * Sets the size of the packet.
		 * <p>
		 * The value {@code null} is used to prevent performing memory access when building the packet.
		 *
		 * @param size The size of the packet.
		 */
		@Setter
		@SuppressWarnings("JavaDoc")
		@Getter(value = AccessLevel.PROTECTED, onMethod_ = {@Contract(pure = true)})
		private @Nullable Integer size;

		/**
		 * Builds the packet with the properties of the builder.
		 * <p>
		 * If a property is {@code null}, it is recommended to not set it, as it may have been set on purpose to prevent
		 * memory access.
		 *
		 * @return The new packet.
		 */
		@Contract(value = " -> new", pure = true)
		public abstract @NotNull IxyPacketBuffer build();

	}

	/**
	 * Returns the virtual address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of virtual memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The virtual address.
	 */
	@Contract(pure = true)
	long getVirtualAddress();

	/**
	 * Returns the physical memory address in which this packet buffer is allocated.
	 * <p>
	 * The packet should be allocated in a contiguous region of physical memory, and the address should be the first
	 * writable byte of the packet.
	 *
	 * @return The physical memory address.
	 */
	@Contract(pure = true)
	long getPhysicalAddress();

	/**
	 * Reads the memory pool id of the underlying packet buffer.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @return The memory pool id.
	 */
	@Contract(pure = true)
	int getMemoryPoolId();

	/**
	 * Returns the size of the packet.
	 *
	 * @return The size of the packet.
	 */
	@Contract(pure = true)
	int getSize();

	/**
	 * Sets the size of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param size The size of the packet.
	 */
	@Contract(pure = true)
	void setSize(int size);

	/**
	 * Reads a {@code byte} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByte(int offset);

	/**
	 * Reads a {@code byte} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code byte}.
	 */
	@Contract(pure = true)
	byte getByteVolatile(int offset);
	
	/**
	 * Writes a {@code byte} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByte(int offset, byte value);

	/**
	 * Writes a {@code byte} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code byte} to write.
	 */
	@Contract(pure = true)
	void putByteVolatile(int offset, byte value);

	/**
	 * Reads a {@code short} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShort(int offset);

	/**
	 * Reads a {@code short} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code short}.
	 */
	@Contract(pure = true)
	short getShortVolatile(int offset);

	/**
	 * Writes a {@code short} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShort(int offset, short value);

	/**
	 * Writes a {@code short} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code short} to write.
	 */
	@Contract(pure = true)
	void putShortVolatile(int offset, short value);
	
	/**
	 * Reads a {@code int} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getInt(int offset);

	/**
	 * Reads a {@code int} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code int}.
	 */
	@Contract(pure = true)
	int getIntVolatile(int offset);

	/**
	 * Writes a {@code int} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	@Contract(pure = true)
	void putInt(int offset, int value);

	/**
	 * Writes a {@code int} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code int} to write.
	 */
	@Contract(pure = true)
	void putIntVolatile(int offset, int value);

	/**
	 * Reads a {@code long} from the packet data.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLong(int offset);

	/**
	 * Reads a {@code long} from the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start reading from.
	 * @return The read {@code long}.
	 */
	@Contract(pure = true)
	long getLongVolatile(int offset);

	/**
	 * Writes a {@code long} to the packet data.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLong(int offset, long value);

	/**
	 * Writes a {@code long} to the packet data using volatile memory addresses.
	 *
	 * @param offset The offset to start writing to.
	 * @param value  The {@code long} to write.
	 */
	@Contract(pure = true)
	void putLongVolatile(int offset, long value);

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(value = "_, _, null -> fail", mutates = "param3")
	void get(int offset, int length, @NotNull byte[] buffer);

	/**
	 * Copies a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @return A copy the packet segment.
	 */
	@Contract(pure = true)
	default @NotNull byte[] get(int offset, int length) {
		val buffer = new byte[length];
		get(offset, length, buffer);
		return buffer;
	}

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(value = "_, _, null -> fail", mutates = "param3")
	void getVolatile(int offset, int length, @NotNull byte[] buffer);

	/**
	 * Copies a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @return A copy the packet segment.
	 */
	@Contract(pure = true)
	default @NotNull byte[] getVolatile(int offset, int length) {
		val buffer = new byte[length];
		getVolatile(offset, length, buffer);
		return buffer;
	}

	/**
	 * Writes a segment of the packet.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(value = "_, _, null -> fail", pure = true)
	void put(int offset, int length, @NotNull byte[] buffer);

	/**
	 * Writes a segment of the packet using volatile memory addresses.
	 * <p>
	 * This method is necessary for packet generation applications.
	 *
	 * @param offset The offset to start copying from.
	 * @param length The amount of data to copy.
	 * @param buffer The buffer to copy the data to.
	 */
	@Contract(value = "_, _, null -> fail", pure = true)
	void putVolatile(int offset, int length, @NotNull byte[] buffer);

}
