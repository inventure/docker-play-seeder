# Docker Play Seeder

Contains utilities for generating a seed image that contains appropriate dependencies needed to speed up the time
spent packing a Play! application for deployment using Docker.

When packaging a Docker image for a Play! application, most of the time is spent downloading Scala, Play!, and SBT
dependencies from the internet. Naturally, if we can preemptively download these resources, the build should be faster.

Preliminary tests have shown that preemptively downloading and caching the above resources speeds by the build time by
10 times.

### Usage

You can use this project to generate Docker images containing cached versions of `play`, `sbt`, `scala` and `play-slick` 
deployed to a Docker registry of your choice — you must have permissions to push to the specified Docker registry.

Below are the various ways of generating images;

- Using default values specified in `project/versions.scala` file
  
  ``` 
  sbt "dockerSeed with-defaults"
  ```

- Interactive using custom values
  
  ```
  sbt dockerSeed
  ```
  
  You will be asked for the versions to be used. Below is a sample log of events from this process
  
  ``` 
  $ sbt dockerSeed
  [info] Loading settings for project global-plugins from idea.sbt ...
  [info] Loading global plugins from /Users/ivan/.sbt/1.0/plugins
  [info] Loading settings for project play-docker-seeder-build from plugins.sbt ...
  [info] Loading project definition from /Users/ivan/Code/env/inventure/apps/play-docker-seeder/project
  [info] Loading settings for project root from dependencies.sbt,build.sbt ...
  [info] Set current project to play-docker-seeder (in build file:/Users/ivan/Code/env/inventure/apps/play-docker-seeder/)
  [info] ### Inquiring versions
  Play! version [2.7.3] : 2.7.2
  Scala version [2.12.16] : 2.12.16
  Play-Slick version [3.0.3] :
  Sbt version [1.7.1] :
  Docker registry [ivanoronee] :
  [info] Working with versions:
  [info] - play       => 2.7.2
  [info] - scala      => 2.12.16
  [info] - play-slick => 3.0.3
  [info] - sbt        => 1.7.1
  [info] - registry   => ivanoronee
  [info] ### Updating dependencies
  [info] ### Updating plugins
  [info] ### Updating build properties
  [info] ### Building docker image
  [info] Sending build context to Docker daemon  355.3kB
  ```
  
- Non interactive using custom values
   
  ``` 
  sbt "dockerSeed play-version 2.7.2 scala-version 2.12.16 play-slick-version 3.0.3 sbt-version 1.7.1 docker-registry funkychicken" 
  ```
  
 - Non interactive with some custom values and some default values
 
   ``` 
   sbt "dockerSeed with-defaults sbt-version 1.7.1 docker-registry monkeybusiness"
   ``` 
   
 When the command returns, an image will be deployed to the specified docker registry. Below is the format of the image
 
 ``` 
 s"$registry/play-dependencies-seed:$playVersion-sbt-$sbtVersion-scala-$scalaVersion-play-slick-$playSlickVersion"
 ```

### Note

- When pushing to Dockerhub, the repository that will be pushed to is `[specified registry]/play-dependencies-seed`. If
you encounter permission problems pushing ensure the following is in order;
  - If the repository `play-dependencies-seed` does not already exist in the target account, ensure that you have 
permissions to create repositories in the account 
  - If the repository `play-dependencies-seed` already exists, ensure that you have write permissions to the repository
