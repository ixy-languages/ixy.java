#!/usr/bin/env bash

# Get the IP address of the SSH gateway
gw="$(getent ahosts kaunas | awk '{print $1; exit}')"

# Get the interface used to connect to the gateway
iface="$(ip route get "$gw" | awk '{print $3; exit}')"

# Call the original script ignoring the interface which is being used by the SSH session
bash pci2nic.sh "$iface"
