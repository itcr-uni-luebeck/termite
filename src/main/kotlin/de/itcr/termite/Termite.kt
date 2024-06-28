package de.itcr.termite

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.config.DatabaseProperties
import de.itcr.termite.database.sql.TerminologyDatabase
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.index.provider.r4b.RocksDBIndexStore
import de.itcr.termite.metadata.MetadataCompiler
import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.hl7.fhir.r4b.model.OperationDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import java.net.URI
import java.nio.file.Path
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
class Termite(compilationResult: Pair<CapabilityStatement, Array<OperationDefinition>>) {

    @Autowired
    private lateinit var properties: ApplicationConfig

    private val capabilityStatement: CapabilityStatement = compilationResult.first

    private val operationDefinitions: List<OperationDefinition> = compilationResult.second.toList()

    @Bean
    fun logger(): Logger = LogManager.getLogger(Termite::class.java)

    @Bean
    fun fhirContext(): FhirContext = FhirContext.forR4B()

    @Bean
    fun capabilityStatement(): CapabilityStatement = this.capabilityStatement

    @Bean
    fun operationDefinitions(): List<OperationDefinition> = this.operationDefinitions

    @Bean
    fun indexStore(): FhirIndexStore<ByteArray, Function<ByteArray>, ByteArray, Function<ByteArray>> {
        return when (properties.index.type) {
            // TODO: Create single global FhirContext object
            "rocksdb" -> RocksDBIndexStore.open(fhirContext(), Path.of(properties.index.path), capabilityStatement)
            else -> throw RuntimeException("Unknown index type '${properties.index.type}'")
        }
    }

}

fun main(args: Array<String>) {
    runApplication<Termite>(*args)
}