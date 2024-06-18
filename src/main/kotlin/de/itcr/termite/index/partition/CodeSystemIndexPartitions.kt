package de.itcr.termite.index.partition

enum class CodeSystemIndexPartitions(value: String): FhirIndexPartitions {

    CODE_SYSTEM_LOOKUP("CodeSystem.lookup");

    private val value: ByteArray = value.toByteArray(Charsets.UTF_8)

    override fun bytes(): ByteArray = value

}