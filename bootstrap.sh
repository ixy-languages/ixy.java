#!/usr/bin/env sh

## This script uses the following programs:
##  * test
##  * apt-get
##  * cut
##  * echo
##  * grep
##  * head
##  * id
##  * mkdir
##  * move
##  * mv
##  * rm
##  * sh
##  * tar
##  * tar
##  * wget

# Variables that shape the behaviour of this script
JDK_VERSION='13'
JDK_MINOR=33

# Make sure that we are root or we won't able to write into the sysfs mount
if [ $(id -u) -ne 0 ]; then
    echo 'Please run this script as root or using sudo'
    exit 1
fi

## The main code of the script hoisted here
main() {
	install_openjdk ${JDK_VERSION}
	install_dependencies
	echo "Run this to put JDK ${JDK_VERSION} on your path:"
	echo "source /etc/profile.d/jdk.sh"
}

## Downloads an OpenJDK release given its version number
download_openjdk() {
	JDK_TAR="openjdk-${1}_linux-x64_bin.tar.gz"
	echo "Downloading OpenJDK ${1}. This could take some time..."
	wget -q https://github.com/AdoptOpenJDK/openjdk13-binaries/releases/download/jdk-${JDK_VERSION}%2B${JDK_MINOR}/OpenJDK13U-jdk_x64_linux_hotspot_${JDK_VERSION}_${JDK_MINOR}.tar.gz -O ${JDK_TAR}
}


## Creates the profile scripts and moves the contents of an already extracted OpenJDK to the standard installation path
configure_openjdk() {
	JDK_TAR="openjdk-${1}_linux-x64_bin.tar.gz"
	echo 'Extracting archive...'
	tar -zxf ${JDK_TAR}
	rm ${JDK_TAR}
	# Move the OpenJDK installation to the correct path and create a script to set
	# the JAVA_HOME environment variable and fix the PATH environment variable too
	echo 'Moving the installation path and creating profile scripts...'
	mv jdk-${1}* /usr/local/jdk-${1}
	echo '#!/usr/bin/env sh'                    >  /etc/profile.d/jdk.sh
	echo "export JAVA_HOME=/usr/local/jdk-${1}" >> /etc/profile.d/jdk.sh
	echo "export JDK_HOME=/usr/local/jdk-${1}"  >> /etc/profile.d/jdk.sh
	echo 'PATH=$JAVA_HOME/bin:$PATH'            >> /etc/profile.d/jdk.sh
}

## Installs the OpenJDK if necessary
install_openjdk() {
	if command -v java > /dev/null; then
		if java -version 2>&1 | grep ${JDK_VERSION}; then
			echo "OpenJDK ${JDK_VERSION} is already installed, skipping installation"
			return
		fi
	fi
	download_openjdk ${1}
	configure_openjdk ${1}
}

## Installs the given dependency and logs it in a specific format
install_dependency() {
	echo -n " * ${1} "
	DEBIAN_FRONTEND=noninteractive apt-get install --yes ${1} > /dev/null
	echo '✓'
}

## Installs the rest of dependencies needed by other scripts or the project itself
install_dependencies() {
	echo 'Installing the dependencies...'
	install_dependency 'ethtool'
	install_dependency 'gcc'
	install_dependency 'g++'
	install_dependency 'git'
	install_dependency 'numactl'
	install_dependency 'grep'
	install_dependency 'sed'
	install_dependency 'gawk'
	install_dependency 'wget'
}

# Execute everything
main
