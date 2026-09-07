#!/bin/bash

source $SDKMAN_DIR/bin/sdkman-init.sh

# The base image (Amazon Corretto DHI) already ships a hardened, pinned JDK, so we
# skip SDKMAN's Java install to avoid installing a second, non-hardened JVM. Only
# SBT is installed via SDKMAN. Reference: https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html
sdk update
sdk install sbt 1.13.0

# Create a symlink to /usr/bin so it can be used in plain sh
ln -s $SDKMAN_DIR/candidates/sbt/current/bin/sbt /usr/bin/sbt

# Remove temporary files
rm -rf $SDKMAN_DIR/archives/* && rm -rf $SDKMAN_DIR/tmp/*

# Pull all dependencies
sbt clean update
