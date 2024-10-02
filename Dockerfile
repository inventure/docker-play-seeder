ARG BASE_IMAGE
FROM $BASE_IMAGE AS compile

# Install commonly used packages
RUN apt-get update
RUN apt-get install -y zip
RUN apt-get install -y unzip
RUN apt-get install -y curl
RUN apt-get install -y git

# Cleanup temp files
RUN rm -rf /var/lib/apt/lists/*
RUN rm -rf /tmp/*

# Create the default user
ARG USERNAME=scalaplay
ARG USER_UID=1000
ARG USER_GID=$USER_UID
RUN groupadd --gid $USER_GID $USERNAME && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME

# Set the default user
USER $USERNAME

# Download SDKMAN
ENV SDKMAN_DIR="/usr/local/sdkman"
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
