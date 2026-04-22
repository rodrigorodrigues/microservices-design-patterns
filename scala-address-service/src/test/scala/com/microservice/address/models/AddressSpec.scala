package com.microservice.address.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import com.microservice.address.models.AddressJsonProtocol._
import java.time.Instant
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AddressSpec extends AnyFlatSpec with Matchers {

  "Address" should "be instantiatable" in {
    val address = Address(
      street = "123 Main St",
      city = "Springfield",
      state = "IL",
      zipCode = "62704",
      country = "USA"
    )
    address.street shouldBe "123 Main St"
    address.city shouldBe "Springfield"
    address.id shouldBe None
  }

  it should "support JSON encoding and decoding" in {
    val now = Instant.parse("2026-04-17T12:00:00Z")
    val address = Address(
      id = Some("addr1"),
      street = "123 Main St",
      city = "Springfield",
      state = "IL",
      zipCode = "62704",
      country = "USA",
      createdByUser = Some("admin"),
      createdDate = Some(now)
    )

    val json = address.asJson
    val jsonString = json.noSpaces
    
    jsonString should include ("\"street\":\"123 Main St\"")
    jsonString should include ("\"createdDate\":\"2026-04-17T12:00:00Z\"")

    val decoded = decode[Address](jsonString)
    decoded shouldBe Right(address)
  }
}
