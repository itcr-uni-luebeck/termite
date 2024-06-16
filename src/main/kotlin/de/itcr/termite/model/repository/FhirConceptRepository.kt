package de.itcr.termite.model.repository

import de.itcr.termite.model.entity.FhirConcept
import org.springframework.data.repository.CrudRepository

interface FhirConceptRepository: CrudRepository<FhirConcept, Long>