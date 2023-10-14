package com.jobgun.scraper.workshub

import zio._
import zio.kafka.consumer._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._
import zio.stream.ZStream

import org.openqa.selenium.{By, WebDriverException}
import org.openqa.selenium.chrome.ChromeDriver

import io.github.bonigarcia.wdm.WebDriverManager
import zio.selenium._

trait Application {

  def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List("localhost:29092"))
      )
    )

  def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:29092")).withGroupId("group")
      )
    )

  def driverLayer: Layer[WebDriverException, WebDriver] = WebDriver.layer(new ChromeDriver())
}