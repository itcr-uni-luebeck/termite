package de.itcr.termite.metadata.annotation

import kotlin.reflect.KClass

annotation class SearchParameter(
    val name: String,
    val type: String,
    val documentation: String = "",
    val processing: ProcessingHint
)
