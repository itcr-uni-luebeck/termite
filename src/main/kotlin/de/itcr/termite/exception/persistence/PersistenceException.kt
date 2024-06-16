package de.itcr.termite.exception.persistence

class PersistenceException(message: String, e: Throwable?): Exception(message, e)

fun PersistenceException(message: String) = PersistenceException(message, null)