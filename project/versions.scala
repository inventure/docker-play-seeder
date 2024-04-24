object versions {
  val baseImage = "debian:bullseye-20240311-slim"
  val javaVersion = "11.0.22-amzn"
  val playVersion = "2.9.2" //make sure the project/plugins.sbt matches this number
  val playSlickVersion = "6.0.0"
  val scalaVersion = "2.13.13"
  val sbtVersion = "1.9.9" //make sure to also bump build.properties and sbt-init.sh
}
