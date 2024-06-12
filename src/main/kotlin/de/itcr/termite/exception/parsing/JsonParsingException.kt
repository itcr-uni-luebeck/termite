package de.itcr.termite.exception.parsing

class JsonParsingException(message: String, e: Throwable?): Exception(message, e)

fun JsonParsingException(message: String) = JsonParsingException(message, null)