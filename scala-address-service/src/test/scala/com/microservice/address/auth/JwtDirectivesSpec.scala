package com.microservice.address.auth

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.jose4j.jwt.JwtClaims
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.keys.HmacKey
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class JwtDirectivesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest {
  val secret = "very-secret-key-that-is-at-least-32-characters-long"
  val jwtDirectives = new JwtDirectives(None, Some(secret))
  import jwtDirectives._

  def createToken(roles: Seq[String], expired: Boolean = false): String = {
    val claims = new JwtClaims()
    claims.setExpirationTimeMinutesInTheFuture(if (expired) -60f else 60f)
    claims.setClaim("authorities", roles.asJava)
    
    val jws = new JsonWebSignature()
    jws.setPayload(claims.toJson)
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256)
    jws.setKey(new HmacKey(secret.getBytes("UTF-8")))
    jws.getCompactSerialization
  }

  val testRoute = Route.seal(authenticate { claims =>
    path("test-auth") {
      get {
        complete(s"Success: ${claims.getClaimValue("authorities")}")
      }
    } ~
    authorizeRoles("ROLE_ADMIN")(claims) { _ =>
      path("test-admin") {
        get {
          complete("Admin Access")
        }
      }
    }
  })

  "JwtDirectives" should "authenticate valid token" in {
    val token = createToken(Seq("ROLE_USER"))
    Get("/test-auth") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] should include("Success")
    }
  }

  it should "reject expired token with 401" in {
    val token = createToken(Seq("ROLE_USER"), expired = true)
    Get("/test-auth") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      status shouldBe StatusCodes.Unauthorized
    }
  }

  it should "reject request without token with 401" in {
    Get("/test-auth") ~> testRoute ~> check {
      status shouldBe StatusCodes.Unauthorized
    }
  }

  it should "authorize based on roles" in {
    val token = createToken(Seq("ROLE_ADMIN"))
    Get("/test-admin") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "Admin Access"
    }
  }

  it should "reject unauthorized roles with 403" in {
    val token = createToken(Seq("ROLE_USER"))
    Get("/test-admin") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }
}
