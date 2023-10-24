
scalaVersion := versions.scalaVersion

libraryDependencies := Seq(
  "com.typesafe.play" %% "play-slick" % versions.playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlickVersion
)

dependencyOverrides ++= Seq(
  "com.typesafe.play" %% "play" % versions.playVersion
)
