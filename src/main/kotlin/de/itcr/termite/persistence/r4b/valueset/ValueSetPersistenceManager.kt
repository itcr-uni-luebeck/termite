package de.itcr.termite.persistence.r4b.valueset

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.model.repository.ValueSetDataRepository
import de.itcr.termite.util.r4b.tagAsSummarized
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CodeSystem
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
    private val repository: ValueSetDataRepository,
    @Autowired
    private val conceptRepository : CSConceptDataRepository,
    @Autowired
    private val indexStore: FhirIndexStore<ByteArray, ByteArray>
): IValueSetPersistenceManager<Int> {

    companion object {

        private val logger = LogManager.getLogger(this::class)

    }

    override fun create(instance: ValueSet): ValueSet {
        val vsData = instance.toValueSetData()
        val storedData: ValueSetData
        try { storedData = repository.save(vsData) }
        catch (e: Exception) { throw PersistenceException("Failed to store ValueSet data. Reason: ${e.message}", e) }
        return storedData.toValueSetResource().tagAsSummarized()
    }

    override fun update(id: Int, instance: ValueSet): ValueSet {
        TODO("Not yet implemented")
    }

    override fun read(id: Int): ValueSet {
        val vsOptional = repository.findById(id)
        if (vsOptional.isEmpty) throw NotFoundException<ValueSet>("id", id)
        else return vsOptional.get().toValueSetResource()
    }

    //TODO: Change repository deleteByID method such that it returns the deleted instance. Is this faster?
    override fun delete(id: Int): ValueSet {
        val storedMetadata: ValueSetData
        logger.debug("Checking if ValueSet instance exists [id: $id]")
        try { storedMetadata = repository.findById(id).get() }
        catch (e: NoSuchElementException) { throw NotFoundException<ValueSet>("id", id) }
        catch (e: Exception) { throw PersistenceException("Error occurred while searching ValueSet data. Reason: ${e.message}", e) }
        logger.debug("Deleting ValueSet instance data [id: $id]")
        val instance = storedMetadata.toValueSetResource()
        try { repository.deleteById(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete ValueSet data. Reason: ${e.message}", e) }
        return instance.tagAsSummarized()
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