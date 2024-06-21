package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.FhirIndexPartitions

enum class CodeSystemIndexPartitions(indexName: String) : FhirIndexPartitions {

    CRUD("CodeSystem.crud"),
    LOOKUP("CodeSystem.lookup");

    private val bytes: ByteArray = indexName.toByteArray(Charsets.UTF_8)

    override fun bytes(): ByteArray = bytes

}