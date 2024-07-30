package de.itcr.termite.persistence.r4b.valueset

import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.Parameters
import org.hl7.fhir.r4b.model.ValueSet

interface IValueSetPersistenceManager<ID_TYPE>: FhirPersistenceManager<ValueSet, ID_TYPE> {

    fun validateCode(
        url: String,
        valueSetVersion: String?,
        system: String,
        code: String,
        systemVersion: String?,
        display: String?
    ): Parameters

    fun validateCode(
        url: String,
        valueSetVersion: String?,
        coding: Coding
    ): Parameters = validateCode(url, valueSetVersion, coding.system, coding.code, coding.version, coding.display)

    fun validateCode(
        id: Int,
        system: String,
        code: String,
        systemVersion: String?,
        display: String?
    ): Parameters

    fun validateCode(
        id: Int,
        coding: Coding
    ): Parameters = validateCode(id, coding.system, coding.code, coding.version, coding.display)

    fun expand(
        url: String,
        version: String?
    ): ValueSet

    fun expand(
        id: Int
    ): ValueSet

}