package de.itcr.termite.metadata.annotation

import kotlin.reflect.KClass

annotation class Parameter(
    val name: String,
    val use: String,
    val min: Int,
    val max: String,
    val documentation: String = "",
    val type: String = "",
    val part: Array<Parameter> = []
)