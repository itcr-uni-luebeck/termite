package de.itcr.termite.persistence.r4b.conceptmap

import de.itcr.termite.index.partition.FhirIndexPartitions

enum class ConceptMapIndexPartitions(indexName: String) : FhirIndexPartitions {

    CRUD("ConceptMap.crud"),
    TRANSLATE("ConceptMap.translate");

    private val bytes: ByteArray = indexName.toByteArray(Charsets.UTF_8)

    override fun bytes(): ByteArray = bytes

}