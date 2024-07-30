package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.CodeSystemMetadata
import org.springframework.data.repository.CrudRepository

interface CodeSystemMetadataRepository: CrudRepository<CodeSystemMetadata, Int>