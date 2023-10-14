package com.jobgun.jobetl.consumer.config

import io.weaviate.client.Config

object WeaviateConfig {
    val config = 
        new Config("http", "jobs-vhoc539u.weaviate.network")
}