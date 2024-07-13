package de.itcr.termite.persistence.r4b.concept

import de.itcr.termite.index.partition.IFhirOperationIndexPartition
import de.itcr.termite.model.entity.FhirConcept
import de.itcr.termite.util.serialize
import java.nio.ByteBuffer
import java.util.*

sealed class ConceptOperationIndexPartitionI<ELEMENT, ID_TYPE, PREFIX_GENERATOR: Function<ByteArray>, KEY_GENERATOR: Function<ByteArray>, VALUE_GENERATOR: Function<ByteArray>>(
    indexName: String,
    prefixLength: Int,
    elementPath: (Iterable<FhirConcept>) -> Iterable<ELEMENT>,
    prefixGenerator: PREFIX_GENERATOR,
    keyGenerator: KEY_GENERATOR,
    private val valueGenerator: VALUE_GENERATOR
): ConceptIndexPartitionI<ELEMENT, ID_TYPE, PREFIX_GENERATOR, KEY_GENERATOR>(indexName, prefixLength, elementPath, prefixGenerator, keyGenerator),
    IFhirOperationIndexPartition<ELEMENT, Iterable<FhirConcept>, ID_TYPE, ByteArray, PREFIX_GENERATOR, KEY_GENERATOR, ByteArray, VALUE_GENERATOR> {

    data object LOOKUP: ConceptOperationIndexPartitionI<FhirConcept, Int, (FhirConcept, String) -> ByteArray, (FhirConcept, String, String?, Int) -> ByteArray, (Long) -> ByteArray>(
        "CodeSystem.lookup",
        8,
        { cList: Iterable<FhirConcept> -> cList },
        { value: FhirConcept, system: String -> ByteBuffer.allocate(8)
            .putInt(system.hashCode())
            .putInt(value.code.hashCode())
            .array()},
        { value: FhirConcept, system: String, version: String?, id: Int -> ByteBuffer.allocate(20)
            .putInt(system.hashCode())
            .putInt(value.code.hashCode())
            .putInt(Objects.hashCode(version))
            .putInt(Objects.hashCode(value.display))
            .putInt(id)
            .array() },
        { conceptId: Long -> serialize(conceptId) }
    )

    override fun valueGenerator(): VALUE_GENERATOR = valueGenerator

}