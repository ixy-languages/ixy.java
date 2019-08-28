#!/usr/bin/env bash

# Set the performance mode manually
echo -n 'Setting the scaling governor to performance mode (manually)... '
for file in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
	echo 'performance' > "$file"
done
echo 'OK'

# Use 'cpupower' to set the frequency and mode
echo -n 'Setting scaling governor, minimum and maximum frequency (cpupower)... '
cpupower frequency-set -g performance > /dev/null
cpupower frequency-set -d 3300MHz     > /dev/null
cpupower frequency-set -u 3300MHz     > /dev/null
echo 'Ok'
