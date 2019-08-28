#!/usr/bin/env sh

## This script uses the following programs:
##  * [
##  * cut
##  * echo
##  * grep
##  * head
##  * id
##  * mkdir
##  * mount
##  * numactl
##  * seq
##  * sysctl

# Make sure that we are root or we won't able to write into the sysfs mount
if [ $(id -u) -ne 0 ]; then
    echo 'Please run this script as root or using sudo'
    exit 1
fi

# Mount the hugetlbfs if not mounted already
mkdir -p /mnt/huge
(mount | grep /mnt/huge) > /dev/null || mount -t hugetlbfs hugetlbfs /mnt/huge

# Loop for each NUMA node in the machine and increase the number of hugepages to 512
numa=$(numactl --hardware | head -n 1 | cut -d' ' -f2)
nodes=$(seq 0 $((${numa} - 1)))
for i in ${nodes}; do
	if [ -e /sys/devices/system/node/node${i} ]; then
		echo 512 > /sys/devices/system/node/node${i}/hugepages/hugepages-2048kB/nr_hugepages
	fi
done

# Update the number of hugepages in the sysctl configuration file and commit the changes
echo 'vm.nr_hugepages = 512' > /etc/sysctl.d/ixy.conf
sysctl --system
