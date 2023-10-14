package com.jobgun.jobetl.consumer.services

import zio.openai.Embeddings
import zio.openai.model.CreateEmbeddingRequest.Input
import zio.openai.model.OpenAIFailure
import zio._

trait EmbeddingService {
  def embedTexts(texts: String*): IO[OpenAIFailure, Chunk[Chunk[Double]]]
  def embedDescription(description: String): IO[OpenAIFailure, Chunk[Double]]
  def embedUser(userResume: String): IO[OpenAIFailure, Chunk[Double]]
}

object EmbeddingService {
  lazy val default: ZLayer[Any, Throwable, EmbeddingService] =
    Embeddings.default >>> ZLayer {
      for {
        embeddings <- ZIO.service[Embeddings]
      } yield new EmbeddingService {
        def embedTexts(texts: String*): IO[OpenAIFailure, Chunk[Chunk[Double]]] = 
          for {
            embeddings <-
              ZIO.foreachPar(texts) { text =>
                embeddings.createEmbedding(
                  model = "text-embedding-ada-002",
                  input = Input.String(text)
                )
              }
          } yield Chunk.fromIterable(embeddings.flatMap(_.data).map(_.embedding))
        

        def embedDescription(description: String): IO[OpenAIFailure, Chunk[Double]] =
            embedTexts(description).map(_.head)

        def embedUser(userResume: String): IO[OpenAIFailure, Chunk[Double]] =
            embedDescription(userResume)
      }
    }

  def embedTexts(texts: String*): ZIO[EmbeddingService, OpenAIFailure, Chunk[Chunk[Double]]] =
    ZIO.serviceWithZIO[EmbeddingService](_.embedTexts(texts: _*))

  def embedDescription(description: String): ZIO[EmbeddingService, OpenAIFailure, Chunk[Double]] =
    ZIO.serviceWithZIO[EmbeddingService](_.embedDescription(description))

  def embedUser(userResume: String): ZIO[EmbeddingService, OpenAIFailure, Chunk[Double]] =
    ZIO.serviceWithZIO[EmbeddingService](_.embedUser(userResume))
}