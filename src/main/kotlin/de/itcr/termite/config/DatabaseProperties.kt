package de.itcr.termite.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("database")
open class DatabaseProperties {

    val connection = Connection()

    class Connection {

        var url: String = "jdbc:sqlite:database/termite.db"

    }

}