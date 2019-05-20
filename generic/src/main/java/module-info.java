// Define the module name
module ixy.generic {
	requires ixy.pci;                   // Ixy's PCI module
	requires lombok;                    // Lombok annotations
	requires annotations;               // JetBrains annotations
	requires org.slf4j;                 // Simple Logging Facade for Java
	exports  de.tum.in.net.ixy.generic; // Export the memory package
}
