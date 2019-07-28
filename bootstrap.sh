#!/usr/bin/env sh

## This script uses the following programs:
##  * [
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
##  * sha256sum
##  * tar
##  * tar
##  * wget

# Variables that shape the behaviour of this script
JDK_VERSION='12.0.1'
JDK_HASH='69cfe15208a647278a19ef0990eea691'

# Make sure that we are root or we won't able to write into the sysfs mount
if [ $(id -u) -ne 0 ]; then
    echo 'Please run this script as root or using sudo'
    exit 1
fi

## The main code of the script hoisted here
main() {
	install_openjdk ${JDK_VERSION} ${JDK_HASH}
	install_dependencies
}

## Downloads an OpenJDK release given its version number and the hash used by java.net to identify the release
download_openjdk() {
	JDK_VERSION_MAJOR=$(echo ${1} | cut -d'.' -f1)
	JDK_TAR="openjdk-${1}_linux-x64_bin.tar.gz"
	echo "Downloading OpenJDK ${1}. This could take some time..."
	wget -q https://download.java.net/java/GA/jdk${1}/${2}/${JDK_VERSION_MAJOR}/GPL/${JDK_TAR}        -O ${JDK_TAR}
	wget -q https://download.java.net/java/GA/jdk${1}/${2}/${JDK_VERSION_MAJOR}/GPL/${JDK_TAR}.sha256 -O ${JDK_TAR}.sha256
}

## Verifies the integrity of the OpenJDK release
extract_openjdk() {
	JDK_TAR="openjdk-${1}_linux-x64_bin.tar.gz"
	echo 'Formatting the integrity file to be ''sha256sum'' compliant...'
	cp ${JDK_TAR}.sha256 openjdk-${1}.sha256
	echo "  ${JDK_TAR}" >> openjdk-${1}.sha256

	echo 'Checking integrity of archive...'
	if ! sha256sum --check --strict openjdk-${1}.sha256 > /dev/null; then
		echo 'The downloaded OpenJDK release is not valid because the SHA-256 integrity check failed'
		return 1
	fi
	rm openjdk-${1}.sha256
	return 0
}

## Creates the profile scripts and moves the contents of an already extracted OpenJDK to the standard installation path
configure_openjdk() {
	JDK_TAR="openjdk-${1}_linux-x64_bin.tar.gz"
	echo 'Extracting archive...'
	tar -zxf ${JDK_TAR}
	rm ${JDK_TAR}
	rm ${JDK_TAR}.sha256
	# Move the OpenJDK installation to the correct path and create a script to set
	# the JAVA_HOME environment variable and fix the PATH environment variable too
	echo 'Moving the installation path and creating profile scripts...'
	cp -r jdk-${1} /usr/local
	rm -rf jdk-${1}
	echo '#!/usr/bin/env sh'                    >  /etc/profile.d/jdk.sh
	echo "export JAVA_HOME=/usr/local/jdk-${1}" >> /etc/profile.d/jdk.sh
	echo "export JDK_HOME=/usr/local/jdk-${1}"  >> /etc/profile.d/jdk.sh
	echo 'PATH=$JAVA_HOME/bin:$PATH'            >> /etc/profile.d/jdk.sh
}

## Installs the OpenJDK if necessary
install_openjdk() {
	if command -v java > /dev/null; then
		version=$(java -version)
		if [ $(cut -d' ' -f-2) = "openjdk ${JDK_VERSION}" ]; then
			echo "OpenJDK ${JDK_VERSION} is already installed, skipping installation"
			return
		fi
	fi
	download_openjdk ${1} ${2}
	extract_openjdk ${1} && configure_openjdk ${1}
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
	wget -q https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh -O git-lfs.sh
	bash git-lfs.sh > /dev/null
	install_dependency 'ethtool'
	install_dependency 'gcc'
	install_dependency 'g++'
	install_dependency 'git'
	install_dependency 'git-lfs'
	install_dependency 'numactl'
	install_dependency 'grep'
	install_dependency 'sed'
	install_dependency 'gawk'
	install_dependency 'wget'
}

# Execute everything
main
