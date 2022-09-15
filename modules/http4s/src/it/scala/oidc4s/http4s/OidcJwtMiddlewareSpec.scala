package oidc4s.http4s

import cats.effect._
import cats.implicits._
import io.circe.parser.parse
import io.circe.Json
import oidc4s.Error.{AccessTokenNotFound, InvalidAccessToken}
import oidc4s.OidcJwtVerifier
import org.http4s.{Uri => _, _}
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{`WWW-Authenticate`, Authorization}
import pdi.jwt.JwtClaim
import sttp.client3._
import sttp.client3.circe.asJson
import sttp.client3.http4s.Http4sBackend
import sttp.model.{Method => _, Headers => _, _}
import weaver._

object OidcJwtMiddlewareSpec extends IOSuite {
  override type Res = (SttpBackend[IO, Any], OidcJwtVerifier[IO])

  val issuerUri: Uri = uri"http://localhost:8080/realms/test"

  override def sharedResource: Resource[IO, (SttpBackend[IO, Any], OidcJwtVerifier[IO])] =
    EmberClientBuilder
      .default[IO]
      .build
      .map(Http4sBackend.usingClient(_))
      .flatMap(client => OidcJwtVerifier.create(client, issuerUri).tupleLeft(client))

  val getToken: SttpBackend[IO, Any] => IO[String] =
    httpClient =>
      basicRequest
        .post(uri"$issuerUri/protocol/openid-connect/token")
        .body(
          "client_id"  -> "test",
          "grant_type" -> "password",
          "username"   -> "test",
          "password"   -> "test"
        )
        .response(asJson[Json])
        .send(httpClient)
        .map(_.body)
        .rethrow
        .map(_.hcursor.get[String]("access_token"))
        .rethrow

  val expiredToken: String =
    """eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1aEtra1JzR0hLeHZD
      |cGpBN1dLcm9sQXo4dXVaUmIzTzVLSGwyYlktZzJjIn0.eyJleHAiOjE2NjMyMzA0Nzgs
      |ImlhdCI6MTY2MzIzMDE3OCwianRpIjoiMTM0YWNmZjMtMjJkMC00OWEwLWJiNTItODI5
      |MDg2ZGE2YWY0IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy90ZXN0
      |IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjE3YjZmNWJhLTFhYjItNDNkYy04NmYyLWFi
      |ZjMzNmNhMmQ5NCIsInR5cCI6IkJlYXJlciIsImF6cCI6InRlc3QiLCJzZXNzaW9uX3N0
      |YXRlIjoiZjQ2ZWMzMmYtYjY2My00ZjA3LWIwZWMtNTJhZGJiYTRhYzBmIiwiYWNyIjoi
      |MSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWZhdWx0LXJvbGVzLXRlc3QiLCJv
      |ZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nl
      |c3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1h
      |Y2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9m
      |aWxlIiwic2lkIjoiZjQ2ZWMzMmYtYjY2My00ZjA3LWIwZWMtNTJhZGJiYTRhYzBmIiwi
      |ZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJ0ZXN0IHRlc3QiLCJodHRwOi8vbG9j
      |YWxob3N0OjgwODAvb3JnX2lkIjoib3JnMSIsInByZWZlcnJlZF91c2VybmFtZSI6InRl
      |c3QiLCJnaXZlbl9uYW1lIjoidGVzdCIsImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC91c2Vy
      |X2lkIjoidXNlcjEiLCJmYW1pbHlfbmFtZSI6InRlc3QiLCJlbWFpbCI6InRlc3RAdGVz
      |dC5jb20ifQ.Rht-XH7gXu0t74ef2saxD3PwCnylAZvXkK__MH3Wt0sG9yITeGlqSHbF2
      |eUOBjQohBXU9Cd1KPgGEpVY3kLxKSRbpjfJmYPKp44HE69QHEeqKQfTsMH4z5GlnefMc
      |6gReDvf9K9AOSoQzO0dVNzRXohp9hifduUwSKbzatFzhBc3OEOT2zJ2imAqWgQzpp3ob
      |tUiBnmIhQ3LNq9Ph6bEHumiMw3M2i_Tdxm7XltckmenfihGSC3DNMaEOQR3gthf13-A3
      |KLTg14zvicUNy2ocyXP133NUOMmvNHPj8Z-ARQEX3EVBSoaip6Cwuj5Uq2QfEjM63HAy
      |4uY1rPmLS9xmA""".stripMargin.replaceAll("\n", "")

  test("a valid token gets verified and claims get extracted successfully") { clientAndVerifier =>
    for {
      token     <- getToken(clientAndVerifier._1)
      extractor  = (c: JwtClaim) =>
                     parse(c.content).flatMap(j =>
                       (
                         j.hcursor.get[String]("http://localhost:8080/org_id"),
                         j.hcursor.get[String]("http://localhost:8080/user_id")
                       ).tupled
                     )
      routes     = AuthedRoutes.of[(String, String), IO] { case GET -> Root as user => Ok(s"${user._1}-${user._2}") }
      middleware = OidcJwtMiddleware(clientAndVerifier._2, issuerUri, extractor, _.toString)
      authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, token))
      resp      <- middleware(routes).orNotFound(Request[IO](Method.GET, headers = Headers(authHeader)))
      respBody  <- resp.as[String]
    } yield expect.all(
      Status.Ok == resp.status,
      respBody == "org1-user1"
    )
  }

  test("a request without a token returns 401 Unauthorized with a WWW-Authenticate header") { clientAndVerifier =>
    for {
      routes    <- AuthedRoutes.of[Unit, IO] { case GET -> Root as _ => Ok() }.pure[IO]
      extractor  = (_: JwtClaim) => Right(())
      middleware = OidcJwtMiddleware(clientAndVerifier._2, issuerUri, extractor, _.getMessage)
      resp      <- middleware(routes).orNotFound(Request[IO](Method.GET))
      respBody  <- resp.as[String]
    } yield expect.all(
      Status.Unauthorized == resp.status,
      resp.headers.contains[`WWW-Authenticate`],
      resp.headers.get[`WWW-Authenticate`].exists(_.values.exists(_ == Challenge("Bearer", issuerUri.toString))),
      respBody == AccessTokenNotFound.getMessage
    )
  }

  test("a request with an invalid token returns 401 Unauthorized with a WWW-Authenticate header") { clientAndVerifier =>
    for {
      routes    <- AuthedRoutes.of[Unit, IO] { case GET -> Root as _ => Ok() }.pure[IO]
      extractor  = (_: JwtClaim) => Right(())
      middleware = OidcJwtMiddleware(clientAndVerifier._2, issuerUri, extractor, _.getMessage)
      authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, expiredToken))
      resp      <- middleware(routes).orNotFound(Request[IO](Method.GET, headers = Headers(authHeader)))
      respBody  <- resp.as[String]
    } yield expect.all(
      Status.Unauthorized == resp.status,
      resp.headers.contains[`WWW-Authenticate`],
      resp.headers.get[`WWW-Authenticate`].exists(_.values.exists(_ == Challenge("Bearer", issuerUri.toString))),
      respBody == InvalidAccessToken.getMessage
    )
  }

}
