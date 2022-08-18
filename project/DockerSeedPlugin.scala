import java.io.{PrintWriter, Serializable}

import scala.io.Source

import scala.sys.process.{ProcessBuilder, ProcessLogger, stringToProcess}
import scala.util.{Failure, Try}

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.complete.Parser

object DockerSeedPlugin extends AutoPlugin {

  object autoImport {

    object DockerSeedKeys {

      val useDefaults = AttributeKey[Boolean]("use-defaults")

      val commandLinePlayVersion = AttributeKey[Option[String]]("command-line-play-version")
      val desiredPlayVersion: AttributeKey[String] = AttributeKey[String]("desired-play-version")


      val commandLineScalaVersion = AttributeKey[Option[String]]("command-line-scala-version")
      val desiredScalaVersion = AttributeKey[String]("desired-scala-version")

      val commandLinePlaySlickVersion = AttributeKey[Option[String]]("command-line-slick-version")
      val desiredPlaySlickVersion = AttributeKey[String]("desired-slick-version")

      val commandLineSbtVersion = AttributeKey[Option[String]]("command-line-sbt-version")
      val desiredSbtVersion = AttributeKey[String]("desired-sbt-version")

      val commandLineDockerRegistry = AttributeKey[Option[String]]("command-line-registry")
      val desiredDockerRegistry = AttributeKey[String]("desired-docker-registry")

      private lazy val dockerSeedCommandKey = "dockerSeed"

      private[this] sealed abstract class ParseResult extends Product with Serializable

      private[this] val PlayVersion: Parser[ParseResult] =
        (Space ~> token("play-version") ~> Space ~> token(StringBasic, "<play version>")).map(ParseResult.PlayVersion)

      private[this] val ScalaVersion: Parser[ParseResult] =
        (Space ~> token("scala-version") ~> Space ~> token(StringBasic, "<scala version>"))
          .map(ParseResult.ScalaVersion)

      private[this] val playSlickVersion: Parser[ParseResult] =
        (Space ~> token("play-slick-version") ~> Space ~> token(StringBasic, "<play-slick version>"))
          .map(ParseResult.playSlickVersion)

      private[this] val SbtVersion: Parser[ParseResult] =
        (Space ~> token("sbt-version") ~> Space ~> token(StringBasic, "<sbt version>"))
          .map(ParseResult.SbtVersion)

      private[this] val DockerRegistry: Parser[ParseResult] =
        (Space ~> token("docker-registry") ~> Space ~> token(StringBasic, "<docker-registry>"))
          .map(ParseResult.DockerRegistry)

      private[this] val WithDefaults: Parser[ParseResult] =
        (Space ~> token("with-defaults")) ^^^ ParseResult.WithDefaults

      private[this] object ParseResult {

        final case class PlayVersion(value: String) extends ParseResult

        final case class ScalaVersion(value: String) extends ParseResult

        final case class playSlickVersion(value: String) extends ParseResult

        final case class SbtVersion(value: String) extends ParseResult

        final case class DockerRegistry(value: String) extends ParseResult

        case object WithDefaults extends ParseResult

      }

      private[this] val dockerSeedParser: Parser[Seq[ParseResult]] =
        (ScalaVersion | PlayVersion | playSlickVersion | WithDefaults | SbtVersion | DockerRegistry).*

      val dockerSeedCommand: Command = Command(dockerSeedCommandKey)(_ => dockerSeedParser) { (st, args) =>
        val startState: State = st
          .put(useDefaults, args.contains(ParseResult.WithDefaults))
          .put(commandLinePlayVersion, args.collectFirst { case ParseResult.PlayVersion(value) => value })
          .put(commandLineScalaVersion, args.collectFirst { case ParseResult.ScalaVersion(value) => value })
          .put(commandLinePlaySlickVersion, args.collectFirst { case ParseResult.playSlickVersion(value) => value })
          .put(commandLineSbtVersion, args.collectFirst { case ParseResult.SbtVersion(value) => value })
          .put(commandLineDockerRegistry, args.collectFirst { case ParseResult.DockerRegistry(value) => value })

        Function.chain(
          Seq(
            inquireVersions, updateDependencies, updatePlugins, updateBuildProperties, runDockerBuild,
            runDockerPublish, resetDependencies
          )
        )(startState)
      }
    }

  }

