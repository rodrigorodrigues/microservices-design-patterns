package com.microservice.address.auth

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{Directive1, AuthorizationFailedRejection}
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.JwtClaims
import scala.util.{Try, Success, Failure}
import scala.jdk.CollectionConverters._

trait JwtDirectives {
  // Simple JWT Consumer that skips signature verification for this demo,
  // in production this would use a public key/JWKS endpoint.
  private val jwtConsumer = new JwtConsumerBuilder()
    .setSkipSignatureVerification() 
    .setRequireExpirationTime()
    .build()

  def authenticate: Directive1[JwtClaims] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        Try(jwtConsumer.processToClaims(token)) match {
          case Success(claims) => provide(claims)
          case Failure(_)      => reject(AuthorizationFailedRejection)
        }
      case _ => reject(AuthorizationFailedRejection)
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
