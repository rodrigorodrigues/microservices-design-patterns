package com.microservice.address.routes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import com.microservice.address.repository.AddressRepository
import com.microservice.address.models.Address
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import reactor.core.publisher.{Flux, Mono}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import com.microservice.address.models.AddressJsonProtocol._
import io.circe.generic.auto._
import org.apache.pekko.http.scaladsl.marshalling.ToEntityMarshaller
import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.model.ContentTypes
import org.apache.pekko.http.scaladsl.model.HttpEntity
import io.circe.syntax._
import org.jose4j.jwt.JwtClaims
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.keys.HmacKey
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class AddressRoutesSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with JsonSupport {

  implicit val addressMarshaller: ToEntityMarshaller[Address] = 
    Marshaller.opaque { a =>
      HttpEntity(ContentTypes.`application/json`, a.asJson.noSpaces)
    }

  val mockRepository = mock(classOf[AddressRepository])
  val addressRoutes = new AddressRoutes(mockRepository)
  val route = addressRoutes.routes

  def createToken(roles: Seq[String]): String = {
    val claims = new JwtClaims()
    claims.setExpirationTimeMinutesInTheFuture(10)
    claims.setClaim("authorities", roles.asJava)
    val jws = new JsonWebSignature()
    jws.setPayload(claims.toJson)
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256)
    jws.setKey(new HmacKey("secretsecretsecretsecretsecretsecretsecret".getBytes))
    jws.getCompactSerialization
  }

  val adminToken = createToken(Seq("ROLE_ADMIN"))
  val personToken = createToken(Seq("ROLE_PERSON_READ", "ROLE_PERSON_SAVE"))

  "AddressRoutes" should "return all addresses for admin" in {
    val addresses = Seq(Address(Some("1"), "Main St", "City", "State", "123", "Country"))
    when(mockRepository.findAll()).thenReturn(Flux.fromIterable(addresses.asJava))

    Get("/api/addresses") ~> addHeader("Authorization", s"Bearer $adminToken") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] should include("Main St")
    }
  }

  it should "return 403 for GET all as non-admin" in {
    Get("/api/addresses") ~> addHeader("Authorization", s"Bearer $personToken") ~> route ~> check {
      rejections should not be empty
    }
  }

  it should "get address by id" in {
    val address = Address(Some("1"), "Main St", "City", "State", "123", "Country")
    when(mockRepository.findById("1")).thenReturn(Mono.just(address))

    Get("/api/addresses/1") ~> addHeader("Authorization", s"Bearer $personToken") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] should include("Main St")
    }
  }

  it should "return 404 for missing address" in {
    when(mockRepository.findById("2")).thenReturn(Mono.empty())

    Get("/api/addresses/2") ~> addHeader("Authorization", s"Bearer $personToken") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  it should "create a new address" in {
    val address = Address(None, "New St", "New City", "NS", "456", "Country")
    val saved = address.copy(id = Some("new-id"))
    when(mockRepository.save(any(classOf[Address]))).thenReturn(Mono.just(saved))

    Post("/api/addresses", address) ~> addHeader("Authorization", s"Bearer $personToken") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldBe "\"new-id\""
    }
  }

  it should "delete an address as admin" in {
    when(mockRepository.deleteById("1")).thenReturn(Mono.empty[Void]())

    Delete("/api/addresses/1") ~> addHeader("Authorization", s"Bearer $adminToken") ~> route ~> check {
      status shouldBe StatusCodes.NoContent
    }
  }
}
