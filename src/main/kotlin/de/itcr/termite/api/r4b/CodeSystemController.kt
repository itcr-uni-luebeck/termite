package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import de.itcr.termite.api.r4b.handler.*
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.api.*
import de.itcr.termite.exception.fhir.r4b.UnexpectedResourceTypeException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemPersistenceManager
import de.itcr.termite.util.*
import de.itcr.termite.util.r4b.parseCodeTypeParameterValue
import de.itcr.termite.util.r4b.tagAsSummarized
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.net.URI

private typealias IssueSeverity = OperationOutcome.IssueSeverity
private typealias IssueType = OperationOutcome.IssueType

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
            name = "code",
            type = "token",
            documentation = "A code defined in the code system",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "CodeSystem.concept",
                special = true
            )
        ),
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
    name = "ValueSet-validate-code",
    title = "ValueSet-validate-code",
    status = "active",
    kind = "operation",
    experimental = false,
    description = "Validate that a coded value is in the set of codes allowed by a value set",
    affectState = false,
    code = "validate-code",
    resource = ["ValueSet"],
    system = false,
    type = true,
    instance = true,
    parameter = [
        Parameter(
            name = "url",
            use = "in",
            min = 0,
            max = "1",
            documentation = "URL of the code system",
            type = "uri"
        ),
        Parameter(
            name = "code",
            use = "in",
            min = 1,
            max = "1",
            documentation = "Code of the coding to be validated",
            type = "code"
        ),
        Parameter(
            name = "version",
            use = "in",
            min = 0,
            max = "1",
            documentation = "The version of the code system, if one was provided in the source data",
            type = "string"
        ),
        Parameter(
            name = "display",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Display value of the coding",
            type = "uri"
        ),
        Parameter(
            name = "coding",
            use = "in",
            min = 0,
            max = "1",
            documentation = "A coding to validate",
            type = "Coding"
        ),
        Parameter(
            name = "result",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Indicates validity of the supplied concept details",
            type = "boolean"
        ),
        Parameter(
            name = "message",
            use = "out",
            min = 0,
            max = "1",
            documentation = "Error details, if result = false. If this is provided when result = true, the message carries hints and warnings",
            type = "string"
        ),
        Parameter(
            name = "display",
            use = "out",
            min = 0,
            max = "1",
            documentation = "A valid display for the concept if the system wishes to display this to a user",
            type = "string"
        )
    ]
)
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
            name = "code",
            use = "in",
            min = 0,
            max = "1",
            documentation = "The code that is to be located. If a code is provided, a system must be provided",
            type = "token"
        ),
        Parameter(
            name = "system",
            use = "in",
            min = 0,
            max = "1",
            documentation = "The system for the code that is to be located",
            type = "uri"
        ),
        Parameter(
            name = "version",
            use = "in",
            min = 0,
            max = "1",
            documentation = "The version of the system, if one was provided in the source data",
            type = "string"
        ),
        Parameter(
            name = "displayLanguage",
            use = "in",
            min = 0,
            max = "*",
            documentation = "The requested language for display",
            type = "token"
        ),
        Parameter(
            name = "property",
            use = "in",
            min = 0,
            max = "*",
            documentation = "A property that the client wishes to be returned in the output",
            type = "code"
        ),
        Parameter(
            name = "name",
            use = "out",
            min = 1,
            max = "1",
            documentation = "A display name for the code system",
            type = "string"
        ),
        Parameter(
            name = "version",
            use = "out",
            min = 0,
            max = "1",
            documentation = "The version that these details are based on",
            type = "string"
        ),
        Parameter(
            name = "display",
            use = "out",
            min = 0,
            max = "1",
            documentation = "The preferred display for this concept",
            type = "string"
        ),
        Parameter(
            name = "designation",
            use = "out",
            min = 0,
            max = "*",
            documentation = "Additional representations for this concept",
        ),
        Parameter(
            name = "designation.language",
            use = "out",
            min = 0,
            max = "1",
            documentation = "The language this designation is defined for",
            type = "code"
        ),
        Parameter(
            name = "designation.use",
            use = "out",
            min = 0,
            max = "1",
            documentation = "A code that details how this designation would be used",
            type = "Coding"
        ),
        Parameter(
            name = "designation.value",
            use = "out",
            min = 0,
            max = "1",
            documentation = "The text value for this designation",
            type = "string"
        ),
        Parameter(
            name = "property",
            use = "out",
            min = 0,
            max = "*",
            documentation = "One or more properties that contain additional information about the code, including status"
        ),
        Parameter(
            name = "property.code",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Identifies the property returned",
            type = "code"
        ),
        Parameter(
            name = "property.value",
            use = "out",
            min = 0,
            max = "1",
            documentation = "The value of the property returned",
            type = "code|Coding|string|integer|boolean|dateTime|decimal"
        ),
        Parameter(
            name = "property.description",
            use = "out",
            min = 0,
            max = "1",
            documentation = "Human Readable representation of the property value (e.g. display for a code)",
            type = "string"
        ),
        Parameter(
            name = "property.subproperty",
            use = "out",
            min = 0,
            max = "*",
            documentation = "Nested Properties (mainly used for SNOMED CT decomposition, for relationship Groups)"
        ),
        Parameter(
            name = "property.subproperty.code",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Identifies the sub-property returned",
            type = "code"
        ),
        Parameter(
            name = "property.subproperty.value",
            use = "out",
            min = 1,
            max = "1",
            documentation = "The value of the sub-property returned",
            type = "code|Coding|string|integer|boolean|dateTime|decimal"
        ),
        Parameter(
            name = "property.subproperty.description",
            use = "out",
            min = 0,
            max = "1",
            documentation = "Human Readable representation of the property value (e.g. display for a code)",
            type = "string"
        )
    ]
)
@RestController
@RequestMapping("fhir/CodeSystem")
class CodeSystemController(
    @Autowired override val persistence: CodeSystemPersistenceManager,
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
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?,
        @RequestHeader("Prefer") prefer: String?,
        @RequestHeader("If-None-Exists") ifNoneExists: String?
    ): ResponseEntity<String> {
        return try {
            logger.info("Received CREATE request for CodeSystem")
            if (ifNoneExists.isNullOrEmpty()) doCreate(requestEntity, contentType, accept)
            else doConditionalCreate(requestEntity, contentType, accept, prefer, ifNoneExists)
        }
        catch (e: UnsupportedFormatException) { return handleUnsupportedFormat(e, accept) }
        catch (e: DataFormatException) { return handleUnparsableEntity(e, accept) }
        catch (e: UnexpectedResourceTypeException) { return handleUnexpectedResourceType(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    private fun doCreate(
        requestEntity: RequestEntity<String>,
        @RequestHeader("Content-Type", defaultValue = "application/fhir+json") contentType: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?
    ): ResponseEntity<String> {
        val cs = parseBodyAsResource(requestEntity, contentType)
        if (cs is CodeSystem) {
            logger.debug("Creating CodeSystem instance [url: ${cs.url}, version: ${cs.version}]")
            val responseMediaType = determineResponseMediaType(accept, contentType)
            val createdCs = persistence.create(cs)
            logger.debug("Created CodeSystem instance [id: ${createdCs.id}, url: ${createdCs.url}, version: ${createdCs.version}]")
            return ResponseEntity.created(URI(createdCs.id))
                .contentType(responseMediaType)
                .eTag("W/\"${createdCs.meta.versionId}\"")
                .lastModified(createdCs.meta.lastUpdated.time)
                .body(encodeResourceToSting(createdCs, responseMediaType))
        }
        else { throw UnexpectedResourceTypeException(ResourceType.CodeSystem, (cs as Resource).resourceType) }
    }

    private fun doConditionalCreate(
        requestEntity: RequestEntity<String>,
        @RequestHeader("Content-Type", defaultValue = "application/fhir+json") contentType: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?,
        @RequestHeader("Prefer") prefer: String?,
        @RequestHeader("If-None-Exists") ifNoneExists: String
    ): ResponseEntity<String> {
        val handling = parsePreferHandling(prefer)
        val params = parseQueryParameters(ifNoneExists)
        val filteredParams = validateSearchParameters(params, handling, "${properties.api.baseUrl}/ValueSet", HttpMethod.POST)
        logger.debug("Creating CodeSystem instance if not exists [${parametersToString(filteredParams)}]")
        // TODO: Implement search version only returning number of matches or list of IDs thereof
        val matches = persistence.search(filteredParams)
        return when (matches.size) {
            0 -> doCreate(requestEntity, contentType, accept)
            1 -> {
                val responseMediaType = determineResponseMediaType(accept, contentType)
                val cs = matches[0]
                logger.debug("CodeSystem instance already exists [id: ${cs.id}, url: ${cs.url}, version: ${cs.version}]")
                ResponseEntity.ok()
                    .contentType(responseMediaType)
                    .eTag("W/\"${cs.meta.versionId}\"")
                    .lastModified(cs.meta.lastUpdated.time)
                    .body(encodeResourceToSting(cs, responseMediaType))
            }
            else -> handlePreconditionFailed(matches, accept)
        }
    }

    @PutMapping(
        path = ["{id}"],
        consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    fun update(
        requestEntity: RequestEntity<String>,
        @PathVariable id: String,
        @RequestHeader("Content-Type", defaultValue = "application/fhir+json") contentType: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?
    ): ResponseEntity<String> {
        logger.info("Received UPDATE request for CodeSystem")
        try {
            if (!isPositiveInteger(id)) throw IdFormatException(id)
            val idInt = id.toInt()
            val cs = parseBodyAsResource(requestEntity, contentType)
            val responseMediaType = determineResponseMediaType(accept, contentType)
            if (cs is CodeSystem) {
                logger.debug("Updating CodeSystem instance [id: ${id}, url: ${cs.url}, version: ${cs.version}]")
                val updatedCs = persistence.update(idInt, cs)
                logger.debug("Updated CodeSystem instance [id: ${updatedCs.id}, url: ${updatedCs.url}, version: ${updatedCs.version}]")
                return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(updatedCs, responseMediaType))
            }
            else { throw UnexpectedResourceTypeException(ResourceType.CodeSystem, (cs as Resource).resourceType) }
        }
        catch (e: UnsupportedFormatException) { return handleUnsupportedFormat(e, accept) }
        catch (e: DataFormatException) { return handleUnparsableEntity(e, accept) }
        catch (e: UnexpectedResourceTypeException) { return handleUnexpectedResourceType(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    @DeleteMapping(
        path = ["{id}"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun delete(
        @PathVariable id: String,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String
    ): ResponseEntity<String> {
        try{
            logger.info("Received DELETE request for CodeSystem")
            logger.debug("Deleting CodeSystem instance [id: $id]")
            val responseMediaType = determineResponseMediaType(accept)
            val cs = persistence.delete(id.toInt()).tagAsSummarized()
            logger.debug("Deleted CodeSystem instance [id: ${cs.id}, url: ${cs.url}, version: ${cs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(cs, responseMediaType))
        }
        catch (e: NotFoundException) { return handleNotFound(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
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
            val cs = persistence.read(id.toInt()).tagAsSummarized()
            logger.debug("Found CodeSystem instance [id: $id, url: ${cs.url}, version: ${cs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(cs, responseMediaType))
        }
        catch (e: NotFoundException) { return handleNotFound(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    // TODO: Implement paging
    @GetMapping(
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun search(
        @RequestParam params: MultiValueMap<String, String>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received SEARCH request for CodeSystem")
        try {
            logger.debug("Searching for CodeSystem instances with parameters [${parametersToString(params)}]")
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val filteredParams = validateSearchParameters(params, handling, "${properties.api.baseUrl}/CodeSystem", HttpMethod.GET)
            val instances = persistence.search(filteredParams).map { it.tagAsSummarized() }
            return ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(generateBundleString(Bundle.BundleType.SEARCHSET, instances, responseMediaType))
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    @GetMapping(
        path = ["\$validate-code", "{id}/\$validate-code"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun validateCode(
        @PathVariable(name = "id", required = false) id: String?,
        @RequestParam params: MultiValueMap<String, String>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received validate-code request for CodeSystem")
        try {
            logger.debug("Validating if coding is in CodeSystem with parameters [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val apiPath = "${properties.api.baseUrl}/CodeSystem${if (id != null) "/{id}" else ""}/\$validate-code"
            val curatedParams = validateOperationParameters("validate-code", params, handling, apiPath, HttpMethod.GET)
            //validateIdentifierPresenceInParameters(id, curatedParams)
            val outcome = persistence.validateCode(
                id?.toInt(),
                curatedParams
            )
            return if (outcome.hasLeft()) ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(encodeResourceToSting(outcome.left!!, responseMediaType))
            else if (outcome.hasRight()) {
                val opOutcome = outcome.right!!
                ValidateCodeHandler.handle(this, responseMediaType, opOutcome)
            }
            else throw NoResultException("Operation returned no result")
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    fun lookup(
        @RequestParam params: Map<String, List<String>>,
        @RequestHeader("Content-Type", defaultValue = "application/fhir+json") contentType: String?,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received CodeSystem-lookup request [${params.map { "${it.key}: '${it.value}'" }.joinToString(", ")}]")
        try {
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val filteredParams = validateSearchParameters(params, handling, "${properties.api.baseUrl}/CodeSystem", HttpMethod.GET)
            TODO("Not yet implemented")
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    private object ValidateCodeHandler: OperationOutcomeHandler(
        IssueType.NOTFOUND to HttpStatus.NOT_FOUND,
        IssueType.MULTIPLEMATCHES to HttpStatus.INTERNAL_SERVER_ERROR
    )

}