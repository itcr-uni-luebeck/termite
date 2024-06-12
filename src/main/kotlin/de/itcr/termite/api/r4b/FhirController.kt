package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.http.RequestEntity

abstract class FhirController(val fhirContext: FhirContext) {

    val parsers: Map<String, Pair<IParser, String>>
    val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)
    val xmlParser: IParser = fhirContext.newXmlParser().setPrettyPrint(true)
    val ndjsonParser: IParser = fhirContext.newNDJsonParser()

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
        val (parser, parserFormat) = parsers[contentType] ?: throw Exception("Unsupported content type: $contentType")
        try {
            return parser.parseResource(requestEntity.body)
        } catch (e: DataFormatException) {
            val message = "Data is not in $parserFormat format"
            throw Exception(message, e)
        }
    }

    protected fun encodeResourceToString(resource: IBaseResource, contentType: String): String {
        val (parser, parserFormat) = parsers[contentType] ?: throw Exception("Unsupported content type: $contentType")
        try {
            return parser.encodeResourceToString(resource)
        } catch (e: DataFormatException) {
            val message = "Data is not in $parserFormat format"
            throw Exception(message, e)
        }
    }

}