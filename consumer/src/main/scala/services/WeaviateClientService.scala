package com.jobgun.jobetl.consumer.services

import zio._
import io.weaviate.client._
import io.weaviate.client.base.Result
import io.weaviate.client.v1.data.model.WeaviateObject
import io.weaviate.client.v1.graphql.model.GraphQLResponse
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument
import io.weaviate.client.v1.graphql.query.fields.Field

import com.jobgun.jobetl.common.domain.JobListing
import com.jobgun.jobetl.consumer.config.WeaviateConfig

trait WeaviateClientService {
    def addVector(vector: Chunk[Double], metadata: JobListing): Task[Result[WeaviateObject]]
    def findJobs(userEmbedding: Chunk[Double]): IO[Throwable, String]
}

object WeaviateClientService {
    import scala.jdk.CollectionConverters._
    import com.google.common.primitives.Floats
    import com.google.gson.Gson
    import java.util.UUID

    class WeaviateClientException(message: String) extends Exception

    private lazy val defaultClient = ZLayer {
        ZIO.succeed(new WeaviateClient(WeaviateConfig.config))
    }

    def parseGraphQLResponse(response: GraphQLResponse) = {
        val gson: Gson = new Gson()
        gson.toJson(response.getData)
    }

    lazy val default: ZLayer[Any, Throwable, WeaviateClientService] = defaultClient >>> ZLayer {
        for {
            client <- ZIO.service[WeaviateClient]
        } yield new WeaviateClientService {
            def addVector(vector: Chunk[Double], metadata: JobListing): Task[Result[WeaviateObject]] = {
                val request = 
                    client.data.creator
                        .withVector(Floats.asList(vector.map(_.toFloat).toSeq: _*).asScala.toArray)
                        .withProperties(Map(
                            "created"         -> metadata.created,
                            "title"           -> metadata.title,
                            "description"     -> metadata.description,
                            "employment_type" -> metadata.employmentType,
                            "location"        -> metadata.location,
                            "url"             -> metadata.url,
                            "company_name"    -> metadata.companyName,
                        ).asJava.asInstanceOf[java.util.Map[String, Object]])
                        .withClassName("JobListing")
                        .withID(UUID.randomUUID.toString)

                ZIO.attempt(request.run)
            }
            
            def findJobs(userEmbedding: Chunk[Double]): IO[Throwable, String] = {
                val created = Field.builder.name("created").build
                val title = Field.builder.name("title").build
                val description = Field.builder.name("description").build
                val employmentType = Field.builder.name("employment_type").build
                val location = Field.builder.name("location").build
                val url = Field.builder.name("url").build
                val companyName = Field.builder.name("company_name").build
                val companyUrl = Field.builder.name("company_url").build
                val country = Field.builder.name("country").build
                val request = 
                    client.graphQL.get
                        .withClassName("JobListing")
                        .withFields(created, title, description, employmentType, location, url, companyName, companyUrl, country)
                        .withNearVector(
                            NearVectorArgument.builder.vector(
                                Floats.asList(userEmbedding.map(_.toFloat).toSeq: _*).asScala.toArray
                            ).build
                        )
                        .withLimit(50)

                ZIO.attempt(request.run).flatMap { (result: Result[GraphQLResponse]) =>
                    if (result.hasErrors)
                        ZIO.fail(new WeaviateClientException(result.toString))
                    else
                        ZIO.succeed(new Gson().toJson(result))
                }
            }
        }
    }

    def addVector(vector: Chunk[Double], metadata: JobListing) =
        ZIO.serviceWithZIO[WeaviateClientService](_.addVector(vector, metadata))

    def findJobs(userEmbedding: Chunk[Double]) =
        ZIO.serviceWithZIO[WeaviateClientService](_.findJobs(userEmbedding))
}