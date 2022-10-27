package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.util.generateOperationOutcomeString
import de.itcr.termite.util.generateParametersString
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.util.*
import kotlin.Exception

/**
 * Handles request regarding instances of the ValueSet resource
 */
@Controller
@RequestMapping("fhir/ValueSet")
class ValueSetController(
    @Autowired val database: TerminologyStorage,
    @Autowired val fhirContext: FhirContext
    ) {

    private val parsers: Map<String, Pair<IParser, String>>
    private val jsonParser = fhirContext.newJsonParser().setPrettyPrint(true)
    //private val baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

    init{
        val json = jsonParser to "json"
        val xml = fhirContext.newXmlParser() to "xml"
        val ndjson = fhirContext.newNDJsonParser() to "ndjson"
        this.parsers = mapOf(
            "application/json" to json,
            "application/fhir+json" to json,
            "application/xml" to xml,
            "application/fhir+xml" to xml,
            "application/ndjson" to ndjson,
            "application/fhir+ndjson" to ndjson
        )
    }

    companion object{
        private val logger: Logger = LogManager.getLogger(ValueSetController::class.java)
        //private val delegator = Delegator<ValueSetController, ResponseEntity<String>>()
    }

    /**
     * Adds a ValueSet instance to the database via the CREATE interaction
     * @see <a href= "https://www.hl7.org/fhir/http.html#create">create interaction</a>
     */
    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun addValueSet(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String>{
        try{
            val vs = parseBodyAsResource(requestEntity, contentType)
            if(vs is ValueSet) {
                try{
                    val (vsId, versionId, lastUpdated) = database.addValueSet(vs)
                    logger.info("Added value set [url: ${vs.url}, version: ${vs.version}] to database")
                    return ResponseEntity.created(URI("$vsId"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(null)
                }
                catch (e: Exception){
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Adding of ValueSet instance failed during database access")
                    logger.debug(e.stackTraceToString())
                    return ResponseEntity.unprocessableEntity()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(opOutcome)
                }
            } else {
                val message = "Request body contained instance which was not of type ValueSet but ${vs.javaClass.simpleName}"
                val opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.INVALID,
                    message,
                    jsonParser
                )
                logger.warn("Request body contained instance which was not of type ValueSet but ${vs.javaClass.simpleName}")
                return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(opOutcome)
            }
        }
        catch (e: Exception){
            if(e is ResponseStatusException) throw e
            val message = "No parser was able to handle resource; the HTTP headers were: ${requestEntity.headers}"
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.STRUCTURE,
                message,
                jsonParser
            )
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
    }

    @GetMapping("{id}")
    @ResponseBody
    fun readValueSet(@RequestParam id: String){

    }

    @GetMapping("{id}/_history/{version}")
    @ResponseBody
    fun vreadValueSet(@RequestParam id: String, @RequestParam version: String){

    }

    @GetMapping(params = ["url"])
    @ResponseBody
    fun searchValueSet(@RequestParam url: String, @RequestParam(required = false) valueSetVersion: String?): ResponseEntity<String>{
        try{
            val vsList = database.searchValueSet(url, valueSetVersion)
            val bundle = Bundle()
            bundle.id = UUID.randomUUID().toString()
            bundle.type = Bundle.BundleType.SEARCHSET
            bundle.total = vsList.size
            val baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            vsList.forEach { vs ->
                bundle.addEntry(
                    Bundle.BundleEntryComponent()
                        .setFullUrl("http://$baseURL/${vs.id}")
                        .setResource(vs)
                )
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(bundle))
        }
        catch(e: Exception){
            val message = "Search for ValueSet instances [url = $url, version = $valueSetVersion] failed"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

    /**
     * Validates a code with respect to the given value set
     * @see <a href="http://www.hl7.org/FHIR/valueset-operation-validate-code.html">validate-code operation</a>
     *
     * @param url URL of the value set against which the code is validated
     * @param valueSetVersion (optional) version of the value set assigned by its maintainer
     * @param system URI defining the code system to which the code belongs
     * @param code value of the code
     * @param display (optional) display value of the code in the given value set
     */
    @GetMapping(path = ["\$validate-code"])
    @ResponseBody
    fun validateCode(@RequestParam url: String,
                     @RequestParam(required = false) valueSetVersion: String?,
                     @RequestParam system: String,
                     @RequestParam code: String,
                     @RequestParam(required = false) display: String?): ResponseEntity<String>{
        logger.info("Validating code [system=$system, code=$code, display=$display] against value set [url=$url, version=$valueSetVersion]")
        try{
            val (result, version) = database.validateCodeVS(url, valueSetVersion, system, code, display)
            val body = generateParametersString(
                jsonParser,
                Parameters.ParametersParameterComponent(StringType("result")).setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent(StringType("message")).setValue(StringType(
                    "Code [system = $system and code = $code] ${if(result) "was" else "wasn't"} in value set " +
                            "[url = $url and version = $version]"
                ))
            )
            logger.info("Validated if code [system = $system, code = $code${if(display != null) ", display = $display" else ""}] is in value set [url = $url, version = $version]: $result")
            return ResponseEntity.ok().body(body)
        }
        catch (e: Exception){
            val message = "Failed to validate code [system = $system, code = $code${if(display != null) ", display = $display" else ""}] against value system [url = $url${if(valueSetVersion != null) ", version = $valueSetVersion" else ""}]"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                //TODO: Here INTERNAL_SERVER_ERROR or UNPROCESSABLE_ENTITY?
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

    /**
     * Parses the html message body based on the provided value of the Content-Type header. The design is heavily
     * inspired by the FHIR-Marshal handles this issue
     * @see <a href="https://github.com/itcr-uni-luebeck/fhir-marshal/blob/main/src/main/kotlin/de/uksh/medic/fhirmarshal/controller/ValidationController.kt">FHIR-Marshal</a>
     */
    private fun parseBodyAsResource(requestEntity: RequestEntity<String>, contentType: String): IBaseResource{
        try{
            val (parser, parserFormat) = parsers[contentType] ?: throw Exception("Unsupported content type: $contentType")
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