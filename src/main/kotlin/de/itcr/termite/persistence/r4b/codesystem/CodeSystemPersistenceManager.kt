package de.itcr.termite.persistence.r4b.codesystem

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.index.partition.CodeSystemIndexPartitions
import de.itcr.termite.model.entity.*
import de.itcr.termite.model.repository.FhirCodeSystemMetadataRepository
import de.itcr.termite.model.repository.FhirConceptRepository
import de.itcr.termite.util.serialize
import de.itcr.termite.util.tagAsSummarized
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Parameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.nio.ByteBuffer

class CodeSystemPersistenceManager(
    @Autowired private val fhirCtx: FhirContext,
    @Autowired private val repository: FhirCodeSystemMetadataRepository,
    @Autowired private val conceptRepository : FhirConceptRepository,
    @Autowired @Qualifier("RocksDB") private val indexStore: FhirIndexStore
): ICodeSystemPersistenceManager<Int> {

    override fun create(instance: CodeSystem): CodeSystem {
        val csMetadata = instance.toFhirCodeSystemMetadata()
        val storedMetadata: FhirCodeSystemMetadata
        try { storedMetadata = repository.save(csMetadata) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem metadata. Reason: ${e.message}", e) }
        val storedConcepts: Iterable<FhirConcept>
        try { storedConcepts = conceptRepository.saveAll(instance.concept.map { it.toFhirConcept(storedMetadata.id) }) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem concepts. Reason: ${e.message}", e) }
        try {
            val conceptBatch = storedConcepts.map {
                val buffer = ByteBuffer.allocate(32)
                buffer.putInt(instance.url.hashCode())
                buffer.putInt(it.code.hashCode())
                buffer.putInt(instance.version.hashCode())
                buffer.putInt(storedMetadata.id)
                return@map Pair(buffer.array(), serialize(it.id))
            }
            indexStore.put(CodeSystemIndexPartitions.CODE_SYSTEM_LOOKUP, conceptBatch)
        }
        catch (e: Exception) {
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

    override fun validateCode(parameters: Parameters): Parameters {
        TODO("Not yet implemented")
    }

    override fun lookup(parameters: Parameters): Parameters {
        TODO("Not yet implemented")
    }

}