#installs MongoDB on
wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | sudo apt-key add -
arch=$(uname -a)
if [[ $arch == *"aarch64"* ]]; then
    echo "deb [ arch=arm64 ] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/4.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.2.list

fi
if [[ $arch == *"x86"* ]]; then
  echo "deb [ arch= amd64] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/4.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.2.list
 
fi
apt update
apt install -y mongodb-org
