/**
 * Defines a Java 9 compatible module.
 *
 * @author Esaú García Sánchez-Torija
 */
module ixy.library {
	requires jdk.unsupported;                 // Brings access to sun.misc.Unsafe, amongst others
	requires lombok;                          // Lombok library
	requires com.github.spotbugs.annotations; // FindBugs annotations, provided by SpotBugs, needed by Lombok
	requires org.slf4j;                       // Simple Logging Facade 4 Java library
	requires org.jetbrains.annotations;       // JetBrains annotations

	exports de.tum.in.net.ixy;
	exports de.tum.in.net.ixy.memory;
	exports de.tum.in.net.ixy.ixgbe;
	exports de.tum.in.net.ixy.utils;
}
