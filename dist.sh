#!/usr/bin/env bash

# Build a distribution tarfile

# mvn install:install-file -Dfile=proguard4.8/lib/proguard.jar -DgroupId=net.sf.proguard -DartifactId=proguard -Dpackaging=jar -Dversion=4.8

LOG=dist.log
rm -f ${LOG}

## Download all dependencies
echo "Creating lib directory with all dependencies..."
mkdir -p lib
rm -f lib/*
lein with-profile +jclouds,+palletops pom >> ${LOG} \
  || { echo "Failed to generate pom"; exit 1; }
mvn dependency:copy-dependencies -DoutputDirectory=lib >> ${LOG} \
  || { echo "Failed to get dependencies"; exit 1; }
rm -f lib/palletops* lib/hadoop-* lib/collectd* lib/graphite*
cp resources/* lib >> ${LOG}

## Build a project jar, and add it to the libs
## TODO: use AOT
echo "Obfuscating..."
lein do \
  with-profile +palletops clean, \
  with-profile +dist,+aot-filter,+proguard,+palletops,+jclouds proguard >> ${LOG} \
  || { echo "Failed to obfuscate"; exit 1; }

cp target/proguard/*.jar lib

## Build a tafile
echo "Building tarfile..."
rm -rf palletops-hadoop
mkdir -p palletops-hadoop/lib
mkdir -p palletops-hadoop/bin
mkdir -p palletops-hadoop/examples

cp bin/* palletops-hadoop/bin
cp lib/* palletops-hadoop/lib
cp examples/* palletops-hadoop/examples
cp README.md \
    cluster_spec.clj cluster_spec_graphite.clj credentials.clj job_spec.clj ReleaseNotes.md \
    palletops-hadoop

tar cvfz ./palletops-hadoop.tar.gz palletops-hadoop >> ${LOG} 2>&1
