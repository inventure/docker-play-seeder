name := "docker-play-seeder"
organization := "co.tala"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerSeedPlugin).disablePlugins(PlayLayoutPlugin)
