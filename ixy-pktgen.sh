#!/usr/bin/env bash

source /etc/profile.d/jdk.sh
eval "$(bash pci2nic.sh)"

# Shenandoah Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseShenandoahGC"

# Serial Garbage Collector
#export JAVA_OPTS="-XX:+UseSerialGC"

# G1 Garbage Collector
export JAVA_OPTS="-XX:+UseG1GC"
#export JAVA_OPTS="$JAVA_OPTS -XX:+AggressiveHeap"

# Parallel Garbage Collector
#export JAVA_OPTS="-XX:+UseParallelGC"

# Epsilon Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseEpsilonGC"

# CMS Garbage Collector
#export JAVA_OPTS="-XX:+UseConcMarkSweepGC"

# Z Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"

export JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=75"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"
export JAVA_OPTS="$JAVA_OPTS -XX:+OptimizeStringConcat"
export JAVA_OPTS="$JAVA_OPTS -Xlog:gc:./gc.log"
export JAVA_OPTS="$JAVA_OPTS -Xshare:off"
#export JAVA_OPTS="$JAVA_OPTS -Xmx16g"
export JAVA_OPTS="$JAVA_OPTS -server"
export JAVA_OPTS="$JAVA_OPTS -Dlogback.configurationFile=$(realpath logback.xml)"

if [ $# -eq 0 ]; then
	bash pktgen/build/install/pktgen/bin/pktgen $IXY_IXGBE_ADDR_1
else
	bash pktgen/build/install/pktgen/bin/pktgen $@
fi
