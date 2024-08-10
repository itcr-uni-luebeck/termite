package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.ValueSetData
import org.springframework.data.repository.CrudRepository

interface ValueSetDataRepository: CrudRepository<ValueSetData, Int>