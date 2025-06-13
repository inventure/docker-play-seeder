import java.io.{PrintWriter, Serializable}

import scala.io.Source
import scala.sys.process.{ProcessBuilder, ProcessLogger, stringToProcess}
import scala.util.{Failure, Try}

import sbt.*
import sbt.Keys.*
import sbt.complete.DefaultParsers.*
import sbt.complete.Parser

object DockerSeedPlugin extends AutoPlugin {

  object autoImport {

    object DockerSeedKeys {

      val useDefaults = AttributeKey[Boolean]("use-defaults")

      val commandLineBaseImage = AttributeKey[Option[String]]("command-line-base-image")
      val desiredBaseImage: AttributeKey[String] = AttributeKey[String]("desired-base-image")

      val commandLinePlayVersion = AttributeKey[Option[String]]("command-line-play-version")
      val desiredPlayVersion: AttributeKey[String] = AttributeKey[String]("desired-play-version")

      val commandLineScalaVersion = AttributeKey[Option[String]]("command-line-scala-version")
      val desiredScalaVersion = AttributeKey[String]("desired-scala-version")

      val commandLineJavaVersion = AttributeKey[Option[String]]("command-line-java-version")
      val desiredJavaVersion = AttributeKey[String]("desired-java-version")

      val commandLinePlaySlickVersion = AttributeKey[Option[String]]("command-line-slick-version")
      val desiredPlaySlickVersion = AttributeKey[String]("desired-slick-version")

      val commandLineSbtVersion = AttributeKey[Option[String]]("command-line-sbt-version")
      val desiredSbtVersion = AttributeKey[String]("desired-sbt-version")

      val commandLineAddOsSuffix = AttributeKey[Option[String]]("command-line-add-os-suffix")
      val desiredAddOsSuffix = AttributeKey[String]("desired-add-os-suffix")

      val commandLineDockerRegistry = AttributeKey[Option[String]]("command-line-registry")
      val desiredDockerRegistry = AttributeKey[String]("desired-docker-registry")

      val commandLineImageTag = AttributeKey[Option[String]]("command-line-image-tag")
      val desiredDockerImageTag = AttributeKey[String]("desired-image-tag")

      private lazy val dockerSeedCommandKey = "dockerSeed"

      private[this] sealed abstract class ParseResult extends Product with Serializable

      private[this] val BaseImage: Parser[ParseResult] =
        (Space ~> token("base-image") ~> Space ~> token(StringBasic, "<base image>")).map(ParseResult.BaseImage)

      private[this] val PlayVersion: Parser[ParseResult] =
        (Space ~> token("play-version") ~> Space ~> token(StringBasic, "<play version>")).map(ParseResult.PlayVersion)

      private[this] val ScalaVersion: Parser[ParseResult] =
        (Space ~> token("scala-version") ~> Space ~> token(StringBasic, "<scala version>"))
          .map(ParseResult.ScalaVersion)

      private[this] val JavaVersion: Parser[ParseResult] =
        (Space ~> token("java-version") ~> Space ~> token(StringBasic, "<java version>"))
          .map(ParseResult.ScalaVersion)

      private[this] val playSlickVersion: Parser[ParseResult] =
        (Space ~> token("play-slick-version") ~> Space ~> token(StringBasic, "<play-slick version>"))
          .map(ParseResult.playSlickVersion)

      private[this] val SbtVersion: Parser[ParseResult] =
        (Space ~> token("sbt-version") ~> Space ~> token(StringBasic, "<sbt version>"))
          .map(ParseResult.SbtVersion)

      private[this] val AddOsSuffix: Parser[ParseResult] =
        (Space ~> token("add-os-suffix") ~> Space ~> token(StringBasic, "<add-os-suffix>"))
          .map(ParseResult.AddOsSuffix)

      private[this] val DockerRegistry: Parser[ParseResult] =
        (Space ~> token("docker-registry") ~> Space ~> token(StringBasic, "<docker-registry>"))
          .map(ParseResult.DockerRegistry)

