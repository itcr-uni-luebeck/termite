package de.itcr.termite.util

import org.apache.commons.collections4.trie.PatriciaTrie

inline fun <T, V> Iterable<T>.associateTrie(transform: (T) -> Pair<String, V>): PatriciaTrie<V> {
    val trie = PatriciaTrie<V>()
    for (element in this) {
        val tuple = transform(element)
        trie[tuple.first] = tuple.second
    }
    return trie
}

fun <T> List<Pair<T, T>>.flatten(): List<T> {
    val accumulator = ArrayList<T>(size*2)
    forEach { elem -> accumulator.add(elem.first); accumulator.add(elem.second) }
    return accumulator
}