package de.itcr.termite.parser.r4b

import de.itcr.termite.exception.parsing.FhirParsingException
import de.itcr.termite.util.r4b.JsonUtil
import org.hl7.fhir.r4b.formats.JsonParser
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumerations.FilterOperator

class BackboneElementParser {

    companion object {

        private val csfEnumFactory: EnumFactory<FilterOperator> = Enumerations.FilterOperatorEnumFactory()
        private val supportedTypesMap = SupportedTypes.entries.associateBy { it.className }

        enum class SupportedTypes(val className: String) {

            CODE_SYSTEM_FILTER_COMPONENT("CodeSystem.CodeSystemFilterComponent"),
            CODE_SYSTEM_PROPERTY_COMPONENT("CodeSystem.PropertyComponent")

        }

        fun supportsType(className: String) = className in supportedTypesMap

    }

    private val jsonParser = JsonParser(false)

    fun parse(string: String, typeName: String): BackboneElement {
        try {
            val keyValuePairs = JsonUtil.splitJsonObjectString(string)

            return when (typeName) {
                SupportedTypes.CODE_SYSTEM_FILTER_COMPONENT.className -> parseCodeSystemFilterComponent(keyValuePairs)
                SupportedTypes.CODE_SYSTEM_PROPERTY_COMPONENT.className -> parseCodeSystemPropertyComponent(keyValuePairs)
                else -> throw FhirParsingException("Unsupported BackboneElement type '$typeName'")
            }
        }
        catch (e: Exception) {
            throw FhirParsingException("Failed to parse string as $typeName instance. Reason: ${e.message}. Data: '$string'. ", e)
        }
    }

    private fun parseCodeSystemFilterComponent(keyValuePairs: Map<String, String>): CodeSystem.CodeSystemFilterComponent {
        val csfComponent = CodeSystem.CodeSystemFilterComponent()
        addBaseElements(csfComponent, keyValuePairs)

        if ("code" in keyValuePairs) csfComponent.code = keyValuePairs["code"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.filter is missing required 'code' element")

        if ("description" in keyValuePairs) csfComponent.description = keyValuePairs["description"]

        if ("operator" in keyValuePairs) {
            val operatorList = JsonUtil.deserializeStringList(keyValuePairs["operator"]).map { Enumeration(csfEnumFactory, it) }
            if (operatorList.isEmpty()) throw FhirParsingException("BackboneElement instance @ CodeSystem.filter has to have at least one entry in the 'operator' element")
            csfComponent.operator = operatorList
        }
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.filter is missing required 'operator' element")

        if ("value" in keyValuePairs) csfComponent.value = keyValuePairs["value"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.filter is missing required 'value' element")

        return csfComponent
    }

    private fun parseCodeSystemPropertyComponent(keyValuePairs: Map<String, String>): CodeSystem.PropertyComponent {
        val cspComponent = CodeSystem.PropertyComponent()
        addBaseElements(cspComponent, keyValuePairs)

        if ("code" in keyValuePairs) cspComponent.code = keyValuePairs["code"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.property is missing required 'code' element")

        if ("uri" in keyValuePairs) cspComponent.uri = keyValuePairs["uri"]
        if ("description" in keyValuePairs) cspComponent.description = keyValuePairs["description"]

        if ("type" in keyValuePairs) cspComponent.type = CodeSystem.PropertyType.fromCode(keyValuePairs["type"])
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.property is missing required 'type' element")

        return cspComponent
    }

    private fun addBaseElements(backboneElement: BackboneElement, keyValuePairs: Map<String, String>) {
        if ("id" in keyValuePairs) backboneElement.id = keyValuePairs["id"]
        if ("extension" in keyValuePairs) backboneElement.setExtension(parseExtensions(keyValuePairs["extension"]!!))
        if ("modifierExtension" in keyValuePairs) backboneElement.setModifierExtension(parseExtensions(keyValuePairs["modifierExtension"]!!))
    }

    private fun parseExtensions(string: String): List<Extension> {
        val jsonList = JsonUtil.splitJsonArrayString(string)
        return jsonList.map { jsonParser.parseType(string, "Extension") } as List<Extension>
    }

}
