package com.jobgun.scraper.workshub

import com.jobgun.scraper.workshub.functional.Functional._

import org.openqa.selenium.{By, WebDriverException}
import org.openqa.selenium.firefox.FirefoxDriver

import zio._
import zio.selenium._

object FindElement extends ZIOAppDefault {

  val app: ZIO[WebDriver, Throwable, Unit] =
    for {
      numberOfCompanyPages <- numOfCompanyPages
      companiesLinks <- companiesLinks(3)
      names <- ZIO.collectAll(companiesLinks.map(companyNames(_)))
      companyLinks <- companyPageLinks(names.flatten)
      jobLinks <- ZIO.collectAll(companyLinks.map(jobLinks(_)))
      _ <- Console.printLine(s"\n\n${jobLinks.mkString("\n")}\n\n")
    } yield ()

  val layer: Layer[WebDriverException, WebDriver] = WebDriver.layer(new FirefoxDriver())

  override def run = app.provide(layer)
}