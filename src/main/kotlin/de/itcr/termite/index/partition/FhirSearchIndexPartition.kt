package de.itcr.termite.index.partition

import de.itcr.termite.metadata.annotation.SearchParameter
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.model.BaseResource

class FhirSearchIndexPartition<FHIR_MODEL_TYPE, ELEMENT, KEY>(
    private val indexName: String,
    private val prefixLength: Int,
    private val keyLength: Int,
    private val elementPath: (FHIR_MODEL_TYPE) -> Iterable<ELEMENT>,
    private val prefixGenerator: (ELEMENT) -> KEY,
    private val keyGenerator: (ELEMENT, Int) -> KEY,
    private val parameter: SearchParameter
): IFhirSearchIndexPartition<FHIR_MODEL_TYPE, ELEMENT, KEY> {

    private val bytes = indexName.toByteArray(Charsets.UTF_8)

    override fun indexName(): String = indexName

    override fun keyLength(): Int = keyLength

    override fun prefixLength(): Int = prefixLength

    override fun bytes(): ByteArray = bytes

    override fun elementPath(): (FHIR_MODEL_TYPE) -> Iterable<ELEMENT> = elementPath

    override fun prefixGenerator(): (ELEMENT) -> KEY = prefixGenerator

    override fun keyGenerator(): (ELEMENT, Int) -> KEY = keyGenerator

    override fun parameter(): SearchParameter = parameter
}