
# Example Build Script on Unix with MySQL

This is an example build script.  It may **NOT** be appropriate for your environment
but does demonstrate what a typical build process **might** look like on a 
Unix system using MySQL.

Please consult our [Setup](Setup.md) and [Configuration](Configuration.md) 
documentation for additional information.


```
# Install prereqs
apt-get install tomcat8 git ant openjdk-8-jdk nodejs
# Upped tomcat memory per Apollo devs instructions:
echo "export CATALINA_OPTS="-Xms512m -Xmx1g \
              -XX:+CMSClassUnloadingEnabled \
              -XX:+CMSPermGenSweepingEnabled \
              -XX:+UseConcMarkSweepGC" >> /etc/default/tomcat8

# Download and extract their tarball
npm install -g bower
wget https://github.com/GMOD/Apollo/archive/2.0.4.tar.gz
mv 2.0.4.tar.gz Apollo-2.0.4.tar.gz
tar xf Apollo-2.0.4.tar.gz
# Setup apollo mysql user and database
CREATE USER 'apollo'@'localhost' IDENTIFIED BY 'THE_PASSWORD';
CREATE DATABASE `apollo-production`;
GRANT ALL PRIVILEGES ON `apollo-production`.* To 'apollo'@'localhost' IDENTIFIED BY 'THE_PASSWORD';
# Configure apollo for mysql.
cd ~/src/Apollo-2.0.4
# Let's store the config file outside of the source tree.
mkdir ~/apollo.config
# Copy the template
cp sample-mysql-apollo-config.groovy ~/apollo.config/apollo-config.groovy 
ln -s ~/apollo.config/apollo-config.groovy
# For now, turn off tomcat8 so that we can see if the locally-run version works service tomcat8 stop
# Run the local version, which verifies install reqs, and does a bunch of stuff (see below) 
cd Apollo-2.0.4
./apollo run-local

# Some of what the Apollo installer does:
# Clones a bunch of git submodules into apollo-2.0.4/src
# Does a bunch of java compiling.
# Downloads and installs grails for you here: $HOME/.grails . 
# Installs perl modules here: $HOME/.cpanm
# Installs java stuff here: $HOME/.java and $HOME/.m2

# If a pre-installed instance: 
rm -rf /var/lib/tomcat/webapps/apollo
rm -f /var/lib/tomcat/webapps/apollo.war
# Startup tomcat again
service tomcat8 start

# ... without javascript minimization
#  ./apollo deploy
# Above creates this file: target/apollo-2.0.4.war
sudo cp target/apollo-2.0.4.war /var/lib/tomcat/webapps/apollo.war

# Prepare JBrowse data
# Add the FASTA assembly
~/src/Apollo-2.0.4/bin/prepare-refseqs.pl \
--fasta /research/dre/assembly/assembly1.fasta.gz \
--out ~/organisms/dre

# Add annotations
~/src/Apollo-2.0.4/bin/flatfile-to-json.pl \
--gff /research/dre/annotation/FINAL_annotations/ssc_v4.gff \ 
--type mRNA --trackLabel Annotations --out ~/organisms/dre

# In interface point to directory ~/organisms/dre
```

