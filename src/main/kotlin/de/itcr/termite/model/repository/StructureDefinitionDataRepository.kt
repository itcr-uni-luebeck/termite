package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.StructureDefinitionData
import org.springframework.data.repository.CrudRepository

interface StructureDefinitionDataRepository: CrudRepository<StructureDefinitionData, Int>