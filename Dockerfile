FROM openjdk:8-jdk AS compile

# Install apt-transport-https; otherwise, custom APT https repositories will not work.
RUN apt-get update \
  && apt-get install -y apt-transport-https

# ADD SBT PPA
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823

# Install packages
RUN apt-get update && apt-get install -y \
    sbt \
 && rm -rf /var/lib/apt/lists/*

# Define working directory
ARG COMPILE_WORKDIR=/tmp/play-dependencies-seeder

# Set working directory
WORKDIR $COMPILE_WORKDIR

# Copy all resources
COPY . .

# Pull all dependencies
RUN sbt clean update

# Set defaults to empty
CMD []
