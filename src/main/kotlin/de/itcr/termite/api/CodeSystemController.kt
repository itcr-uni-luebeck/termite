package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.util.generateOperationOutcomeString
import de.itcr.termite.util.generateParametersString
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Controller
@RequestMapping("fhir/CodeSystem")
class CodeSystemController(
    @Autowired val database: TerminologyStorage,
    @Autowired val fhirContext: FhirContext
    ) {

    private val logger = LogManager.getLogger(this)
    private val jsonParser = fhirContext.newJsonParser().setPrettyPrint(true)

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

    @GetMapping(params = ["url"])
    @ResponseBody
    fun searchCodeSystem(@RequestParam url: String): ResponseEntity<String>{
        try{
            val csList = database.searchCodeSystem(url)
            val bundle = Bundle()
            bundle.id = UUID.randomUUID().toString()
            bundle.type = Bundle.BundleType.SEARCHSET
            bundle.total = csList.size
            //Should be faster since otherwise an internal array list would have to resized all the time
            bundle.entry = csList.map { cs -> Bundle.BundleEntryComponent().setResource(cs) }
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