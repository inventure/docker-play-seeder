
scalaVersion := "[scala_version]"

libraryDependencies := Seq(
  "com.typesafe.play" %% "play-slick" % "[play_slick_version]",
  "com.typesafe.play" %% "play-slick-evolutions" % "[play_slick_version]"
)

dependencyOverrides ++= Seq(
  "com.typesafe.play" %% "play" % "[play_version]"
)

// The selected version of the framework may contain packages with known vulnerabilities
// Exclude the common ones from the base Docker image as they will be added back when compiling the application
excludeDependencies ++= Seq(
  ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
  ExclusionRule("com.google.guava", "guava")
)
