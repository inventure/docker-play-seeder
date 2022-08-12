#!/bin/bash

source $HOME/.sdkman/bin/sdkman-init.sh

# Install Java and SBT - Reference: https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
# By default, it installs the latest version of Java 8 from OpenJDK (jdk.java.net)
sdk install java 8.0.342-amzn
sdk install sbt 1.7.1

# Create a symlink to /usr/bin so they can be used in plain sh
ln -s $HOME/.sdkman/candidates/sbt/current/bin/sbt /usr/bin/sbt
ln -s $HOME/.sdkman/candidates/java/current/bin/java /usr/bin/java

# Remove temporary files
rm -rf $HOME/.sdkman/archives/* && rm -rf $HOME/.sdkman/tmp/*

# Pull all dependencies
sbt clean update
