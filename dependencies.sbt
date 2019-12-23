
scalaVersion := versions.scalaVersion

libraryDependencies := Seq(
  ws,
  guice,
  caffeine,
  specs2 % Test,
  "com.typesafe.play" %% "play-json" % versions.playVersion,
  "com.typesafe.play" %% "play-slick" % versions.slickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % versions.slickVersion
)
