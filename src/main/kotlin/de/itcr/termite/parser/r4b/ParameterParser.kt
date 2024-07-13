package de.itcr.termite.parser.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.util.UrlUtil
import de.itcr.termite.exception.parsing.ParameterParsingException
import de.itcr.termite.util.parseDate
import de.itcr.termite.util.parseParameters
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Enumerations.FHIRAllTypes
import org.hl7.fhir.r4b.model.Identifier
import org.hl7.fhir.r4b.model.Quantity
import org.hl7.fhir.r4b.model.Reference
import java.util.Date

class ParameterParser() {

    fun parseRequestParameter(paramString: String, tokenType: String): Any {
        FhirContext.forR4B().getResourceDefinition("").searchParams
        return when (tokenType) {
            "number" ->
            "string" -> paramString
            "uri" -> validateURI(paramString)
            "token" -> parseToken(paramString)
            "date" -> parseDate(paramString)
            "reference" -> parseReference(paramString)
            "quantity" -> parseQuantity(paramString)
        }
    }

    private fun validateURI(paramString: String): String {
        if (UrlUtil.isValid(paramString)) return paramString
        else throw ParameterParsingException("Parameter of type 'uri' is not a valid URI. Value: $paramString")
    }

    private fun parseToken(paramString: String): Date = parseDate(paramString)

    private fun parseReference(paramString: String): Reference {
        val splitParam = paramString.split("/", limit = 2)
        if (splitParam.size == 1) return Reference(paramString)
        else if (splitParam.size == 2) {
            try {
                FHIRAllTypes.fromCode(splitParam[0])
                return Reference(paramString)
            }
            catch (exc: FHIRException) {
                throw ParameterParsingException("Parameter of type 'reference' contains unknown FHIR resource type '${splitParam[0]}'. Value: $paramString")
            }
        }
        else {
            if (UrlUtil.isValid(paramString)) return Reference(paramString)
            else throw ParameterParsingException("Parameter of type 'reference' is not a valid URL. Value: $paramString")
        }
    }

    private fun parseQuantity(paramString: String): Quantity {
        val splitParam = paramString.split("|")
        val quantity = Quantity()
        if (splitParam.isNotEmpty()) quantity.value = splitParam[0].toBigDecimal()
        if (splitParam.size >= 2) quantity.system = splitParam[1]
        if (splitParam.size == 3) { quantity.code = splitParam[2]; quantity.unit = splitParam[2] }
        return quantity
    }

    private fun parseNumber(paramString: String): Double {
        val regex = Regex("^(?<prefix>[a-z]{2}|[a-z]{3})(?<number>.+)\$")
        val match = regex.matchEntire(paramString)
        if (match != null) {
            val number = match.groups["number"]
            if (number != null) {
                
            }
        }
        else { throw ParameterParsingException("Parameter is not of type 'number': Value: $paramString") }
    }

}