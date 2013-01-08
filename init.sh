#!/bin/bash 
JBOSS_HOME=./target/jboss-soa-p-5
SERVER_DIR=$JBOSS_HOME/jboss-as/server/default
INBOUND_DIR=/tmp/inboundLoanApplications
SRC_DIR=./installs
SOA_P=soa-p-5.3.0.GA.zip
BRMS=brms-p-5.3.0.GA-deployable.zip
JBOSS_VERSION=5.3.0.GA
VERSION=5.3.0.BRMS

echo
echo Setting up the Home Loan SOA-P + BRMS demo environment...
echo

command -v mvn -q >/dev/null 2>&1 || { echo >&2 "Maven is required but not installed yet... aborting."; exit 1; }

# make some checks first before proceeding.	
if [[ -x $SRC_DIR/$SOA_P || -L $SRC_DIR/$SOA_P ]]; then
	echo SOA-P sources are present...
	echo
else
	echo Need to download $SOA_P package from the Customer Support Portal 
	echo and place it in the $SRC_DIR directory to proceed...
	echo
	exit
fi

if [[ -x $SRC_DIR/$BRMS || -L $SRC_DIR/$BRMS ]]; then
	echo BRMS sources are present...
	echo
else
	echo Need to download $BRMS package from the Customer Support Portal 
	echo and place it in the $SRC_DIR directory to proceed...
	echo
	exit
fi

# Make the required /tmp directories used by the demo. 
# WARNING: If you modify these locations, you will need to modify similar locations
# in the appropriate jboss-esb.xml configuration file!!!
if [ -x $INBOUND_DIR ]; then
	echo "  - listener directory $INBOUND_DIR exists, no need to make it again..."
	echo
else
	echo "  - creating Listener directory $INBOUND_DIR for demo..."
	echo
	mkdir -p $INBOUND_DIR
fi

# we need this for the demo to work.
if [ ! -x $INBOUND_DIR ]; then
	echo Was not able to create the Listener directory $INBOUND_DIR pls do this by hand and run script again...
	echo
	exit
fi

# Create the target directory if it does not already exist
if [ ! -x target ]; then
	echo "  - creating the target directory..."
	echo
  mkdir target
else
	echo "  - detected target directory, moving on..."
	echo
fi

# Move the old JBoss instance, if it exists, to the OLD position
if [ -x $JBOSS_HOME ]; then
	echo "  - existing JBoss Enterprise SOA Platform $JBOSS_VERSION detected..."
	echo
	echo "  - moving existing JBoss Enterprise SOA Platform $JBOSS_VERSION aside..."
	echo
  rm -rf $JBOSS_HOME.OLD
  mv $JBOSS_HOME $JBOSS_HOME.OLD

	# Unzip the JBoss SOA-P instance
	echo Unpacking JBoss Enterprise SOA Platform $JBOSS_VERSION...
	echo
	unzip -q -d target $SRC_DIR/$SOA_P
else
	# Unzip the JBoss SOA-P instance
	echo Unpacking new JBoss Enterprise SOA Platform $JBOSS_VERSION...
	echo
	unzip -q -d target $SRC_DIR/$SOA_P
fi

# Unzip the required files from JBoss BRMS Deployable
echo Unpacking JBoss Enterprise BRMS $JBOSS_VERSION...
echo

unzip -q $SRC_DIR/$BRMS jboss-brms-manager.zip 
echo "  - deploying JBoss Enterprise BRMS Manager WAR..."
echo
unzip -q -d $SERVER_DIR/deploy jboss-brms-manager.zip
rm jboss-brms-manager.zip

unzip -q $SRC_DIR/$BRMS jboss-jbpm-console.zip 
echo "  - deploying jBPM Console WARs..."
echo
unzip -q -d $SERVER_DIR/deploy jboss-jbpm-console.zip
rm jboss-jbpm-console.zip

unzip -q $SRC_DIR/$BRMS jboss-jbpm-engine.zip 
echo "  - copying jBPM client JARs..."
echo
unzip -q -d $JBOSS_HOME/jboss-as/common jboss-jbpm-engine.zip lib/netty.jar
rm jboss-jbpm-engine.zip

echo Rounding up, setting permissions and copying support files...
echo

# Add execute permissions to the run.sh script
echo "  - making sure run.sh for server is executable..."
echo
chmod u+x $JBOSS_HOME/jboss-as/bin/run.sh

echo "  - adding BRMS policy in login-config.xml file..."
echo
cp support/login-config.xml $SERVER_DIR/conf

echo "  - enabling admin account in soa-users.properties file..."
echo
cp support/soa-users.properties $SERVER_DIR/conf/props

echo "  - registering an additional RiftSaw event listener in bpel.properties file..."
echo
cp support/bpel.properties $SERVER_DIR/deploy/riftsaw.sar

echo "  - copying custom RiftSaw event listener implementation jar to project..."
echo 
cp support/droolsfusion-eventlistener.jar $SERVER_DIR/deploy/riftsaw.sar/lib

echo "  - registering work item handlers (custom) with business-central-server.war component for process..."
echo 
cp support/drools.session.conf $SERVER_DIR/deploy/business-central-server.war/WEB-INF/classes/META-INF
cp support/CustomWorkItemDefinitions.conf $SERVER_DIR/deploy/business-central-server.war/WEB-INF/classes/META-INF

# Maven artifacts need to be installed for BRMS.

echo Installing the BRMS binaries into the Maven repository...
echo

unzip -q $SRC_DIR/$BRMS jboss-brms-engine.zip
unzip -q jboss-brms-engine.zip binaries/*
cd binaries

echo Installing Drools binary deps...
echo
mvn -q install:install-file -Dfile=drools-ant-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-ant -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-camel-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-camel -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-compiler-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-compiler -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-core-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-core -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-decisiontables-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-decisiontables -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=droolsjbpm-ide-$VERSION.jar -DgroupId=org.drools -DartifactId=droolsjbpm-ide -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-jsr94-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-jsr94 -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-persistence-jpa-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-persistence-jpa -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-templates-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-templates -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=drools-verifier-$VERSION.jar -DgroupId=org.drools -DartifactId=drools-verifier -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=knowledge-api-$VERSION.jar -DgroupId=org.drools -DartifactId=knowledge-api -Dversion=$VERSION -Dpackaging=jar

echo Installing jBPM binary deps...
echo
mvn -q install:install-file -Dfile=jbpm-bam-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-bam -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-bpmn2-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-bpmn2 -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-flow-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-flow -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-flow-builder-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-flow-builder -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-human-task-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-human-task -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-persistence-jpa-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-persistence-jpa -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-test-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-test -Dversion=$VERSION -Dpackaging=jar
mvn -q install:install-file -Dfile=jbpm-workitems-$VERSION.jar -DgroupId=org.jbpm -DartifactId=jbpm-workitems -Dversion=$VERSION -Dpackaging=jar

cd ..
rm -rf binaries
rm jboss-brms-engine.zip

echo Installation of binaries "for" BRMS $VERSION complete.
echo

echo Integration $JBOSS_VERSION Home Loan Demo Setup Complete.
echo
