package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import de.itcr.termite.util.Either
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.OperationOutcome
import org.hl7.fhir.r4b.model.Parameters

interface ICodeSystemPersistenceManager<ID_TYPE>: FhirPersistenceManager<CodeSystem, ID_TYPE> {

    fun validateCode(id: Int?, parameters: Map<String, List<String>>): Either<Parameters, OperationOutcome>

    fun lookup(
        system: String,
        code: String,
        version: String?,
        display: String?,
        displayLanguage: String?,
        property: Set<String>?
    ): Parameters

}