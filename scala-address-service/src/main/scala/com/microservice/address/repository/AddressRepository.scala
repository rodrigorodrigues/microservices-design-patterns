package com.microservice.address.repository

import com.microservice.address.models.Address
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
trait AddressRepository extends ReactiveMongoRepository[Address, String]
