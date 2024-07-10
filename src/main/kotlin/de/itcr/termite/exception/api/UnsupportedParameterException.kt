package de.itcr.termite.exception.api

import org.springframework.http.HttpMethod

class UnsupportedParameterException(paramName: String, apiPath: String, methodName: HttpMethod, e: Throwable?)
    : ApiException("Parameter '$paramName' not supported for ${methodName.name} $apiPath", e)

fun UnsupportedParameterException(paramName: String, apiPath: String, methodName: HttpMethod) =
    UnsupportedParameterException(paramName, apiPath, methodName, null)