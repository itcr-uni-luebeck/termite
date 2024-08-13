package de.itcr.termite.persistence.r4b.valueset

import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import de.itcr.termite.util.Either
import org.hl7.fhir.r4b.model.*

interface IValueSetPersistenceManager<ID_TYPE>: FhirPersistenceManager<ValueSet, ID_TYPE> {

    fun validateCode(id: Int?, parameters: Map<String, List<String>>): Either<Parameters, OperationOutcome>

    fun expand(id: Int?, parameters: Map<String, List<String>>): Either<ValueSet, OperationOutcome>

}