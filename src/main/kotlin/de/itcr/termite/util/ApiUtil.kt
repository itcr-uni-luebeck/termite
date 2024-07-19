package de.itcr.termite.util

import ca.uhn.fhir.rest.api.PreferHandlingEnum

fun parseHeaderValueAsList(value: String?): List<String> = value?.split(";")?.map { it.trim() } ?: emptyList()

// TODO: Handle malformed header values
fun parseHeaderValueAsMap(value: String?): Map<String, String> {
    return if (value == null) emptyMap()
    else parseHeaderValueAsList(value).associate { val split = it.split("="); split[0].trim() to split[1].trim() }
}

fun parsePreferHandling(prefer: String?, default: PreferHandlingEnum = PreferHandlingEnum.LENIENT) =
    PreferHandlingEnum.fromHeaderValue(parseHeaderValueAsMap(prefer).getOrDefault("handling", null)) ?: default