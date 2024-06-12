package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.api.ValueSetController.Companion
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.util.generateOperationOutcomeString
import de.itcr.termite.util.generateParametersString
import de.itcr.termite.util.isPositiveInteger
import de.itcr.termite.util.parseParameters
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.util.*

@ForResource(
    type = "CodeSystem",
    versioning = "no-version",
    readHistory = false,
    updateCreate = true,
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
        ),
        SearchParameter(
            name = "version",
            type = "string",
            documentation = "Version of the resource to locate"
        )
    ]
)
@SupportsInteraction(["create", "search-type", "delete"])
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
    fun create(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    val (createdCS, versionId, lastUpdated) = database.addCodeSystem(cs)
                    logger.info("Added code system [url: ${cs.url}, version: ${cs.version}] to database")
                    return ResponseEntity.created(URI(createdCS.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(createdCS))
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

    @PutMapping(path = ["{id}"], consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun updateCreate(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String, @PathVariable(name = "id") id: String): ResponseEntity<String> {
        logger.info("Creating CodeSystem instance if not present")
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    if (isPositiveInteger(id)) {
                        try {
                            // NotFoundException is thrown if resource is not present
                            database.readCodeSystem(id)
                            return ResponseEntity.ok().build()
                        }
                        catch (e: NotFoundException) { logger.debug("No CodeSystem instance with ID ${id} present") }
                    }
                    val (createdCS, versionId, lastUpdated) = database.addCodeSystem(cs)
                    logger.info("Added CodeSystem instance [url: ${cs.url}, version: ${cs.version}] to database")
                    return ResponseEntity.created(URI(createdCS.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(createdCS))
                } catch (e: Exception) {
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

    @GetMapping
    @ResponseBody
    fun search(
        @RequestParam url: String,
        @RequestParam(required = false) version: String?
    ): ResponseEntity<String>{
        logger.info("Searching for code system [url = $url]")
        try{
            val csList = database.searchCodeSystem(url, version)
            val bundle = Bundle()
            bundle.id = UUID.randomUUID().toString()
            bundle.type = Bundle.BundleType.SEARCHSET
            bundle.total = csList.size
            //Should be faster since otherwise an internal array list would have to resized all the time
            bundle.entry = csList.map { cs -> Bundle.BundleEntryComponent().setResource(cs) }
            logger.debug("Found ${csList.size} CodeSystem instances for URL $url" + if (version != null) " and version $version" else "")
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

    @DeleteMapping("{id}")
    @ResponseBody
    fun delete(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Deleting CodeSystem instance [id = $id]")
        try {
            var opOutcome: String
            try {
                database.deleteCodeSystem(id)
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "Successfully deleted CodeSystem instance [ID = $id]",
                    jsonParser
                )
            }
            catch (e: NotFoundException) {
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "No CodeSystem instance with such an ID [ID = $id]",
                    jsonParser
                )
            }
            return ResponseEntity.ok().eTag("W/\"0\"").contentType(MediaType.APPLICATION_JSON).body(opOutcome)
        }
        catch (e: Exception) {
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.PROCESSING,
                e.message,
                jsonParser
            )
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(opOutcome)
        }
    }

}