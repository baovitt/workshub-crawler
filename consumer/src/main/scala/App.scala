package com.jobgun.jobetl.consumer

import zio._
import zio.json._
import zio.kafka.consumer._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._
import zio.stream.ZStream

import com.jobgun.jobetl.common.domain.JobListing
import com.jobgun.jobetl.consumer.services._
import com.jobgun.jobetl.consumer.domain._
import com.jobgun.jobetl.consumer.config._
import zio.openai._

object MyApp extends ZIOAppDefault {

  def run = consumer.runDrain.provide(
    consumerLayer, 
    CompletionsParserService.default,
    EmbeddingService.default,
    WeaviateClientService.default
  )

  val consumer: ZStream[Consumer with CompletionsParserService with EmbeddingService with WeaviateClientService, Throwable, Nothing] =
    Consumer
      .plainStream(Subscription.topics("workshub-jobs"), Serde.string, Serde.string)
      .tap { listing => 
        listing.record.value.fromJson[JobListing] match {
          case Right(listing) => Console.printLine(listing) *> logic(listing)
          case Left => ZIO.succeed(())
        }
      }
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapZIO(_.commit)
      .drain

  def logic(listing: JobListing) = {
    for {
      parsedListing <- CompletionsParserService.parseListing(listing)
      embeddedListings <- EmbeddingService.embedDescription(parsedListing.get.toJson)
      _ <- WeaviateClientService.addVector(embeddedListings, listing)
    } yield ()
  }.mapError(e => new Exception(e.toString))

  def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:29092")).withGroupId("group")
      )
    )
}