#!/usr/bin/env bash

# Get the location of the ethtool command
ethtool=$(which "ethtool")

# Guess if the command is installed although not in the path
if ([[ -z "$ethtool" ]] || ! [[ -x "$ethtool" ]]) && [[ -x "/sbin/ethtool" ]]; then
	ethtool=/sbin/ethtool
fi

# If not found or not executable, we can't continue
if [[ -z "$ethtool" ]] || ! [[ -x "$ethtool" ]]; then
	echo '# The program "ethtool" could not be found or it is not executable'
	exit 1
fi

# List of devices
ifaces=$(ip link | grep -E '^[[:digit:]]+: ' | awk '{print $2}' | sed 's/:\s*$//')

# Loop counter
i=1
v=1

# Iterate over all devices
for iface in ${ifaces}; do

	# Get the driver module used by the interface and skip this iteration if it does not exist
	drv=$(readlink /sys/class/net/${iface}/device/driver/module)
	[[ -z "$drv" ]] && continue

	# Get the driver module name and act accordingly to the driver
	drv=$(basename ${drv})

	# Virtio devices
	if [[ "$drv" == "virtio_net" ]]; then

		# Get the PCI bus device
		addr=$(${ethtool} -i ${iface} | grep bus-info | cut -d' ' -f2)

		# Output the exports
		echo "export IXY_VIRTIO_ADDR_$i='$addr'"
		echo "export IXY_VIRTIO_NAME_$i='$iface'"

		# Loop counter increment
		v=$(($v + 1))

	elif [[ "$drv" == "ixgbe" ]]; then

		# Get the PCI bus device
		addr=$(${ethtool} -i ${iface} | grep bus-info | cut -d' ' -f2)

		# Output the exports
		echo "export IXY_IXGBE_ADDR_$i='$addr'"
		echo "export IXY_IXGBE_NAME_$i='$iface'"

		# Loop counter increment
		i=$(($i + 1))

	fi
done

# Export another variable that says how many variables were exported
echo "export IXY_IXGBE_COUNT=$(($i - 1))"
echo "export IXY_VIRTIO_COUNT=$(($v - 1))"
