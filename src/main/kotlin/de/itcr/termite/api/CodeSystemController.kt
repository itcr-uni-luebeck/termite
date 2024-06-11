package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.databind.ObjectMapper
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import de.itcr.termite.model.entity.toCodeSystemResource
import de.itcr.termite.model.entity.toFhirCodeSystemMetadata
import de.itcr.termite.model.repository.FhirCodeSystemMetadataRepository
import de.itcr.termite.util.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.formats.JsonParserBase
import org.hl7.fhir.r4b.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.util.*

@ForResource(
    type = "CodeSystem",
    versioning = "no-version",
    readHistory = false,
    updateCreate = false,
    conditionalCreate = false,
    conditionalRead = "not-supported",
    conditionalUpdate = false,
    conditionalDelete = "not-supported",
    referencePolicy = [],
    searchInclude = [],
    searchRevInclude = [],
    searchParam = [
        SearchParameter(
            name = "url",
            type = "uri",
            documentation = "URL of the resource to locate"
        )
    ]
)
@SupportsInteraction(["create", "search-type"])
@SupportsOperation(
    name = "CodeSystem-lookup",
    title = "CodeSystem-lookup",
    status = "active",
    kind = "operation",
    experimental = false,
    description = "Checks whether a given concept is in a code system",
    affectState = false,
    code = "lookup",
    resource = ["CodeSystem"],
    system = false,
    type = true,
    instance = false,
    parameter = [
        Parameter(
            name = "url",
            use = "in",
            min = 1,
            max = "1",
            documentation = "URL of the CodeSystem instance",
            type = "uri"
        ),
        Parameter(
            name = "valueSetVersion",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Version of the CodeSystem instance",
            type = "string"
        ),
        Parameter(
            name = "code",
            use = "in",
            min = 1,
            max = "1",
            documentation = "Code of the coding to be located",
            type = "code"
        ),
        Parameter(
            name = "system",
            use = "in",
            min = 1,
            max = "1",
            documentation = "System from which the code originates",
            type = "uri"
        ),
        Parameter(
            name = "display",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Display value of the concept",
            type = "uri"
        )
    ]
)
@RestController
@RequestMapping("fhir/CodeSystem")
class CodeSystemController(
    @Autowired repository: FhirCodeSystemMetadataRepository,
    @Autowired fhirContext: FhirContext
    ): ResourceController<FhirCodeSystemMetadata, FhirCodeSystemMetadataRepository>(repository, fhirContext) {

    companion object{
        private val logger: Logger = LogManager.getLogger(this)
    }

    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun create(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    val csMetadata = repository.save(cs.toFhirCodeSystemMetadata())
                    // FIXME: Might be a bit too much to create another CodeSystem instance?
                    val createdCs = csMetadata.toCodeSystemResource().tagAsSummarized()
                    logger.info("Added code system [url: ${createdCs.url}, version: ${createdCs.version}] to database")
                    return ResponseEntity.created(URI(csMetadata.id.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"${csMetadata.versionId}\"")
                        .lastModified(csMetadata.lastUpdated!!.toInstant())
                        .body(jsonParser.encodeResourceToString(createdCs))
                } catch (e: Exception) {
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Addition of CodeSystem instance failed during database access")
                    logger.info(e.stackTraceToString())
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

    /*@PutMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun conditionalCreate(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        logger.info("Creating CodeSystem instance if not present")
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    if (cs.id != null && isPositiveInteger(cs.idPart)) {
                        try {
                            // NotFoundException is thrown if resource is not present
                            database.readCodeSystem(cs.idPart)
                            return ResponseEntity.ok().build()
                        }
                        catch (e: NotFoundException) { logger.debug("No CodeSystem instance with ID ${cs.idPart} present") }
                    }
                    val (createdCS, versionId, lastUpdated) = database.addCodeSystem(cs)
                    logger.info("Added CodeSystem instance [url: ${cs.url}, version: ${cs.version}] to database")
                    return ResponseEntity.created(URI(createdCS.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(createdCS))
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Addition of CodeSystem instance failed during database access")
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

    @GetMapping(path = ["{id}"])
    @ResponseBody
    fun read(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Reading CodeSystem instance [id = $id]")
        try{
            val cs = database.readCodeSystem(id)
            logger.debug("Found code systems with ID $id [url = ${cs.url} and version = ${cs.version}]")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(cs))
        }
        catch (e: NotFoundException) {
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.INFORMATION,
                OperationOutcome.IssueType.NOTFOUND,
                "No CodeSystem instance with ID $id",
                jsonParser
            )
            logger.debug(e.message)
            return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
        catch (e: Exception) {
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
                Parameters.ParametersParameterComponent("result").setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent("message").setValue(
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
    fun search(@RequestParam url: String): ResponseEntity<String>{
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
    }*/

}