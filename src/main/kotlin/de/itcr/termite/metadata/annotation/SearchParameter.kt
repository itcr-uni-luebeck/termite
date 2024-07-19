package de.itcr.termite.metadata.annotation

import kotlin.reflect.KClass

annotation class SearchParameter(
    val name: String,
    val type: String = "string",
    val documentation: String = "",
    val processing: ProcessingHint = ProcessingHint(Nothing::class, ""),
    val sameAs: String = ""
)
