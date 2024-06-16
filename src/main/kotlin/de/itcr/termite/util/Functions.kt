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