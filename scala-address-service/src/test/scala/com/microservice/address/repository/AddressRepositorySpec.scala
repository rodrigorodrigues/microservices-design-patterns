package com.microservice.address.repository

import com.microservice.address.models.Address
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AddressRepositorySpec extends AnyFlatSpec with Matchers {
  
  "AddressRepository" should "compile and be recognizable" in {
    val address = Address(street = "Main St", city = "Anytown", state = "CA", zipCode = "12345", country = "USA")
    address.street should be ("Main St")
  }
}
