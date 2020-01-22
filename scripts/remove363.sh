service mongodb stop
service mongod stop
servicename=mongodb
apt-get purge -y mongodb-org*
rm -r /var/log/mongodb
rm -r /var/lib/mongodb/
systemctl stop ${servicename}
systemctl disable ${servicename}
rm /etc/systemd/system/${servicename}
rm /etc/systemd/system/${servicename} 
systemctl daemon-reload
systemctl reset-failed
find / -type f -name "mongo*" -exec rm -f {} \;

