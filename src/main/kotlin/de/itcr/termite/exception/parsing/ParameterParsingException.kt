package de.itcr.termite.exception.parsing

import de.itcr.termite.parser.r4b.ParameterParser

class ParameterParsingException(message: String, e: Throwable?): Exception(message, e)

fun ParameterParsingException(message: String) = ParameterParsingException(message, null)