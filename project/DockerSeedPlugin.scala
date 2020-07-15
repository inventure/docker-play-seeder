import java.io.{PrintWriter, Serializable}

import scala.io.Source

// Avoid conflict with classes of the same name in sbt._ when using SBT 0.13
import scala.sys.process.{ProcessBuilder => SysProcessBuilder, ProcessLogger => SysProcessLogger, stringToProcess => sysStringToProcess}
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

      val commandLineSlickVersion = AttributeKey[Option[String]]("command-line-slick-version")
      val desiredSlickVersion = AttributeKey[String]("desired-slick-version")

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

      private[this] val SlickVersion: Parser[ParseResult] =
        (Space ~> token("slick-version") ~> Space ~> token(StringBasic, "<slick version>"))
          .map(ParseResult.SlickVersion)

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

        final case class SlickVersion(value: String) extends ParseResult

        final case class SbtVersion(value: String) extends ParseResult

        final case class DockerRegistry(value: String) extends ParseResult

        case object WithDefaults extends ParseResult

      }

      private[this] val dockerSeedParser: Parser[Seq[ParseResult]] =
        (ScalaVersion | PlayVersion | SlickVersion | WithDefaults | SbtVersion | DockerRegistry).*

      val dockerSeedCommand: Command = Command(dockerSeedCommandKey)(_ => dockerSeedParser) { (st, args) =>
        val startState: State = st
          .put(useDefaults, args.contains(ParseResult.WithDefaults))
          .put(commandLinePlayVersion, args.collectFirst { case ParseResult.PlayVersion(value) => value })
          .put(commandLineScalaVersion, args.collectFirst { case ParseResult.ScalaVersion(value) => value })
          .put(commandLineSlickVersion, args.collectFirst { case ParseResult.SlickVersion(value) => value })
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
    val slick = readVersion(slickVersion, "Slick version [%s] : ", useDefs, state.get(commandLineSlickVersion).flatten)
    val sbt = readVersion(sbtVersion, "Sbt version [%s] : ", useDefs, state.get(commandLineSbtVersion).flatten)
    val registry = readVersion(
      docker.registry, "Docker registry [%s] : ", useDefs, state.get(commandLineDockerRegistry).flatten
    )

    val newState = state
      .put(desiredPlayVersion, play)
      .put(desiredScalaVersion, scala)
      .put(desiredSlickVersion, slick)
      .put(desiredSbtVersion, sbt)
      .put(desiredDockerRegistry, registry)

    newState.log.info(
      s"""Working with versions:
         |- play      => $play
         |- scala     => $scala
         |- slick     => $slick
         |- sbt       => $sbt
         |- registry  => $registry
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
    val log: SysProcessLogger = processLogger(state)
    val process: SysProcessBuilder = sysStringToProcess(s"docker build -t ${getDockerImageTag(state)} .")
    if (process ! log != 0) sys.error("Error building image")
    state
  }

  private val runDockerPublish = { state: State =>
    state.log.info("### Pushing docker image. This process will take a while depending on your internet connection")
    val log: SysProcessLogger = processLogger(state)
    val process: SysProcessBuilder = sysStringToProcess(s"docker push ${getDockerImageTag(state)}")
    if (process ! log != 0) sys.error("Error pushing docker image")
    state
  }

  private val resetDependencies: State => State = { state: State =>
    state.log.info("#### Resetting project dependencies to initial state")
    val process: SysProcessBuilder = sysStringToProcess(s"git reset --hard HEAD")
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
          .replaceAll("\\[slick_version]", getAttributeKey(desiredSlickVersion))
          .replaceAll("\\[sbt_version]", getAttributeKey(desiredSbtVersion))
          .replaceAll("\\[scala_version]", getAttributeKey(desiredScalaVersion))
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
    val slickVersion = getAttributeKey(desiredSlickVersion)
    val sbtVersion = getAttributeKey(desiredSbtVersion)
    val scalaVersion = getAttributeKey(desiredScalaVersion)
    val registry = getAttributeKey(desiredDockerRegistry)

    s"$registry/play-dependencies-seed:$playVersion-sbt-$sbtVersion-scala-$scalaVersion-slick-$slickVersion"
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

  private def processLogger(st: State): SysProcessLogger = new SysProcessLogger {
    override def err(s: => String): Unit = st.log.info(s)

    override def out(s: => String): Unit = st.log.info(s)

    override def buffer[T](f: => T): T = st.log.buffer(f)
  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq[Setting[_]](
    commands += dockerSeedCommand
  )
}

