package de.itcr.termite.persistence.r4b.valueset

import de.itcr.termite.index.partition.FhirIndexPartitions

enum class ValueSetIndexPartitions(indexName: String) : FhirIndexPartitions {

    CRUD("ValueSet.crud"),
    VALIDATE_CODE("ValueSet.validateCode");

    private val bytes: ByteArray = indexName.toByteArray(Charsets.UTF_8)

    override fun bytes(): ByteArray = bytes

}