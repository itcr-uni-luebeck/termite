package de.itcr.termite.metadata.annotation.interaction.type

import de.itcr.termite.metadata.annotation.SearchParameter

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SupportsSearch(
    val searchParameters: Array<SearchParameter> = [],
    val searchInclude: Array<String> = [],
    val searchRevInclude: Array<String> = []
)
