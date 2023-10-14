package com.jobgun.jobetl.consumer.domain

import zio.Chunk
import zio.json.jsonField

final case class ParsedJobDescription(
  @jsonField("required/preferred certifications") certifications: Chunk[String],
  @jsonField("required/preferred education") education: Chunk[String],
  @jsonField("required/preferred experience") experience: Chunk[String],
  @jsonField("required/preferred licenses") licenses: Chunk[String],
  @jsonField("required/preferred security credentials") securityCredentials: Chunk[String],
  @jsonField("required/preferred misc requirements") misc: Chunk[String],
)

object ParsedJobDescription {
  import zio.json.{JsonDecoder, JsonEncoder, DeriveJsonDecoder, DeriveJsonEncoder}

  implicit val jsonDecoder: JsonDecoder[ParsedJobDescription] = DeriveJsonDecoder.gen
  implicit val jsonEncoder: JsonEncoder[ParsedJobDescription] = DeriveJsonEncoder.gen
}