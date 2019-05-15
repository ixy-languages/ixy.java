// Define the module name
module ixy.memory {
	requires lombok;                   // Lombok annotations
	requires annotations;              // JetBrains annotations
	requires org.slf4j;                // Simple Logging Facade for Java
	exports  de.tum.in.net.ixy.memory; // Export the memory package
}
