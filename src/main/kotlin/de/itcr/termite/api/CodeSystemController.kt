package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.util.generateOperationOutcomeString
import de.itcr.termite.util.generateParametersString
import de.itcr.termite.util.parseParameters
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
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
import java.net.URI
import java.util.*

@Controller
@RequestMapping("fhir/CodeSystem")
class CodeSystemController(
    @Autowired database: TerminologyStorage,
    @Autowired fhirContext: FhirContext
    ): ResourceController(database, fhirContext) {

    companion object{
        private val logger: Logger = LogManager.getLogger(this)
    }

    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun addCodeSystem(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    val (csId, versionId, lastUpdated) = database.addCodeSystem(cs)
                    logger.info("Added code system [url: ${cs.url}, version: ${cs.version}] to database")
                    return ResponseEntity.created(URI("$csId"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(null)
                } catch (e: Exception) {
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Adding of CodeSystem instance failed during database access")
                    logger.debug(e.stackTraceToString())
                    return ResponseEntity.unprocessableEntity()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(opOutcome)
                }
            } else {
                val message =
                    "Request body contained instance which was not of type CodeSystem but ${cs.javaClass.simpleName}"
                val opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.INVALID,
                    message,
                    jsonParser
                )
                logger.warn(message)
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

    @GetMapping(path = ["\$validate-code"])
    @ResponseBody
    fun validateCode(@RequestParam url: String,
                     @RequestParam code: String,
                     @RequestParam(required = false) display: String?): ResponseEntity<String> {
        logger.info("Validating code [code=$code, display=$display] against code system [url=$url]")
        try{
            val result = database.validateCodeCS(code, display, url)
            val message = "Code [code = $code, display = $display] ${if(result) "is" else "isn't"} in code system [url = $url]"
            val body = generateParametersString(
                jsonParser,
                Parameters.ParametersParameterComponent(StringType("result")).setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent(StringType("message")).setValue(
                    StringType(message)
                )
            )
            logger.info("Validation result: $message")
            return ResponseEntity.ok().body(body)
        }
        catch (e: Exception){
            val message = "Failed to validate code [code = $code${if(display != null) ", display = $display" else ""}] against code system [url = $url]"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

    @PostMapping(path = ["\$validate-code"])
    @ResponseBody
    fun validateCode(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        logger.info("POST: Validating code against code system with request body: ${requestEntity.body}")
        try{
            val parameters = parseBodyAsResource(requestEntity, contentType) as Parameters
            val paramMap = parseParameters(parameters)
            val url = paramMap["url"] ?: throw Exception("url has to be provided in parameters in request body")
            val code = paramMap["code"] ?: throw Exception("code has to be provided in parameters in request body")
            val display = paramMap["display"]
            //TODO: Implement other parameters like version
            val result = database.validateCodeCS(code, display, url)
            val resultParam = generateParametersString(
                jsonParser,
                Parameters.ParametersParameterComponent()
                    .setName("result").setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent()
                    .setName("message").setValue(StringType(
                        "Code [code = $code and display = $display] ${if(result) "was" else "wasn't"} in code system " +
                                "[url = $url]"
                    ))
            )
            logger.debug("Validation result: $resultParam")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultParam)
        }
        catch (e: Exception){
            logger.warn("Validation of code against code system failed")
            logger.debug(e.stackTraceToString())
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.INVALID,
                e.message,
                jsonParser
            )
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
    }

    @GetMapping(params = ["url"])
    @ResponseBody
    fun searchCodeSystem(@RequestParam url: String): ResponseEntity<String>{
        logger.info("Searching for code system [url = $url]")
        try{
            val csList = database.searchCodeSystem(url)
            val bundle = Bundle()
            bundle.id = UUID.randomUUID().toString()
            bundle.type = Bundle.BundleType.SEARCHSET
            bundle.total = csList.size
            //Should be faster since otherwise an internal array list would have to resized all the time
            bundle.entry = csList.map { cs -> Bundle.BundleEntryComponent().setResource(cs) }
            logger.debug("Found ${csList.size} code systems for URL $url")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(bundle))
        }
        catch (e: Exception){
            val message = e.message
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.PROCESSING,
                message,
                jsonParser
            )
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
    }

}