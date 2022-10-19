addCommandAlias("fmt", "all scalafmtSbt scalafmtAll; all scalafixAll; all headerCreate")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll; all scalafixAll --check; all headerCheck")

val catsV       = "2.8.0"
val catsEffectV = "3.3.14"
val circeV      = "0.14.3"
val http4sV     = "0.23.16"
val sttpV       = "3.8.0"
val jwtV        = "9.1.1"
val jwkV        = "1.2.24"
val weaverV     = "0.8.0"
val slf4jV      = "1.7.36"

val cats         = "org.typelevel"                 %% "cats-core"           % catsV
val catsEffect   = "org.typelevel"                 %% "cats-effect"         % catsEffectV
val circeCore    = "io.circe"                      %% "circe-core"          % circeV
val circeGeneric = "io.circe"                      %% "circe-generic"       % circeV
val circeParser  = "io.circe"                      %% "circe-parser"        % circeV
val http4sCore   = "org.http4s"                    %% "http4s-core"         % http4sV
val http4sDsl    = "org.http4s"                    %% "http4s-dsl"          % http4sV
val http4sServer = "org.http4s"                    %% "http4s-server"       % http4sV
val http4sClient = "org.http4s"                    %% "http4s-ember-client" % http4sV
val http4sCirce  = "org.http4s"                    %% "http4s-circe"        % http4sV
val sttp         = "com.softwaremill.sttp.client3" %% "core"                % sttpV
val sttpHttp4s   = "com.softwaremill.sttp.client3" %% "http4s-backend"      % sttpV
val sttpCirce    = "com.softwaremill.sttp.client3" %% "circe"               % sttpV
val jwtCirce     = "com.github.jwt-scala"          %% "jwt-circe"           % jwtV
val jwk          = "com.chatwork"                  %% "scala-jwk"           % jwkV
val weaver       = "com.disneystreaming"           %% "weaver-cats"         % weaverV
val sl4fjNop     = "org.slf4j"                      % "slf4j-nop"           % slf4jV

inThisBuild(
  List(
    organization                           := "io.github.andreamarcolin",
    homepage                               := Some(url("https://github.com/andreamarcolin/oidc4s")),
    ThisBuild / semanticdbEnabled          := true,
    ThisBuild / semanticdbVersion          := scalafixSemanticdb.revision,
    ThisBuild / scalafixDependencies       := List("com.github.liancheng" %% "organize-imports" % "0.6.0"),
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    ThisBuild / versionScheme              := Some("early-semver"),
    sonatypeCredentialHost                 := "s01.oss.sonatype.org",
    sonatypeRepository                     := "https://s01.oss.sonatype.org/service/local",
    licenses                               := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers                             := List(
      Developer(
        "andreamarcolin",
        "Andrea Marcolin",
        "a.marcolin@outlook.com",
        url("https://github.com/andreamarcolin")
      )
    )
  )
)

val scala213       = "2.13.10"
val scala3         = "3.2.0"
lazy val mainScala = scala213
lazy val allScala  = Seq(scala213, scala3)

lazy val common = List(
  scalaVersion       := mainScala,
  crossScalaVersions := allScala,
  organizationName   := "Andrea Marcolin",
  startYear          := Some(2022),
  headerLicense      := Some(HeaderLicense.ALv2("2022", "Andrea Marcolin")),
  tpolecatScalacOptions ~= {
    _.filterNot(f => f == ScalacOptions.warnUnusedLocals || f == ScalacOptions.warnUnusedParams)
  },
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

lazy val root = project
  .in(file("."))
  .aggregate(core, http4s)
  .settings(
    name               := "oidc4s",
    crossScalaVersions := Nil,
    publish            := {},
    publishLocal       := {},
    publishArtifact    := false,
    publish / skip     := true
  )
  .disablePlugins(HeaderPlugin) // see https://github.com/sbt/sbt-header/issues/153

lazy val core = project
  .in(file("modules/core"))
  .configs(IntegrationTest extend Test)
  .settings(
    common,
    Defaults.itSettings,
    name := "oidc4s-core",
    libraryDependencies ++= List(
      cats,
      catsEffect,
      circeCore,
      circeGeneric,
      sttp,
      sttpCirce,
      jwtCirce,
      jwk,
      weaver       % Test,
      sttpHttp4s   % Test,
      http4sClient % Test,
      circeParser  % Test,
      sl4fjNop     % Test
    )
  )

lazy val http4s = project
  .in(file("modules/http4s"))
  .dependsOn(core)
  .configs(IntegrationTest extend Test)
  .settings(
    common,
    Defaults.itSettings,
    name := "oidc4s-http4s",
    libraryDependencies ++= List(
      http4sCore,
      http4sDsl,
      http4sServer,
      http4sCirce,
      weaver       % Test,
      sttpHttp4s   % Test,
      http4sClient % Test,
      circeParser  % Test,
      sl4fjNop     % Test
    )
  )
