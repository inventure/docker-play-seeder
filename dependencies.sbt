
scalaVersion := versions.scalaVersion

libraryDependencies := Seq(
  "org.playframework" %% "play-slick" % versions.playSlickVersion,
  "org.playframework" %% "play-slick-evolutions" % versions.playSlickVersion
)

dependencyOverrides ++= Seq(
  "org.playframework" %% "play" % versions.playVersion
)
