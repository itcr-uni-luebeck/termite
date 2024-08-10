package de.itcr.termite.util.r4b

import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.exception.parsing.FhirParsingException
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.util.parseDate
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4b.model.*

fun parametersToMap(parameters: Parameters): Map<String, List<String>> {
    return parameters.parameter.groupBy { it.name }.mapValues { it.value.map { v -> v.value.primitiveValue() } }
}

fun parseParameterValue(paramDef: SearchParameter, value: String): IBase {
    return when (paramDef.type) {
        "number" -> parseNumberParameterValue(paramDef, value)
        "date" -> DateType(parseDate(value))
        "string" -> StringType(value)
        "token" -> parseTokenParameterValue(paramDef, value)
        "reference" -> Reference(value)
        "uri" -> UriType(value)
        else -> throw FhirParsingException("Unsupported type '${paramDef.type}' for parameter '${paramDef.name}'")
    }
}

fun parseNumberParameterValue(paramDef: SearchParameter, value: String): IBase {
    return when (paramDef.processing.targetType) {
        IntegerType::class -> IntegerType(value.toInt())
        DecimalType::class -> DecimalType(value)
        else -> throw FhirParsingException("Unsupported target type ${paramDef.processing.targetType.simpleName} for parameter '${paramDef.name}' of type 'number'")
    }
}

fun parseTokenParameterValue(paramDef: SearchParameter, value: String): IBase {
    return when (paramDef.processing.targetType) {
        StringType::class -> StringType(value)
        CodeType::class -> parseCodeTypeParameterValue(paramDef.name, value)
        Identifier::class -> parseIdentifierParameterValue(paramDef.name, value)
        Coding::class -> parseCodingParameterValue(paramDef.name, value)
        else -> throw FhirParsingException("Unsupported target type ${paramDef.processing.targetType.simpleName} for parameter '${paramDef.name}' of type 'token'")
    }
}

fun parseCodeTypeParameterValue(name: String, value: String): CodeType {
    val splitParam = value.split("|")
    return when (splitParam.size) {
        1 -> if (value.trim().startsWith('|')) CodeType(splitParam[0]) else CodeType().setSystem(splitParam[0])
        2 -> CodeType(splitParam[1].ifBlank { null }).setSystem(splitParam[0])
        else -> throw UnsupportedValueException("Parameter '$name' of type 'token' with target class 'CodeType' can at most consists of two parts. Actual: '$value'")
    }
}

fun parseIdentifierParameterValue(name: String, value: String): Identifier {
    val splitParam = value.split("|")
    return when (splitParam.size) {
        2 -> Identifier().setValue(splitParam[1].ifBlank { null }).setSystem(splitParam[0])
        else -> throw UnsupportedValueException("Parameter '$name' of type 'token' with target class 'Identifier' has to consists of two parts. Actual: '$value'")
    }
}

fun parseCodingParameterValue(name: String, value: String): Coding {
    val splitParam = value.split("|")
    return when (splitParam.size) {
        1 -> if (value.trim().startsWith('|')) Coding().setCode(splitParam[0]) else Coding().setSystem(splitParam[0])
        2 -> Coding().setSystem(splitParam[0]).setCode(splitParam[1].ifBlank { null })
        else -> throw UnsupportedValueException("Parameter '$name' of type 'token' with target class 'CodeType' can at most consists of two parts. Actual: '$value'")
    }
}

inline fun <reified T: CanonicalResource> T.tagAsSummarized(): T {
    meta.addTag(
        Coding(
            "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
            "SUBSETTED",
            "subsetted"
        )
    )
    return this
}

fun ValidateCodeParameters(result: Boolean, message: String? = null, display: String? = null) = Parameters().apply {
    setParameter("result", result)
    if (message != null) setParameter("message", message)
    if (display != null) setParameter("display", display)
}

fun parseParamMaxValue(value: String): Int {
    return if (value == "*") Int.MAX_VALUE
    else value.toInt()
}