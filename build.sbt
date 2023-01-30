name := "docker-play-seeder"
organization := "co.tala"
version := "1.0-SNAPSHOT"

// Fix Scala XML compatibility issue when using SBT 1.8.x https://eed3si9n.com/sbt-1.8.0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerSeedPlugin).disablePlugins(PlayLayoutPlugin)
