#!/bin/bash

source $SDKMAN_DIR/bin/sdkman-init.sh

# Install Java and SBT - Reference: https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
sdk update
sdk install java 21.0.9-amzn
sdk install sbt 1.11.7

# Create a symlink to /usr/bin so they can be used in plain sh
ln -s $SDKMAN_DIR/candidates/sbt/current/bin/sbt /usr/bin/sbt
ln -s $SDKMAN_DIR/candidates/java/current/bin/java /usr/bin/java

# Remove temporary files
rm -rf $SDKMAN_DIR/archives/* && rm -rf $SDKMAN_DIR/tmp/*

# Pull all dependencies
sbt clean update
