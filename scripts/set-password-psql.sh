password=$1
#su - postgres -c "psql -U postgres -d postgres -c \"alter user postgres with password '${password}';\""
su - postgres -c "psql -U postgres -d postgres -c \"alter user postgres with password '${1}';\""



