package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.FhirOperationIndexPartition
import de.itcr.termite.util.serialize
import org.hl7.fhir.r4b.model.CodeSystem
import java.nio.ByteBuffer
import java.util.*

typealias Concept = CodeSystem.ConceptDefinitionComponent

sealed class CodeSystemOperationIndexPartitions<ELEMENT, ID_TYPE, PREFIX_GENERATOR: Function<ByteArray>, KEY_GENERATOR: Function<ByteArray>, VALUE_GENERATOR: Function<ByteArray>>(
    indexName: String,
    elementPath: (CodeSystem) -> Iterable<ELEMENT>,
    prefixGenerator: PREFIX_GENERATOR,
    keyGenerator: KEY_GENERATOR,
    valueGenerator: VALUE_GENERATOR
): CodeSystemIndexPartitions<ELEMENT, ID_TYPE, Function<ByteArray>, Function<ByteArray>>(indexName, elementPath, prefixGenerator, keyGenerator),
    FhirOperationIndexPartition<ELEMENT, CodeSystem, ID_TYPE, ByteArray, Function<ByteArray>, Function<ByteArray>, ByteArray, Function<ByteArray>> {

    data object LOOKUP: CodeSystemOperationIndexPartitions<Concept, Int, (Concept, String) -> ByteArray, (Concept, String, String?, Int) -> ByteArray, (Long) -> ByteArray> (
        "CodeSystem.search.code",
        { cs: CodeSystem -> cs.concept },
        { value: Concept, system: String -> ByteBuffer.allocate(8)
            .putInt(system.hashCode())
            .putInt(value.code.hashCode())
            .array()},
        { value: Concept, system: String, version: String?, id: Int -> ByteBuffer.allocate(20)
            .putInt(system.hashCode())
            .putInt(value.code.hashCode())
            .putInt(Objects.hashCode(version))
            .putInt(Objects.hashCode(value.display))
            .putInt(id)
            .array() },
        { conceptId: Long -> serialize(conceptId) }
    )

    override fun valueGenerator(): Function<ByteArray> = valueGenerator()

}