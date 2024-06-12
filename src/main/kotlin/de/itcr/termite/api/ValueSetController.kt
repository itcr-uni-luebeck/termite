package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.api.CodeSystemController.Companion
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.ValueSetException
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.util.*
import kotlin.Exception
import kotlin.math.min

/**
 * Handles request regarding instances of the ValueSet resource
 */
@ForResource(
    type = "ValueSet",
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
    name = "ValueSet-lookup",
    title = "ValueSet-lookup",
    status = "active",
    kind = "operation",
    experimental = false,
    description = "Checks whether a given concept is in a value set",
    affectState = false,
    code = "lookup",
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
    @Autowired database: TerminologyStorage,
    @Autowired fhirContext: FhirContext
): ResourceController(database, fhirContext) {

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
                    val (vsCreated, versionId, lastUpdated) = database.addValueSet(vs)
                    logger.info("Added value set [url: ${vs.url}, version: ${vs.version}] to database")
                    return ResponseEntity.created(URI(vsCreated.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(vsCreated))
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

    @PutMapping(path = ["{id}"], consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun updateCreate(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String, @PathVariable(name = "id") id: String): ResponseEntity<String> {
        logger.info("Creating ValueSet instance if not present")
        try {
            val vs = parseBodyAsResource(requestEntity, contentType)
            if (vs is ValueSet) {
                try {
                    if (isPositiveInteger(id)) {
                        try {
                            // NotFoundException is thrown if resource is not present
                            database.readValueSet(id)
                            return ResponseEntity.ok().build()
                        }
                        catch (e: NotFoundException) { logger.debug("No ValueSet instance with ID $id present") }
                    }
                    val (createdVS, versionId, lastUpdated) = database.addValueSet(vs)
                    logger.info("Added ValueSet instance [url: ${vs.url}, version: ${vs.version}] to database")
                    return ResponseEntity.created(URI(createdVS.id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .eTag("W/\"$versionId\"")
                        .lastModified(lastUpdated.time)
                        .body(jsonParser.encodeResourceToString(createdVS))
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.PROCESSING,
                        e.message,
                        jsonParser
                    )
                    logger.warn("Addition of ValueSet instance failed during database access")
                    logger.debug(e.stackTraceToString())
                    return ResponseEntity.unprocessableEntity()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(opOutcome)
                }
            } else {
                val message =
                    "Request body contained instance which was not of type ValueSet but ${vs.javaClass.simpleName}"
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

    @GetMapping("{id}")
    @ResponseBody
    fun readValueSet(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Reading code system [id = $id]")
        try{
            val vs = database.readValueSet(id)
            logger.debug("Found value set with ID $id [url = ${vs.url} and version = ${vs.version}]")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonParser.encodeResourceToString(vs))
        }
        catch (e: NotFoundException) {
            val opOutcome = generateOperationOutcomeString(
                OperationOutcome.IssueSeverity.INFORMATION,
                OperationOutcome.IssueType.NOTFOUND,
                "No ValueSet instance with ID $id",
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

    @GetMapping(params = ["url"])
    @ResponseBody
    fun searchValueSet(@RequestParam url: String, @RequestParam(required = false) valueSetVersion: String?): ResponseEntity<String>{
        logger.info("Searching for value set [url = $url,  version = $valueSetVersion]")
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
            logger.debug("Found ${vsList.size} value sets for URL $url and value set version $valueSetVersion")
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

    @DeleteMapping("{id}")
    @ResponseBody
    fun delete(@PathVariable id: String): ResponseEntity<String> {
        logger.info("Deleting ValueSet instance [id = $id]")
        try {
            var opOutcome: String
            try {
                database.deleteValueSet(id)
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "Successfully deleted ValueSet instance [ID = $id]",
                    jsonParser
                )
            }
            catch (e: NotFoundException) {
                opOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.INFORMATIONAL,
                    "No ValueSet instance with such an ID [ID = $id]",
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
        var mutableUrl = url
        val urlParts = mutableUrl.split("|")
        mutableUrl = if (urlParts.isNotEmpty()) urlParts[0] else mutableUrl
        logger.info("Validating code [system=$system, code=$code, display=$display] against value set [url=$mutableUrl, version=$valueSetVersion]")
        try{
            val (result, version) = database.validateCodeVS(mutableUrl, valueSetVersion, system, code, display)
            val body = generateParametersString(
                jsonParser,
                Parameters.ParametersParameterComponent("result").setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent("message").setValue(StringType(
                    "Code [system = $system and code = $code] ${if(result) "was" else "wasn't"} in value set " +
                            "[url = $mutableUrl and version = $version]"
                ))
            )
            logger.info("Validated if code [system = $system, code = $code${if(display != null) ", display = $display" else ""}] is in value set [url = $mutableUrl, version = $version]: $result")
            return ResponseEntity.ok().body(body)
        }
        catch (e: Exception){
            val message = "Failed to validate code [system = $system, code = $code${if(display != null) ", display = $display" else ""}] against value system [url = $mutableUrl${if(valueSetVersion != null) ", version = $valueSetVersion" else ""}]"
            logger.warn(message)
            logger.debug(e.stackTraceToString())
            throw ResponseStatusException(
                //TODO: Here INTERNAL_SERVER_ERROR or UNPROCESSABLE_ENTITY?
                HttpStatus.INTERNAL_SERVER_ERROR,
                message
            )
        }
    }

    @PostMapping(path = ["\$validate-code"])
    @ResponseBody
    fun validateCode(requestEntity: RequestEntity<String>, @RequestHeader("Content-Type") contentType: String): ResponseEntity<String>{
        logger.info("POST: Validating code against value set with request body: ${requestEntity.body}")
        try{
            val parameters = parseBodyAsResource(requestEntity, contentType) as Parameters
            val paramMap = parseParameters(parameters)
            var url = paramMap["url"] ?: throw Exception("url has to be provided in parameters in request body")
            val urlParts = url.split("|")
            url = if (urlParts.isNotEmpty()) urlParts[0] else url
            val system = paramMap["system"] ?: throw Exception("system has to be provided in parameters in request body")
            val code = paramMap["code"] ?: throw Exception("code has to be provided in parameters in request body")
            val display = paramMap["display"]
            //TODO: Implement other parameters like version
            val (result, version) = database.validateCodeVS(url, null, system, code, display)
            val resultParam = generateParametersString(
                jsonParser,
                Parameters.ParametersParameterComponent()
                    .setName("result").setValue(BooleanType(result)),
                Parameters.ParametersParameterComponent()
                    .setName("message").setValue(StringType(
                        "Code [system = $system and code = $code] ${if(result) "was" else "wasn't"} in value set " +
                                "[url = $url and version = $version]"
                    ))
            )
            logger.debug("Validation result: $resultParam")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultParam)
        }
        catch (e: Exception){
            logger.warn("Validation of code against value set failed")
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

    @GetMapping(path = ["\$expand"])
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

}
