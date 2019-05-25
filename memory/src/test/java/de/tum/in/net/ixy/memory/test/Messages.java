package de.tum.in.net.ixy.memory.test;

/**
 * This class contains all the messages used by the assertions of {@link MemoryTest}.
 * <p>
 * No documentation is provided for these messages since they are supposed to be human readable and self explaining.
 */
final class Messages {

	// Messages related to addresses or sizes
	static final String MSG_NULL         = "the address should be 0";
	static final String MSG_NOT_NULL     = "the address should NOT be 0";
	static final String MSG_POWER_OF_TWO = "should be a power of 2";
	static final String MSG_ALIGNED      = "the address should be aligned";

	// Messages related to JNI and Unsafe comparisons
	static final String MSG_SAME_JNI     = "JNI-based value should be the same";
	static final String MSG_SAME_UNSAFE  = "Unsafe-based value should be the same";
	static final String MSG_SAME_SMART   = "Smart-based value should be the same";
	static final String MSG_UNSAFE_THROW = "the Unsafe-based implementation should throw";

	// Messages related to read/write operations on arbitrary memory addresses
	static final String MSG_CORRECT      = "the value should be the same that was written";
	static final String MSG_ADDRESS_FMTR = "the address 0x%x should be %s-readable";
	static final String MSG_ADDRESS_FMTW = "the address 0x%x should be %s-writable";

	// Messages related to packet buffers
	static final String MSG_VALID       = "the packet buffer should be valid";
	static final String MSG_VALID_NOT   = "the packet buffer should NOT be valid";
	static final String MSG_INVALID     = "the packet buffer should be invalid";
	static final String MSG_INVALID_NOT = "the packet buffer should NOT be invalid";

	// Messages related to memory pools
	static final String MSG_UNIQUE    = "the memory pool should be unique";
	static final String MSG_ADDED     = "the memory pool should be added correctly";
	static final String MSG_ADDED_NOT = "the memory pool should NOT be added correctly";

	// Messages related to sizes
	static final String MSG_SIZE_MORE  = "the size should have incremented";
	static final String MSG_SIZE_LESS  = "the size should have decremented";
	static final String MSG_SIZE_0     = "the size should be zero";
	static final String MSG_SIZE_NOT_0 = "the size should NOT be zero";
}
