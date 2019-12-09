#start with killing and removing the unattended-upgrades installed by Fireflycurrent_dir=`id -u`
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
apt update

#test if java installed and which Java it is
if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME 
    _java="$JAVA_HOME/bin/java"
else
    echo "no java"
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')

    echo version "$version"
    if [[ "$version" < "10" ]]; then
        echo  Java version should be upgraded to java 11 and java 8 removed
        service monit stop
	echo  "Stopped monit before killing all Java processes."
	service wildfly stop #better to stop service properly
        pkill -9 java
	echo "killed java processes"
	apt purge -y  oracle-java8-installer 
	echo "have removed java 8"
        apt install -y openjdk-11-jre openjdk-11-jdk
[ -d "/usr/lib/jvm/java-11-openjdk-arm64/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-arm64" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-arm64"
[ -d "/usr/lib/jvm/java-11-openjdk-amd64/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
[ -d "/usr/lib/jvm/java-11-openjdk-armhf/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-armhf"
        echo "have installed OpenJDK java 11 $JAVA_HOME " 
	version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
 	 echo Java version is version $version
else
	 echo  java version  is okay, no change is needed
    fi
else
     #no java is installed , will install Java 11
     apt install -y openjdk-11-jre openjdk-11-jdk
[ -d "/usr/lib/jvm/java-11-openjdk-arm64/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-arm64" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-arm64"
[ -d "/usr/lib/jvm/java-11-openjdk-amd64/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
[ -d "/usr/lib/jvm/java-11-openjdk-armhf/" ] &&  echo "JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf" >> /etc/environment && JAVA_HOME="/usr/lib/jvm/java-11-openjdk-armhf"
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
     echo Java version is version $version

fi
return 0



