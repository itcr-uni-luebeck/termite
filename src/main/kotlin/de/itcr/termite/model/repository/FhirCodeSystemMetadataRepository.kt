package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import org.springframework.data.repository.CrudRepository

interface FhirCodeSystemMetadataRepository: CrudRepository<FhirCodeSystemMetadata, Long>