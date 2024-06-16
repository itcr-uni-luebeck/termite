package de.itcr.termite.persistence.r4b

import org.hl7.fhir.r4b.model.Parameters

interface FhirPersistenceManager<RESOURCE_TYPE, ID_TYPE> {

    fun create(instance: RESOURCE_TYPE): RESOURCE_TYPE

    fun update(id: ID_TYPE, instance: RESOURCE_TYPE): RESOURCE_TYPE

    fun read(id: ID_TYPE): RESOURCE_TYPE

    fun delete(id: ID_TYPE)

    fun search(parameters: Parameters): List<RESOURCE_TYPE>

    fun search(parameters: Map<String, Any>): List<RESOURCE_TYPE>

}