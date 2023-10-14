package com.jobgun.scraper.workshub

import com.jobgun.scraper.workshub.WorksHubScraper

import org.openqa.selenium.{By, WebDriverException}
import org.openqa.selenium.chrome.ChromeDriver

import io.github.bonigarcia.wdm.WebDriverManager

import zio._
import zio.json._
import zio.kafka.consumer._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._
import zio.stream.ZStream
import zio.selenium._

object FunctionalWorksApp extends ZIOAppDefault with Application {

  WebDriverManager.chromedriver().setup()

  val worksHubScraper = new WorksHubScraper("functional")

  val app = 
    worksHubScraper.findAllStream().mapZIO { listing =>
      Producer.produce[Any, String, String](
        topic = "workshub-jobs",
        key = "1",
        value = listing.toJson,
        keySerializer = Serde.string,
        valueSerializer = Serde.string
      )
    }
    .drain

  // val consumer: ZStream[Consumer, Throwable, Nothing] =
  //   Consumer
  //     .plainStream(Subscription.topics("workshub-jobs"), Serde.string, Serde.string)
  //     // .tap(r => Console.printLine(r.value))
  //     .map(_.offset)
  //     .aggregateAsync(Consumer.offsetBatches)
  //     .mapZIO(_.commit)
  //     .drain

  override def run = app.runDrain.provide(this.driverLayer, this.producerLayer, this.consumerLayer)
}