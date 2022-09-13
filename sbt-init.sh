#!/bin/bash

source $SDKMAN_DIR/bin/sdkman-init.sh

# Install Java and SBT - Reference: https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
# By default, it installs the latest version of Java 17 from OpenJDK (jdk.java.net)
sdk install java 17.0.4-amzn
sdk install sbt 1.7.1

# Create a symlink to /usr/bin so they can be used in plain sh
ln -s $SDKMAN_DIR/candidates/sbt/current/bin/sbt /usr/bin/sbt
ln -s $SDKMAN_DIR/candidates/java/current/bin/java /usr/bin/java

# Remove temporary files
rm -rf $SDKMAN_DIR/archives/* && rm -rf $SDKMAN_DIR/tmp/*

# Pull all dependencies
sbt clean update
