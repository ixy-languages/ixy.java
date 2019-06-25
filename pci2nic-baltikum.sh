#!/usr/bin/env bash

## This script uses the following programs:
##  * [
##  * awk
##  * basename
##  * cut
##  * cut
##  * echo
##  * grep
##  * ip
##  * readlink
##  * sed
##  * which

# Get the location of the ethtool command
ethtool=$(which "ethtool")

# Guess if the command is installed but it is not in the path
if ([ -z "${ethtool}" ] || ! [ -x "${ethtool}" ]) && [ -x /sbin/ethtool ]; then
	ethtool=/sbin/ethtool
fi

# If not found or not executable, we can't continue
if [ -z "${ethtool}" ] || ! [ -x "${ethtool}" ]; then
	echo '# The program "ethtool" could not be found or it is not executable'
	exit 1
fi

# Get the IP address of the SSH gateway
gw="$(getent ahosts kaunas | awk '{print $1; exit}')"

# Get the interface used to connect to the gateway
iface="$(ip route get "$gw" | awk '{print $3; exit}')"

# Get the PCI bus device
addr=$(${ethtool} -i ${iface} | grep bus-info | cut -d' ' -f2)

# List of PCI devices that look like 10 GbE cards
pcis=$(lspci -d '8086:' | grep 'Ethernet' | grep 10-Gigabit | awk '{print $1}')

# Loop counter
i=1

# Iterate over all devices
for pci in ${pcis}; do
	# Check if the address is the excluded one
	[[ "${addr}" =~ "${pci}" ]] && continue

	# Output the exports
	echo "export IXY_IXGBE_ADDR_${i}='0000:${pci}'"
	echo "export IXY_IXGBE_NAME_${i}=''"

	# Loop counter increment
	i=$((${i} + 1))
done

# Export another variable that says how many variables were exported
echo "export IXY_IXGBE_COUNT=$((${i} - 1))"
echo "export IXY_VIRTIO_COUNT=0"
