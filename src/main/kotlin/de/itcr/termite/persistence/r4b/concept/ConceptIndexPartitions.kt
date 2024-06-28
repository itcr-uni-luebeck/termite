package de.itcr.termite.persistence.r4b.concept

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import de.itcr.termite.model.entity.FhirConcept
import org.hl7.fhir.r4b.model.CodeSystem

abstract class ConceptIndexPartitions <ELEMENT, ID_TYPE, PREFIX_GENERATOR: Function<ByteArray>, KEY_GENERATOR: Function<ByteArray>>(
    private val indexName: String,
    private val prefixLength: Int,
    private val elementPath: (Iterable<FhirConcept>) -> Iterable<ELEMENT>,
    private val prefixGenerator: PREFIX_GENERATOR,
    private val keyGenerator: KEY_GENERATOR
) : FhirIndexPartitions<Iterable<FhirConcept>, ELEMENT, ID_TYPE, ByteArray, PREFIX_GENERATOR, KEY_GENERATOR> {

    private val bytes = indexName.toByteArray(Charsets.UTF_8)

    override fun indexName(): String = indexName

    override fun prefixLength(): Int = prefixLength

    override fun bytes(): ByteArray = bytes

    override fun elementPath(): (Iterable<FhirConcept>) -> Iterable<ELEMENT> = elementPath

    override fun prefixGenerator(): PREFIX_GENERATOR = prefixGenerator

    override fun keyGenerator(): KEY_GENERATOR = keyGenerator

}