package de.itcr.termite.index.partition

open class FhirOperationIndexPartition<FHIR_MODEL, KEY_ELEMENT, KEY, VALUE_ELEMENT, VALUE>(
    private val indexName: String,
    private val prefixLength: Int,
    private val keyLength: Int,
    private val prefixGenerator: (KEY_ELEMENT) -> KEY,
    private val keyGenerator: (KEY_ELEMENT) -> KEY,
    private val valueGenerator: (VALUE_ELEMENT) -> VALUE,
    private val valueDestructor: (VALUE) -> VALUE_ELEMENT
): IFhirOperationIndexPartition<FHIR_MODEL, KEY_ELEMENT, KEY, VALUE_ELEMENT, VALUE> {

    override fun indexName(): String = indexName

    override fun prefixLength(): Int = prefixLength

    override fun keyLength(): Int = keyLength

    override fun bytes(): ByteArray = indexName.toByteArray(Charsets.UTF_8)

    override fun prefixGenerator(): (KEY_ELEMENT) -> KEY = prefixGenerator

    override fun keyGenerator(): (KEY_ELEMENT) -> KEY = keyGenerator

    override fun valueGenerator(): (VALUE_ELEMENT) -> VALUE = valueGenerator

    override fun valueDestructor(): (VALUE) -> VALUE_ELEMENT = valueDestructor

}