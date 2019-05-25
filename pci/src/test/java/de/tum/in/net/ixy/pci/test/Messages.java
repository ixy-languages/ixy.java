package de.tum.in.net.ixy.pci.test;

/**
 * This class contains all the messages used by the assertions of {@link IxgbePciTest} and {@link VirtioPciTest}.
 * <p>
 * No documentation is provided for these messages since they are supposed to be human readable and self explaining.
 *
 * @author Esaú García Sánchez-Torija
 * @see IxgbePciTest
 * @see VirtioPciTest
 */
final class Messages {

	// Messages related to the vendor id
	static final String MSG_VENDOR_METHOD     = "vendor id retrieval should throw an exception";
	static final String MSG_VENDOR_METHOD_NOT = "vendor id retrieval should NOT throw an exception";
	static final String MSG_VENDOR_VALUE      = "the vendor id should be correct";

	// Messages related to the device id
	static final String MSG_DEVICE_METHOD     = "device id retrieval should throw an exception";
	static final String MSG_DEVICE_METHOD_NOT = "device id retrieval should NOT throw an exception";
	static final String MSG_DEVICE_VALUE      = "the device id should be correct";

	// Messages related to the class id
	static final String MSG_CLASS_METHOD     = "class id retrieval should throw an exception";
	static final String MSG_CLASS_METHOD_NOT = "class id retrieval should NOT throw an exception";
	static final String MSG_CLASS_VALUE      = "the class id should be correct";

	// Messages related to the resource "bind"
	static final String MSG_BIND_METHOD     = "driver binding should throw an exception";
	static final String MSG_BIND_METHOD_NOT = "driver binding should NOT throw an exception";
	static final String MSG_BIND_CAUSE      = "the cause of the exception should be that the device is not found";

	// Messages related to the resource "unbind"
	static final String MSG_UNBIND_METHOD     = "the driver unbinding should throw an exception";
	static final String MSG_UNBIND_METHOD_NOT = "the driver unbinding should NOT throw an exception";

	// Messages related to the DMA status
	static final String MSG_DMA_ENABLE_METHOD      = "DMA enabling should throw an exception";
	static final String MSG_DMA_ENABLE_METHOD_NOT  = "DMA enabling should NOT throw an exception";
	static final String MSG_DMA_STATUS_METHOD      = "DMA status checking should throw an exception";
	static final String MSG_DMA_STATUS_METHOD_NOT  = "DMA status checking should NOT throw an exception";
	static final String MSG_DMA_STATUS_VALUE_0     = "the DMA status should be disabled";
	static final String MSG_DMA_STATUS_VALUE_1     = "the DMA status should be enabled";
	static final String MSG_DMA_DISABLE_METHOD     = "DMA disabling should throw an exception";
	static final String MSG_DMA_DISABLE_METHOD_NOT = "DMA disabling should NOT throw an exception";

	// Messages related to the map method
	static final String MSG_MAPABLE_METHOD     = "checking memory mapability should throw an exception";
	static final String MSG_MAPABLE_METHOD_NOT = "checking memory mapability should NOT throw an exception";

	// Messages related to the map method
	static final String MSG_MAP_METHOD     = "memory mapping the resource 'resource0' should throw an exception";
	static final String MSG_MAP_METHOD_NOT = "memory mapping the resource 'resource0' should NOT throw an exception";
	static final String MSG_MAP_NULL       = "the memory mapping should be null";
	static final String MSG_MAP_NOT_NULL   = "the memory mapping should NOT be null";

	// Messages related to the constructor
	static final String MSG_CONSTRUCTOR_METHOD     = "instance construction should throw an exception";
//	static final String MSG_CONSTRUCTOR_METHOD_NOT = "instance construction should NOT throw an exception";

	// Messages related to the close method
	static final String MSG_CLOSE_METHOD_NOT = "releasing the resources should NOT throw an exception";

}
