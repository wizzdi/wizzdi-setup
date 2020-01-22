echo "will now run postgresql database drop"
sudo -u postgres bash -c "psql -c \"DROP DATABASE flexicore ;\""
echo "Done database drop"
