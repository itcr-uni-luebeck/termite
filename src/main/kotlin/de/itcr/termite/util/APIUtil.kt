package de.itcr.termite.util

import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4b.model.OperationOutcome
import org.hl7.fhir.r4b.model.Parameters

fun generateOperationOutcome(severity: OperationOutcome.IssueSeverity, code: OperationOutcome.IssueType, diagnostics: String?): OperationOutcome {
    val opOutcome = OperationOutcome.OperationOutcomeIssueComponent()
        .setSeverity(severity)
        .setCode(code)
    if(diagnostics != null) opOutcome.diagnostics = diagnostics
    return OperationOutcome().addIssue(opOutcome)
}

fun generateOperationOutcomeString(severity: OperationOutcome.IssueSeverity, code: OperationOutcome.IssueType, diagnostics: String?, parser: IParser): String {
    return parser.encodeResourceToString(generateOperationOutcome(severity, code, diagnostics))
}

fun generateParameters(vararg parameters: Parameters.ParametersParameterComponent): Parameters{
    return Parameters().setParameter(parameters.toMutableList())
}

fun generateParametersString(parser: IParser, vararg parameters: Parameters.ParametersParameterComponent): String{
    return parser.encodeResourceToString(generateParameters(*parameters))
}

fun parseParameters(parameters: Parameters): Map<String, String>{
    return parameters.parameter.associate { component -> component.name to component.value.primitiveValue() }
}

