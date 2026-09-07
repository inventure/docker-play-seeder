object versions {
  // Docker Hardened Image (DHI) - Debian 13 (trixie) based Amazon Corretto image.
  // Requires `docker login dhi.io` (see CI workflows). The "-dev" variant is required
  // because it retains a shell + apt-get, which sbt-init.sh needs to run SDKMAN/SBT.
  val baseImage = "dhi.io/amazoncorretto:21-debian13-dev"
  val javaVersion = "21.0.12-amzn"
  val playVersion = "3.0.11"
  val playSlickVersion = "6.2.0"
  val scalaVersion = "2.13.18"
  val sbtVersion = "1.13.0"
}
