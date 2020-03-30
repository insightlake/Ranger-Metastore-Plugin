# Ranger-Metastore-Plugin
Ranger-Metastore-Plugin is intended to integrate Metastore service into Apache Ranger.

# Installation

## Check $HIVE_HOME env variable
Make sure that $HIVE_HOME env variable is set and points to metastore directory.

## Create a repository in Ranger Policy Manager. 
Create repository in 'Ranger Policy Manager' E.g. "metastore". 
The same name needs to be configured during plugin setup.

## Build the project
Execute next command to build the project

~~~~
mvn clean compile package install assembly:assembly
~~~~

As a result this will create a .tar.gz file 

~~~~
./target/ranger-metastore-plugin-2.1.0-SNAPSHOT-metastore-plugin.tar.gz
~~~~

## Extract binaries at the appropriate place.
~~~~
sudo tar zxf ./target/ranger-metastore-plugin-2.1.0-SNAPSHOT-metastore-plugin.tar.gz -C /usr/local
cd /usr/local/ranger-metastore-plugin
~~~~

## Update installation.properties file
Here are the relevant lines that you should edit:
~~~~
COMPONENT_INSTALL_DIR_NAME=[PATH_TO_META-STORE]
POLICY_MGR_URL=[RANGER_URL]
REPOSITORY_NAME=metastore
~~~~
Where
- [PATH_TO_META-STORE] - Path to installed meta-store 
- [RANGER_URL] - Apache Ranger http url. 
     Ex:http://localhost:6080 if it runs on localhost.  

## Enable metastore plugin
Now enable the metastore-plugin by running the enable-metastore-plugin.sh command (Remember to set JAVA_HOME)

~~~~
cd /usr/local/ranger-metastore-plugin
sudo ./enable-metastore-plugin.sh
~~~~
To check if plugin was installed by success you can verify it on Ranger web-interface going to next menu
'Ranger Admin Web interface -> Audit Tab -> Plugin Status'

## Restart your metastore instance