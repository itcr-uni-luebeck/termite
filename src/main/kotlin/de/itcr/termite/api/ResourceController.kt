package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import de.itcr.termite.database.TerminologyStorage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.http.RequestEntity

abstract class ResourceController(protected val database: TerminologyStorage, private val fhirContext: FhirContext) {

    protected val parsers: Map<String, Pair<IParser, String>>
    protected val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)

    init{
        val json = jsonParser to "json"
        val xml = fhirContext.newXmlParser() to "xml"
        val ndjson = fhirContext.newNDJsonParser() to "ndjson"
        this.parsers = mapOf(
            "application/json" to json,
            "application/json;charset=utf-8" to json,
            "application/fhir+json" to json,
            "application/fhir+json;charset=utf-8" to json,
            "application/xml" to xml,
            "application/xml;charset=utf-8" to xml,
            "application/fhir+xml" to xml,
            "application/fhir+xml;charset=utf-8" to xml,
            "application/ndjson" to ndjson,
            "application/ndjson;charset=utf-8" to ndjson,
            "application/fhir+ndjson" to ndjson,
            "application/fhir+ndjson;charset=utf-8" to ndjson
        )
    }

    /**
     * Parses the html message body based on the provided value of the Content-Type header. The design is heavily
     * inspired by the FHIR-Marshal handles this issue
     * @see <a href="https://github.com/itcr-uni-luebeck/fhir-marshal/blob/main/src/main/kotlin/de/uksh/medic/fhirmarshal/controller/ValidationController.kt">FHIR-Marshal</a>
     */
    protected fun parseBodyAsResource(requestEntity: RequestEntity<String>, contentType: String): IBaseResource {
        try{
            val (parser, parserFormat) = parsers[contentType.trim().lowercase().replace(" ", "")] ?: throw Exception("Unsupported content type: $contentType")
            try {
                return parser.parseResource(requestEntity.body)
            } catch (e: DataFormatException) {
                val message = "Data is not in $parserFormat"
                throw Exception(message, e)
            }
        }
        catch (e: Exception) {
            val message = "No parser was able to handle resource; the HTTP headers were: ${requestEntity.headers}"
            throw Exception(message, e)
        }
    }

}