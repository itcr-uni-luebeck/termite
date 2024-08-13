package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.api.r4b.handler.handleUnparsableEntity
import de.itcr.termite.api.r4b.handler.handleUnsupportedFormat
import de.itcr.termite.api.r4b.handler.handleUnsupportedParameterValue
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.api.UnsupportedFormatException
import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemPersistenceManager
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumerations.PublicationStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Date
import java.util.zip.DataFormatException

typealias TerminologyCapabilitiesSoftwareComponent = TerminologyCapabilities.TerminologyCapabilitiesSoftwareComponent
typealias TerminologyCapabilitiesImplementationComponent = TerminologyCapabilities.TerminologyCapabilitiesImplementationComponent
typealias TerminologyCapabilitiesCodeSystemComponent = TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent
typealias CodeSystemVersionComponent = TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent
typealias FilterComponent = TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionFilterComponent

/**
 * Handles requests which don't represent interactions regarding any specific resource but provide metadata about the
 * terminology service
 */
@RestController
@RequestMapping("fhir")
class BaseController (
    @Autowired properties: ApplicationConfig,
    @Autowired val csPersistence: CodeSystemPersistenceManager,
    @Autowired fhirContext: FhirContext,
    @Autowired val capabilityStatement: CapabilityStatement
): FhirController(fhirContext, properties, logger) {

    companion object {
        private val logger = LogManager.getLogger(this::class)
    }

    /**
     * Returns CapabilityStatement instance of this terminology service
     * @return ResponseEntity instance the body of which contains the CapabilityStatement instance as a JSON string
     */
    @GetMapping("metadata")
    @ResponseBody
    fun metadata(
        @RequestParam(required = false, name = "mode", defaultValue = "full") mode: String = "full",
        @RequestParam(required = false, name = "_format", defaultValue = "application/fhir+json") format: String,
        @RequestParam(required = false, name = "_summary", defaultValue = "false") summary: Boolean
    ): ResponseEntity<String>{
        logger.info("Received metadata request")
        try {
            val metadataResource = when (mode) {
                "full" -> capabilityStatement
                "normative" -> throw UnsupportedValueException(mode, "mode", "fhir/metadata", HttpMethod.GET)
                "terminology" -> getTerminologyCapabilities()
                else -> throw UnsupportedValueException(mode, "mode", "fhir/metadata", HttpMethod.GET)
            }
            return ResponseEntity.ok()
                .eTag("W/\"0\"")
                .header("Content-Type", format)
                .body(encodeResourceToString(metadataResource, format, summary))
        }
        catch (exc: UnsupportedValueException) { return handleUnsupportedParameterValue(exc, format) }
        catch (exc: UnsupportedFormatException) { return handleUnsupportedFormat(exc, format) }
        catch (exc: DataFormatException) { return handleUnparsableEntity(exc, format) }
    }

    // TODO: Should be updated upon change in underlying database and new instance provided to this endpoint
    private fun getTerminologyCapabilities(): TerminologyCapabilities {
        return TerminologyCapabilities().apply {
            url = "fhir/metadata?mode=terminology"
            name = "termite-terminology-capabilities"
            title = "Termite Terminology Capabilities"
            status = PublicationStatus.ACTIVE
            experimental = false
            date = Date()
            description = "Supported terminology resources of Termite instance @ ${properties.api.baseUrl}"
            jurisdiction = listOf(CodeableConcept().addCoding(Coding("urn:iso:std:iso:3166", "DE", "Germany")))
            purpose = "Lists the currently available terminology resources on the server"
            kind = Enumerations.CapabilityStatementKind.INSTANCE
            software = TerminologyCapabilitiesSoftwareComponent("termite")
            implementation = TerminologyCapabilitiesImplementationComponent("termite instance")
                .setUrl(properties.api.baseUrl)
            codeSystem = getSupportedCodeSystems()
            // TODO: Implement operation information
        }
    }

    private fun getSupportedCodeSystems(): List<TerminologyCapabilitiesCodeSystemComponent> {
        return csPersistence.search(emptyMap()).groupBy { cs -> cs.url }.map { csGroup ->
            TerminologyCapabilitiesCodeSystemComponent().apply {
                uri = csGroup.key
                version = csGroup.value.map { cs ->
                    CodeSystemVersionComponent().apply {
                        code = cs.version
                        // TODO: isDefault
                        compositional = cs.compositional
                        // TODO: language
                        filter = cs.filter.map {
                            FilterComponent()
                            .setCode(it.code)
                            .setOp(it.operator.map { op -> CodeType(op.code) })
                        }
                        property = cs.property.map { CodeType(it.code) }
                        subsumption = false
                    }
                }
            }
        }
    }

}