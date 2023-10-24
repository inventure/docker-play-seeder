
scalaVersion := "[scala_version]"

libraryDependencies := Seq(
  "com.typesafe.play" %% "play-slick" % "[play_slick_version]",
  "com.typesafe.play" %% "play-slick-evolutions" % "[play_slick_version]"
)

dependencyOverrides ++= Seq(
  "com.typesafe.play" %% "play" % "[play_version]"
)
