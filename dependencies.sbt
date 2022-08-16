
scalaVersion := versions.scalaVersion

libraryDependencies := Seq(
  ws,
  guice,
  caffeine,
  specs2 % Test,
  //Play-json only syncs with play to 2.7.4, while we currently want to be at 2.7.9
  "com.typesafe.play" %% "play-json" % "2.7.4",//versions.playVersion,
  "com.typesafe.play" %% "play-slick" % versions.playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlickVersion
)
