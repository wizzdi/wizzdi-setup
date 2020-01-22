#!/bin/bash
echo "welcome to Wizzdi wildfly application server installer"

#We assume that the folder in /opt has been copied
Version_Number="16.0.0.Final"
echo $Version_Number
#create the wildfly group and user
groupadd -r wildfly
useradd -r -g wildfly -d /opt/wildfly -s /sbin/nologin wildfly
rm /opt/wildfly #removing old link
#
if  [ ! -d "/opt/wildfly" ] 
then 
	ln -s /opt/wildfly-$Version_Number /opt/wildfly
echo "Has defined a link to /opt/$Version_Number"
else 
echo "Link to $Version_Number is defined"
fi
[ -d /opt/wildfly ]  && echo "wildfly link found, success"
[ ! -d /opt/wildfly ] && echo "Error !! wildfly link cannot be found"
echo "JBOSS_HOME=/opt/wildfly" >> /etc/environment
source /etc/environment
chown -R wildfly.wildfly  /opt/wildfly-16.0.0.Final
chown -R wildfly.wildfly /opt/wildfly
mkdir -p /etc/wildfly

[ ! -d /etc/wildfly ] && echo " error !! /etc/wildfly cannot be found"
[  -d /etc/wildfly ] && echo " /etc/wildfly found, success "

#assume that this file (wildfly.conf) is correctly set
cp /opt/wildfly/docs/contrib/scripts/systemd/wildfly.conf /etc/wildfly/
[ ! -f /etc/wildfly/wildfly.conf ] && echo " error !! /etc/wildfly/wildfly.conf  cannot be found"
[ -f /etc/wildfly/wildfly.conf ] && echo " /etc/wildfly/wildfly.conf found, success"
cp /opt/wildfly/docs/contrib/scripts/systemd/wildfly.service /etc/systemd/system/
[ ! -f /etc/systemd/system/wildfly.service ] && echo " error !! /etc/systemd/system/wildfly.service cannot be found"

[ -f /etc/systemd/system/wildfly.service ] && echo "/etc/systemd/system/wildfly.service  be found, success "
 cp /opt/wildfly/docs/contrib/scripts/systemd/launch.sh /opt/wildfly/bin/


chmod +x /opt/wildfly/bin/launch.sh

[[ ! -f /opt/wildfly/bin/launch.sh ]] && echo " error !! /opt/wildfly/bib/launch.sh cannot be found"

systemctl enable wildfly 

systemctl daemon-reload







echo "have configured wildfly to run as a service"


