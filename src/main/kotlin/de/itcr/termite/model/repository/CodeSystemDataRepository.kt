package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.CodeSystemData
import org.springframework.data.repository.CrudRepository

interface CodeSystemDataRepository: CrudRepository<CodeSystemData, Int>