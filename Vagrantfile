# -*- mode: ruby -*-
# vi: set ft=ruby :

# Configure syntax compatible with Vagrant 2.x
Vagrant.configure("2") do |config|

	# Use Debian 9.9 (Stretch)
	config.vm.box         = "debian/stretch64"
	config.vm.box_version = "=9.9.0"

	# Increase the boot timeout to 20 minutes
	config.vm.boot_timeout = 1200

	# Mount the whole project inside a subfolder
	config.vm.synced_folder ".", "/home/vagrant/ixy.java"

	# Show a custom message when the machine is up and ready
	config.vm.post_up_message = '
VM started! Run "vagrant ssh <vmname>" to connect to the VM\'s shell.
The two VMs defined here are connected with two virtual links with VirtIO NICs on each side.
Use "lspci" to find the bus addresses of the VirtIO NICs, it will probably be 0000:00:08.0 and 0000:00:09.0.
Ixy is installed in ~/ixy, run with sudo, e.g. sudo ~/ixy/ixy-pktgen 0000:00:08.0
'

	# Configure Linux Huge Pages
	config.vm.provision "shell", privileged: true, run: "always", path: "hugetlbfs.sh"

	# Install OpenJDK 12
	config.vm.provision "shell", privileged: true, run: "always", path: "bootstrap.sh"

	# Copy the script that maps PCI devices to NICs
	config.vm.provision "file", source: "pci2nic.sh", destination: "$HOME/bin/pci2nic"

	# Configure the environment variables that map the VirtIO PCI devices to the NICs
	config.vm.provision "shell", privileged: false, run: "always", inline: <<-SHELL
		chmod +x "$HOME/bin/pci2nic"
		echo '' >> ~/.profile
		echo '# Evaluating the output of this script allows us to map the NICs to the PCI bus devices' >> ~/.profile
		echo 'eval "$($HOME/bin/pci2nic)"' >> ~/.profile
	SHELL

	# Allocate some resources
	config.vm.provider "virtualbox" do |vb|
		vb.memory = "2048"
		vb.cpus   = "4"
		vb.customize ["modifyvm", :id, "--nicpromisc2", "allow-all"]
		vb.customize ["modifyvm", :id, "--nicpromisc3", "allow-all"]
	end

	# Define the packet generator VM
	config.vm.define :pktgen do |config|
		config.vm.network "private_network", ip: "10.100.1.10", nic_type: "virtio", virtualbox__intnet: "ixy_net1", libvirt__network_name: "ixy_net1", :libvirt__dhcp_enabled => false
		config.vm.network "private_network", ip: "10.100.2.10", nic_type: "virtio", virtualbox__intnet: "ixy_net2", libvirt__network_name: "ixy_net2", :libvirt__dhcp_enabled => false
	end

	# Define the packet forwarder VM
	config.vm.define :pktfwd do |config|
		config.vm.network "private_network", ip: "10.100.1.11", nic_type: "virtio", virtualbox__intnet: "ixy_net1", libvirt__network_name: "ixy_net1", :libvirt__dhcp_enabled => false
		config.vm.network "private_network", ip: "10.100.2.11", nic_type: "virtio", virtualbox__intnet: "ixy_net2", libvirt__network_name: "ixy_net2", :libvirt__dhcp_enabled => false
	end

end

# Check if a VM was already created before
def created?(vm_name, provider="virtualbox")
	File.exist?(".vagrant/machines/#{vm_name}/#{provider}/id")
end
