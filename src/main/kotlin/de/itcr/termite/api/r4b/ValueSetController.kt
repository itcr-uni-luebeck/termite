package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import de.itcr.termite.api.r4b.exc.*
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.api.*
import de.itcr.termite.exception.fhir.r4b.UnexpectedResourceTypeException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.persistence.r4b.valueset.ValueSetPersistenceManager
import de.itcr.termite.util.isPositiveInteger
import de.itcr.termite.util.parametersToString
import de.itcr.termite.util.parsePreferHandling
import de.itcr.termite.util.parseQueryParameters
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumeration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * Handles request regarding instances of the ValueSet resource
 */

@ForResource(
    type = "ValueSet",
    versioning = "no-version",
    readHistory = false,
    updateCreate = false,
    conditionalCreate = true,
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
            documentation = "A coding in the value set",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "ValueSet.compose.include.concept",
                special = true
            )
        ),
        /*
        SearchParameter(
            name = "context",
            type = "token",
            documentation = "A use context assigned to the value set",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "(ValueSet.useContext.value as CodeableConcept)"
            )
        ),*/
        SearchParameter(
            name = "context-type",
            type = "token",
            documentation = "A type of use context assigned to the value set",
            processing =  ProcessingHint(
                targetType = Coding::class,
                elementPath = "ValueSet.useContext.code"
            )
        ),
        SearchParameter(
            name = "date",
            type = "date",
            documentation = "The code system publication date",
            processing = ProcessingHint(
                targetType = DateTimeType::class,
                elementPath = "ValueSet.date"
            )
        ),
        SearchParameter(
            name = "description",
            type = "string",
            documentation = "The description of the value set",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "ValueSet.description"
            )
        ),
        SearchParameter(
            name = "identifier",
            type = "token",
            documentation = "External identifier for the value set",
            processing = ProcessingHint(
                targetType = Identifier::class,
                elementPath = "ValueSet.identifier"
            )
        ),
        SearchParameter(
            name = "jurisdiction",
            type = "token",
            documentation = "Intended jurisdiction for the value set",
            processing = ProcessingHint(
                targetType = Coding::class,
                elementPath = "ValueSet.jurisdiction.coding"
            )
        ),
        SearchParameter(
            name = "name",
            type = "string",
            documentation = "Computationally friendly name of the value set",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "ValueSet.name"
            )
        ),
        SearchParameter(
            name = "publisher",
            type = "string",
            documentation = "Name of the publisher of the value set",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "ValueSet.publisher"
            )
        ),
        SearchParameter(
            name = "status",
            type = "token",
            documentation = "The current status of the value set",
            processing = ProcessingHint(
                targetType = Enumeration::class,
                elementPath = "ValueSet.status"
            )
        ),
        SearchParameter(
            name = "title",
            type = "string",
            documentation = "The human-friendly name of the value set",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "ValueSet.title"
            )
        ),
        SearchParameter(
            name = "url",
            type = "uri",
            documentation = "The uri that identifies the value set",
            processing = ProcessingHint(
                targetType = UriType::class,
                elementPath = "ValueSet.url"
            )
        ),
        SearchParameter(
            name = "version",
            type = "token",
            documentation = "The business version of the value set",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "ValueSet.version"
            )
        ),
        SearchParameter(
            name = "system",
            type = "uri",
            documentation = "Code system the value set contains codes of",
            processing = ProcessingHint(
                targetType = UriType::class,
                elementPath = "ValueSet.compose.include.system"
            )
        )
    ]
)
@SupportsInteraction(["create", "update", "read", "delete", "search-type"])
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
            documentation = "URL of the value set",
            type = "uri"
        ),
        Parameter(
            name = "valueSetVersion",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Version of the value set",
            type = "string"
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
            name = "system",
            use = "in",
            min = 1,
            max = "1",
            documentation = "System from which the coding originates",
            type = "uri"
        ),
        Parameter(
            name = "systemVersion",
            use = "in",
            min = 0,
            max = "1",
            documentation = "System from which the coding originates",
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
    name = "ValueSet-expand",
    title = "ValueSet-expand",
    status = "active",
    kind = "operation",
    experimental = false,
    description = "Expands a value set, returning an explicit list of codings it contains",
    affectState = false,
    code = "expand",
    resource = ["ValueSet"],
    system = false,
    type = true,
    instance = true,
    parameter = [
        Parameter(
            name = "url",
            use = "in",
            min = 1,
            max = "1",
            documentation = "URL of the ValueSet instance",
            type = "uri"
        ),
        Parameter(
            name = "valueSetVersion",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Version of the ValueSet instance",
            type = "string"
        ),
        Parameter(
            name = "includeDesignations",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Controls whether concept designations are to be included or excluded in value set expansions",
            type = "boolean"
        ),
        Parameter(
            name = "designation",
            use = "in",
            min = 0,
            max = "*",
            documentation = "A token that specifies a system+code that is either a use or a language. Designations " +
                    "that match by language or use are included in the expansion",
            type = "string"
        ),
        Parameter(
            name = "activeOnly",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Controls whether inactive concepts are included or excluded in value set expansions",
            type = "boolean"
        ),
        Parameter(
            name = "displayLanguage",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Specifies the language to be used for description in the expansions i.e. the language to " +
                    "be used for ValueSet.expansion.contains.display",
            type = "code"
        ),
        Parameter(
            name = "exclude-system",
            use = "in",
            min = 0,
            max = "*",
            documentation = "Code system, or a particular version of a code system to be excluded from the value set " +
                    "expansion. The format is the same as a canonical URL: [system]|[version]",
            type = "canonical"
        ),
        Parameter(
            name = "system-version",
            use = "in",
            min = 0,
            max = "*",
            documentation = "Specifies a version to use for a system, if the value set does not specify which one to " +
                    "use. The format is the same as a canonical URL: [system]|[version]",
            type = "canonical"
        ),
        Parameter(
            name = "return",
            use = "out",
            min = 1,
            max = "1",
            documentation = "The result of the expansion. Note: as this is the only out parameter, it is a resource, " +
                    "and it has the name 'return', the result of this operation is returned directly as a resource",
            type = "ValueSet"
        )
    ]
)
@Controller
@RequestMapping("fhir/ValueSet")
class ValueSetController(
    @Autowired override val persistence: ValueSetPersistenceManager,
    @Autowired fhirContext: FhirContext,
    @Autowired properties: ApplicationConfig
): ResourceController<ValueSet, Int>(persistence, fhirContext, properties, logger) {

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
            logger.info("Received CREATE request for ValueSet")
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
        val vs = parseBodyAsResource(requestEntity, contentType)
        if (vs is ValueSet) {
            logger.debug("Creating ValueSet instance [url: ${vs.url}, version: ${vs.version}]")
            val responseMediaType = determineResponseMediaType(accept, contentType)
            val createdVs = persistence.create(vs)
            logger.debug("Created ValueSet instance [id: ${createdVs.id}, url: ${createdVs.url}, version: ${createdVs.version}]")
            return ResponseEntity.created(URI(createdVs.id))
                .contentType(responseMediaType)
                .eTag("W/\"${createdVs.meta.versionId}\"")
                .lastModified(createdVs.meta.lastUpdated.time)
                .body(encodeResourceToSting(createdVs, responseMediaType))
        }
        else { throw UnexpectedResourceTypeException(ResourceType.ValueSet, (vs as Resource).resourceType) }
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
        logger.debug("Creating ValueSet instance if not exists [${parametersToString(filteredParams)}]")
        // TODO: Implement search version only returning number of matches or list of IDs thereof
        val matches = persistence.search(filteredParams)
        return when (matches.size) {
            0 -> doCreate(requestEntity, contentType, accept)
            1 -> {
                val responseMediaType = determineResponseMediaType(accept, contentType)
                val vs = matches[0]
                logger.debug("ValueSet instance already exists [id: ${vs.id}, url: ${vs.url}, version: ${vs.version}]")
                ResponseEntity.ok()
                    .contentType(responseMediaType)
                    .eTag("W/\"${vs.meta.versionId}\"")
                    .lastModified(vs.meta.lastUpdated.time)
                    .body(encodeResourceToSting(vs, responseMediaType))
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
        logger.info("Received UPDATE request for ValueSet")
        try {
            if (!isPositiveInteger(id)) throw IdFormatException(id)
            val idInt = id.toInt()
            val vs = parseBodyAsResource(requestEntity, contentType)
            val responseMediaType = determineResponseMediaType(accept, contentType)
            if (vs is ValueSet) {
                logger.debug("Updating ValueSet instance [id: ${id}, url: ${vs.url}, version: ${vs.version}]")
                /*
                if (persistence.exists(idInt)) {
                    logger.info("Updating ValueSet instance [id: ${id}, url: ${vs.url}, version: ${vs.version}]")
                    val updatedVs = persistence.update(idInt, vs)
                }
                else {
                    logger.info("Creating ValueSet instance [id: ${id}, url: ${vs.url}, version: ${vs.version}]")
                }
                */
                val updatedVs = persistence.update(idInt, vs)
                logger.debug("Updated ValueSet instance [id: ${updatedVs.id}, url: ${updatedVs.url}, version: ${updatedVs.version}]")
                return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(updatedVs, responseMediaType))
            }
            else { throw UnexpectedResourceTypeException(ResourceType.ValueSet, (vs as Resource).resourceType) }
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
            logger.info("Received DELETE request for ValueSet")
            logger.debug("Deleting ValueSet instance [id: $id]")
            val responseMediaType = determineResponseMediaType(accept)
            val vs = persistence.delete(id.toInt())
            logger.debug("Deleted ValueSet instance [id: ${vs.id}, url: ${vs.url}, version: ${vs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(vs, responseMediaType))
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
        logger.info("Reading ValueSet instance [id: $id]")
        try{
            val responseMediaType = determineResponseMediaType(accept)
            val vs = persistence.read(id.toInt())
            logger.debug("Found ValueSet instance [id: $id, url: ${vs.url}, version: ${vs.version}]")
            return ResponseEntity.ok().contentType(responseMediaType).body(encodeResourceToSting(vs, responseMediaType))
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
        @RequestParam params: Map<String, List<String>>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received SEARCH request for ValueSet")
        try {
            logger.debug("Searching for ValueSet instances with parameters [${parametersToString(params)}]")
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val filteredParams = validateSearchParameters(params, handling, "${properties.api.baseUrl}/ValueSet", HttpMethod.GET)
            val instances = persistence.search(filteredParams)
            return ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(generateBundleString(Bundle.BundleType.SEARCHSET, instances, responseMediaType))
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    // TODO: Allow multiple search parameters with same name
    @GetMapping(
        path = ["\$validate-code", "{id}/\$validate-code"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun validateCode(
        @PathVariable(name = "id", required = false) id: String?,
        @RequestParam params: Map<String, List<String>>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received validate-code request for ValueSet")
        try {
            logger.debug("Validating if coding is in ValueSet with parameters [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val apiPath = "${properties.api.baseUrl}/ValueSet${if (id != null) "/{id}" else ""}/\$validate-code"
            val filteredParams = validateOperationParameters("validate-code", params, handling, apiPath, HttpMethod.GET)
            validateIdentifierPresenceInParameters(id, filteredParams)
            val parameters = persistence.validateCode(
                id?.toInt(),
                filteredParams["url"]?.getOrNull(0),
                filteredParams["valueSetVersion"]?.getOrNull(0),
                filteredParams["code"]!![0],
                filteredParams["system"]!![0],
                filteredParams["systemVersion"]?.getOrNull(0),
                filteredParams["display"]?.getOrNull(0))
            return ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(encodeResourceToSting(parameters, responseMediaType))
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

    @GetMapping(
        path = ["\$expand", "{id}/\$expand"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun expand(
        @PathVariable(name = "id", required = false) id: String?,
        @RequestParam params: Map<String, List<String>>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String> {
        logger.info("Received expand request for ValueSet")
        try {
            logger.debug("Expanding ValueSet with parameters [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val apiPath = "${properties.api.baseUrl}/ValueSet${if (id != null) "/{id}" else ""}/\$expand"
            val filteredParams = validateOperationParameters("expand", params, handling, apiPath, HttpMethod.GET)
            validateIdentifierPresenceInParameters(id, filteredParams)
            TODO("Not yet implemented")
        }
        catch (e: UnsupportedValueException) { return handleUnsupportedParameterValue(e, accept) }
        catch (e: UnsupportedParameterException) { return handleUnsupportedParameter(e, accept) }
        catch (e: PersistenceException) { return handlePersistenceException(e, accept) }
        catch (e: Throwable) { return handleUnexpectedError(e, accept) }
    }

/*    @GetMapping(path = ["\$expand"])
    @ResponseBody
    fun expand(@RequestParam url: String, @RequestParam(required = false) valueSetVersion: String?): ResponseEntity<String>{
        logger.info("Expanding value set [url = $url,  version = $valueSetVersion]")
        try {
            val vs = database.expandValueSet(url, valueSetVersion)
            logger.info(jsonParser.encodeResourceToString(vs))
            logger.debug("Found value set for URL $url and value set version $valueSetVersion")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(vs))
        }
        catch (e: ValueSetException) {
            val message = "Value set with URL $url and version $valueSetVersion not found"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.NOTFOUND,
                e.message,
                jsonParser
            )
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
        catch(e: Exception){
            val message = "Expanding ValueSet [url = $url, version = $valueSetVersion] failed"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }
*/

    private fun validateIdentifierPresenceInParameters(id: String?, params: Map<String, List<String>>) {
        if (id == null && "url" !in params)
            throw MissingParameterException("Parameter 'url' is required for type-level variant")
    }

}