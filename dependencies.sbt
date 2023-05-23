
scalaVersion := versions.scalaVersion

libraryDependencies := Seq(
  ws,
  guice,
  caffeine,
  specs2 % Test,
  "com.typesafe.play" %% "play-json" % versions.playJsonVersion,
  "com.typesafe.play" %% "play-slick" % versions.playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlickVersion
)
