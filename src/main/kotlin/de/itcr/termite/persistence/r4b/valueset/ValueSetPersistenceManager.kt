package de.itcr.termite.persistence.r4b.valueset

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.model.repository.ValueSetDataRepository
import de.itcr.termite.util.r4b.parametersToMap
import de.itcr.termite.util.r4b.parseParameterValue
import de.itcr.termite.util.r4b.tagAsSummarized
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.Parameters
import org.hl7.fhir.r4b.model.ValueSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

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
        instance.id = storedData.id.toString()
        try { indexStore.putValueSet(instance, emptyList()) }
        catch (e: Exception) {
            repository.delete(storedData)
            throw PersistenceException("Failed to index ValueSet instance. Reason: ${e.message}", e)
        }
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
        val storedData: ValueSetData
        logger.debug("Checking if ValueSet instance exists [id: $id]")
        try { storedData = repository.findById(id).get() }
        catch (e: NoSuchElementException) { throw NotFoundException<ValueSet>("id", id) }
        catch (e: Exception) { throw PersistenceException("Error occurred while searching ValueSet data. Reason: ${e.message}", e) }
        val instance = storedData.toValueSetResource()
        logger.debug("Deleting ValueSet instance index data [id: $id]")
        try { indexStore.deleteValueSet(instance, emptyList()) }
        catch (e: Exception) { throw PersistenceException("Failed to remove ValueSet instance from index. Reason: ${e.message}", e) }
        logger.debug("Deleting ValueSet instance data [id: $id]")
        try { repository.deleteById(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete ValueSet data. Reason: ${e.message}", e) }
        return instance.tagAsSummarized()
    }

    override fun search(parameters: Parameters): List<ValueSet> = search(parametersToMap(parameters))

    override fun search(parameters: Map<String, String>): List<ValueSet> {
        return if (parameters.isEmpty()) repository.findAll().map { it.toValueSetResource() }
        else {
            val supportedParams = indexStore.searchPartitionsByType(ValueSet::class)
            val parsedParams = parameters.entries.filter { it.key != "_id" }.associate {
                val paramDef = supportedParams["ValueSet.search.${it.key}"]
                return@associate it.key to parseParameterValue(paramDef!!.parameter(), it.value)
            }
            @Suppress("UNCHECKED_CAST")
            var ids = indexStore.search(parsedParams, ValueSet::class as KClass<out IResource>)
            // Special handling for '_id' search parameters as indexing it makes no sense
            if ("_id" in parameters) {
                val idSet = setOf(parameters["_id"]!!.toInt())
                ids = if (parsedParams.isNotEmpty()) ids intersect idSet else idSet
            }
            repository.findAllById(ids).map { it.toValueSetResource() }
        }
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