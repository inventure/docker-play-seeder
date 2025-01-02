object versions {
  val baseImage = "debian:bullseye-20241223-slim"
  val javaVersion = "11.0.25-amzn"
  val playVersion = "2.9.6"
  val playSlickVersion = "5.3.1"
  val scalaVersion = "2.13.15"
  val sbtVersion = "1.10.7" //make sure to also bump build.properties and sbt-init.sh
}
