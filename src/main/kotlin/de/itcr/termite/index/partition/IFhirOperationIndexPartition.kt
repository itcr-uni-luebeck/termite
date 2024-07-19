package de.itcr.termite.index.partition

interface IFhirOperationIndexPartition<FHIR_MODEL_TYPE, KEY_ELEMENT, KEY, VALUE_ELEMENT, VALUE>:
    IFhirIndexPartition<FHIR_MODEL_TYPE, KEY, (KEY_ELEMENT) -> KEY, (KEY_ELEMENT) -> KEY> {

    fun valueGenerator(): (VALUE_ELEMENT) -> VALUE

    fun valueDestructor(): (VALUE) -> VALUE_ELEMENT

}