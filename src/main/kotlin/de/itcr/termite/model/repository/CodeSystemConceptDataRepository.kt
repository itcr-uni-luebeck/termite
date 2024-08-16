package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.CodeSystemConceptData
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import javax.transaction.Transactional

typealias CSConceptDataRepository = CodeSystemConceptDataRepository

interface CodeSystemConceptDataRepository: CrudRepository<CodeSystemConceptData, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM cs_concept WHERE cs_id = ?1 RETURNING *", nativeQuery = true)
    fun deleteByCodeSystem(csId: Int): List<CodeSystemConceptData>

}