package de.itcr.termite.persistence.r4b.valueset

import de.itcr.termite.index.partition.FhirIndexPartitions

enum class ValueSetIndexPartitions(private val indexName: String) : FhirIndexPartitions {

    CRUD("ValueSet.crud"),
    VALIDATE_CODE("ValueSet.validateCode");

    override fun indexName() = indexName

    override fun bytes(): ByteArray = indexName.toByteArray(Charsets.UTF_8)

}