package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.api.UnsupportedFormatException
import de.itcr.termite.util.encodeResourceToString
import org.apache.logging.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.model.Bundle
import org.hl7.fhir.r4b.model.Media
import org.hl7.fhir.r4b.model.OperationOutcome
import org.hl7.fhir.r4b.model.Parameters
import org.hl7.fhir.r4b.model.Resource
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import java.util.*

abstract class FhirController(val fhirContext: FhirContext, val properties: ApplicationConfig, val logger: Logger) {

    val parsers: Map<String, Pair<IParser, String>>
    val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)
    val xmlParser: IParser = fhirContext.newXmlParser().setPrettyPrint(true)
    val ndjsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(false)

    init{
        val json = jsonParser to "json"
        val xml = xmlParser to "xml"
        val ndjson = ndjsonParser to "ndjson"
        this.parsers = mapOf(
            "application/json" to json,
            "application/fhir+json" to json,
            "application/fhir+json; charset=UTF-8" to json,
            "application/xml" to xml,
            "application/fhir+xml" to xml,
            "application/ndjson" to ndjson,
            "application/fhir+ndjson" to ndjson
        )
    }

    /**
     * Parses the html message body based on the provided value of the Content-Type header. The design is heavily
     * inspired by the FHIR-Marshal handles this issue
     * @see <a href="https://github.com/itcr-uni-luebeck/fhir-marshal/blob/main/src/main/kotlin/de/uksh/medic/fhirmarshal/controller/ValidationController.kt">FHIR-Marshal</a>
     */
    protected fun parseBodyAsResource(requestEntity: RequestEntity<String>, contentType: String): IBaseResource {
        val (parser, parserFormat) = parsers[contentType] ?: throw UnsupportedFormatException("Unsupported content type: $contentType")
        try {
            return parser.parseResource(requestEntity.body)
        } catch (e: DataFormatException) {
            val message = "Data is not in $parserFormat format"
            throw DataFormatException(message, e)
        }
    }

    protected fun parseBodyAsResource(requestEntity: RequestEntity<String>, contentType: MediaType): IBaseResource =
        parseBodyAsResource(requestEntity, contentType.toString())

    protected fun encodeResourceToString(resource: IBaseResource, contentType: String, summarized: Boolean = false): String {
        val (parser, parserFormat) = parsers[contentType] ?:
            throw UnsupportedFormatException("Unsupported content type: $contentType. Only supports: ${parsers.keys.joinToString(", ")}")
        try {
            return parser.encodeResourceToString(resource, summarized)
        } catch (e: DataFormatException) {
            val message = "Data is not in $parserFormat format"
            throw DataFormatException(message, e)
        }
    }

    protected fun encodeResourceToSting(resource: IBaseResource, contentType: MediaType, summarized: Boolean = false): String =
        encodeResourceToString(resource, contentType.toString(), summarized)

    fun generateOperationOutcome(severity: OperationOutcome.IssueSeverity, code: OperationOutcome.IssueType, diagnostics: String?): OperationOutcome {
        val opOutcome = OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(severity)
            .setCode(code)
        if(diagnostics != null) opOutcome.diagnostics = diagnostics
        return OperationOutcome().addIssue(opOutcome)
    }

    fun generateOperationOutcomeString(severity: OperationOutcome.IssueSeverity, code: OperationOutcome.IssueType, diagnostics: String?, contentType: String): String {
        val parser = if (contentType.trim() != "*/*") parsers[contentType]!!.first else jsonParser
        return parser.encodeResourceToString(generateOperationOutcome(severity, code, diagnostics))
    }

    fun generateOperationOutcomeString(severity: OperationOutcome.IssueSeverity, code: OperationOutcome.IssueType, diagnostics: String?, contentType: MediaType): String =
        generateOperationOutcomeString(severity, code, diagnostics, contentType.toString())

    fun generateParameters(vararg parameters: Parameters.ParametersParameterComponent): Parameters {
        return Parameters().setParameter(parameters.toMutableList())
    }

    fun generateParametersString(vararg parameters: Parameters.ParametersParameterComponent, contentType: String): String {
        return parsers[contentType]!!.first.encodeResourceToString(generateParameters(*parameters))
    }

    fun generateParametersString(vararg parameters: Parameters.ParametersParameterComponent, contentType: MediaType): String =
        generateParametersString(*parameters, contentType = contentType.toString())

    fun generateBundle(type: Bundle.BundleType, entries: List<IBaseResource>): Bundle {
        return Bundle().apply {
            this.type = type
            this.total = entries.size
            this.timestamp = Date()
            this.entry = entries.map { entry ->
                val entryComponent = Bundle.BundleEntryComponent()
                entryComponent.search = Bundle.BundleEntrySearchComponent().apply { mode = Bundle.SearchEntryMode.MATCH }
                entryComponent.fullUrl = "${properties.api.baseUrl}/${entry.fhirType()}/${entry.idElement.idPart}"
                entryComponent.resource = entry as Resource
                return@map entryComponent
            }
        }
    }

    fun generateBundleString(type: Bundle.BundleType, entries: List<IBaseResource>, contentType: String): String {
        return parsers[contentType]!!.first.encodeResourceToString(generateBundle(type, entries))
    }

    fun generateBundleString(type: Bundle.BundleType, entries: List<IBaseResource>, contentType: MediaType): String =
        generateBundleString(type, entries, contentType.toString())

    fun determineResponseMediaType(accept: String? = null, contentType: String? = null): MediaType {
        return if (accept != null && accept.trim() != "*/*") MediaType.parseMediaType(accept)
        else if (contentType != null && contentType.trim() != "*/*") MediaType.parseMediaType(contentType)
        else MediaType.parseMediaType(properties.api.defaultResponseContentType)
    }

}