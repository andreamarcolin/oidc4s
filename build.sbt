addCommandAlias("fmt", "all scalafmtSbt scalafmtAll; all scalafixAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll; all scalafixAll --check")

val catsV       = "2.8.0"
val catsEffectV = "3.3.14"
val circeV      = "0.14.2"
val http4sV     = "0.23.15"
val sttpV       = "3.7.6"
val jwtV        = "9.1.1"
val jwkV        = "1.2.24"

val cats         = "org.typelevel"                 %% "cats-core"     % catsV
val catsEffect   = "org.typelevel"                 %% "cats-effect"   % catsEffectV
val circeCore    = "io.circe"                      %% "circe-core"    % circeV
val circeGeneric = "io.circe"                      %% "circe-generic" % circeV
val http4sCore   = "org.http4s"                    %% "http4s-core"   % http4sV
val http4sDsl    = "org.http4s"                    %% "http4s-dsl"    % http4sV
val http4sServer = "org.http4s"                    %% "http4s-server" % http4sV
val http4sCirce  = "org.http4s"                    %% "http4s-circe"  % http4sV
val sttp         = "com.softwaremill.sttp.client3" %% "core"          % sttpV
val sttpCirce    = "com.softwaremill.sttp.client3" %% "circe"         % sttpV
val jwtCirce     = "com.github.jwt-scala"          %% "jwt-circe"     % jwtV
val jwk          = "com.chatwork"                  %% "scala-jwk"     % jwkV

inThisBuild(
  List(
    organization                           := "io.github.andreamarcolin",
    ThisBuild / semanticdbEnabled          := true,
    ThisBuild / semanticdbVersion          := scalafixSemanticdb.revision,
    ThisBuild / scalafixDependencies       := List("com.github.liancheng" %% "organize-imports" % "0.6.0"),
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
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

val scala213       = "2.13.8"
val scala3         = "3.1.3"
lazy val mainScala = scala213
lazy val allScala  = Seq(scala213, scala3)

lazy val common = List(
  scalaVersion       := mainScala,
  crossScalaVersions := allScala,
  startYear          := Some(2022),
  headerLicense      := Some(HeaderLicense.ALv2("2022", "Andrea Marcolin")),
  tpolecatScalacOptions ~= {
    _.filterNot(f => f == ScalacOptions.warnUnusedLocals || f == ScalacOptions.warnUnusedParams)
  }
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

lazy val core = project
  .in(file("modules/core"))
  .settings(
    common,
    name := "oidc4s-core",
    libraryDependencies ++= List(
      cats,
      catsEffect,
      circeCore,
      circeGeneric,
      sttp,
      sttpCirce,
      jwtCirce,
      jwk
      // compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
  )

lazy val http4s = project
  .in(file("modules/http4s"))
  .dependsOn(core)
  .settings(
    common,
    name := "oidc4s-http4s",
    libraryDependencies ++= List(
      http4sCore,
      http4sDsl,
      http4sServer,
      http4sCirce
      // compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
  )
