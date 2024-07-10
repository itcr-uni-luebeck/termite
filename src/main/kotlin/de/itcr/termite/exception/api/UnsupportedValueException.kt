package de.itcr.termite.exception.api

import org.springframework.http.HttpMethod

class UnsupportedValueException(value: String, paramName: String, apiPath: String, methodName: HttpMethod, e: Throwable?)
    : ApiException("Value '$value' for parameter '$paramName' not supported for ${methodName.name} $apiPath", e)

fun UnsupportedValueException(value: String, paramName: String, apiPath: String, methodName: HttpMethod) =
    UnsupportedValueException(value, paramName, apiPath, methodName, null)