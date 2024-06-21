package de.itcr.termite.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration

@ConstructorBinding
@ConfigurationProperties(prefix = "termite")
data class ApplicationConfig (
    val index: IndexConfig
) {

    data class IndexConfig(
        val type: String = "rocksdb",
        val path: String = "index",
        val schema: SchemaConfig
    ) {

        data class SchemaConfig(
            val file: String = "config/index/index.json"
        )

    }

}