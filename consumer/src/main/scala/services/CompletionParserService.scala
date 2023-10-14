package com.jobgun.jobetl.consumer.services

import zio._
import zio.openai._
import zio.openai.model.{OpenAIFailure, Temperature, CreateCompletionResponse}
import zio.openai.model.CreateCompletionRequest.{MaxTokens, Prompt}
import com.jobgun.jobetl.consumer.domain._
import zio.json._
import com.jobgun.jobetl.common.domain.JobListing

trait CompletionsParserService {
  def parseUser(
      user: String
  ): ZIO[Any, OpenAIFailure, Option[ParsedJobDescription]]

  def parseListing(
      listing: JobListing
  ): ZIO[Any, OpenAIFailure, Option[ParsedJobDescription]]
}

object CompletionsParserService { 

  private def generateUserPrompt(user: String): String =
    s"""You're given the task of parsing a users resume into an ideal job listing to fill in the following fields:

    {
        "required/preferred certifications": [], // What specific certifications does the user have?
        "required/preferred education": [], // What specific education does the user have?
        "required/preferred experience": [], // What specific work or military experience does the user have? Include years of experience for each item if mentioned.
        "required/preferred licenses": [], // What specific licenses does the user have
        "required/preferred security credentials": [], // What specific security credentials does this user have?
        "required/preferred misc requirements": [] // What misc qualifications does this user have? For example, patents, publications, achievements, skills, or training.
    }

    Please extract the necessary information thoroughly from the users resume and fill in the JSON structure accordingly. Provide pure json, no other content. The users resume is:

    $user
    """.stripMargin

  private def generateListingPrompt(listingDescription: String): String =
    s"""You're given the task of reading a job listing and extracting information to fill in the following fields:

    {
        "required/preferred certifications": [], // What specific educational degrees or coursework are required for this job?
        "required/preferred education": [], // What prior job/military experiences are required for this role?
        "required/preferred experience": [], // Are there any specific certifications required for this job?
        "required/preferred licenses": [], // Are there any specific licenses required for this job?
        "required/preferred security credentials": [], // Are there any specific security credentials required for this job?
        "required/preferred misc requirements": [] // Any other requirements for this job?
    }

    Please extract the necessary information thoroughly from the job listing and fill in the JSON structure accordingly. Provide pure json, no other content. The job listing is:

    $listingDescription
    """.stripMargin

  private def runUserModelAndParse(
      user: String,
      temperature: Temperature = Temperature(0.2),
      attempts:    Int = 1
  )(completions: Completions): ZIO[Any, OpenAIFailure, Option[ParsedJobDescription]] =
    for {
      result   <-
        completions.createCompletion(
          model = "text-davinci-003",
          prompt = Prompt.String(generateUserPrompt(user)),
          temperature = temperature,
          maxTokens = MaxTokens(2500)
        )
      output    =
        result
          .choices
          .headOption
          .flatMap(completion => (completion.text getOrElse "").fromJson[ParsedJobDescription].toOption)
      next     <- output match {
        case Some(_) => ZIO.succeed(output)
        case None if attempts == 1 => runUserModelAndParse(user, Temperature(0.5), attempts + 1)(completions)
        case None if attempts == 2 => runUserModelAndParse(user, Temperature(0.8), attempts + 1)(completions)
        case None => ZIO.succeed(None)
      }
    } yield next

  private def parseListingLogic(listing: JobListing)(completions: Completions) =
    for {
        result <- completions.createCompletion(
            model = "text-davinci-003",
            prompt = Prompt.String(generateListingPrompt(listing.description)),
            temperature = Temperature(0.5),
            maxTokens = MaxTokens(2500)
          )
        output = result
            .choices
            .map(completion => (completion.text getOrElse "").fromJson[ParsedJobDescription])
    } yield output.collect { case Right(x) => x }.headOption

  lazy val default: ZLayer[Any, Throwable, CompletionsParserService] = Completions.default >>> ZLayer {
    for {
      completions <- ZIO.service[Completions]
    } yield new CompletionsParserService {
      def parseUser(
          user: String
      ): ZIO[Any, OpenAIFailure, Option[ParsedJobDescription]] =
          runUserModelAndParse(user)(completions)
        
      def parseListing(
          listing: JobListing
      ): ZIO[Any, OpenAIFailure, Option[ParsedJobDescription]] =
        parseListingLogic(listing)(completions)
    }
  }

  def parseUser(
    user: String
  ): ZIO[CompletionsParserService, OpenAIFailure, Option[ParsedJobDescription]] =
    ZIO.serviceWithZIO[CompletionsParserService](_.parseUser(user))

  def parseListing(
    listing: JobListing
  ): ZIO[CompletionsParserService, OpenAIFailure, Option[ParsedJobDescription]] =
    ZIO.serviceWithZIO[CompletionsParserService](_.parseListing(listing))

}