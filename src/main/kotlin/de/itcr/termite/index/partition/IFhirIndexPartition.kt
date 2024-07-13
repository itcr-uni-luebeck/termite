package de.itcr.termite.index.partition

interface IFhirIndexPartition<FHIR_MODEL_TYPE, ELEMENT, ID_TYPE, KEY, PREFIX_GENERATOR: Function<KEY>, KEY_GENERATOR: Function<KEY>> {

    fun indexName(): String

    fun prefixLength(): Int

    fun bytes(): ByteArray

    fun elementPath(): (FHIR_MODEL_TYPE) -> Iterable<ELEMENT>

    fun prefixGenerator(): PREFIX_GENERATOR

    fun keyGenerator(): KEY_GENERATOR

}