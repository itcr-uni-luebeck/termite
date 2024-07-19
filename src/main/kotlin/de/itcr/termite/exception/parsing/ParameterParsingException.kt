package de.itcr.termite.exception.parsing

class ParameterParsingException(message: String, e: Throwable?): Exception(message, e)

fun ParameterParsingException(message: String) = ParameterParsingException(message, null)