
scalaVersion := "[scala_version]"

libraryDependencies := Seq(
  ws,
  guice,
  specs2 % Test,[caffeine]
  "com.typesafe.play" %% "play-json" % "[play_version]",
  "com.typesafe.play" %% "play-slick" % "[play_slick_version]",
  "com.typesafe.play" %% "play-slick-evolutions" % "[play_slick_version]"
)
