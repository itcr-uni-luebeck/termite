package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.ValueSetMetadata
import org.springframework.data.repository.CrudRepository

interface ValueSetMetadataRepository: CrudRepository<ValueSetMetadata, Int>