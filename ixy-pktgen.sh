#!/usr/bin/env bash

source /etc/profile.d/jdk.sh

# Shenandoah Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=static"

# Serial Garbage Collector
#export JAVA_OPTS="-XX:+UseSerialGC"

# G1 Garbage Collector
#export JAVA_OPTS="-XX:+UseG1GC"

# Parallel Garbage Collector, best trade-off between throughput and latency
export JAVA_OPTS="-XX:+UseParallelGC"

# Epsilon Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -Xmx8g -Xms8g -XX:+AlwaysPreTouch"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseEpsilonGC"

# CMS Garbage Collector
#export JAVA_OPTS="-XX:+UseConcMarkSweepGC"

# Z Garbage Collector
#export JAVA_OPTS="-XX:+UnlockExperimentalVMOptions"
#export JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"

export JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=1"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"
export JAVA_OPTS="$JAVA_OPTS -XX:+OptimizeStringConcat"
#export JAVA_OPTS="$JAVA_OPTS -Xlog:gc:./gc.log"
export JAVA_OPTS="$JAVA_OPTS -Xshare:off"
export JAVA_OPTS="$JAVA_OPTS -server"
export JAVA_OPTS="$JAVA_OPTS -Dlogback.configurationFile=$(realpath logback.xml)"

bash pktgen/build/install/pktgen/bin/pktgen $@
