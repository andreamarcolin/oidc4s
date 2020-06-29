import sbt._

object Build {

  object Versions {
    val http4s     = "0.21.6"
    val cats       = "2.1.1"
    val catsEffect = "2.1.3"
    val circe      = "0.13.0"
  }

  val dependencies: Seq[ModuleID] = List(
    "org.http4s"        %% "http4s-dsl"           % Versions.http4s,
    "org.http4s"        %% "http4s-blaze-server"  % Versions.http4s,
    "org.typelevel"     %% "cats-core"            % Versions.cats,
    "org.typelevel"     %% "cats-effect"          % Versions.catsEffect,
    "io.circe"          %% "circe-core"           % Versions.circe,
    "io.circe"          %% "circe-generic"        % Versions.circe,
    "io.circe"          %% "circe-generic-extras" % Versions.circe,
    "com.github.j5ik2o" %% "base64scala"          % "1.0.5",
    compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )
}
