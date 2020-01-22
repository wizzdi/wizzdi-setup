#!/bin/bash
####################################
#
# setup postgresql 
#
####################################
echo "Installing Postgresql-11"
apt install -y wget
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
RELEASE=$(lsb_release -cs)
echo "deb http://apt.postgresql.org/pub/repos/apt/ ${RELEASE}"-pgdg main | sudo tee  /etc/apt/sources.list.d/pgdg.list
apt update
MACHINE_TYPE=`uname -m`
if [ ${MACHINE_TYPE} == 'x86_64' ]; then
  apt -y install postgresql-11
else
  apt -y install postgresql-10
fi