  import autoImport.DockerSeedKeys._

  private val inquireVersions = { state: State =>
    import versions._
    state.log.info("### Inquiring versions")
    val useDefs = state.get(useDefaults).getOrElse(false)
    val play = readVersion(playVersion, "Play! version [%s] : ", useDefs, state.get(commandLinePlayVersion).flatten)
    val scala = readVersion(scalaVersion, "Scala version [%s] : ", useDefs, state.get(commandLineScalaVersion).flatten)
    val slick = readVersion(playSlickVersion, "Play-Slick version [%s] : ", useDefs, state.get(commandLinePlaySlickVersion).flatten)
    val sbt = readVersion(sbtVersion, "Sbt version [%s] : ", useDefs, state.get(commandLineSbtVersion).flatten)
    val registry = readVersion(
      docker.registry, "Docker registry [%s] : ", useDefs, state.get(commandLineDockerRegistry).flatten
    )

    val newState = state
      .put(desiredPlayVersion, play)
      .put(desiredScalaVersion, scala)
      .put(desiredPlaySlickVersion, slick)
      .put(desiredSbtVersion, sbt)
      .put(desiredDockerRegistry, registry)

    newState.log.info(
      s"""Working with versions:
         |- play       => $play
         |- scala      => $scala
         |- play-slick => $slick
         |- sbt        => $sbt
         |- registry   => $registry
         |""".stripMargin)

    newState
  }

  private val updateDependencies = { implicit state: State =>
    state.log.info("### Updating dependencies")
    val projectBase = Project.extract(state).get(baseDirectory)
    replacePlaceholders(
      placeholderFile = projectBase / "project" / "placeholders" / "dependencies.sbt",
      destinationFile = projectBase / "dependencies.sbt"
    )
  }

  private val updatePlugins = { implicit state: State =>
    state.log.info("### Updating plugins")
    val projectBase = Project.extract(state).get(baseDirectory)
    replacePlaceholders(
      placeholderFile = projectBase / "project" / "placeholders" / "plugins.sbt",
      destinationFile = projectBase / "project" / "plugins.sbt"
    )
  }

  private val updateBuildProperties = { implicit state: State =>
    state.log.info("### Updating build properties")
    val projectBase = Project.extract(state).get(baseDirectory)
    replacePlaceholders(
      placeholderFile = projectBase / "project" / "placeholders" / "build.properties",
      destinationFile = projectBase / "project" / "build.properties"
    )
  }

  private val runDockerBuild = { state: State =>
    state.log.info("### Building docker image")
    implicit val st: State = state
    val log: ProcessLogger = processLogger(state)
    val process: ProcessBuilder = stringToProcess(s"docker build -t ${getDockerImageTag(state)} .")
    if (process ! log != 0) sys.error("Error building image")
    // The easy way to build multi-platform images. Still experimental enough it doesn't seem to work for us
    //val log: ProcessLogger = processLogger(state)
    //val process: ProcessBuilder = stringToProcess(s"docker buildx build --platform linux/arm64/v8,linux/amd64 -t ${getDockerImageTag(state)} .")
    //if (process ! log != 0) sys.error("Error building image")

    //This is the harder way to build multi-platform images
    //val process1: ProcessBuilder = stringToProcess(s"docker build -t ${getDockerImageTag(state)}-manifest-amd64 --build-arg ARCH=amd64/ .")
    //if (process1 ! log != 0) sys.error("Error building amd64 image")
    //val process2: ProcessBuilder = stringToProcess(s"docker build -t ${getDockerImageTag(state)}-manifest-arm64v8 --build-arg ARCH=arm64v8/ .")
    //if (process2 ! log != 0) sys.error("Error building arm64v8 image")
    state
  }

