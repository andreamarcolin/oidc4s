addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")

lazy val scala212  = "2.12.11"
lazy val scala213  = "2.13.2"
lazy val mainScala = scala213
lazy val allScala  = Seq(scala212, mainScala)

val commonSettings = List(
  organization := "io.github.andreamarcolin",
  organizationName := "Andrea Marcolin",
  name := """http4s-oidc-verifier""",
  description := "A OpenID Connect token verifier for http4s",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := mainScala,
  crossScalaVersions := allScala,
  startYear := Some(2020),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  headerLicense := Some(HeaderLicense.ALv2("2020", "Andrea Marcolin")),
  resolvers += "Apache public" at "https://repository.apache.org/content/groups/public/",
  developers := List(
    Developer(
      "andreamarcolin",
      "Andrea Marcolin",
      "a.marcolin@outlook.com",
      url("https://github.com/andreamarcolin")
    )
  )
)

lazy val root = (project in file("."))
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    skip in publish := true
  )
  .aggregate(core)

lazy val core = (project in file("core"))
  .settings(
    name := "http4s-oidc-verifier",
    libraryDependencies ++= Build.dependencies
  )
  .settings(commonSettings: _*)