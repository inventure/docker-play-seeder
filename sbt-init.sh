#!/bin/bash

source $HOME/.sdkman/bin/sdkman-init.sh

# Install Java and SBT - Reference: https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
sdk install java $(sdk list java | grep -o "8\.[0-9]*\.[0-9]*\.hs-adpt" | head -1)
sdk install sbt

# Remove temporary files
rm -rf $HOME/.sdkman/archives/* && rm -rf $HOME/.sdkman/tmp/*

# Pull all dependencies
sbt clean update
