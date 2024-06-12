package de.itcr.termite.util

import java.net.URI
import java.net.URL
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