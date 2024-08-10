package de.itcr.termite.exception.api

class MissingParameterException(message: String, e: Throwable?) : ApiException(message, e)

fun MissingParameterException(message: String) = MissingParameterException(message, null)

fun MissingParameterException(params: Iterable<String>) =
    MissingParameterException("Missing required parameter(s): ${params.joinToString(", ")}")

fun MissingParameterException(vararg params: String) =
    MissingParameterException("Missing required parameter(s): ${params.joinToString(", ")}")