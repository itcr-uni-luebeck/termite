package de.itcr.termite.index.partition

interface IFhirIndexPartition<FHIR_MODEL_TYPE, KEY, PREFIX_GENERATOR, KEY_GENERATOR> {

    fun indexName(): String

    fun prefixLength(): Int

    fun keyLength(): Int

    fun bytes(): ByteArray

    fun prefixGenerator(): PREFIX_GENERATOR

    fun keyGenerator(): KEY_GENERATOR

}