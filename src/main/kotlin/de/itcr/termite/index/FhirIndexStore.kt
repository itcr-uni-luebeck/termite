package de.itcr.termite.index

import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.index.partition.IFhirSearchIndexPartition
import de.itcr.termite.model.entity.CodeSystemConceptData
import de.itcr.termite.model.entity.VSConceptData
import org.apache.tomcat.util.http.fileupload.util.Closeable
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.CodeableConcept
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.ValueSet
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
interface FhirIndexStore<KEY, VALUE>: BatchSupport<KEY, VALUE>, IteratorSupport<KEY, VALUE>, Closeable {

    fun searchPartitionsByType(type: KClass<out IBaseResource>): Map<String, IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>

    fun searchPartitionByTypeAndName(type : KClass<out IBaseResource>, name: String): IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>?

    fun putCodeSystem(resource: CodeSystem, concepts: Iterable<CodeSystemConceptData>)

    fun deleteCodeSystem(resource: CodeSystem, concepts: Iterable<CodeSystemConceptData>)

    fun search(parameters: Map<String, List<IBase>>, type: KClass<out IBaseResource>): Set<Int>

    fun search(name: String, value: IBase, type: KClass<out IBaseResource>): Set<Int>

    fun codeSystemLookup(code: String, system: String, version: String?): Long

    fun codeSystemLookup(coding: Coding): Long

    fun codeSystemValidateCode(csId: Int, system: String, code: String): Long?

    fun codeSystemValidateCode(csId: Int, coding: Coding): Long?

    fun codeSystemValidateCode(csId: Int, concept: CodeableConcept): Long?

    fun putValueSet(resource: ValueSet, concepts: Iterable<VSConceptData>)

    fun deleteValueSet(resource: ValueSet, concepts: Iterable<VSConceptData>)

    fun valueSetValidateCode(vsId: Int, system: String, code: String, version: String?): Long?

    fun valueSetValidateCode(vsId: Int, coding: Coding): Long?

    fun valueSetValidateCode(vsId: Int, concept: CodeableConcept): Long?

}

inline fun <reified FHIR_TYPE: IResource> FhirIndexStore<*, *>.search(parameters: Map<String, List<IBase>>): Set<Int> =
    search(parameters, FHIR_TYPE::class)
