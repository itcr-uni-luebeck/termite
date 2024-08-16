package de.itcr.termite.persistence.r4b.codesystem

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.CodeSystemDataRepository
import de.itcr.termite.model.repository.CSConceptDataRepository
import de.itcr.termite.util.Either
import de.itcr.termite.util.Leither
import de.itcr.termite.util.r4b.*
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4b.model.*
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
        val csData = instance.toCodeSystemData()
        val storedData: CodeSystemData
        try { storedData = repository.save(csData) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem data. Reason: ${e.message}", e) }
        instance.id = storedData.id.toString()
        try { indexStore.putCodeSystem(instance, csData.concept) }
        catch (e: Exception) {
            repository.delete(storedData)
            throw PersistenceException("Failed to index CodeSystem instance. Reason: ${e.message}", e)
        }
        return storedData.toCodeSystemResource()
    }

    override fun update(id: Int, instance: CodeSystem): CodeSystem {
        instance.id = id.toString()
        val vsData = repository.save(instance.toCodeSystemData())
        return vsData.toCodeSystemResource()
    }

    override fun read(id: Int): CodeSystem {
        val csOptional = repository.findById(id)
        if (csOptional.isEmpty) throw NotFoundException<CodeSystem>("id", id)
        else return csOptional.get().toCodeSystemResource()
    }

    override fun delete(id: Int): CodeSystem {
        val storedMetadata: CodeSystemData
        logger.debug("Checking if CodeSystem instance exists [id: $id]")
        try { storedMetadata = repository.findById(id).get() }
        catch (e: NoSuchElementException) { throw NotFoundException<CodeSystem>("id", id) }
        catch (e: Exception) { throw PersistenceException("Error occurred while searching CodeSystem metadata. Reason: ${e.message}", e) }
        logger.debug("Deleting CodeSystem instance index data [id: $id]")
        logger.debug("Deleting CodeSystem instance concept data [id: $id]")
        val instance = storedMetadata.toCodeSystemResource()
        val concepts: Iterable<CodeSystemConceptData>
        try { concepts = conceptRepository.deleteByCodeSystem(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete CodeSystem concepts. Reason: ${e.message}", e) }
        logger.debug("Deleting CodeSystem instance index data [id: $id]")
        try { indexStore.deleteCodeSystem(instance, concepts) }
        catch (e: Exception) {
            conceptRepository.saveAll(concepts)
            throw PersistenceException("Failed to remove CodeSystem instance from index. Reason: ${e.message}", e)
        }
        logger.debug("Deleting CodeSystem instance metadata [id: $id]")
        try { repository.deleteById(id) }
        catch (e: Exception) { throw PersistenceException("Failed to delete CodeSystem metadata. Reason: ${e.message}", e) }
        return instance.tagAsSummarized()
    }

    override fun search(parameters: Parameters): List<CodeSystem> = search(parametersToMap(parameters))

    override fun search(parameters: Map<String, List<String>>): List<CodeSystem> {
        return if (parameters.isEmpty()) repository.findAll().map { it.toCodeSystemResource() }
        else {
            val supportedParams = indexStore.searchPartitionsByType(CodeSystem::class)
            val parsedParams = parameters.entries.filter { it.key != "_id" }.associate {
                val paramDef = supportedParams["CodeSystem.search.${it.key}"]!!
                return@associate it.key to it.value.map { v -> parseParameterValue(paramDef.parameter(), v) }
            }
            var ids: Set<Int>? = null
            // Special handling for '_id' search parameters as indexing it makes no sense
            if ("_id" in parameters) {
                val idSet = parameters["_id"]!!.map { it.toInt() }.toSet()
                if (idSet.size > 1) return emptyList()
                ids = idSet
            }
            @Suppress("UNCHECKED_CAST")
            val idSet = indexStore.search(parsedParams, CodeSystem::class as KClass<out IResource>)
            ids = if (ids != null) ids intersect idSet else idSet
            repository.findAllById(ids).map { it.toCodeSystemResource() }
        }
    }

    override fun validateCode(id: Int?, parameters: Map<String, List<String>>): Either<Parameters, OperationOutcome> {
        val actualId: Int
        val url: String
        val coding = parseCodeTypeParameterValue("code", parameters["code"]!![0]) // Always present after validation
        if (id == null) {
            url = parameters["url"]?.getOrNull(0) ?: coding.system
                ?: throw PersistenceException("Cannot determine CodeSystem instance: No ID nor URL provided")
            val params = mutableMapOf<String, List<IBase>>()
            params["url"] = listOf(UriType(url))
            val version = parameters["version"]?.getOrNull(0)
            if (version != null) params["version"] = listOf(StringType(version))
            @Suppress("UNCHECKED_CAST")
            val ids = indexStore.search(params, CodeSystem::class as KClass<out IResource>)
            if (ids.size > 1) throw PersistenceException("Cannot determine CodeSystem instance: Multiple instances match: IDs: $ids")
            else if (ids.isEmpty()) return Leither(ValidateCodeParameters(false, "No CodeSystem instance matched criteria"))
            actualId = ids.first()
        }
        else {
            actualId = id
            url = repository.findById(id).get().url
        }
        val conceptId = indexStore.codeSystemValidateCode(actualId, coding.system?: url, coding.code)
            ?: return Leither(ValidateCodeParameters(false, "Coding not in ValueSet instance [id: $actualId]"))
        val concept = conceptRepository.findById(conceptId).get() // Should never be null unless inconsistencies exist
        val display = parameters["display"]!!.getOrNull(0)
        return Leither(if (display != null && display != concept.display)
            ValidateCodeParameters(false, "Coding present in CodeSystem instance [id: $actualId] but displays did not match [expected: '${concept.display}', actual: $display]")
        else ValidateCodeParameters(true, "Coding present in CodeSystem instance [id: $actualId]", concept.display)
        )
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

    override fun exists(id: Int): Boolean = repository.existsById(id)
}