package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Parameters

interface ICodeSystemPersistenceManager<ID_TYPE>: FhirPersistenceManager<CodeSystem, ID_TYPE> {

    fun validateCode(url: String, code: String, version: String?, display: String?): Parameters

    fun lookup(
        system: String,
        code: String,
        version: String?,
        display: String?,
        displayLanguage: String?,
        property: Set<String>?
    ): Parameters

}