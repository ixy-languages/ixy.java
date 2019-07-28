/**
 * Defines a Java 9 compatible module.
 *
 * @author Esaú García Sánchez-Torija
 */
module ixy.pktfwd {
	requires lombok;                    // Lombok library
	requires org.slf4j;                 // Simple Logging Facade 4 Java library
	requires org.jetbrains.annotations; // JetBrains annotations
	requires ixy.library;               // The Ixy library
}
