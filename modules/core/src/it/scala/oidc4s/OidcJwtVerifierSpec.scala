package oidc4s

import cats.effect.{IO, Resource}
import cats.syntax.all._
import io.circe.parser.parse
import io.circe.Json
import oidc4s.Error._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.noop.NoOpFactory
import org.typelevel.log4cats.LoggerFactory
import pdi.jwt.JwtClaim
import sttp.client3._
import sttp.client3.circe.asJson
import sttp.client3.http4s.Http4sBackend
import sttp.model.Uri
import weaver._

object OidcJwtVerifierSpec extends IOSuite {

  override type Res = SttpBackend[IO, Any]

  override def sharedResource: Resource[IO, SttpBackend[IO, Any]] =
    EmberClientBuilder.default[IO].build.map(Http4sBackend.usingClient(_))

  implicit val logging: LoggerFactory[IO] = NoOpFactory[IO]

  val issuerUri: Uri = uri"http://localhost:8080/realms/test"

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

  test("a valid token gets verified") { client =>
    for {
      token <- getToken(client)
      res   <- OidcJwtVerifier.create(client, issuerUri).use(_.verifyAndExtract(token))
    } yield expect(res.isRight)
  }

  test("claims get extracted successfully") { client =>
    for {
      token <- getToken(client)
      res   <- OidcJwtVerifier.create(client, issuerUri).use(_.verifyAndExtract(token))
    } yield expect.same(Right("org1" -> "user1"), res)
  }

  test("an expired token fails verification") { client =>
    OidcJwtVerifier
      .create(client, issuerUri)
      .use(_.verifyAndExtract(expiredToken))
      .map(res => expect.same(Left(InvalidAccessToken), res))
  }
}
