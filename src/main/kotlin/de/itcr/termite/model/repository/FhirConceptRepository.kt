package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import de.itcr.termite.model.entity.FhirConcept
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import javax.transaction.Transactional

interface FhirConceptRepository: CrudRepository<FhirConcept, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM FhirConcept WHERE cs_id = ?1")
    fun deleteByCodeSystem(csId: Int)

}