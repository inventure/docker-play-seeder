object versions {
  val baseImage = "debian:bullseye-20240701-slim"
  val javaVersion = "11.0.23-amzn"
  val playVersion = "2.9.4"
  val playSlickVersion = "5.3.1"
  val scalaVersion = "2.13.14"
  val sbtVersion = "1.10.1" //make sure to also bump build.properties and sbt-init.sh
}
