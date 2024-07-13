package de.itcr.termite.index.partition

interface IFhirOperationIndexPartition<ELEMENT, FHIR_MODEL_TYPE, ID_TYPE, KEY, PREFIX_GENERATOR: Function<KEY>, KEY_GENERATOR: Function<KEY>, VALUE, VALUE_GENERATOR: Function<VALUE>>:
    IFhirIndexPartition<FHIR_MODEL_TYPE, ELEMENT, ID_TYPE, KEY, PREFIX_GENERATOR, KEY_GENERATOR> {

    fun valueGenerator(): VALUE_GENERATOR

}