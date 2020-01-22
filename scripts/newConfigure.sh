#!/bin/bash
####################################
#
# setup autossh 
#
####################################
echo "configuring autossh"
useradd -m -s /sbin/nologin autossh
su autossh -s /bin/bash -c "ssh-keygen -f /home/autossh/.ssh/id_rsa -t rsa -N ''"
su autossh -s /bin/bash -c "ssh-keygen -f /home/autossh/.ssh/id_dsa -t dsa -N ''"
su autossh -s /bin/bash -c "ssh-keygen -f /home/autossh/.ssh/id_ed25519 -t ed25519 -N ''"
su autossh -s /bin/bash -c "sshpass -p 4ccztAZHgD ssh-copy-id -i /home/autossh/.ssh/id_ed25519.pub -o StrictHostKeyChecking=no -f 116.203.115.104"
su autossh -s /bin/bash -c "sshpass -p 4ccztAZHgD ssh-copy-id -i /home/autossh/.ssh/id_dsa.pub -o StrictHostKeyChecking=no -f 116.203.115.104"
su autossh -s /bin/bash -c "sshpass -p 4ccztAZHgD ssh-copy-id -i /home/autossh/.ssh/id_rsa.pub -o StrictHostKeyChecking=no -f 116.203.115.104"

