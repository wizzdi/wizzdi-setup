result=`id -u`
echo this is the result $result
if [ "$result" -eq  0 ]
 then
    echo running as root OK
else
    echo  this script must run as root
    exit
fi
pkill -9 unattended-upgr 
apt remove unattended-upgrades -y



