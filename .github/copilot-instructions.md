# Copilot Instructions

## Purpose

This tool generates Docker "seed" images that pre-cache Play! framework (Scala) dependencies to drastically speed up
Docker build times for Play! applications (~10x improvement). It is an SBT project whose only output is a Docker image
pushed to a registry.

## Commands

```shell
# Interactive (prompts for each version)
sbt dockerSeed

# Use all defaults from project/versions.scala and project/docker.scala
sbt "dockerSeed with-defaults"

# Non-interactive with explicit values
sbt "dockerSeed base-image debian:trixie-20260610-slim play-version 3.0.11 scala-version 2.13.18 java-version 21.0.11-amzn play-slick-version 6.2.0 sbt-version 1.12.11 docker-registry myregistry"

# Mix: use defaults but override specific values
sbt "dockerSeed with-defaults sbt-version 1.12.11 docker-registry myregistry"

# Build the image locally without pushing (skip publish)
sbt "dockerSeed with-defaults skip-publish"

# Override the generated image tag entirely
sbt "dockerSeed with-defaults docker-registry myregistry image-tag my-custom-tag"

# Build without the os.arch suffix
sbt "dockerSeed with-defaults docker-registry myregistry add-os-suffix n"
```

The working directory must have a clean git state for non-placeholder files — `resetDependencies` runs `git reset --hard HEAD` at the end to restore generated files. **Always commit your changes before running `dockerSeed`**, or they will be lost.

There are no automated tests in this project.

## Architecture

The core logic lives in `project/DockerSeedPlugin.scala` — an SBT `AutoPlugin` that registers the `dockerSeed` command.
When run, it executes this pipeline in sequence:

1. **`inquireVersions`** — Collects versions interactively, from CLI args, or from defaults in
   `project/versions.scala` / `project/docker.scala`
2. **`updateDependencies`** — Renders `project/placeholders/dependencies.sbt` → `dependencies.sbt`
3. **`updatePlugins`** — Renders `project/placeholders/plugins.sbt` → `project/plugins.sbt`
4. **`updateBuildProperties`** — Renders `project/placeholders/build.properties` → `project/build.properties`
5. **`updateSbtInit`** — Renders `project/placeholders/sbt-init.sh` → `sbt-init.sh`
6. **`runDockerBuild`** — Runs `docker build --provenance=false -t <tag> --build-arg BASE_IMAGE=<image> .`
7. **`runDockerPublish`** — Runs `docker push <tag>`
8. **`resetDependencies`** — Runs `git reset --hard HEAD` to restore modified files

The `Dockerfile` uses SDKMAN to install Java and SBT, then runs `sbt clean update` (via `sbt-init.sh`) to pull and cache
all dependencies into the image layer.

## Key Conventions

### Placeholder syntax

Files under `project/placeholders/` use `[token]` syntax for version substitution:

- `[play_version]`, `[scala_version]`, `[java_version]`, `[sbt_version]`, `[play_slick_version]`

These are the canonical templates; the root-level `dependencies.sbt`, `sbt-init.sh`, `project/plugins.sbt`, and
`project/build.properties` are generated files that get reset after each build.

### Default versions

All defaults live in two files:

- `project/versions.scala` — component versions (Play, Scala, Java, SBT, play-slick, base image)
- `project/docker.scala` — default Docker registry (`changeme`)

### Image tag format

```
$registry/play-dependencies-seed:play-$playVersion-sbt-$sbtVersion-scala-$scalaVersion-play-slick-$playSlickVersion-java-$javaVersion-$baseImage[-$osArch]
```

The `os.arch` suffix is appended when `add-os-suffix` is `y`/`yes` (default). Use this to build arch-specific images 
(e.g., `amd64`, `aarch64`) before combining into a multiarch manifest.

### Play plugin version alignment

The `sbt-plugin` version in `project/placeholders/plugins.sbt` (and the checked-in `project/plugins.sbt`) must stay in
sync with the Play version in `project/versions.scala`. There is a comment in `project/plugins.sbt` as a reminder.

### Dockerfile is Debian-specific

The `Dockerfile` uses `apt-get`. For non-Debian distros, replace with `yum` or `apk` as needed.

### Vulnerability exclusions

The placeholder `dependencies.sbt` and `plugins.sbt` intentionally exclude known-vulnerable transitive dependencies 
(`jackson-databind`, `guava`, `ant`, `commons-compress`, `protobuf-java`). These are expected to be re-added by the
consuming application's own dependency declarations.
