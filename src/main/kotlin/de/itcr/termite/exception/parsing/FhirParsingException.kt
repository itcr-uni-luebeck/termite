package de.itcr.termite.exception.parsing

class FhirParsingException(message: String, e: Throwable?): Exception(message, e)

fun FhirParsingException(message: String) = FhirParsingException(message, null)