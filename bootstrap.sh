#!/usr/bin/env bash

# Download the latest release (OpenJDK 12.0.1)
wget -q https://download.java.net/java/GA/jdk12.0.1/69cfe15208a647278a19ef0990eea691/12/GPL/openjdk-12.0.1_linux-x64_bin.tar.gz        -O openjdk-12.0.1_linux-x64_bin.tar.gz
wget -q https://download.java.net/java/GA/jdk12.0.1/69cfe15208a647278a19ef0990eea691/12/GPL/openjdk-12.0.1_linux-x64_bin.tar.gz.sha256 -O openjdk-12.0.1_linux-x64_bin.tar.gz.sha256

# Append the name of the file to the hash file so that we can test it with sha256sum
echo '  openjdk-12.0.1_linux-x64_bin.tar.gz' >> openjdk-12.0.1_linux-x64_bin.tar.gz.sha256

# Check the integrity of the downloaded file
if ! sha256sum --check --strict openjdk-12.0.1_linux-x64_bin.tar.gz.sha256; then
    echo 'The downloaded OpenJDK release is not valid because the SHA-256 integrity check failed'
    exit 1
fi

# Extract the archive and remove it (along with the checksum file)
tar -zxf openjdk-12.0.1_linux-x64_bin.tar.gz
rm openjdk-12.0.1_linux-x64_bin.tar.gz
rm openjdk-12.0.1_linux-x64_bin.tar.gz.sha256

# Move the OpenJDK installation to the correct path and create a script to set
# the JAVA_HOME environment variable and fix the PATH environment variable
mv jdk-12.0.1 /usr/local
echo 'export JAVA_HOME=/usr/local/jdk-12.0.1' >> /etc/profile.d/jdk.sh
echo 'PATH=$PATH:$JAVA_HOME/bin'              >> /etc/profile.d/jdk.sh

# Update all the sources
DEBIAN_FRONTEND=noninteractive apt-get update

# Upgrade all the packages
DEBIAN_FRONTEND=noninteractive apt-get upgrade

# Install ethtool to get PCI bus of NICs
DEBIAN_FRONTEND=noninteractive apt-get install ethtool
