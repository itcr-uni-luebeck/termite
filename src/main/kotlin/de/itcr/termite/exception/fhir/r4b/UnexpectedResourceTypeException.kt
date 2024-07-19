package de.itcr.termite.exception.fhir.r4b

import org.hl7.fhir.r4b.model.ResourceType

class UnexpectedResourceTypeException (message: String, e: Throwable?): Exception(message, e)

fun UnexpectedResourceTypeException(message: String) =
    UnexpectedResourceTypeException(message, null)

fun UnexpectedResourceTypeException(expected: ResourceType, actual: ResourceType) =
    UnexpectedResourceTypeException("Unexpected resource type '${actual.name}'. Expected '${expected.name}' ")
