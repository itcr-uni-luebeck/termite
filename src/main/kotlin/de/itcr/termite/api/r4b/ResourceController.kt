package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import org.springframework.data.repository.Repository

abstract class ResourceController <TYPE, REPOSITORY: Repository<TYPE, Long>>
constructor (val repository: REPOSITORY, fhirContext: FhirContext): FhirController(fhirContext)