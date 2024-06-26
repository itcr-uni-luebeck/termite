package de.itcr.termite.persistence.r4b.codesystem

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.FhirCodeSystemMetadataRepository
import de.itcr.termite.model.repository.FhirConceptRepository
import de.itcr.termite.util.serialize
import de.itcr.termite.util.serializeInOrder
import de.itcr.termite.util.tagAsSummarized
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Parameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.Objects
import kotlin.random.Random

@Component
class CodeSystemPersistenceManager(
    @Autowired private val fhirCtx: FhirContext,
    @Autowired private val repository: FhirCodeSystemMetadataRepository,
    @Autowired private val conceptRepository : FhirConceptRepository,
    @Autowired private val indexStore: FhirIndexStore
): ICodeSystemPersistenceManager<Int> {

    private val random = Random(0)

    override fun create(instance: CodeSystem): CodeSystem {
        // Calculate concept count if necessary
        instance.count = if (instance.count > 0) instance.count else instance.concept.size
        val csMetadata = instance.toFhirCodeSystemMetadata()
        val storedMetadata: FhirCodeSystemMetadata
        try { storedMetadata = repository.save(csMetadata) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem metadata. Reason: ${e.message}", e) }
        val concepts: Iterable<FhirConcept>
        try { concepts = conceptRepository.saveAll(instance.concept.map { it.toFhirConcept(random.nextLong(), storedMetadata) }) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem concepts. Reason: ${e.message}", e) }
        try {
            val batch = indexStore.createBatch()
            val system = instance.url
            val version = instance.version
            // TODO: Extract key generators to some external place where they can more easily be found
            val keySelector: FhirConcept.() -> ByteArray = {
                val buffer = ByteBuffer.allocate(16)
                buffer.putInt(system.hashCode())
                buffer.putInt(code.hashCode())
                buffer.putInt(Objects.hashCode(version)) // Since version can be null
                buffer.putInt(csMetadata.id)
                buffer.array()
            }
            val valueSelector: FhirConcept.() -> ByteArray = { serialize(id) }
            batch.put(CodeSystemIndexPartitions.LOOKUP, concepts, keySelector, valueSelector)
            indexStore.processBatch(batch)
        }
        catch (e: Exception) {
            conceptRepository.deleteByCodeSystem(storedMetadata.id)
            repository.delete(storedMetadata)
            throw PersistenceException("Failed to index CodeSystem concepts. Reason: ${e.message}", e)
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

    override fun delete(id: Int) {
        TODO("Not yet implemented")
    }

    override fun search(parameters: Parameters): List<CodeSystem> {
        TODO("Not yet implemented")
    }

    override fun search(parameters: Map<String, Any>): List<CodeSystem> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

}