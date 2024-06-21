package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import org.hl7.fhir.r4b.model.CodeSystem
import org.springframework.data.repository.Repository

abstract class ResourceController <TYPE, ID>
constructor (val persistence: FhirPersistenceManager<TYPE, ID>, fhirContext: FhirContext): FhirController(fhirContext)