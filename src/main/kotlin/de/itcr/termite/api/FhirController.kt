package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.config.DatabaseProperties
import de.itcr.termite.database.sql.TerminologyDatabase
import de.itcr.termite.metadata.MetadataCompiler
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Handles requests which don't represent interactions regarding any specific resource but provide metadata about the
 * terminology service
 */
@Controller
@RequestMapping("fhir")
class FhirController (
    @Autowired database: TerminologyDatabase,
    @Autowired fhirContext: FhirContext,
    @Autowired val capabilityStatement: CapabilityStatement
): ResourceController(database, fhirContext) {

    companion object {
        private val logger = LogManager.getLogger(this::class)
    }

    /**
     * Returns CapabilityStatement instance of this terminology service
     * @return ResponseEntity instance the body of which contains the CapabilityStatement instance as a JSON string
     */
    @GetMapping("metadata")
    @ResponseBody
    fun getCapabilityStatement(): ResponseEntity<String>{
        logger.info("Processing metadata request")
        return ResponseEntity.ok()
            .eTag("W/\"0\"")
            .header("Content-Type", "application/json")
            .body(jsonParser.encodeResourceToString(capabilityStatement))
    }

}