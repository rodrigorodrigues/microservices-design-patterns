package com.microservice.address.auth

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, Directive1}
import org.apache.pekko.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.apache.pekko.http.scaladsl.model.headers.HttpChallenges
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwk.HttpsJwks
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.jose4j.keys.HmacKey
import scala.util.{Try, Success, Failure}
import scala.jdk.CollectionConverters._
import org.slf4j.LoggerFactory

class JwtDirectives(jwksUrl: Option[String], jwtKeyValue: Option[String], allowUnverified: Boolean = false) {
  private val logger = LoggerFactory.getLogger(classOf[JwtDirectives])
  private val challenge = HttpChallenges.oAuth2("scala-address-service")

  private val jwtConsumerBuilder = new JwtConsumerBuilder()
    .setRequireExpirationTime()
    .setAllowedClockSkewInSeconds(30)

  jwksUrl match {
    case Some(url) if url.nonEmpty =>
      logger.info(s"Configuring JWT validation with JWKS: $url")
      val httpsJwks = new HttpsJwks(url)
      jwtConsumerBuilder.setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(httpsJwks))
    case _ =>
      jwtKeyValue match {
        case Some(keyValue) if keyValue.nonEmpty =>
          logger.info("Configuring JWT validation with com.microservice.authentication.jwt.key-value")
          jwtConsumerBuilder.setVerificationKey(new HmacKey(keyValue.getBytes("UTF-8")))
        case _ if allowUnverified =>
          logger.warn("No JWKS or key-value configured; allow-unverified override is set. Skipping JWT signature verification (UNSAFE)!")
          jwtConsumerBuilder.setSkipSignatureVerification()
        case _ =>
          throw new IllegalStateException(
            "No JWT verification method configured. Set com.microservice.authentication.jwk.key-set-uri or " +
              "com.microservice.authentication.jwt.key-value, or explicitly opt into insecure mode with " +
              "com.microservice.authentication.jwt.allow-unverified=true."
          )
      }
  }

  private val jwtConsumer = jwtConsumerBuilder.build()

  def authenticate: Directive1[JwtClaims] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        Try(jwtConsumer.processToClaims(token)) match {
          case Success(claims) => provide(claims)
          case Failure(e) =>
            logger.debug(s"JWT validation failed: ${e.getMessage}")
            reject(AuthenticationFailedRejection(CredentialsRejected, challenge))
        }
      case Some(_) =>
        reject(AuthenticationFailedRejection(CredentialsRejected, challenge))
      case None =>
        reject(AuthenticationFailedRejection(CredentialsMissing, challenge))
    }
  }

  def authorizeRoles(roles: String*)(claims: JwtClaims): Directive1[JwtClaims] = {
    val authorities = Option(claims.getClaimValue("authorities")) match {
      case Some(list: java.util.List[?]) => list.asScala.map(_.toString).toSeq
      case _ => Seq.empty
    }

    if (roles.exists(authorities.contains)) provide(claims)
    else reject(AuthorizationFailedRejection)
  }
}
