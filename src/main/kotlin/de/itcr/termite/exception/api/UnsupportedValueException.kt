package de.itcr.termite.exception.api

import org.springframework.http.HttpMethod

class UnsupportedValueException(message: String, e: Throwable?): ApiException(message, e)

fun UnsupportedValueException(message: String) =
    UnsupportedValueException(message, null)

fun UnsupportedValueException(value: String, paramName: String, apiPath: String, methodName: HttpMethod, e: Throwable?) =
    UnsupportedValueException("Value '$value' for parameter '$paramName' not supported for ${methodName.name} $apiPath", e)

fun UnsupportedValueException(value: String, paramName: String, apiPath: String, methodName: HttpMethod) =
    UnsupportedValueException(value, paramName, apiPath, methodName, null)