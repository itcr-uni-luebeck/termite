package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import de.itcr.termite.api.r4b.exc.*
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.api.UnsupportedFormatException
import de.itcr.termite.exception.api.UnsupportedParameterException
import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.exception.fhir.r4b.UnexpectedResourceTypeException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemPersistenceManager
import de.itcr.termite.util.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.net.URI

typealias IssueSeverity = OperationOutcome.IssueSeverity
typealias IssueType = OperationOutcome.IssueType

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
            name = "content-mode",
            type = "token",
            documentation = "not-present | example | fragment | complete | supplement",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.contentMode"
            )
        ),
        SearchParameter(
            name = "context",
            type = "token",
            documentation = "A use context assigned to the code system",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "(CodeSystem.useContext.value as CodeableConcept)"
            )
        ),
        SearchParameter(
            name = "context-type",
            type = "token",
            documentation = "A type of use context assigned to the code system",
            processing =  ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.useContext.code"
            )
        ),
        SearchParameter(
            name = "date",
            type = "date",
            documentation = "The code system publication date",
            processing = ProcessingHint(
                targetType = DateTimeType::class,
                elementPath = "CodeSystem.date"
            )
        ),
        SearchParameter(
            name = "description",
            type = "string",
            documentation = "The description of the code system",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.description"
            )
        ),
        SearchParameter(
            name = "identifier",
            type = "token",
            documentation = "External identifier for the code system",
            processing = ProcessingHint(
                targetType = Identifier::class,
                elementPath = "CodeSystem.identifier"
            )
        ),
        SearchParameter(
            name = "jurisdiction",
            type = "token",
            documentation = "Intended jurisdiction for the code system",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "CodeSystem.jurisdiction"
            )
        ),
        SearchParameter(
            name = "name",
            type = "string",
            documentation = "Computationally friendly name of the code system",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.name"
            )
        ),
        SearchParameter(
            name = "publisher",
            type = "string",
            documentation = "Name of the publisher of the code system",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.publisher"
            )
        ),
        SearchParameter(
            name = "status",
            type = "token",
            documentation = "The current status of the code system",
            processing = ProcessingHint(
                targetType = Enumeration::class,
                elementPath = "CodeSystem.status"
            )
        ),
        SearchParameter(
            name = "url",
            type = "uri",
            documentation = "The uri that identifies the code system",
            processing = ProcessingHint(
                targetType = UriType::class,
                elementPath = "CodeSystem.url"
            )
        ),
        SearchParameter(
            name = "system",
            documentation = "The system for any codes defined by this code system",
            sameAs = "url"
        ),
        SearchParameter(
            name = "title",
            type = "string",
            documentation = "The human-friendly name of the code system",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.title"
            )
        ),
        SearchParameter(
            name = "version",
            type = "token",
            documentation = "The business version of the code system",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "CodeSystem.version"
            )
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
    @Autowired persistence: CodeSystemPersistenceManager,
    @Autowired fhirContext: FhirContext,
    @Autowired properties: ApplicationConfig
    ): ResourceController<CodeSystem, Int>(persistence, fhirContext, properties, logger) {

    companion object{
        private val logger: Logger = LogManager.getLogger(this)
    }

    @PostMapping(
        consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun create(
        requestEntity: RequestEntity<String>,
        @RequestHeader("Content-Type", defaultValue = "application/fhir+json") contentType: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?
    ): ResponseEntity<String> {
        try {
            val cs = parseBodyAsResource(requestEntity, contentType)
            if (cs is CodeSystem) {
                try {
                    val responseMediaType = determineResponseMediaType(accept, contentType)
                    val createdCs = persistence.create(cs)
                    logger.info("Created CodeSystem instance [id: ${createdCs.id}, url: ${createdCs.url}, version: ${createdCs.version}]")
                    return ResponseEntity.created(URI(createdCs.id))
                        .contentType(responseMediaType)
                        .eTag("W/\"${createdCs.meta.versionId}\"")
                        .lastModified(createdCs.meta.lastUpdated.time)
                        .body(encodeResourceToSting(createdCs, responseMediaType))
                }
                catch (e: PersistenceException) {
                    logger.warn(e.stackTraceToString())
                    return handleException(
                        e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR, IssueType.PROCESSING,
                        "Creation of CodeSystem instance failed during database access. Reason: {e}"
                    )
                }
            } else { throw UnexpectedResourceTypeException(ResourceType.CodeSystem, (cs as Resource).resourceType) }
        }
        catch (e: UnsupportedFormatException) { return handleUnsupportedFormat(e, accept) }
        catch (e: DataFormatException) { return handleUnparsableEntity(e, accept) }
        catch (e: UnexpectedResourceTypeException) { return handleUnexpectedResourceType(e, accept) }
        catch (e: Exception) { return handleException(e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR, IssueType.PROCESSING, "Unexpected error: {e}") }
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
    }*/

    @DeleteMapping(
        path = ["{id}"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun delete(
        @PathVariable id: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String
    ): ResponseEntity<String> {
        logger.info("Deleting CodeSystem instance [id: $id]")
        try{
            val responseMediaType = determineResponseMediaType(accept)
            val cs = persistence.delete(id.toInt())
            logger.debug("Deleted CodeSystem instance [id: $id, url: ${cs.url}, version: ${cs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(cs, responseMediaType))
        }
        catch (e: NotFoundException) { return handleNotFound(e, accept) }
        catch (e: Exception) { return handleException(e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR,
            IssueType.PROCESSING, "Deletion of CodeSystem instance failed during database access. Reason: {e}"
        ) }
    }

    @GetMapping(
        path = ["{id}"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun read(
        @PathVariable id: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String
    ): ResponseEntity<String> {
        logger.info("Reading CodeSystem instance [id: $id]")
        try{
            val responseMediaType = determineResponseMediaType(accept)
            val cs = persistence.read(id.toInt())
            logger.debug("Found CodeSystem instance [id: $id, url: ${cs.url}, version: ${cs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(cs, responseMediaType))
        }
        catch (e: NotFoundException) { return handleNotFound(e, accept) }
        catch (e: Exception) { return handleException(e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR,
            IssueType.PROCESSING, "Reading of CodeSystem instance failed during database access. Reason: {e}"
        ) }
    }
/*
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
*/

    // TODO: Implement paging
    @GetMapping(
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun search(
        @RequestParam params: Map<String, String>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received search request for CodeSystem [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
        try {
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val filteredParams = validateSearchParameters(params, handling, "${properties.api.baseUrl}/CodeSystem", HttpMethod.GET)
            val instances = persistence.search(filteredParams)
            return ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(generateBundleString(Bundle.BundleType.SEARCHSET, instances, responseMediaType))
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: Exception) { e.printStackTrace(); return handleException(e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR,
            IssueType.PROCESSING, "Unexpected internal error: {e}") }
    }

}