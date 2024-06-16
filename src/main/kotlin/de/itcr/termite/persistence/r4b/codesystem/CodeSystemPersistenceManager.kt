package de.itcr.termite.persistence.r4b.codesystem

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import de.itcr.termite.model.entity.toCodeSystemResource
import de.itcr.termite.model.entity.toFhirCodeSystemMetadata
import de.itcr.termite.model.repository.FhirCodeSystemMetadataRepository
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Parameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class CodeSystemPersistenceManager(
    @Autowired private val fhirCtx: FhirContext,
    @Autowired private val repository: FhirCodeSystemMetadataRepository,
    @Autowired @Qualifier("RocksDB") private val indexStore: FhirIndexStore
): ICodeSystemPersistenceManager<Int> {

    override fun create(instance: CodeSystem): CodeSystem {
        val csMetadata = instance.toFhirCodeSystemMetadata()
        val storedMetadata: FhirCodeSystemMetadata
        try { storedMetadata = repository.save(csMetadata) }
        catch (e: Exception) { throw PersistenceException("Failed to store CodeSystem metadata. Reason ${e.message}", e) }
        try {
            val conceptBatch = instance.concept.map { Pair("${instance.url}", it) }
        }
        catch (e: Exception) {
            repository.delete(storedMetadata)
            throw PersistenceException("Failed to index CodeSystem concepts. Reason: ${e.message}", e)
        }
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