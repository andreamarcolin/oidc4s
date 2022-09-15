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

package oidc4s.http4s

import cats.MonadThrow
import cats.data.{Kleisli, OptionT}
import cats.syntax.all._
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthScheme, AuthedRoutes, Challenge, EntityEncoder, Request}
import pdi.jwt.JwtClaim
import sttp.model.Uri

import oidc4s.Error.{AccessTokenNotFound, InvalidAccessToken}
import oidc4s.{Error, OidcJwtVerifier}

object OidcJwtMiddleware {

  def apply[F[_]: MonadThrow, E, U](
      oidcJwtVerifier: OidcJwtVerifier[F],
      issuer: Uri,
      claimExtractor: JwtClaim => Either[Throwable, U],
      errorBody: Error => E
  )(implicit E: EntityEncoder[F, E]): AuthMiddleware[F, U] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val authUser: Kleisli[F, Request[F], Either[Error, U]] =
      Kleisli { request =>
        request
          .headers
          .get[`Authorization`]
          .collect {
            case Authorization(Token(AuthScheme.Bearer, token)) => token
          }
          .fold(AccessTokenNotFound.asLeft[U].leftWiden[Error].pure[F]) { token =>
            oidcJwtVerifier.verifyAndExtract[U](token, claimExtractor).map(_.leftMap(_ => InvalidAccessToken))
          }
      }

    val onFailure: AuthedRoutes[Error, F] =
      Kleisli { request =>
        OptionT.liftF(
          Unauthorized(
            `WWW-Authenticate`(Challenge("Bearer", issuer.toString)),
            errorBody(request.context)
          )
        )
      }

    AuthMiddleware[F, Error, U](authUser, onFailure)
  }

}
