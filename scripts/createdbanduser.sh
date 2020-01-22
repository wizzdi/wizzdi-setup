echo "will now run postgresql database and user creation, this is non destructive and will not delete an existing database"
sudo -u postgres bash -c "psql -c \"CREATE database flexicore;\""
sudo -u postgres bash  -c "psql -c \"CREATE USER flexicore WITH PASSWORD 'flexicore';\""      
sudo -u postgres bash  -c "psql -c \"grant all privileges on database flexicore  to flexicore ;\""
echo "Done database and user creation"

