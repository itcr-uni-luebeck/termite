package de.itcr.termite.model.lazy

import ca.uhn.fhir.model.api.annotation.Block
import ca.uhn.fhir.model.api.annotation.ResourceDef
import org.hl7.fhir.r4b.model.CodeSystem

@ResourceDef(name = "CodeSystem", profile = "http://hl7.org/fhir/StructureDefinition/CodeSystem")
class LazyCodeSystem(
    private var lazyConcept: Lazy<MutableList<ConceptDefinitionComponent>>
): CodeSystem() {

    constructor(): this(lazy { mutableListOf<ConceptDefinitionComponent>() })

    constructor(block: () -> MutableList<ConceptDefinitionComponent>): this(lazy(block))

    override fun getConcept(): MutableList<ConceptDefinitionComponent> {
        if (lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.getConcept()
    }

    override fun setConcept(theConcept: MutableList<ConceptDefinitionComponent>?): CodeSystem {
        lazyConcept = AlwaysInitialized
        return super.setConcept(theConcept)
    }

    override fun hasConcept(): Boolean {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.hasConcept()
    }

    override fun addConcept(): ConceptDefinitionComponent {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.addConcept()
    }

    override fun addConcept(t: ConceptDefinitionComponent?): CodeSystem {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.addConcept(t)
    }

    override fun getConceptFirstRep(): ConceptDefinitionComponent {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.getConceptFirstRep()
    }

}