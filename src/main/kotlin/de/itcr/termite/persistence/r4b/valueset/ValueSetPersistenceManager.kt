package de.itcr.termite.persistence.r4b.valueset

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.model.repository.ValueSetConceptDataRepository
import de.itcr.termite.model.repository.ValueSetDataRepository
import de.itcr.termite.util.r4b.*
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4b.model.*
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
    private val conceptRepository : ValueSetConceptDataRepository,
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
        try { indexStore.putValueSet(instance, vsData.composeInclude.map { it.concept }.flatten()) }
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
            // Special handling for 'code' search parameters as it is indexed differently than other search parameters
            if ("code" in parameters) {
                val coding = parseCodeTypeParameterValue("code", parameters["code"]!!)
                indexStore.valueSetValidateCode()
            }
            repository.findAllById(ids).map { it.toValueSetResource() }
        }
    }

    override fun validateCode(
        id: Int?,
        url: String?,
        valueSetVersion: String?,
        system: String,
        code: String,
        systemVersion: String?,
        display: String?
    ): Parameters {
        val actualId: Int
        if (id == null) {
            if (url == null) throw PersistenceException("Cannot determine ValueSet instance: No ID nor URL provided")
            val params = mutableMapOf<String, IBase>()
            params["url"] = UriType(url)
            if (valueSetVersion != null) params["version"] = StringType(valueSetVersion)
            @Suppress("UNCHECKED_CAST")
            val ids = indexStore.search(params, ValueSet::class as KClass<out IResource>)
            if (ids.size > 1) throw PersistenceException("Cannot determine ValueSet instance: Multiple instances match: IDs: $ids")
            else if (ids.isEmpty()) return ValidateCodeParameters(false, "No ValueSet instance matched criteria")
            actualId = ids.first()
        }
        else actualId = id
        val conceptId = indexStore.valueSetValidateCode(actualId, system, code, systemVersion)
            ?: return ValidateCodeParameters(false, "Coding not in ValueSet instance [id: $actualId]")
        val concept = conceptRepository.findById(conceptId).get() // Should never be null unless inconsistencies exist
        return if (display != null && display != concept.display)
            ValidateCodeParameters(false, "Coding present in ValueSet instance [id: $actualId] but displays did not match [expected: '${concept.display}', actual: $display]")
        else ValidateCodeParameters(true, "Coding present in ValueSet instance [id: $actualId]", concept.display)
    }

    override fun validateCode(id: Int?, url: String?, valueSetVersion: String?, concept: CodeableConcept): Parameters {
        TODO("Not yet implemented")
    }

    override fun expand(url: String, version: String?): ValueSet {
        TODO("Not yet implemented")
    }

    override fun expand(id: Int): ValueSet {
        TODO("Not yet implemented")
    }

}