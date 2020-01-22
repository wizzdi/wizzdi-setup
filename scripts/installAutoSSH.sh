#!/bin/bash
####################################
#
# install autossh 
#
####################################
echo "Installing autossh"

gunzip -c autossh-1.4e.tgz | tar xvf -
cd autossh-1.4e
./configure
make
sudo make install

