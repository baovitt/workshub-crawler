package com.jobgun.scraper.workshub

// SELENIUM Imports
import org.openqa.selenium.{By, WebDriverException, WebElement}
import org.openqa.selenium.firefox.FirefoxDriver

// ZIO Imports
import zio._
import zio.stream.ZStream
import zio.selenium._
import zio.selenium.Selector._

import com.jobgun.jobetl.common.domain.JobListing

class WorksHubScraper(hubName: String) {

    private val companiesLinkBase: String = s"https://$hubName.works-hub.com/companies?jobs=1"

    // Generates links for the list of pages that list companies
    private val companiesLinks: Int => UIO[Seq[String]] = (numOfPages: Int) =>
        ZIO.succeed(for (i <- 1 to numOfPages) yield s"$companiesLinkBase&page=$i")

    // Generates links for the home pages for all of the member companies
    private val companyPageLinks: Seq[Option[String]] => UIO[Seq[String]] = (names: Seq[Option[String]]) => {
        val filteredNames = names.collect { case Some(n) => n }
        ZIO.succeed(for (n <- filteredNames) yield s"https://$hubName.works-hub.com/companies/$n")
    }

    private val numOfCompanyPages: ZIO[WebDriver, Throwable, Option[String]] = for {
        _ <- WebDriver.get(s"https://$hubName.works-hub.com/companies?jobs=1")
        paginations <- WebDriver.findElement(By.className("pagination-list"))
        paginationTabs <- paginations.findElements(By.tagName("li"))
        num <- paginationTabs.last.getAttribute("key")
    } yield (num)

    private val companyNames = (companiesPagesLink: String) => for {
        _ <- WebDriver.get(companiesPagesLink)
        containers <-
            WebDriver.findElements(By.className("companies__company-container"))
        links <- ZIO.collectAllPar(containers.map(_.getAttribute("key")
            .map(_.map(str => str.take(str.length - 6)))
        ))
    } yield links

    private val jobListing = (jobPageLink: String) => for {
        _ <- WebDriver.get(jobPageLink)
        jobTitle <- WebDriver.findElement(By.className("css-1q9iviz")).flatMap(_.getText)
        jobDescription <- WebDriver.findElement(By.className("HtmlContent_markdown-body__mjIfb")).flatMap(_.getText)
        roleType <- WebDriver.findElements(by(klass equalsTo "chakra-text css-1mbviog" in p)).flatMap(_(1).getText)
        location <- WebDriver.findElement(By.className("css-1k9bm2e")).flatMap(_.getText)
        companyName <- WebDriver.findElement(By.className("css-1y77mwq")).flatMap(_.getText)
    } yield JobListing(
        java.time.LocalDateTime.now.toString,
        jobTitle,
        jobDescription,
        roleType,
        location,
        jobPageLink,
        companyName 
    )

    private val jobLinks = (companyPageLink: String) => for {
        _ <- WebDriver.get(companyPageLink)
        jobCards <- WebDriver.findElements(By.className("buttons"))
        linkTags <- ZIO.collectAll(jobCards.map(_.findElement(By.tagName("a"))))
        links <- ZIO.collectAll(linkTags.map(_.getAttribute("href")))
    } yield links

    def findAll(): ZIO[WebDriver, Throwable, Seq[JobListing]] = for {
        numberOfCompanyPages <- numOfCompanyPages
        companiesLinks <- companiesLinks(1)
        names <- ZIO.collectAll(companiesLinks.map(companyNames(_)))
        companyLinks <- companyPageLinks(names.flatten)
        jobLinks <- ZIO.collectAll(companyLinks.map(jobLinks(_)))
        jobListings <- ZIO.collectAll(jobLinks.flatten.collect { case Some(n) => jobListing(n) })
    } yield jobListings

    def findAllStream(): ZStream[WebDriver, Throwable, JobListing] = 
        ZStream.fromIteratorZIO(findAll().map(_.toIterator))
}