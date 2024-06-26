package de.itcr.termite.index.partition

interface FhirIndexPartitions<KEY, KEY_GENERATOR: Function<KEY>, VALUE, VALUE_GENERATOR: Function<VALUE>> {

    fun indexName(): String

    fun bytes(): ByteArray

    fun keyGenerator(): KEY_GENERATOR

    fun valueGenerator(): VALUE_GENERATOR

}