package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.index.partition.Generator

enum class CodeSystemIndexPartitions<KEY_GENERATOR, VALUE_GENERATOR>(
    private val indexName: String,
    private val keyGenerator: Function<ByteArray>,
    private val valueGenerator: Function<ByteArray>
) : FhirIndexPartitions<ByteArray, KEY_GENERATOR, ByteArray, VALUE_GENERATOR> {

    CRUD("CodeSystem.crud"),
    LOOKUP("CodeSystem.lookup", {system: String, code: String, display: String, id: Int -> byteArrayOf()}, {id: Int -> byteArrayOf()});

    override fun indexName(): String = indexName

    override fun bytes(): ByteArray = indexName.toByteArray(Charsets.UTF_8)

    override fun keyGenerator(): Function<ByteArray> = keyGenerator

    override fun valueGenerator(): Function<ByteArray> = valueGenerator

}