package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.metadata.annotation.SupportsInteraction
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemPersistenceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping

@ForResource(
    type = "StructureDefinition",
    versioning = "no-version",
    readHistory = false,
    updateCreate = false,
    conditionalCreate = false,
    conditionalRead = "not-supported",
    conditionalUpdate = false,
    conditionalDelete = "not-supported",
    referencePolicy = [],
    searchInclude = [],
    searchRevInclude = [],
    searchParam = []
)
@SupportsInteraction(["search-type", "read"])
@RequestMapping("fhir/StructureDefinition")
class StructureDefinitionController(
    @Autowired properties: ApplicationConfig,
    @Autowired val csPersistence: CodeSystemPersistenceManager,
    @Autowired fhirContext: FhirContext,
    @Autowired val capabilityStatement: CapabilityStatement
): FhirController(fhirContext, properties, logger) {

    companion object {
        private val logger: Logger = LogManager.getLogger(this)
    }

}