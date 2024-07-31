package de.itcr.termite.persistence.r4b.codesystem

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.CodeSystemDataRepository
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.util.r4b.parametersToMap
import de.itcr.termite.util.r4b.parseParameterValue
import de.itcr.termite.util.r4b.tagAsSummarized
import de.itcr.termite.util.serializeInOrder
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Parameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.random.Random
import kotlin.reflect.KClass

@Component
class CodeSystemPersistenceManager(
    @Autowired private val fhirCtx: FhirContext,
    @Autowired private val repository: CodeSystemDataRepository,
    @Autowired private val conceptRepository : CSConceptDataRepository,
    @Autowired private val indexStore: FhirIndexStore<ByteArray, ByteArray>
): ICodeSystemPersistenceManager<Int> {

    companion object {

        private val logger = LogManager.getLogger(this::class)

    }

    private val random = Random(0)

    override fun create(instance: CodeSystem): CodeSystem {
        // Calculate concept count if necessary
        instance.count = if (instance.count > 0) instance.count else instance.concept.size
        val csMetadata = instance.toCodeSystemData()
        val storedMetadata: CodeSystemData
        try { storedMetadata = repository.save(csMetadata) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem metadata. Reason: ${e.message}", e) }
        instance.id = storedMetadata.id.toString()
        val concepts: Iterable<CodeSystemConceptData>
        try { concepts = conceptRepository.saveAll(instance.concept.map { it.toCSConceptData(random.nextLong(), storedMetadata) }) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem concepts. Reason: ${e.message}", e) }
        try { indexStore.putCodeSystem(instance, concepts) }
        catch (e: Exception) {
            conceptRepository.deleteByCodeSystem(storedMetadata.id)
            repository.delete(storedMetadata)
            throw PersistenceException("Failed to index CodeSystem instance. Reason: ${e.message}", e)
        }
        return storedMetadata.toCodeSystemResource().tagAsSummarized()
    }

    override fun update(id: Int, instance: CodeSystem): CodeSystem {
        TODO("Not yet implemented")
    }

    override fun read(id: Int): CodeSystem {
        val csOptional = repository.findById(id)
        if (csOptional.isEmpty) throw NotFoundException<CodeSystem>("id", id)
        else return csOptional.get().toCodeSystemResource()
    }

    override fun delete(id: Int): CodeSystem {
        val storedMetadata: CodeSystemData
        try { storedMetadata = repository.findById(id).get() }
        catch (e: NoSuchElementException) { throw NotFoundException<CodeSystem>("id", id) }
        catch (e: Exception) { throw PersistenceException("Error occurred while searching CodeSystem metadata. Reason: ${e.message}", e) }
        val instance = storedMetadata.toCodeSystemResource()
        val concepts: Iterable<CodeSystemConceptData>
        try { concepts = conceptRepository.deleteByCodeSystem(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete CodeSystem concepts. Reason: ${e.message}", e) }
        try { indexStore.deleteCodeSystem(instance, concepts) }
        catch (e: Exception) {
            conceptRepository.deleteByCodeSystem(storedMetadata.id)
            repository.delete(storedMetadata)
            throw PersistenceException("Failed to remove CodeSystem instance from index. Reason: ${e.message}", e)
        }
        try { repository.deleteById(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete CodeSystem metadata. Reason: ${e.message}", e) }
        return instance.tagAsSummarized()
    }

    override fun search(parameters: Parameters): List<CodeSystem> = search(parametersToMap(parameters))

    override fun search(parameters: Map<String, String>): List<CodeSystem> {
        return if (parameters.isEmpty()) repository.findAll().map { it.toCodeSystemResource() }
        else {
            val supportedParams = indexStore.searchPartitionsByType(CodeSystem::class)
            val parsedParams = parameters.entries.filter { it.key != "_id" }.associate {
                val paramDef = supportedParams["CodeSystem.search.${it.key}"]
                return@associate it.key to parseParameterValue(paramDef!!.parameter(), it.value)
            }
            @Suppress("UNCHECKED_CAST")
            var ids = indexStore.search(parsedParams, CodeSystem::class as KClass<out IResource>)
            // Special handling for '_id' search parameters as indexing it makes no sense
            if ("_id" in parameters) ids = ids intersect setOf(parameters["_id"]!!.toInt())
            repository.findAllById(ids).map { it.toCodeSystemResource() }
        }
    }

    override fun validateCode(url: String, code: String, version: String?, display: String?): Parameters {
        val prefix = serializeInOrder(url.hashCode(), code.hashCode(), version.hashCode())
        TODO()
    }

    override fun lookup(
        system: String,
        code: String,
        version: String?,
        display: String?,
        displayLanguage: String?,
        property: Set<String>?
    ): Parameters {
        val conceptId = indexStore.codeSystemLookup(code, system, version)
        TODO("Not yet implemented")
    }

}