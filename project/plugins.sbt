// Fix Scala XML compatibility issue when using SBT 1.8.x https://eed3si9n.com/sbt-1.8.0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

//Make sure this matches the version of play in versions.scala
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.6")
