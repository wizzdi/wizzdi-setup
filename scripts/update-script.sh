echo "updating flexicore"
service monit stop
pkill -9 java
cp FlexiCore.war.zip  /opt/wildfly/standalone/FlexiCore.war.zip
rm -r /opt/wildfly/standalone/deployments/FlexiCore.war
unzip /opt/wildfly/standalone/deployments/FlexiCore.war.zip 
mv FlexiCore.war /opt/wildfly/standalone/deployments/
chown -R wildfly.wildfly /opt/wildfly/standalone/
service monit start
service shekel-date
