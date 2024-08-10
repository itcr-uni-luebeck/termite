package de.itcr.termite.util

import ca.uhn.fhir.rest.api.PreferHandlingEnum
import org.apache.http.client.utils.URLEncodedUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

fun parseHeaderValueAsList(value: String?): List<String> = value?.split(";")?.map { it.trim() } ?: emptyList()

// TODO: Handle malformed header values
fun parseHeaderValueAsMap(value: String?): Map<String, String> {
    return if (value == null) emptyMap()
    else parseHeaderValueAsList(value).associate { val split = it.split("="); split[0].trim() to split[1].trim() }
}

fun parsePreferHandling(prefer: String?, default: PreferHandlingEnum = PreferHandlingEnum.LENIENT) =
    PreferHandlingEnum.fromHeaderValue(parseHeaderValueAsMap(prefer).getOrDefault("handling", null)) ?: default

// TODO: Update Spring dependencies
fun parseQueryParameters(query: String): Map<String, List<String>> =
    URLEncodedUtils.parse(query, Charsets.UTF_8).associate { it.name to it.value.split(',') }

fun parametersToString(parameters: Map<String, List<String>>) =
    parameters.flatMap { it.value.map { v -> Pair(it.key, v) } }.joinToString { "${it.first} = ${it.second}" }