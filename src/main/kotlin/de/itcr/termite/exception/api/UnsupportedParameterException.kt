package de.itcr.termite.exception.api

import org.springframework.http.HttpMethod

class UnsupportedParameterException(message: String, e: Throwable?)
    : ApiException(message, e)

fun UnsupportedParameterException(message: String) =
    UnsupportedParameterException(message, null)

fun UnsupportedParameterException(paramName: String, apiPath: String, method: HttpMethod) =
    UnsupportedParameterException("Parameter '$paramName' not supported for ${method.name} $apiPath")