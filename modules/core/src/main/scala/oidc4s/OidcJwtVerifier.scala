/*
 * Copyright 2022 Andrea Marcolin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oidc4s

import java.security.PublicKey

import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

import cats.Monad
import cats.data.EitherT
import cats.effect.syntax.spawn._
import cats.effect.{Ref, Resource, Temporal}
import cats.syntax.all._
import com.chatwork.scala.jwk.{AssymetricJWK, JWK, JWKSet, KeyId}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import pdi.jwt.{JwtCirce, JwtClaim, JwtHeader, JwtOptions}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{HeaderNames, Uri}

import oidc4s.Error._

trait OidcJwtVerifier[F[_]] {
  def verifyAndExtract[C](jwt: String, claimExtractor: JwtClaim => Either[Throwable, C]): F[Either[Error, C]]
}

object OidcJwtVerifier {

  private def apply[F[_]: Monad](issuer: Uri, jwksCache: Ref[F, JWKSet]): OidcJwtVerifier[F] =
    new OidcJwtVerifier[F] {

      override def verifyAndExtract[C](jwt: String, extractor: JwtClaim => Either[Throwable, C]): F[Either[Error, C]] =
        (for {
          jwtHeader <- EitherT.fromEither[F](decodeAndValidateJwtHeader(jwt))
          keyId     <- EitherT.fromOption[F](jwtHeader.keyId.map(KeyId(_)), InvalidAccessToken)
          jwks      <- EitherT.right[Error](jwksCache.get)
          jwk       <- EitherT.fromOption[F](jwks.keyByKeyId(keyId), InvalidAccessToken)
          publicKey <- EitherT.fromEither[F](getPublicKeyFromJwk(jwk))
          claims    <- EitherT.fromEither[F](decodeAndValidateJwt(jwt, publicKey, issuer))
          userRepr  <- EitherT.fromEither[F](extractor(claims).leftMap[Error](InvalidJwtClaims.apply))
        } yield userRepr).value

      private def decodeAndValidateJwtHeader(jwt: String): Either[Error, JwtHeader] =
        for {
          options <- JwtOptions(signature = false, expiration = false, notBefore = false).asRight[Error]
          header  <- JwtCirce.decodeAll(jwt, options).toEither.bimap(_ => InvalidAccessToken, _._1)
          _       <- header.typ.filter(_ == "JWT").toRight(InvalidAccessToken)
        } yield header

      private def getPublicKeyFromJwk(jwk: JWK): Either[Error, PublicKey] =
        jwk match {
          case a: AssymetricJWK => a.toPublicKey.leftMap(e => UnsupportedSignatureAlgorithm(e.message))
          case s                => UnsupportedSignatureAlgorithm(s.keyType.entryName).asLeft[PublicKey]
        }

      private def decodeAndValidateJwt(jwt: String, publicKey: PublicKey, issuer: Uri): Either[Error, JwtClaim] =
        for {
          body <- JwtCirce.decode(jwt, publicKey).toEither.leftMap(_ => InvalidAccessToken)
          _    <- body.issuer.flatMap(Uri.parse(_).toOption).filter(_ == issuer).toRight(InvalidAccessToken)
        } yield body

    }

  def create[F[_]: Temporal](
      httpClient: SttpBackend[F, Any],
      issuerUri: Uri,
      fallbackJWKRefreshInterval: FiniteDuration = 1.minute
  ): Resource[F, OidcJwtVerifier[F]] = {

    implicit val uriDecoder: Decoder[Uri]                                 = Decoder.decodeString.emap(Uri.parse)
    implicit val openIDConfigurationDecoder: Decoder[OpenIDConfiguration] = deriveDecoder

    val getOpenIDConfiguration: F[OpenIDConfiguration] =
      basicRequest
        .get(issuerUri.addPath(".well-known", "openid-configuration"))
        .response(asJson[OpenIDConfiguration])
        .send(httpClient)
        .map(_.body)
        .rethrow

    val extractMaxAge: String => Option[FiniteDuration] =
      headerValue => {
        val pattern = "max-age=(d+)".r
        headerValue
          .split(",")
          .map(_.trim.toLowerCase)
          .collectFirst({ case pattern(maxAgeSeconds) => maxAgeSeconds })
          .flatMap(_.toLongOption.map(FiniteDuration(_, SECONDS)))
      }

    val getJWKSet: Uri => F[(JWKSet, Option[FiniteDuration])] =
      basicRequest
        .get(_)
        .response(asJson[JWKSet])
        .send(httpClient)
        .map(r => r.body.tupleRight(r.header(HeaderNames.CacheControl).flatMap(extractMaxAge)))
        .rethrow

    val refreshInterval: Option[FiniteDuration] => FiniteDuration =
      _.getOrElse(fallbackJWKRefreshInterval)

    val periodicallyRefreshCache: (Uri, Option[FiniteDuration], Ref[F, JWKSet]) => F[Unit] =
      (jwksUri, jwkSetMaxAge, jwkSetCache) =>
        Monad[F].iterateForeverM(refreshInterval(jwkSetMaxAge)) { initialSleep =>
          for {
            _         <- Temporal[F].sleep(initialSleep)
            newJWKSet <- getJWKSet(jwksUri)
            _         <- jwkSetCache.set(newJWKSet._1)
          } yield refreshInterval(newJWKSet._2)
        }

    for {
      openidConf <- Resource.eval(getOpenIDConfiguration)
      jwks       <- Resource.eval(getJWKSet(openidConf.jwks_uri))
      jwksCache  <- Resource.eval(Ref.of[F, JWKSet](jwks._1))
      _          <- periodicallyRefreshCache(openidConf.jwks_uri, jwks._2, jwksCache).background
    } yield OidcJwtVerifier(openidConf.issuer, jwksCache)
  }

}
