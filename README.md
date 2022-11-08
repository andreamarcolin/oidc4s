oidc4s
===============

[![CI Status](https://github.com/andreamarcolin/oidc4s/workflows/ci/badge.svg)](https://github.com/andreamarcolin/oidc4s/actions) 
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org) <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a></br></br>

A [OpenID Connect](https://openid.net/specs/openid-connect-core-1_0-final.html) [JWT](https://tools.ietf.org/html/rfc7519) **verification** library supporting [JWK](https://tools.ietf.org/html/rfc7517), [JWA](https://tools.ietf.org/html/rfc7518), [JWE](https://tools.ietf.org/html/rfc7516) and configuration via [OpenID Connect Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html).
and also providing first-class integration with [http4s](https://http4s.org/) through a custom middleware.

This library is based on [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/), [sttp](https://sttp.softwaremill.com/), [circe](https://circe.github.io/circe/) and [jwt-scala](https://github.com/jwt-scala/jwt-scala). 
First-class integration with [http4s](https://http4s.org/) is implemented in the dedicated `oidc4s-http4s` module via a http4s `AuthMiddleware`.

Published both for Scala 3 and Scala 2.13.

## Getting Started
To use oidc4s you need to add the following to your `build.sbt`:
```scala
libraryDependencies ++= Seq(
  // Start with this one
  "io.github.andreamarcolin" %% "oidc4s-core" % "<version>",

  // If you are planning to make use of the http4s integration, also add this one
  "io.github.andreamarcolin" %% "oidc4s-http4s" % "<version>"
)
```

## Notes

Note that oidc4s is pre-1.0 software and is still undergoing active development. New versions are not binary compatible with prior versions, although in most cases user code will be source compatible.
See [here](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy) for more details about the adopted version policy
