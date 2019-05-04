#!/usr/bin/env bash

# Ensure the directory exists
mkdir -p /mnt/huge

# Mount the hugetlbfs if not mounted already
(mount | grep /mnt/huge) > /dev/null || mount -t hugetlbfs hugetlbfs /mnt/huge

# Loop for each NUMA node in the machine and increase the pool size of hugepages
for i in {0..7}; do
	if [[ -e /sys/devices/system/node/node${1} ]]; then
		echo 512 > /sys/devices/system/node/node${i}/hugepages/hugepages-2048kB/nr_hugepages
	fi
done
