package de.itcr.termite.exception

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.util.generateOperationOutcomeString
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class ExceptionHandlerAdvice(@Autowired private val ctx: FhirContext) {

    private val parser = ctx.newJsonParser().setPrettyPrint(true)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleException(e: ResponseStatusException): ResponseEntity<String> {
        val body = generateOperationOutcomeString(
            OperationOutcome.IssueSeverity.ERROR,
            OperationOutcome.IssueType.PROCESSING,
            e.message,
            parser
        )
        return ResponseEntity.status(e.status).body(body)
    }

}