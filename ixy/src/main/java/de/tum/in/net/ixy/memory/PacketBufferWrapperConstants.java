package de.tum.in.net.ixy.memory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The interface that contains all the offsets and sizes of the {@link PacketBufferWrapper} fields.
 * The memory layout is depicted below:
 * <pre>
 *                  64 bits
 * /---------------------------------------\
 * |       Physical Address Pointer        |
 * |---------------------------------------|
 * |         Memory Pool "Pointer"         |
 * |---------------------------------------|
 * |     Reserved      |    Packet Size    | 64 bytes
 * |---------------------------------------|
 * |          Headroom (variable)          |
 * \---------------------------------------/
 * </pre>
 *
 * @author Esaú García Sánchez-Torija
 */
@SuppressWarnings("WeakerAccess")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketBufferWrapperConstants {

	/////////////////////////////////////////////////////// SIZES //////////////////////////////////////////////////////

	/** The size in bits of the physical address pointer field. */
	public static final int PAP_SIZE = Long.SIZE;

	/** The size in bits of the memory pool pointer field. */
	public static final int MPP_SIZE = Long.SIZE;

	/** The size in bits of the packet size field. */
	public static final int PKT_SIZE = Integer.SIZE;

	/** The size in bits of the packet buffer header. */
	public static final int HEADER_SIZE = 64 * Byte.SIZE;

	/////////////////////////////////////////////////////// BYTES //////////////////////////////////////////////////////

	/** The size in bytes of the physical address pointer field. */
	public static final int PAP_BYTES = PAP_SIZE / Byte.SIZE;

	/** The size in bytes of the memory pool pointer field. */
	public static final int MPP_BYTES = MPP_SIZE / Byte.SIZE;

	/** The size in bytes of the packet size field. */
	public static final int PKT_BYTES = PKT_SIZE / Byte.SIZE;

	/** The size in bytes of the packet buffer header. */
	public static final int HEADER_BYTES = HEADER_SIZE / Byte.SIZE;

	///////////////////////////////////////////////////// OFFSETS //////////////////////////////////////////////////////

	/** The offset of the header. */
	public static final int HEADER_OFFSET = 0;

	/** The offset of the physical address pointer field. */
	public static final int PAP_OFFSET = HEADER_OFFSET;

	/** The offset of the memory pool pointer field. */
	public static final int MPP_OFFSET = PAP_OFFSET + PAP_BYTES;

	/** The offset of the packet size field. */
	public static final int PKT_OFFSET = MPP_OFFSET + Integer.BYTES + MPP_BYTES;

	/** The offset of the payload of the buffer. */
	public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_BYTES;

}
