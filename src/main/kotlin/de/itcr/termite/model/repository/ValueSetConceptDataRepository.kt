package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.VSConceptData
import org.springframework.data.repository.CrudRepository

interface ValueSetConceptDataRepository: CrudRepository<VSConceptData, Long>