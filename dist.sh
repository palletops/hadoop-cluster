#!/usr/bin/env bash

# Build a distribution tarfile

## Download all dependencies
mkdir -p lib
rm lib/*
lein with-profile +jclouds,+palletops pom
mvn dependency:copy-dependencies -DoutputDirectory=lib
rm lib/palletops* lib/hadoop-* lib/collectd* lib/graphite*
cp resources/* lib

## Build a project jar, and add it to the libs
## TODO: use AOT
lein do \
  with-profile +palletops clean, \
  with-profile +dist,+aot-filter,+proguard,+palletops,+jclouds proguard \
  || { echo "Failed to obfuscate"; exit 1; }

cp target/proguard/*.jar lib

## Build a tafile
rm -rf palletops-hadoop
mkdir -p palletops-hadoop/lib
mkdir -p palletops-hadoop/bin

cp bin/* palletops-hadoop/bin
cp lib/* palletops-hadoop/lib
cp README.md \
    cluster_spec.clj cluster_spec_graphite.clj credentials.clj job_spec.clj ReleaseNotes.md \
    palletops-hadoop

tar cvfz ./palletops-hadoop.tar.gz palletops-hadoop
