package de.itcr.termite.persistence.r4b.valueset

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.model.repository.ValueSetMetadataRepository
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.Parameters
import org.hl7.fhir.r4b.model.ValueSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ValueSetPersistenceManager(
    @Autowired
    private val fhirCtx: FhirContext,
    @Autowired
    private val repository: ValueSetMetadataRepository,
    @Autowired
    private val conceptRepository : CSConceptDataRepository,
    @Autowired
    private val indexStore: FhirIndexStore<ByteArray, ByteArray>
): IValueSetPersistenceManager<Int> {

    override fun create(instance: ValueSet): ValueSet {
        TODO("Not yet implemented")
    }

    override fun update(id: Int, instance: ValueSet): ValueSet {
        TODO("Not yet implemented")
    }

    override fun read(id: Int): ValueSet {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int): ValueSet {
        TODO("Not yet implemented")
    }

    override fun search(parameters: Parameters): List<ValueSet> {
        TODO("Not yet implemented")
    }

    override fun search(parameters: Map<String, String>): List<ValueSet> {
        TODO("Not yet implemented")
    }

    override fun validateCode(
        url: String,
        valueSetVersion: String?,
        system: String,
        code: String,
        systemVersion: String?,
        display: String?
    ): Parameters {
        TODO("Not yet implemented")
    }

    override fun validateCode(url: String, valueSetVersion: String?, coding: Coding): Parameters {
        return super.validateCode(url, valueSetVersion, coding)
    }

    override fun validateCode(
        id: Int,
        system: String,
        code: String,
        systemVersion: String?,
        display: String?
    ): Parameters {
        TODO("Not yet implemented")
    }

    override fun validateCode(id: Int, coding: Coding): Parameters {
        return super.validateCode(id, coding)
    }

    override fun expand(url: String, version: String?): ValueSet {
        TODO("Not yet implemented")
    }

    override fun expand(id: Int): ValueSet {
        TODO("Not yet implemented")
    }

}