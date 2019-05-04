#!/usr/bin/env bash

# Get the location of the ethtool command
ethtool=$(which "ethtool")

# If the command does not exist and is not in /sbin, assume it is not installed
if ([[ -z "$ethtool" ]] && [[ -x "/sbin/ethtool" ]]) || ! [[ -x "$ethtool" ]]; then
	ethtool=/sbin/ethtool
else
	echo 'The program "ethtool" could not be found or it is not executable'
	exit 1
fi

# List of devices
ifaces=$(ip link | grep -E '^[[:digit:]]+: ' | awk '{print $2}' | sed 's/:\s*$//')

# Loop counter
i=1

# Iterate over all devices
for iface in ${ifaces}; do

	# Get the driver module used by the interface and skip this iteration if it does not exist
	drv=$(readlink /sys/class/net/${iface}/device/driver/module)
	[[ -z "$drv" ]] && continue

	# Get the driver module name and skip this iteration if it is not the Virtio driver
	drv=$(basename ${drv})
	[[ "$drv" != "virtio_net" ]] && continue

	# Get the PCI bus device
	addr=$(${ethtool} -i ${iface} | grep bus-info | cut -d' ' -f2)

	# Output the exports
	echo "export IXY_VIRTIO_ADDR_$i='$addr'"
	echo "export IXY_VIRTIO_NAME_$i='$iface'"

	# Loop counter increment
	i=$(($i + 1))

done

# Export another variable that says how many variables were exported
echo "export IXY_VIRTIO_COUNT=$(($i - 1))"
