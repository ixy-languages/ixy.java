package de.tum.in.net.ixy.pci.test;

/**
 * This class contains all the messages used by the assertions of {@link PciIxgbeTest} and {@link PciVirtioTest}.
 * 
 * @author Esaú García Sánchez-Torija
 */
final class Messages {

	static final String MSG_VENDOR_METHOD          = "vendor id retrieval should fail";
	static final String MSG_VENDOR_METHOD_NOT      = "vendor id retrieval should not fail";
	static final String MSG_VENDOR_VALUE           = "vendor id should be correct";
	static final String MSG_DEVICE_METHOD          = "device id retrieval should fail";
	static final String MSG_DEVICE_METHOD_NOT      = "device id retrieval should not fail";
	static final String MSG_DEVICE_VALUE           = "device id should be correct";
	static final String MSG_CLASS_METHOD           = "class id retrieval should fail";
	static final String MSG_CLASS_METHOD_NOT       = "class id retrieval should not fail";
	static final String MSG_CLASS_VALUE            = "class id should be correct";
	static final String MSG_BIND_METHOD            = "driver binding should fail";
	static final String MSG_BIND_METHOD_NOT        = "driver binding should not fail";
	static final String MSG_BIND_EXCEPTION         = "reason should be that the device is not found";
	static final String MSG_UNBIND_METHOD          = "driver unbinding should fail";
	static final String MSG_UNBIND_METHOD_NOT      = "driver unbinding should not fail";
	static final String MSG_DMA_ENABLE_METHOD      = "DMA enabling should fail";
	static final String MSG_DMA_ENABLE_METHOD_NOT  = "DMA enabling should not fail";
	static final String MSG_DMA_STATUS_METHOD      = "DMA status checking should fail";
	static final String MSG_DMA_STATUS_METHOD_NOT  = "DMA status checking should not fail";
	static final String MSG_DMA_STATUS_VALUE_0     = "DMA should be disabled";
	static final String MSG_DMA_STATUS_VALUE_1     = "DMA should be enabled";
	static final String MSG_DMA_DISABLE_METHOD     = "DMA disabling should fail";
	static final String MSG_DMA_DISABLE_METHOD_NOT = "DMA disabling should not fail";
	static final String MSG_MAP_METHOD             = "resource0 mapping should fail";
	static final String MSG_MAP_METHOD_NOT         = "resource0 mapping should not fail";
	static final String MSG_CONSTRUCTOR_METHOD     = "Instantiation should fail";
	static final String MSG_CONSTRUCTOR_METHOD_NOT = "Instantiation should not fail";
	static final String MSG_CLOSE_METHOD_NOT       = "Releasing resources should not fail";

}
