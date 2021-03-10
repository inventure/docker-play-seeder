FROM debian:stretch-slim AS compile

# Install ZIP and cURL
RUN apt-get update && \
    apt-get install -y zip unzip curl git && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

# Download SDKMAN
RUN curl -s "https://get.sdkman.io" | bash

# Define working directory
ARG COMPILE_WORKDIR=/tmp/play-dependencies-seeder

# Set working directory
WORKDIR $COMPILE_WORKDIR

# Copy all resources
COPY . .

# Initiate SBT - SDKMAN requires /bin/bash while Docker by default uses /bin/sh
RUN bash sbt-init.sh

# Set defaults to empty
CMD []
