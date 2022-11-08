oidc4s
===============

[![CI Status](https://github.com/andreamarcolin/oidc4s/workflows/ci/badge.svg)](https://github.com/andreamarcolin/oidc4s/actions) 
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org) <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a></br></br>

A [OpenID Connect](https://openid.net/specs/openid-connect-core-1_0-final.html) [JWT](https://tools.ietf.org/html/rfc7519) **verification** library supporting [JWK](https://tools.ietf.org/html/rfc7517), [JWA](https://tools.ietf.org/html/rfc7518), [JWE](https://tools.ietf.org/html/rfc7516) and configuration via [OpenID Connect Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html).

This library is based on [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/), [sttp](https://sttp.softwaremill.com/), [circe](https://circe.github.io/circe/) and [jwt-scala](https://github.com/jwt-scala/jwt-scala). 
First-class integration with [http4s](https://http4s.org/) is implemented by the dedicated `oidc4s-http4s` module via a http4s `AuthMiddleware`.

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

### Creating a verifier
```scala
import cats.effect._
import cats.syntax.all._
import oidc4s._
import org.http4s.ember.client.EmberClientBuilder
import sttp.client3._
import sttp.client3.http4s.Http4sBackend

for {
  client   <- EmberClientBuilder.default[IO].build.map(Http4sBackend.usingClient(_))
  verifier <- OidcJwtVerifier.create[IO](client, uri"http://localhost:8080/realms/test")
} yield verifier
```

> **Note**
> `OidcJwtVerifier.create` requires a log4cats `LoggerFactory` implicit instance (see [here](https://github.com/typelevel/log4cats#logging-using-capabilities) for details).  
> If your project already depends on log4cats-slf4j, you can just add the following import:  
> `import org.typelevel.log4cats.slf4j._`

### Verifying tokens (and extracting custom claims)
```scala
import io.circe.parser.parse

for {
  claims <- verifier.verifyAndExtract(token)
  content = parse(claims.content).flatMap(_.hcursor.get[String]("custom_claim"))
} yield content
```

### Http4s integration
```scala
import cats.effect._
import cats.syntax.all._
import io.circe._
import org.http4s.{Uri => _, _}
import org.http4s.dsl.io._
import org.http4s.server._
import sttp.client3._

val authedRoutes: AuthedRoutes[String, IO] =
  AuthedRoutes.of[String, IO] {
    case GET -> Root as userId => Ok(userId)
  }

val extractor: JwtClaim => Either[io.circe.Error, String] =
  c => parse(c.content).flatMap(_.hcursor.get[String]("custom_claim"))

val middleware: AuthMiddleware[IO, String] =
  OidcJwtMiddleware(verifier, uri"http://localhost:8080/realms/test", extractor, _.toString)

val routes: HttpRoutes[IO] = middleware(authedRoutes)
```

## Notes
Note that oidc4s is pre-1.0 software and is still undergoing active development. New versions are not binary compatible with prior versions, although in most cases user code will be source compatible.
See [here](https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html#early-semver-and-sbt-version-policy) for more details about the adopted version policy
