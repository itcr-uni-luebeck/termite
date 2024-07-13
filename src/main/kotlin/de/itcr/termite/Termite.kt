package de.itcr.termite

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.index.provider.r4b.rocksdb.RocksDBIndexStore
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
import java.nio.file.Path

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
class Termite(compilationResult: Pair<CapabilityStatement, Array<OperationDefinition>>) {

    @Autowired
    private lateinit var properties: ApplicationConfig

    private lateinit var ctx: FhirContext

    private val capabilityStatement: CapabilityStatement = compilationResult.first

    private val operationDefinitions: List<OperationDefinition> = compilationResult.second.toList()

    @Bean
    fun logger(): Logger = LogManager.getLogger(Termite::class.java)

    @Bean
    fun fhirContext(): FhirContext {
        val normalizedVersionStr = properties.api.fhirApiVersion.uppercase()
        if (!this::ctx.isInitialized) ctx = FhirContext.forCached(FhirVersionEnum.forVersionString(normalizedVersionStr))
        return ctx
    }

    @Bean
    fun capabilityStatement(): CapabilityStatement = this.capabilityStatement

    @Bean
    fun operationDefinitions(): List<OperationDefinition> = this.operationDefinitions

    @Bean
    fun indexStore(): FhirIndexStore<ByteArray, ByteArray> {
        return when (properties.index.type) {
            // TODO: Create single global FhirContext object
            "rocksdb" -> RocksDBIndexStore.open(fhirContext(), Path.of(properties.index.path))
            else -> throw RuntimeException("Unknown index type '${properties.index.type}'")
        }
    }

}

fun main(args: Array<String>) {
    runApplication<Termite>(*args)
}