      private[this] val ImageTag: Parser[ParseResult] =
        (Space ~> token("image-tag") ~> Space ~> token(StringBasic, "<image-tag>"))
          .map(ParseResult.ImageTag)

      private[this] val WithDefaults: Parser[ParseResult] =
        (Space ~> token("with-defaults")) ^^^ ParseResult.WithDefaults

      private[this] object ParseResult {

        final case class BaseImage(value: String) extends ParseResult

        final case class PlayVersion(value: String) extends ParseResult

        final case class ScalaVersion(value: String) extends ParseResult

        final case class JavaVersion(value: String) extends ParseResult

        final case class playSlickVersion(value: String) extends ParseResult

        final case class SbtVersion(value: String) extends ParseResult

        final case class AddOsSuffix(value: String) extends ParseResult

        final case class DockerRegistry(value: String) extends ParseResult

        final case class ImageTag(value: String) extends ParseResult

        case object WithDefaults extends ParseResult

      }

      private[this] val dockerSeedParser: Parser[Seq[ParseResult]] = (
        BaseImage
          | ScalaVersion
          | JavaVersion
          | PlayVersion
          | playSlickVersion
          | WithDefaults
          | SbtVersion
          | AddOsSuffix
          | DockerRegistry
          | ImageTag
        ).*

      val dockerSeedCommand: Command = Command(dockerSeedCommandKey)(_ => dockerSeedParser) { (st, args) =>
        val startState: State = st
          .put(useDefaults, args.contains(ParseResult.WithDefaults))
          .put(commandLineBaseImage, args.collectFirst { case ParseResult.BaseImage(value) => value })
          .put(commandLinePlayVersion, args.collectFirst { case ParseResult.PlayVersion(value) => value })
          .put(commandLineScalaVersion, args.collectFirst { case ParseResult.ScalaVersion(value) => value })
          .put(commandLineJavaVersion, args.collectFirst { case ParseResult.JavaVersion(value) => value })
          .put(commandLinePlaySlickVersion, args.collectFirst { case ParseResult.playSlickVersion(value) => value })
          .put(commandLineSbtVersion, args.collectFirst { case ParseResult.SbtVersion(value) => value })
          .put(commandLineAddOsSuffix, args.collectFirst { case ParseResult.AddOsSuffix(value) => value })
          .put(commandLineDockerRegistry, args.collectFirst { case ParseResult.DockerRegistry(value) => value })
          .put(commandLineImageTag, args.collectFirst { case ParseResult.ImageTag(value) => value })

        Function.chain(
          Seq(
            inquireVersions, updateDependencies, updatePlugins, updateBuildProperties, updateSbtInit, runDockerBuild,
            runDockerPublish, resetDependencies
          )
        )(startState)
      }
    }

  }

  import autoImport.DockerSeedKeys.*

  private val osArch: Option[String] = Option(System.getProperty("os.arch"))

