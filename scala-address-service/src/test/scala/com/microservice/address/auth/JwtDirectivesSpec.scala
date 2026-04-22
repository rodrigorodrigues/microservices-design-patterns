package com.microservice.address.auth

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.http.scaladsl.server.Directives._
import org.jose4j.jwt.JwtClaims
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.keys.HmacKey
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class JwtDirectivesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with JwtDirectives {

  def createToken(roles: Seq[String]): String = {
    val claims = new JwtClaims()
    claims.setExpirationTimeMinutesInTheFuture(10)
    claims.setClaim("authorities", roles.asJava)
    
    val jws = new JsonWebSignature()
    jws.setPayload(claims.toJson)
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256)
    // We skip signature verification in JwtDirectives so any key works
    jws.setKey(new HmacKey("secretsecretsecretsecretsecretsecretsecret".getBytes))
    jws.getCompactSerialization
  }

  val testRoute = authenticate { claims =>
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
  }

  "JwtDirectives" should "authenticate valid token" in {
    val token = createToken(Seq("ROLE_USER"))
    Get("/test-auth") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      responseAs[String] should include("Success")
    }
  }

  it should "reject request without token" in {
    Get("/test-auth") ~> testRoute ~> check {
      rejections should not be empty
    }
  }

  it should "authorize based on roles" in {
    val token = createToken(Seq("ROLE_ADMIN"))
    Get("/test-admin") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      responseAs[String] shouldBe "Admin Access"
    }
  }

  it should "reject unauthorized roles" in {
    val token = createToken(Seq("ROLE_USER"))
    Get("/test-admin") ~> addHeader("Authorization", s"Bearer $token") ~> testRoute ~> check {
      rejections should not be empty
    }
  }
}
