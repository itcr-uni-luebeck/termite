package de.itcr.termite.api

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.metadata.annotation.SupportsInteraction
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@ForResource(
    type = "ConceptMap",
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
    searchParam = [
        SearchParameter(
            name = "url",
            type = "uri",
            documentation = "URL of the resource to locate"
        )
    ]
)
@SupportsInteraction(["create", "search-type"])
@Controller
@RequestMapping("fhir/ConceptMap")
class ConceptMapController(
    @Autowired database: TerminologyStorage,
    @Autowired fhirContext: FhirContext
): ResourceController(database, fhirContext) {

    companion object {
        private val logger: Logger = LogManager.getLogger(ConceptMapController::class.java)
    }

}