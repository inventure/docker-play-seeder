
scalaVersion := "[scala_version]"

libraryDependencies := Seq(
  ws,
  guice,
  caffeine,
  specs2 % Test,
  "com.typesafe.play" %% "play-json" % "[play_version]",
  "com.typesafe.play" %% "play-slick" % "[slick_version]",
  "com.typesafe.play" %% "play-slick-evolutions" % "[slick_version]"
)
