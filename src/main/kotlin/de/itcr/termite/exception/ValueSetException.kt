package de.itcr.termite.exception

class ValueSetException(message: String, e: Throwable?): Exception(message, e)

fun ValueSetException(message: String) = ValueSetException(message, null)