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

sealed abstract class Error extends Throwable with Product with Serializable

object Error {

  case object InvalidAccessToken extends Error {
    override def getMessage: String = "Invalid access token"
  }

  case object AccessTokenNotFound extends Error {
    override def getMessage: String = "Access token not found"
  }

  final case class UnsupportedSignatureAlgorithm(message: String) extends Error {
    override def getMessage: String = s"Unsupported JWT Signature algorithm ($message)"
  }

  final case class InvalidJwtClaims(cause: Throwable) extends Error {
    override def getMessage: String = s"Invalid JWT claims (${cause.getMessage})"
  }

}
