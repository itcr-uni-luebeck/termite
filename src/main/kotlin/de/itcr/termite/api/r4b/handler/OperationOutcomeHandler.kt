package de.itcr.termite.api.r4b.handler

import de.itcr.termite.api.r4b.FhirController
import de.itcr.termite.exception.api.ApiException
import org.hl7.fhir.r4b.model.OperationOutcome
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

abstract class OperationOutcomeHandler(vararg mappings: Pair<IssueType, HttpStatus>) {

    private val mapping: Map<IssueType, (FhirController, MediaType, OperationOutcome) -> ResponseEntity<String>>

    init {
        // Check for duplicate keys
        mappings.groupBy { it.first }.forEach {
            if (it.value.size > 1) throw ApiException("Duplicate key '${it.key}' in OperationOutcomeHandler instance")
        }
        // Build handler
        mapping = mappings.associate { (issueType, httpStatus) ->
            Pair(issueType) { controller: FhirController, mediaType: MediaType, outcome: OperationOutcome ->
                ResponseEntity.status(httpStatus).eTag("W/\"0\"").contentType(mediaType)
                    .body(controller.encodeResourceToSting(outcome, mediaType))
            }
        }
    }

    fun handle(controller: FhirController, mediaType: MediaType, outcome: OperationOutcome): ResponseEntity<String> {
        val issueType = outcome.issueFirstRep.code
        return this.mapping[issueType]?.invoke(controller, mediaType, outcome)
            ?: ResponseEntity.internalServerError().eTag("W/\"0\"").contentType(mediaType)
                .body(controller.encodeResourceToSting(outcome, mediaType))
    }

}