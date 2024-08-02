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
import de.itcr.termite.persistence.r4b.valueset.ValueSetPersistenceManager
import de.itcr.termite.util.parsePreferHandling
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumeration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * Handles request regarding instances of the ValueSet resource
 */

@ForResource(
    type = "ValueSet",
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
            name = "context",
            type = "token",
            documentation = "A use context assigned to the value set",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "(ValueSet.useContext.value as CodeableConcept)"
            )
        ),
        SearchParameter(
            name = "context-type",
            type = "token",
            documentation = "A type of use context assigned to the value set",
            processing =  ProcessingHint(
                targetType = StringType::class,
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
                targetType = CodeType::class,
                elementPath = "ValueSet.jurisdiction"
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
@SupportsInteraction(["create", "read", "delete", "search-type"])
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
            min = 1,
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
            min = 1,
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
    description = "Expands a value set, returning an explicit list of all codings it contains",
    affectState = false,
    code = "expand",
    resource = ["ValueSet"],
    system = false,
    type = true,
    instance = false,
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
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String?
    ): ResponseEntity<String>{
        try {
            val vs = parseBodyAsResource(requestEntity, contentType)
            if (vs is ValueSet) {
                logger.info("Creating ValueSet instance [id: ${vs.id}, url: ${vs.url}, version: ${vs.version}]")
                val responseMediaType = determineResponseMediaType(accept, contentType)
                val createdVs = persistence.create(vs)
                logger.debug("Created ValueSet instance [id: ${createdVs.id}, url: ${createdVs.url}, version: ${createdVs.version}]")
                return ResponseEntity.created(URI(createdVs.id))
                    .contentType(responseMediaType)
                    .eTag("W/\"${createdVs.meta.versionId}\"")
                    .lastModified(createdVs.meta.lastUpdated.time)
                    .body(encodeResourceToSting(createdVs, responseMediaType))
            }
            else { throw UnexpectedResourceTypeException(ResourceType.CodeSystem, (vs as Resource).resourceType) }
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
            logger.info("Deleting ValueSet instance [id: $id]")
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
        @RequestParam params: Map<String, String>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received search request for ValueSet [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
        try {
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

    @GetMapping(
        path = ["\$validate-code", "{id}/\$validate-code"],
        produces = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"]
    )
    @ResponseBody
    fun validateCode(
        @RequestParam(name = "id", required = false) id: String?,
        @RequestParam params: Map<String, String>,
        @RequestHeader("Accept", defaultValue = "application/fhir+json") accept: String,
        @RequestHeader("Prefer") prefer: String?
    ): ResponseEntity<String>{
        logger.info("Received validate-code request for ValueSet [${params.map { "${it.key} = '${it.value}'" }.joinToString(", ")}]")
        try {
            val responseMediaType = determineResponseMediaType(accept)
            val handling = parsePreferHandling(prefer)
            val apiPath = "${properties.api.baseUrl}/ValueSet${if (id != null) "/{id}" else ""}/\$validate-code"
            val filteredParams = validateOperationParameters("validate-code", params, handling, apiPath, HttpMethod.GET)
            val instances = persistence.validateCode(
                id?.toInt(),
                filteredParams["url"],
                filteredParams["valueSetVersion"],
                filteredParams["code"],
                filteredParams["system"],
                filteredParams["systemVersion"],
                filteredParams[""])
            return ResponseEntity.ok()
                .contentType(responseMediaType)
                .body(generateBundleString(Bundle.BundleType.SEARCHSET, instances, responseMediaType))
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

}