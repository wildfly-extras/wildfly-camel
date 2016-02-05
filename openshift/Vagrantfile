# -*- mode: ruby -*-
# vi: set ft=ruby :


VAGRANTFILE_API_VERSION = "2"
Vagrant.require_version ">= 1.7.2"

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version. Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  
  config.vm.box = "thesteve0/openshift-origin"
  #uncomment this line if you downloaded the box and want to use it instead
  #config.vm.box = "openshift3"
  config.vm.box_check_update = false
  config.vm.network "private_network", ip: "10.2.2.2"
  config.vm.synced_folder ".", "/vagrant", disabled: true
  config.vm.hostname = "origin"

  
  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network "forwarded_port", guest: 80, host: 8080
  #config.vm.network "forwarded_port", guest: 80, host: 1080
  #config.vm.network "forwarded_port", guest: 443, host: 1443
  #config.vm.network "forwarded_port", guest: 5000, host: 5000
  #config.vm.network "forwarded_port", guest: 8080, host: 8080
  #config.vm.network "forwarded_port", guest: 8443, host: 8443

  config.vm.provider "virtualbox" do |vb|
     #   vb.gui = true
     vb.memory = "4096"
     vb.cpus = 2
     vb.name = "origin-1.1.1"
  end
 
end