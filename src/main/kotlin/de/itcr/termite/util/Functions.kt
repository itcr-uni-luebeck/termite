package de.itcr.termite.util

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import de.itcr.termite.parser.IBackboneElementParser
import org.apache.commons.collections4.trie.PatriciaTrie
import kotlin.reflect.KClass

inline fun <T, V> Iterable<T>.associateTrie(transform: (T) -> Pair<String, V>): PatriciaTrie<V> {
    val trie = PatriciaTrie<V>()
    for (element in this) {
        val tuple = transform(element)
        trie[tuple.first] = tuple.second
    }
    return trie
}

fun FhirContext.newBackboneElementParser(): IBackboneElementParser {
    return when (val version = this.version.version) {
        FhirVersionEnum.R4B -> de.itcr.termite.parser.r4b.BackboneElementParser()
        else -> throw Exception("Unsupported FHIR version '${version.fhirVersionString}' for BackboneElementParser class")
    }
}

fun Class<*>.simpleQualifiedName(): String {
    return "${enclosingClass?.simpleQualifiedName() ?: ""}.$simpleName"
}

fun KClass<*>.simpleQualifiedName() = this.java.simpleQualifiedName()