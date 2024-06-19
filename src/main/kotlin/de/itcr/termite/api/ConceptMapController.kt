package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.api.ValueSetController.Companion
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.util.generateOperationOutcomeString
import de.itcr.termite.util.generateParameters
import de.itcr.termite.util.generateParametersString
import de.itcr.termite.util.isPositiveInteger
import org.apache.coyote.Response
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.util.*

@ForResource(
    type = "ConceptMap",
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
        )
    ]
)
@SupportsInteraction(["create", "update", "search-type", "read", "delete"])
@SupportsOperation(
    name = "ConceptMap-translate",
    title = "ConceptMap-translate",
    status = "active",
    kind = "operation",
    experimental = false,
    description = "Translates a given concept to concepts in a target system",
    affectState = false,
    code = "translate",
    resource = ["ConceptMap"],
    system = false,
    type = true,
    instance = false,
    parameter = [
        Parameter(
            name = "code",
            use = "in",
            min = 1,
            max = "1",
            documentation = "Code of the source concept",
            type = "string"
        ),
        Parameter(
            name = "system",
            use = "in",
            min = 1,
            max = "1",
            documentation = "URL of the source system",
            type = "uri"
        ),
        Parameter(
            name = "version",
            use = "in",
            min = 0,
            max = "1",
            documentation = "Version of the source system",
            type = "string"
        ),
        Parameter(
            name = "targetSystem",
            use = "in",
            min = 1,
            max = "1",
            documentation = "URL of the target system",
            type = "uri"
        ),
        Parameter(
            name = "result",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Indicates successful translation",
            type = "boolean"
        ),
        Parameter(
            name = "match",
            use = "out",
            min = 0,
            max = "*",
            documentation = "Concept in the target system with some from of equivalence",
        ),
        Parameter(
            name = "match.equivalence",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Type of relationship between source and target concept",
            type = "code"
        ),
        Parameter(
            name = "concept",
            use = "out",
            min = 1,
            max = "1",
            documentation = "Target concept",
            type = "Coding"
        )
    ]
)
@Controller
@RequestMapping("fhir/ConceptMap")
class ConceptMapController(
    @Autowired database: TerminologyStorage,
    @Autowired fhirContext: FhirContext
): ResourceController(database, fhirContext) {

    companion object {
        private val logger: Logger = LogManager.getLogger(ConceptMapController::class.java)
    }

    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun create(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String> {
        logger.info("Creating ConceptMap instance")
        try{
            val cm = parseBodyAsResource(requestEntity, contentType)
            if(cm is ConceptMap) {
                try{
                    val (cmCreated, versionId, lastUpdated) = database.addConceptMap(cm)
                    logger.info("Added ConceptMap instance [url: ${cm.url}, version: ${cm.version}]")
                    return ResponseEntity.created(URI(cmCreated.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(cmCreated))
                }
                catch (e: Exception){
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Addition of ConceptMap instance failed during database access")
                    logger.debug(e.stackTraceToString())
                    return ResponseEntity.unprocessableEntity()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(opOutcome)
                }
            } else {
                val message = "Request body contained instance which was not of type ConceptMap but ${cm.javaClass.simpleName}"
                val opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.INVALID,
                    message,
                    jsonParser
                )
                logger.info(message)
                return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(opOutcome)
            }
        }
        catch (e: Exception){
            if(e is ResponseStatusException) throw e
            val message = "No parser was able to handle resource. The HTTP headers were: ${requestEntity.headers}"
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.STRUCTURE,
                message,
                jsonParser
            )
            logger.info(message)
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
    }

    @PutMapping(path = ["{id}"], consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun updateCreate(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String, @PathVariable(name = "id") id: String): ResponseEntity<String> {
        logger.info("Creating ConceptMap instance if not present")
        try {
            val cm = parseBodyAsResource(requestEntity, contentType)
            if (cm is ConceptMap) {
                try {
                    if (isPositiveInteger(id)) {
                        try {
                            // NotFoundException is thrown if resource is not present
                            database.readConceptMap(id)
                            return ResponseEntity.ok().build()
                        }
                        catch (e: NotFoundException) { logger.debug("No ConceptMap instance with ID $id present") }
                    }
                    val (createdCM, versionId, lastUpdated) = database.addConceptMap(cm)
                    logger.info("Added ConceptMap instance [url: ${cm.url}, version: ${cm.version}]")
                    return ResponseEntity.created(URI(createdCM.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(createdCM))
                } catch (e: Exception) {
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Addition of ConceptMap instance failed during database access")
                    logger.debug(e.stackTraceToString())
                    return ResponseEntity.unprocessableEntity()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(opOutcome)
                }
            } else {
                val message =
                    "Request body contained instance which was not of type ConceptMap but ${cm.javaClass.simpleName}"
                val opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.ERROR,
                    OperationOutcome.IssueType.INVALID,
                    message,
                    jsonParser
                )
                logger.info(message)
                return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(opOutcome)
            }
        }
        catch (e: Exception){
            if(e is ResponseStatusException) throw e
            val message = "No parser was able to handle resource. The HTTP headers were: ${requestEntity.headers}"
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.STRUCTURE,
                message,
                jsonParser
            )
            logger.info(message)
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(opOutcome)
        }
    }

    @GetMapping("{id}")
    @ResponseBody
    fun read(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Reading ConceptMap [id = $id]")
        try{
            val vs = database.readConceptMap(id)
            logger.debug("Found ConceptMap with ID $id [url = ${vs.url} and version = ${vs.version}]")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(vs))
        }
        catch (e: NotFoundException) {
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.INFORMATION,
                OperationOutcome.IssueType.NOTFOUND,
                "No ConceptMap instance with ID $id",
                jsonParser
            )
            logger.info(e.message)
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

    @GetMapping(params = ["url"])
    @ResponseBody
    fun search(@RequestParam url: String, @RequestParam(required = false) version: String?): ResponseEntity<String>{
        logger.info("Searching for ConceptMap instances [url = $url,  version = $version]")
        try{
            val cmList = database.searchConceptMap(url, version)
            val bundle = Bundle()
            bundle.id = UUID.randomUUID().toString()
            bundle.type = Bundle.BundleType.SEARCHSET
            bundle.total = cmList.size
            val baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            cmList.forEach { cm ->
                bundle.addEntry(
                    Bundle.BundleEntryComponent()
                        .setFullUrl("http://$baseURL/${cm.id}")
                        .setResource(cm)
                )
            }
            logger.info("Found ${cmList.size} ConceptMap instance(s) for URL $url and version $version")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(bundle))
        }
        catch(e: Exception){
            val message = "Search for ConceptMap instances [url = $url, version = $version] failed"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

    @DeleteMapping("{id}")
    @ResponseBody
    fun delete(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Deleting ConceptMap instance [id = $id]")
        try {
            var opOutcome: String
            try {
                database.deleteConceptMap(id)
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "Successfully deleted ConceptMap instance [ID = $id]",
                    jsonParser
                )
            }
            catch (e: NotFoundException) {
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "No such ConceptMap instance [ID = $id]",
                    jsonParser
                )
            }
            return ResponseEntity.ok().eTag("W/\"0\"").contentType(MediaType.APPLICATION_JSON).body(opOutcome)
        }
        catch (e: Exception) {
            val message = "Failed to delete ValueSet instance with ID $id. Reason: ${e.message}"
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.ERROR,
                OperationOutcome.IssueType.PROCESSING,
                message,
                jsonParser
            )
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(opOutcome)
        }
    }

    @GetMapping("\$translate")
    @ResponseBody
    fun translate(
        @RequestParam code: String,
        @RequestParam system: String,
        @RequestParam(required = false) version: String?,
        @RequestParam targetSystem: String
    ): ResponseEntity<String> {
        logger.info("Translating concept [URL = $system, code = $code, version = $version] to target system [URL = $targetSystem]")
        try {
            val results = database.translate(Coding(system, code, version), targetSystem, false)
            val parameters = Parameters()
            if (results.isEmpty()) {
                parameters.addParameter("result", false)
            } else {
                parameters.addParameter("result", true)
                results.forEach {
                    val match = Parameters.ParametersParameterComponent("match")
                    match.addPart().setName("equivalence").setValue(CodeType(it.first.toCode()))
                    match.addPart().setName("concept").setValue(it.second)
                    parameters.addParameter(match)
                }
            }
            return ResponseEntity.ok().body(jsonParser.encodeResourceToString(parameters))
        } catch (e: Exception) {
            val message = "Translation of concept failed. Reason: ${e.message}"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

}