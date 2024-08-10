package de.itcr.termite.index

import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.index.partition.IFhirIndexPartition
import de.itcr.termite.index.partition.IFhirOperationIndexPartition
import de.itcr.termite.index.partition.IFhirSearchIndexPartition
import de.itcr.termite.model.entity.FhirConcept
import org.apache.tomcat.util.http.fileupload.util.Closeable
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.model.BaseResource
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.CodeableConcept
import org.hl7.fhir.r4b.model.Coding
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
interface FhirIndexStore<KEY, VALUE>: BatchSupport<KEY, VALUE>, IteratorSupport<KEY, VALUE>, Closeable {

    fun searchPartitionsByType(type: KClass<out IBaseResource>): Map<String, IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>

    fun searchPartitionByTypeAndName(type : KClass<out IBaseResource>, name: String): IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>?

    fun putCodeSystem(resource: CodeSystem, concepts: Iterable<FhirConcept>)

    fun deleteCodeSystem(resource: CodeSystem, concepts: Iterable<FhirConcept>)

    fun search(parameters: Map<String, IBase>, type: KClass<out IResource>): Set<Int>

    fun codeSystemLookup(code: String, system: String, version: String?): Coding

    fun codeSystemLookup(coding: Coding): Coding

    fun codeSystemValidateCode(url: String, code: String, version: String?, display: String?, displayLanguage: String?): Triple<Boolean, String, String>

    fun codeSystemValidateCode(coding: Coding, displayLanguage: String?): Triple<Boolean, String, String>

    fun codeSystemValidateCode(concept: CodeableConcept, displayLanguage: String?): Triple<Boolean, String, String>

}

inline fun <reified FHIR_TYPE: IResource> FhirIndexStore<*, *>.search(parameters: Map<String, IBase>): Set<Int> =
    search(parameters, FHIR_TYPE::class)
