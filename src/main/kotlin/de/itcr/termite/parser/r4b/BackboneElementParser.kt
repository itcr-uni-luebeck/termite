package de.itcr.termite.parser.r4b

import de.itcr.termite.exception.parsing.FhirParsingException
import de.itcr.termite.util.r4b.JsonUtil
import org.apache.commons.collections4.trie.PatriciaTrie
import org.hl7.fhir.r4b.formats.JsonParser
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumerations.FilterOperator

// TODO: If input to server is validated (even against base profile) then many checks do not have to be performed
//       assuming no issues occur during processing
class BackboneElementParser {

    companion object {

        private val csfEnumFactory: EnumFactory<FilterOperator> = Enumerations.FilterOperatorEnumFactory()
        private val supportedTypesMap = SupportedTypes.entries.associateBy { it.className }

        enum class SupportedTypes(val className: String) {

            CODE_SYSTEM_FILTER_COMPONENT("CodeSystem.CodeSystemFilterComponent"),
            CODE_SYSTEM_PROPERTY_COMPONENT("CodeSystem.PropertyComponent"),
            CODE_SYSTEM_CONCEPT_DESIGNATION_COMPONENT("CodeSystem.ConceptDefinitionDesignationComponent"),
            CODE_SYSTEM_CONCEPT_PROPERTY_COMPONENT("CodeSystem.ConceptDefinitionDesignationComponent"),

            VALUE_SET_CONCEPT_DESIGNATION_COMPONENT("ValueSet.ConceptReferenceDesignationComponent"),
            VALUE_SET_CONCEPT_FILTER_COMPONENT("ValueSet.ConceptSetFilterComponent")

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
                SupportedTypes.CODE_SYSTEM_CONCEPT_DESIGNATION_COMPONENT.className -> parseCodeSystemConceptDesignationComponent(keyValuePairs)
                SupportedTypes.CODE_SYSTEM_CONCEPT_PROPERTY_COMPONENT.className -> parseCodeSystemConceptPropertyComponent(keyValuePairs)
                SupportedTypes.VALUE_SET_CONCEPT_DESIGNATION_COMPONENT.className -> parseValueSetConceptDesignationComponent(keyValuePairs)
                SupportedTypes.VALUE_SET_CONCEPT_FILTER_COMPONENT.className -> parseValueSetConceptFilterComponent(keyValuePairs)
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

    private fun parseCodeSystemConceptDesignationComponent(
        keyValuePairs: Map<String, String>
    ): CodeSystem.ConceptDefinitionDesignationComponent {
        val cscdComponent = CodeSystem.ConceptDefinitionDesignationComponent()
        addBaseElements(cscdComponent, keyValuePairs)

        if ("language" in keyValuePairs) cscdComponent.language = keyValuePairs["language"]
        if ("use" in keyValuePairs) cscdComponent.use = JsonUtil.deserialize(keyValuePairs["use"], "Coding") as Coding

        if ("value" in keyValuePairs) cscdComponent.value = keyValuePairs["value"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.concept.designation is missing required 'value' element")

        return cscdComponent
    }

    private fun parseCodeSystemConceptPropertyComponent(
        keyValuePairs: Map<String, String>
    ): CodeSystem.ConceptPropertyComponent {
        val cscpComponent = CodeSystem.ConceptPropertyComponent()
        addBaseElements(cscpComponent, keyValuePairs)

        if ("code" in keyValuePairs) cscpComponent.code = keyValuePairs["code"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.concept.designation is missing required 'code' element")

        if ("value" in keyValuePairs) cscpComponent.value = parsePolymorphicElement("value", keyValuePairs as PatriciaTrie<String>)
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.concept.designation is missing required 'value' element")

        return cscpComponent
    }

    private fun parseValueSetConceptDesignationComponent(
        keyValuePairs: Map<String, String>
    ): ValueSet.ConceptReferenceDesignationComponent {
        val vscrdComponent = ValueSet.ConceptReferenceDesignationComponent()
        addBaseElements(vscrdComponent, keyValuePairs)

        if ("language" in keyValuePairs) vscrdComponent.language = keyValuePairs["language"]
        if ("use" in keyValuePairs) vscrdComponent.use = JsonUtil.deserialize(keyValuePairs["use"], "Coding") as Coding

        if ("value" in keyValuePairs) vscrdComponent.value = keyValuePairs["value"]
        else throw FhirParsingException("BackboneElement instance @ ValueSet.[compose.include.concept|expansion.contains].designation is missing required 'value' element")

        return vscrdComponent
    }

    private fun parseValueSetConceptFilterComponent(
        keyValuePairs: Map<String, String>
    ): ValueSet.ConceptSetFilterComponent {
        val vscsfComponent = ValueSet.ConceptSetFilterComponent()
        addBaseElements(vscsfComponent, keyValuePairs)

        if ("property" in keyValuePairs) vscsfComponent.property = keyValuePairs["property"]
        else throw FhirParsingException("BackboneElement instance @ ValueSet.compose.[include|exclude].concept.filter is missing required 'property' element")

        if ("op" in keyValuePairs) vscsfComponent.op = FilterOperator.valueOf(keyValuePairs["op"]!!)
        else throw FhirParsingException("BackboneElement instance @ ValueSet.compose.[include|exclude].concept.filter is missing required 'value' element")

        if ("value" in keyValuePairs) vscsfComponent.value = keyValuePairs["value"]
        else throw FhirParsingException("BackboneElement instance @ ValueSet.compose.[include|exclude].concept.filter is missing required 'value' element")

        return vscsfComponent
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

    private fun parsePolymorphicElement(elementName: String, trie: PatriciaTrie<String>): DataType? {
        val element = trie.select(elementName)
        val typeName = element.key.substring(elementName.length)
        return JsonUtil.deserialize(element.value, typeName)
    }

}

