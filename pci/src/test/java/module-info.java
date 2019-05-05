// Define the testing module
module ixy.pci.test {
	requires ixy.pci;                  // Ixy's PCI  module
	requires lombok;                   // Lombok annotations
	requires annotations;              // JetBrains annotations
	requires org.junit.jupiter.api;    // JUnit 5 annotations API
	requires org.junit.jupiter.params; // JUnit 5 parameterized test API
	requires org.hamcrest;             // Hamcrest matchers
}
