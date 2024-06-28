package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.*
import org.hl7.fhir.r4b.model.CodeSystem

abstract class CodeSystemIndexPartitions<ELEMENT, ID_TYPE, PREFIX_GENERATOR: Function<ByteArray>, KEY_GENERATOR: Function<ByteArray>>(
    private val indexName: String,
    private val elementPath: (CodeSystem) -> Iterable<ELEMENT>,
    private val prefixGenerator: PREFIX_GENERATOR,
    private val keyGenerator: KEY_GENERATOR
) : FhirIndexPartitions<CodeSystem, ELEMENT, ID_TYPE, ByteArray, PREFIX_GENERATOR, KEY_GENERATOR> {

    private val bytes = indexName.toByteArray(Charsets.UTF_8)

    override fun indexName(): String = indexName

    override fun bytes(): ByteArray = bytes

    override fun elementPath(): (CodeSystem) -> Iterable<ELEMENT> = elementPath

    override fun prefixGenerator(): PREFIX_GENERATOR = prefixGenerator

    override fun keyGenerator(): KEY_GENERATOR = keyGenerator

}