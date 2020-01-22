echo "updating flexicore"
#this script assumes that in the same folder a new version FlexiCore.war.zip is located 
service monit stop
pkill -9 java
cp FlexiCore.war.zip  /opt/wildfly/standalone/FlexiCore.war.zip
rm -r /opt/wildfly/standalone/deployments/FlexiCore.war
unzip /opt/wildfly/standalone/deployments/FlexiCore.war.zip 
mv FlexiCore.war /opt/wildfly/standalone/deployments/
chown -R wildfly.wildfly /opt/wildfly/standalone/
service monit start
service shekel-daemon start 
service wildfly start
echo "done"