addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "[play_version]")

// Fix Scala XML compatibility issue when using SBT 1.8.x https://eed3si9n.com/sbt-1.8.0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
