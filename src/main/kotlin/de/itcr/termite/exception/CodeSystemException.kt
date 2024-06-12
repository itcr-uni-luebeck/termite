package de.itcr.termite.exception

class CodeSystemException(message: String, e: Throwable?): Exception(message, e)

fun CodeSystemException(message: String) = CodeSystemException(message, null)