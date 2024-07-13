package de.itcr.termite.exception.api

open class ApiException(message: String, e: Throwable?): Exception(message, e)

fun ApiException(message: String) = ApiException(message, null)