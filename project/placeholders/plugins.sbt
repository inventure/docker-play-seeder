addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "[play_version]")

// Fix Scala XML compatibility issue when using SBT 1.8.x https://eed3si9n.com/sbt-1.8.0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// The selected version of the framework may contain packages with known vulnerabilities
// Exclude the common ones from the base Docker image as they will be added back when compiling the application
ThisBuild / excludeDependencies ++= Seq(
  ExclusionRule("org.apache.ant", "ant"),
  ExclusionRule("org.apache.commons", "commons-compress"),
  ExclusionRule("com.google.protobuf", "protobuf-java")
)
