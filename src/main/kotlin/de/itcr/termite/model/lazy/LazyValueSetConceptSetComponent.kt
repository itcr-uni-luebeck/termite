package de.itcr.termite.model.lazy

import org.hl7.fhir.r4b.model.ValueSet

typealias LazyConceptSetComponent = LazyValueSetConceptSetComponent

class LazyValueSetConceptSetComponent(
    private var lazyConcept: Lazy<MutableList<ValueSet.ConceptReferenceComponent>>
): ValueSet.ConceptSetComponent() {

    constructor(block: () -> MutableList<ValueSet.ConceptReferenceComponent>) : this(lazy(block))

    override fun getConcept(): MutableList<ValueSet.ConceptReferenceComponent> {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.getConcept()
    }

    override fun setConcept(theConcept: List<ValueSet.ConceptReferenceComponent?>?): ValueSet.ConceptSetComponent {
        lazyConcept = AlwaysInitialized // Override Lazy instance
        return super.setConcept(theConcept)
    }

    override fun hasConcept(): Boolean {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.hasConcept()
    }

    override fun addConcept(): ValueSet.ConceptReferenceComponent {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.addConcept()
    }

    override fun addConcept(t: ValueSet.ConceptReferenceComponent?): ValueSet.ConceptSetComponent {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.addConcept(t)
    }

    override fun getConceptFirstRep(): ValueSet.ConceptReferenceComponent {
        if (!lazyConcept.isInitialized()) concept = lazyConcept.value
        return super.getConceptFirstRep()
    }

}