package com.jobgun.scraper.workshub.functional

// SELENIUM Imports
import org.openqa.selenium.{By, WebDriverException, WebElement}
import org.openqa.selenium.firefox.FirefoxDriver

// ZIO Imports
import zio._
import zio.selenium._

object Functional {
    val comapniesLinkBase: String = "https://functional.works-hub.com/companies?jobs=1"

    // Generates links for the list of pages that list companies
    val companiesLinks: Int => UIO[Seq[String]] = (numOfPages: Int) =>
        ZIO.succeed(for (i <- 1 to numOfPages) yield s"$comapniesLinkBase&page=$i")

    // Generates links for the home pages for all of the member companies
    val companyPageLinks: Seq[Option[String]] => UIO[Seq[String]] = (names: Seq[Option[String]]) => {
        val filteredNames = names.collect { case Some(n) => n }
        ZIO.succeed(for (n <- filteredNames) yield s"https://functional.works-hub.com/companies/$n")
    }

    val numOfCompanyPages: ZIO[WebDriver, Throwable, Option[String]] = for {
        _ <- WebDriver.get("https://functional.works-hub.com/companies?jobs=1")
        paginations <- WebDriver.findElement(By.className("pagination-list"))
        paginationTabs <- paginations.findElements(By.tagName("li"))
        num <- paginationTabs.last.getAttribute("key")
    } yield (num)

    val companyNames = (companiesPagesLink: String) => for {
        _ <- WebDriver.get(companiesPagesLink)
        containers <-
            WebDriver.findElements(By.className("companies__company-container"))
        links <- ZIO.collectAllPar(containers.map(_.getAttribute("key")
                .map(_.map(str => str.take(str.length - 6)))
            ))
    } yield links

    val jobLinks = (companyPageLink: String) => for {
        _ <- WebDriver.get(companyPageLink)
        jobCards <- WebDriver.findElements(By.className("buttons"))
        linkTags <- ZIO.collectAll(jobCards.map(_.findElement(By.tagName("a"))))
        links <- ZIO.collectAll(linkTags.map(_.getAttribute("href")
                .map(_.map(str => s"https://functional.works-hub.com/$str"))
            ))
    } yield links
}