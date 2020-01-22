#!/bin/bash
####################################
#
# make ssh password 
#
####################################
echo "Backing up $backup_files to $dest/$archive_file"

 tar -xvf sshpass-1.06.tar.gz
cd sshpass-1.06
./configure
make install


