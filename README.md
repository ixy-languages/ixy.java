# Ixy.java

ixy.java is a Java port of the [ixy](https://github.com/emmericp/ixy) user space network device driver.
It has been written with a special focus on readability.
It supports Intel 82599 10GbE NICs, also known as `ixgbe`.

## Dependencies and configuration

To build this project you will need a C compiler and the JDK 12.
A handful script will install all the dependencies needed to build the project and the dependencies needed by other bash scripts.

```bash
./boothstrap.sh
source /etc/profile.d/jdk.sh
```

By default, the driver is built in release mode (a.k.a. `OPTIMIZED`), with logging optimization (only messages of priority `INFO` or more are compiled), with the `Unsafe` object as memory manager and with the path `/mnt/huge` as the `hugetlbfs` mount point.

To disable the optimizations, edit the property `MEMORY_MANAGER` of the root Gradle build script.
All the possible values are `true` or `false`.
Hare you can see a slice of the file (line 44).
```groovy
// ...
ext {
	// ...
	OPTIMIZED = true
	// ...
}
// ...
```

To use a different logging level optimization, edit the property `DEBUG` of the root Gradle build script.
All the possible values are right above the property.
Hare you can see a slice of the file (lines 31-37).
```groovy
// ...
ext {
	LOG_NONE  = 0
	LOG_ERROR = 1
	LOG_WARN  = 2
	LOG_INFO  = 3
	LOG_DEBUG = 4
	LOG_TRACE = 5
	DEBUG     = LOG_INFO
	// ...
}
// ...
```

To use a different memory manager, edit the property `MEMORY_MANAGER` of the root Gradle build script.
All the possible values are right above the property.
Hare you can see a slice of the file (lines 39-42).
```groovy
// ...
ext {
	// ...
	PREFER_UNSAFE   = 1
	PREFER_JNI      = 2
	PREFER_JNI_FULL = 3
	MEMORY_MANAGER  = PREFER_UNSAFE
	// ...
}
// ...
```

To use a different memory manager, edit the property `DEFAULT_HUGEPAGE_PATH` of the root Gradle build script.
Hare you can see a slice of the file (line 46).
```groovy
// ...
ext {
	// ...
	DEFAULT_HUGEPAGE_PATH = "/mnt/huge"
	// ...
}
// ...
```

To configure the Logback logging behaviour, edit the file `logback.xml` found on the root of the project.

## Build instructions

After the build script has been configured, the project can be built with:
```bash
gradlew installDist
```

Before running it, in case you haven't modified the default `hugetlbfs` mount path, you can use a handful script to reserve 512 hugepages.
```bash
./hugetlbfs.sh
```

## Usage

To ease the execution of the packet generator and forwarder, a helper script can be evaluated to have all the NICs associated to environment variables.
```bash
./pci2nic.sh           # To see the variables that will be created
eval '$(./pci2nic.sh)' # To evaluate the export statements
```

This creates environment variables of the form `IXY_*_ADDR_*` and `IXY_*_NAME_*` to associated interface names with their fully qualified PCI addresses.
The first `*` is the driver type (`IXGBE` or `VIRTIO`) and the second is the index (starting from 1).
To use it in your own scripts you can loop as many times as `IXY_*_COUNT` says and read the aforementioned environment variables.

### Packet generator

To run the packet generator:
```bash
sudo ./ixy-pktgen [--batch-size N] XXXX:XX:XX.X
```

Remember to use the fully qualified PCI address of the NIC; do not omit the prefix.

### Packet forwarder

To run the packet forwarder:
```bash
sudo ./ixy-pktfwd [--batch-size N] XXXX:XX:XX.X YYYY:YY:YY.Y
```

Remember to use the fully qualified PCI address of the NIC; do not omit the prefix.

## Project structure

The packet generator and forwarder demos are located in their own respective Gradle subprojects, namely `pktgen` and `pktfwd`.
The subdirectory `ixy` contains a Gradle subproject with the driver implementation, split in several Java packages.
- `de.tum.in.net.ixy`: contains a simple class to track the statistics of a NIC (`Stats`) and the base class used to interact with NICs and write custom drivers (`Device`).
- `de.tum.in.net.ixy.memory`: contains the `MemoryManager` specification (to standardise memory access), the `PacketbufferWrapper` implementation and packet pool implementation, named `Mempool`.
- `de.tum.in.net.ixy.utils`: contains static classes to pretty-print numbers, addresses, etc. (`Strings`), a `JNI` library loader (`Native`) and a simple wrapper of the Java class `Thread` to sleep without having to catch the annoying `InterruptedException` (`Threads`).
- `de.tum.in.net.ixy.ixgbe`: contains the implementation of the ixy driver for the Intel 82599 NIC.

## Benchmarking

I recommend [MoonGen](https://github.com/emmericp/MoonGen) and [benchmark-scripts](https://github.com/ixy-languages/benchmark-scripts) to benchmark the performance of this project.
In case **MoonGen** does not compile due to compiler warnings being treated as errors, execute the following script:
```bash
./moongen.sh
```

It will download the latest commit of **MoonGen**, remove the compiler flags that might be causing the issue, compile it and clone the **benchmark-scripts** project along with **ixy** (and compile it).

## License

ixy.java is licensed under the GPLv2 license.

## Disclaimer

**ixy.java is nowhere close to a production-ready driver! Do not use it in critical environments or on any systems with data you don't want to lose!**

## Other languages

Check out the other [ixy implementations](https://github.com/ixy-languages).
