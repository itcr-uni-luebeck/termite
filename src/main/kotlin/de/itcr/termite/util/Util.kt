package de.itcr.termite.util

import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Path

private val cLoader: ClassLoader = Thread.currentThread().contextClassLoader
private val isPositiveIntegerRegex = Regex("^[0-9]\$|^[1-9][0-9]*\$")

fun findResourceURL(path: Path): URL? {
    val stringPath = path.toString()
    return cLoader.getResource(stringPath)
}

fun findResourceURI(path: Path) : URI? {
    return findResourceURL(path)?.toURI()
}

fun isPositiveInteger(s: String): Boolean = s.matches(isPositiveIntegerRegex)

inline fun <T, reified U> Array<T>.map(transform: (T) -> U): Array<out U> {
    val outArr = arrayOfNulls<U>(this.size)
    for (i in this.indices) outArr[i] = transform(this[i])
    @Suppress("UNCHECKED_CAST")
    return outArr as Array<U>
}

inline fun <T> Array<T>.mapToBytes(transform: (T) -> ByteArray = { serialize(it.hashCode()) }, byteSize: Int = 4): ByteArray {
    val outArr = ByteArray(this.size * byteSize)
    for (i in this.indices) {
        val bytes = transform(this[i])
        for (j in bytes.indices) outArr[i + j] = bytes[j]
    }
    return outArr
}