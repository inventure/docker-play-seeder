object versions {
  val baseImage = "debian:bullseye-20240423-slim"
  val javaVersion = "11.0.23-amzn"
  val playVersion = "2.9.3" //make sure the project/plugins.sbt matches this number
  val playSlickVersion = "5.3.0"
  val scalaVersion = "2.13.14"
  val sbtVersion = "1.10.0" //make sure to also bump build.properties and sbt-init.sh
}
