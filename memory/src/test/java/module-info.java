// Define the testing module
module ixy.memory.test {
	requires ixy.memory;               // Ixy's Memory module
	requires lombok;                   // Lombok annotations
	requires annotations;              // JetBrains annotations
	requires org.junit.jupiter.api;    // JUnit 5 annotations API
	requires org.junit.jupiter.params; // JUnit 5 parameterized test API
}
