package de.itcr.termite.exception

class ConceptMapException(message: String, e: Throwable?): Exception(message, e)

fun ConceptMapException(message: String) = ValueSetException(message, null)