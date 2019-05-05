// Define the module name
module ixy.pci {
	requires lombok;                // Lombok annotations
	requires annotations;           // JetBrains annotations
	requires org.slf4j;             // Simple Logging Facade for Java
	exports  de.tum.in.net.ixy.pci; // Export the PCI module
}
