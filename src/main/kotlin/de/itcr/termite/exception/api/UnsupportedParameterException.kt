package de.itcr.termite.exception.api

import org.springframework.http.HttpMethod

class UnsupportedParameterException(paramName: String, apiPath: String, method: HttpMethod, e: Throwable?)
    : ApiException("Parameter '$paramName' not supported for ${method.name} $apiPath", e)

fun UnsupportedParameterException(paramName: String, apiPath: String, method: HttpMethod) =
    UnsupportedParameterException(paramName, apiPath, method, null)