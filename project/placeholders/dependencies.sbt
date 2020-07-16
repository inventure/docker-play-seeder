
scalaVersion := "[scala_version]"

libraryDependencies := Seq(
  ws,
  guice,
  specs2 % Test,[caffeine]
  "com.typesafe.play" %% "play-json" % "[play_version]",
  "com.typesafe.play" %% "play-slick" % "[slick_version]",
  "com.typesafe.play" %% "play-slick-evolutions" % "[slick_version]"
)
