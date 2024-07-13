package de.itcr.termite.exception.api

class UnsupportedFormatException(message: String, e: Throwable?): ApiException(message, e)

fun UnsupportedFormatException(message: String) = UnsupportedFormatException(message, null)