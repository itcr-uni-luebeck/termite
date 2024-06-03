package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.config.DatabaseProperties
import de.itcr.termite.database.sql.TerminologyDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.nio.file.Path

/**
 * Handles requests which don't represent interactions regarding any specific resource but provide metadata about the
 * terminology service
 */
@Controller
@RequestMapping("fhir")
class FhirController (
    @Autowired val database: TerminologyDatabase,
    @Autowired val fhirContext: FhirContext
) {

    private val capabilityStatement: String = loadCapabilityStatement(Path.of("fhir", "metadata.json"))

    /**
     * Returns CapabilityStatement instance of this terminology service
     * @return ResponseEntity instance the body of which contains the CapabilityStatement instance as a JSON string
     */
    @GetMapping("metadata")
    @ResponseBody
    fun getCapabilityStatement(): ResponseEntity<String>{
        //TODO: Proper ETag
        return ResponseEntity.ok().eTag("W/0").header("Content-Type", "application/json").body(capabilityStatement)
    }

    /**
     * Load the content of the JSON file containing the CapabilityStatement instance from the class path
     * @return String instance containing the CapabilityStatement instance in JSON format
     */
    private fun loadCapabilityStatement(path: Path): String {
        return this::class.java.classLoader.getResource(path.toString()).readText()
    }

}