  private val runDockerPublish = { state: State =>
    state.log.info("### Pushing docker image. This process will take a while depending on your internet connection")
    // The code to push a single image
    val log: ProcessLogger = processLogger(state)
    val process: ProcessBuilder = stringToProcess(s"docker push ${getDockerImageTag(state)}")
    if (process ! log != 0) sys.error("Error pushing docker image")

    //the code to combine cross platform images with a single manifest
//    val log1: ProcessLogger = processLogger(state)
//    val process1: ProcessBuilder = stringToProcess(s"docker push ${getDockerImageTag(state)}-manifest-amd64")
//    if (process1 ! log1 != 0) sys.error("Error pushing amd64 docker image")
//    val log2: ProcessLogger = processLogger(state)
//    val process2: ProcessBuilder = stringToProcess(s"docker push ${getDockerImageTag(state)}-manifest-arm64v8")
//    if (process2 ! log2 != 0) sys.error("Error pushing arm64v8 docker image")
//    val log3: ProcessLogger = processLogger(state)
//    val process3: ProcessBuilder = stringToProcess(s"docker manifest create ${getDockerImageTag(state)}-manifest-combined" +
//      s"--amend ${getDockerImageTag(state)}-manifest-amd64 --amend ${getDockerImageTag(state)}-manifest-arm64v8")
//    if (process3 ! log3 != 0) sys.error("Error creating cross platform manifest")
//    val log4: ProcessLogger = processLogger(state)
//    val process4: ProcessBuilder = stringToProcess(s"docker manifest push ${getDockerImageTag(state)}-manifest-combined")
//    if (process4 ! log4 != 0) sys.error("Error pushing combined docker manifest")

    state
  }

  private val resetDependencies: State => State = { state: State =>
    state.log.info("#### Resetting project dependencies to initial state")
    val process: ProcessBuilder = stringToProcess(s"git reset --hard HEAD")
    if (process ! processLogger(state) != 0) sys.error("Error resetting project dependencies")
    state
  }

  private def getAttributeKey[T](attributeKey: AttributeKey[T])(implicit state: State) = {
    state.get(attributeKey).getOrElse(throw new IllegalArgumentException(s"Attribute not found | $attributeKey"))
  }

  private def replacePlaceholders(placeholderFile: File, destinationFile: File)(implicit state: State): State = {
    val fileSource = Source.fromFile(placeholderFile)
    val tmpFile = new File(destinationFile.getAbsolutePath + s".update-${System.currentTimeMillis()}")

    val printWriter = new PrintWriter(tmpFile)
    Try {
      fileSource.getLines.foreach { line =>
        val replacedLine = line
          .replaceAll("\\[play_version]", getAttributeKey(desiredPlayVersion))
          .replaceAll("\\[play_slick_version]", getAttributeKey(desiredPlaySlickVersion))
          .replaceAll("\\[sbt_version]", getAttributeKey(desiredSbtVersion))
          .replaceAll("\\[scala_version]", getAttributeKey(desiredScalaVersion))
          .replaceAll("\\[caffeine]", getAttributeKey(desiredSbtVersion).split('.').headOption match {
            case Some("0") => "" // do not add caffeine for sbt 0.x
            case _ => "\n  caffeine,"
          })
        printWriter.println(replacedLine)
      }
      printWriter.close()
      tmpFile.renameTo(destinationFile)
    } match {
      case Failure(exception) =>
        fileSource.close()
        throw exception
      case _ => fileSource.close()
    }

    state
  }

  private def getDockerImageTag(implicit state: State): String = {
    val playVersion: String = getAttributeKey(desiredPlayVersion)
    val playSlickVersion = getAttributeKey(desiredPlaySlickVersion)
    val sbtVersion = getAttributeKey(desiredSbtVersion)
    val scalaVersion = getAttributeKey(desiredScalaVersion)
    val registry = getAttributeKey(desiredDockerRegistry)

    s"$registry/play-dependencies-seed:play-$playVersion-sbt-$sbtVersion-scala-$scalaVersion-play-slick-$playSlickVersion"
  }

  def readVersion(default: String, prompt: String, useDefault: Boolean, commandLineVersion: Option[String]): String = {
    if (commandLineVersion.isDefined) commandLineVersion.get
    else if (useDefault) default
    else SimpleReader.readLine(prompt format default) match {
      case Some("") => default
      case Some(input) => input
      case None => sys.error("No version provided!")
    }
  }

  private def processLogger(st: State): ProcessLogger = new ProcessLogger {
    override def err(s: => String): Unit = st.log.info(s)

    override def out(s: => String): Unit = st.log.info(s)

    override def buffer[T](f: => T): T = st.log.buffer(f)
  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq[Setting[_]](
    commands += dockerSeedCommand
  )
}

