package com.powerspace.test

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Advertisement(
  id: Int,
  name: String,
  description: String,
  image: String,
  positionCode: String,
  countryCode: String
)

object Advertisement {
  implicit val advertisementEncoder: Encoder[Advertisement] = deriveEncoder[Advertisement]
  implicit val advertisementDecoder: Decoder[Advertisement] = deriveDecoder[Advertisement]
}
