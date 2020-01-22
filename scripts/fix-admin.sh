echo "will now change admin home folder "
sudo -u postgres psql  -d flexicore -c "update baseclass set homedir='/home/flexicore/users/adminPY5xR11lQVqTCPXSQDjPXg' where email='admin@flexicore.com';"
echo "Done admin home folder change "

: