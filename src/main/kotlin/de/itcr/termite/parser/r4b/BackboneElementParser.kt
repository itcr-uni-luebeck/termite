package de.itcr.termite.parser.r4b

import de.itcr.termite.exception.parsing.FhirParsingException
import de.itcr.termite.parser.IBackboneElementParser
import de.itcr.termite.util.r4b.JsonUtil
import org.apache.commons.collections4.trie.PatriciaTrie
import org.fhir.ucum.Concept
import org.hl7.fhir.instance.model.api.IBackboneElement
import org.hl7.fhir.r4b.formats.JsonParser
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.ConceptMap.ConceptMapEquivalence
import org.hl7.fhir.r4b.model.ConceptMap.ConceptMapEquivalenceEnumFactory
import org.hl7.fhir.r4b.model.Enumerations.FilterOperator

class BackboneElementParser: IBackboneElementParser {

    companion object {

        private val csfEnumFactory: EnumFactory<FilterOperator> = Enumerations.FilterOperatorEnumFactory()
        private val supportedTypesMap = SupportedTypes.entries.associateBy { it.className }

        enum class SupportedTypes(val className: String) {

            CODE_SYSTEM_FILTER_COMPONENT("CodeSystem.CodeSystemFilterComponent"),
            CODE_SYSTEM_PROPERTY_COMPONENT("CodeSystem.PropertyComponent"),
            CODE_SYSTEM_CONCEPT_DESIGNATION_COMPONENT("CodeSystem.ConceptDefinitionDesignationComponent"),
            CODE_SYSTEM_CONCEPT_PROPERTY_COMPONENT("CodeSystem.ConceptDefinitionDesignationComponent"),
            CONCEPT_MAP_TARGET_ELEMENT_COMPONENT("ConceptMap.TargetElementComponent"),
            CONCEPT_MAP_OTHER_ELEMENT_COMPONENT("ConceptMap.OtherElementComponent")

        }

        fun supportsType(className: String) = className in supportedTypesMap

    }

    private val jsonParser = JsonParser(false)

    override fun parse(string: String, typeName: String): IBackboneElement {
        try {
            val keyValuePairs = JsonUtil.splitJsonObjectString(string)

            return when (typeName) {
                SupportedTypes.CODE_SYSTEM_FILTER_COMPONENT.className -> parseCodeSystemFilterComponent(keyValuePairs)
                SupportedTypes.CODE_SYSTEM_PROPERTY_COMPONENT.className -> parseCodeSystemPropertyComponent(keyValuePairs)
                SupportedTypes.CODE_SYSTEM_CONCEPT_DESIGNATION_COMPONENT.className -> parseCodeSystemConceptDesignationComponent(keyValuePairs)
                SupportedTypes.CODE_SYSTEM_CONCEPT_PROPERTY_COMPONENT.className -> parseCodeSystemConceptPropertyComponent(keyValuePairs)
                SupportedTypes.CONCEPT_MAP_TARGET_ELEMENT_COMPONENT.className -> parseConceptMapTargetElementComponent(keyValuePairs)
                SupportedTypes.CONCEPT_MAP_OTHER_ELEMENT_COMPONENT.className -> parseConceptMapOtherElementComponent(keyValuePairs)
                else -> throw FhirParsingException("Unsupported BackboneElement type '$typeName'")
            } as IBackboneElement
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

        if ("value" in keyValuePairs) cscpComponent.code = keyValuePairs["code"]
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.concept.designation is missing required 'code' element")

        if ("value" in keyValuePairs) cscpComponent.value = parsePolymorphicElement("value", keyValuePairs as PatriciaTrie<String>)
        else throw FhirParsingException("BackboneElement instance @ CodeSystem.concept.designation is missing required 'value' element")

        return cscpComponent
    }

    private fun parseConceptMapTargetElementComponent(
        keyValuePairs: Map<String, String>
    ): ConceptMap.TargetElementComponent {
        val cmteComponent = ConceptMap.TargetElementComponent()
        addBaseElements(cmteComponent, keyValuePairs)

        if ("value" in keyValuePairs) cmteComponent.code = keyValuePairs["code"]
        if ("display" in keyValuePairs) cmteComponent.display = keyValuePairs["display"]

        if ("equivalence" in keyValuePairs) cmteComponent.equivalence = ConceptMapEquivalence.fromCode(keyValuePairs["equivalence"])
        else throw FhirParsingException("BackboneElement instance @ ConceptMap.group.element.target is missing required 'equivalence' element")

        if ("comment" in keyValuePairs) cmteComponent.comment = keyValuePairs["comment"]

        if ("dependsOn" in keyValuePairs) cmteComponent.dependsOn =
            JsonUtil.deserializeList(keyValuePairs["dependsOn"], "ConceptMap.OtherElementComponent")
                as List<ConceptMap.OtherElementComponent>

        return cmteComponent
    }

    // FIXME: Currently the ConceptMap.group.element.target.dependsOn.product element is not parsed by this method as
    //        the HAPI FHIR library seems to not contains any obvious way to access/set this element
    private fun parseConceptMapOtherElementComponent(
        keyValuePairs: Map<String, String>
    ): ConceptMap.OtherElementComponent {
        val cmoeComponent = ConceptMap.OtherElementComponent()
        addBaseElements(cmoeComponent, keyValuePairs)

        if ("property" in keyValuePairs) cmoeComponent.property = keyValuePairs["property"]
        else throw FhirParsingException("BackboneElement instance @ ConceptMap.group.element.target.dependsOn is missing required 'property' element")

        if ("system" in keyValuePairs) cmoeComponent.system = keyValuePairs["system"]

        if ("value" in keyValuePairs) cmoeComponent.property = keyValuePairs["value"]
        else throw FhirParsingException("BackboneElement instance @ ConceptMap.group.element.target.dependsOn is missing required 'value' element")

        if ("display" in keyValuePairs) cmoeComponent.display = keyValuePairs["display"]

        return cmoeComponent
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