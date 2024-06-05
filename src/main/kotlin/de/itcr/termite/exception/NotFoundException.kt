package de.itcr.termite.exception

import kotlin.reflect.KClass

class NotFoundException constructor(clazz: KClass<*>, identifiers: List<Pair<String, String>>)
    : Exception("${clazz.simpleName} instance [${identifiers.joinToString(" and ") { "${it.first} = ${it.second}" }}] was not found")

inline fun <reified T> NotFoundException(identifiers: List<Pair<String, String>>) = NotFoundException(T::class, identifiers)

inline fun <reified T> NotFoundException(identifier: Pair<String, String>) = NotFoundException(T::class, listOf(identifier))