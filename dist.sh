#!/usr/bin/env bash

# Build a distribution tarfile

## Download all dependencies
lein pom
mkdir -p lib
rm lib/*
mvn dependency:copy-dependencies -DoutputDirectory=lib

## Build a project jar, and add it to the libs
## TODO: use AOT
lein do clean, with-profile +dist jar
cp target/*.jar lib

## Build a tafile
tar cvfz ./palletops-hadoop.tar.gz README.md bin lib
