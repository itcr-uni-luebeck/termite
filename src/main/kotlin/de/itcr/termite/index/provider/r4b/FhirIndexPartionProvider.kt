package de.itcr.termite.index.provider.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemIndexPartitions
import de.itcr.termite.persistence.r4b.conceptmap.ConceptMapIndexPartitions
import de.itcr.termite.persistence.r4b.valueset.ValueSetIndexPartitions
import org.hl7.fhir.instance.model.api.IBase

fun getIndexPartitionsForType(typeName: String): Array<FhirIndexPartitions> {
    return when (typeName) {
        "CodeSystem" -> CodeSystemIndexPartitions.entries.toTypedArray()
        "ValueSet" -> ValueSetIndexPartitions.entries.toTypedArray()
        "ConceptMap" -> ConceptMapIndexPartitions.entries.toTypedArray()
        else -> throw Exception("No FHIR index partitions defined for FHIR type '$typeName'")
    }
}

inline fun <reified TYPE: IBase> getIndexPartitionsForType() = getIndexPartitionsForType(TYPE::class.simpleName!!)