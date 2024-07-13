package de.itcr.termite.index.partition

class FhirSearchIndexPartition<FHIR_MODEL, ELEMENT, KEY>(
    private val indexName: String,
    private val prefixLength: Int,
    private val elementPath: (FHIR_MODEL) -> Iterable<ELEMENT>,
    private val prefixGenerator: (ELEMENT) -> KEY,
    private val keyGenerator: (ELEMENT, Int) -> KEY
): IFhirSearchIndexPartition<FHIR_MODEL, ELEMENT, KEY> {

    private val bytes = indexName.toByteArray(Charsets.UTF_8)

    override fun indexName(): String = indexName

    override fun prefixLength(): Int = prefixLength

    override fun bytes(): ByteArray = bytes

    override fun elementPath(): (FHIR_MODEL) -> Iterable<ELEMENT> = elementPath

    override fun prefixGenerator(): (ELEMENT) -> KEY = prefixGenerator

    override fun keyGenerator(): (ELEMENT, Int) -> KEY = keyGenerator

}