package de.itcr.termite

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.config.DatabaseProperties
import de.itcr.termite.database.sql.TerminologyDatabase
import de.itcr.termite.metadata.MetadataCompiler
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.hl7.fhir.r4b.model.OperationDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.net.URI

@SpringBootApplication
open class Termite {

    @Autowired
    private lateinit var databaseProperties: DatabaseProperties

    private val capabilityStatement: CapabilityStatement

    private val operationDefinitions: List<OperationDefinition>

    init {
        val apiMetadata = MetadataCompiler.compileStaticFhirServerMetadata("de.itcr.termite.api", URI("fhir"))
        this.capabilityStatement = apiMetadata.first
        this.operationDefinitions = apiMetadata.second.toList()
    }

    @Bean
    open fun logger(): Logger = LogManager.getLogger(Termite::class.java)

    @Bean
    open fun fhirContext(): FhirContext = FhirContext.forR4B()

    @Bean
    open fun database(): TerminologyDatabase = TerminologyDatabase(databaseProperties.connection.url)

    @Bean
    open fun capabilityStatement(): CapabilityStatement = this.capabilityStatement

    @Bean
    open fun operationDefinitions(): List<OperationDefinition> = this.operationDefinitions

}

fun main(args: Array<String>) {
    runApplication<Termite>(*args)
}