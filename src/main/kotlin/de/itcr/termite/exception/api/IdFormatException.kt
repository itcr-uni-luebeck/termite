package de.itcr.termite.exception.api

import ca.uhn.fhir.parser.DataFormatException

class IdFormatException(message: String, e: Throwable?): DataFormatException(message, e)

fun IdFormatException(id: Any) =
    IdFormatException("Supplied ID not in expected range [1, 2.147.483.647]", null)