  private val inquireVersions = { state: State =>
    import versions.*
    state.log.info("### Inquiring versions")
    val defaultAddOsSuffix = "y" // for "yes"
    val useDefs = state.get(useDefaults).getOrElse(false)
    val base = readVersion(baseImage, "Base Docker Image [%s] : ", useDefs, state.get(commandLineBaseImage).flatten)
    val play = readVersion(playVersion, "Play! version [%s] : ", useDefs, state.get(commandLinePlayVersion).flatten)
    val scala = readVersion(scalaVersion, "Scala version [%s] : ", useDefs, state.get(commandLineScalaVersion).flatten)
    val java = readVersion(javaVersion, "Java version [%s] : ", useDefs, state.get(commandLineJavaVersion).flatten)
    val slick = readVersion(playSlickVersion, "Play-Slick version [%s] : ", useDefs,
      state.get(commandLinePlaySlickVersion).flatten)
    val sbt = readVersion(sbtVersion, "Sbt version [%s] : ", useDefs, state.get(commandLineSbtVersion).flatten)
    val addOsSuffix = readVersion(defaultAddOsSuffix, "Add os.arch suffix to image name (y/n) [%s] : ", useDefs,
      state.get(commandLineAddOsSuffix).flatten)
    val registry = readVersion(docker.registry, "Docker registry [%s] : ", useDefs,
      state.get(commandLineDockerRegistry).flatten)
    val imageTag = readVersion("", "Image tag (leave blank to use default) : ", useDefs,
      state.get(commandLineImageTag).flatten)

    val newState = state
      .put(desiredBaseImage, base)
      .put(desiredPlayVersion, play)
      .put(desiredScalaVersion, scala)
      .put(desiredJavaVersion, java)
      .put(desiredPlaySlickVersion, slick)
      .put(desiredSbtVersion, sbt)
      .put(desiredAddOsSuffix, addOsSuffix)
      .put(desiredDockerRegistry, registry)
      .put(desiredDockerImageTag, imageTag)

    newState.log.info(
      s"""Working with versions:
         |- base-image       => $base
         |- play             => $play
         |- scala            => $scala
         |- java             => $java
         |- play-slick       => $slick
         |- sbt              => $sbt
         |- registry         => $registry
         |- custom image tag => $imageTag
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

  private val updateSbtInit = { implicit state: State =>
    state.log.info("### Updating sbt-init.sh")
    val projectBase = Project.extract(state).get(baseDirectory)
    replacePlaceholders(
      placeholderFile = projectBase / "project" / "placeholders" / "sbt-init.sh",
      destinationFile = projectBase / "sbt-init.sh"
    )
  }

  private val runDockerBuild = { state: State =>
    state.log.info(s"### Building docker image for os.arch '${osArch.mkString}'")
    val log: ProcessLogger = processLogger(state)
    val imageTag: String = getDockerImageTag(state)
    val imageName: String = getAttributeKey(desiredBaseImage)(state)
    val process: ProcessBuilder = stringToProcess(s"docker build -t $imageTag --build-arg BASE_IMAGE=$imageName .")
    if (process ! log != 0) sys.error("Error building image")
    state
  }

  private val runDockerPublish = { state: State =>
    state.log.info("### Pushing docker image. This process will take a while depending on your internet connection")
    // The code to push a single image
    val log: ProcessLogger = processLogger(state)
    val process: ProcessBuilder = stringToProcess(s"docker push ${getDockerImageTag(state)}")
    if (process ! log != 0) sys.error("Error pushing docker image")
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
          .replaceAll("\\[java_version]", getAttributeKey(desiredJavaVersion))
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
    val baseImage: String = getAttributeKey(desiredBaseImage).replace(':', '-')
    val playVersion: String = getAttributeKey(desiredPlayVersion)
    val playSlickVersion = getAttributeKey(desiredPlaySlickVersion)
    val sbtVersion = getAttributeKey(desiredSbtVersion)
    val scalaVersion = getAttributeKey(desiredScalaVersion)
    val javaVersion = getAttributeKey(desiredJavaVersion)
    val registry = getAttributeKey(desiredDockerRegistry)
    val addOsSuffix = getAttributeKey(desiredAddOsSuffix).toLowerCase match {
      case "y" | "yes" => osArch
      case _ => None
    }
    val defaultTag = Seq(
      Some(s"play-$playVersion"),
      Some(s"sbt-$sbtVersion"),
      Some(s"scala-$scalaVersion"),
      Some(s"play-slick-$playSlickVersion"),
      Some(s"java-$javaVersion"),
      Some(s"$baseImage"),
      addOsSuffix
    ).flatten.mkString("-")
    val customTag = getAttributeKey(desiredDockerImageTag).trim
    val imageTag = if (customTag.isBlank) defaultTag else customTag

    s"$registry/play-dependencies-seed:$imageTag"
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

  override lazy val projectSettings: Seq[Setting[_]] = Seq[Setting[_]](commands += dockerSeedCommand)
}
