package com.microservice.address.models

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import io.circe.generic.auto._
import io.circe.{Encoder, Decoder}
import scala.annotation.meta.field

@Document(collection = "addresses")
case class Address(
  @(Id @field) id: Option[String] = None,
  street: String,
  city: String,
  state: String,
  zipCode: String,
  country: String,
  createdByUser: Option[String] = None,
  createdDate: Option[Instant] = Some(Instant.now()),
  lastModifiedByUser: Option[String] = None,
  lastModifiedDate: Option[Instant] = Some(Instant.now())
)

object AddressJsonProtocol {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
}
