package de.itcr.termite.util.r4b

import org.hl7.fhir.r4b.model.CodeableConcept
import org.hl7.fhir.r4b.model.OperationOutcome
import org.hl7.fhir.r4b.model.StringType

fun OperationOutcome(
    issueSeverity: OperationOutcome.IssueSeverity,
    issueType: OperationOutcome.IssueType,
    diagnostics: String? = null,
    details: CodeableConcept? = null,
    location: List<String> = emptyList(),
    expression: List<String> = emptyList()
): OperationOutcome = OperationOutcome().apply {
    addIssue().apply {
        severity = issueSeverity
        code = issueType
        if (details != null) setDetails(details)
        if (diagnostics != null) setDiagnostics(diagnostics)
        if (location.isNotEmpty()) setLocation(location.map { StringType(it) })
        if (expression.isNotEmpty()) setExpression(expression.map { StringType(it) })
    }
}