package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.metadata.annotation.SupportsInteraction
import de.itcr.termite.util.generateOperationOutcomeString
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.OperationDefinition
import org.hl7.fhir.r4b.model.OperationOutcome
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@ForResource(
    type = "OperationDefinition",
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
    searchParam = []
)
@SupportsInteraction(["read"])
@Controller
@RequestMapping("fhir/OperationDefinition")
class OperationDefinitionController (
    @Autowired operationDefinitions: List<OperationDefinition>,
    @Autowired database: TerminologyStorage,
    @Autowired fhirContext: FhirContext
): ResourceController(database, fhirContext) {

    //TODO: Replace with proper integration into database
    private val opDefMap: Map<String, OperationDefinition> = operationDefinitions.associateBy { opDef -> opDef.id }

    companion object{
        private val logger: Logger = LogManager.getLogger(this)
    }

    @GetMapping("{id}")
    @ResponseBody
    fun searchOperationDefinition(
        @PathVariable id: String,
        @RequestParam(name="_format", required=false) format: String?
    ): ResponseEntity<String> {
        logger.info("Reading OperationDefinition instance [id = $id]")
        try{
            if (id in opDefMap) {
                val mimeType = format ?: "application/json"
                if (mimeType !in parsers) {
                    val opOutcome = generateOperationOutcomeString(
                        OperationOutcome.IssueSeverity.ERROR,
                        OperationOutcome.IssueType.VALUE,
                        "MIME type '$mimeType' not supported. Supported MIME types: ${parsers.keys.joinToString(", ") { "'$it'" }}",
                        jsonParser
                    )
                    logger.info("Request contained unsupported MIME type '$mimeType'")
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(opOutcome)
                }

                val parser = parsers[mimeType]!!.first
                val operationDefinition = opDefMap[id]
                logger.debug("Found OperationDefinition instance with ID $id")
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimeType)).body(parser.encodeResourceToString(operationDefinition))
            }
            else {
                val operationOutcome = generateOperationOutcomeString(
                    OperationOutcome.IssueSeverity.INFORMATION,
                    OperationOutcome.IssueType.NOTFOUND,
                    "No OperationOutcome instance with ID $id",
                    jsonParser
                )
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(operationOutcome)
            }
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