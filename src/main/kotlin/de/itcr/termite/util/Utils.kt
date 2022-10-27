package de.itcr.termite.util

import java.net.URI
import java.net.URL
import java.nio.file.Path

val cLoader: ClassLoader = Thread.currentThread().contextClassLoader

fun findResourceURL(path: Path): URL? {
    val stringPath = path.toString()
    return cLoader.getResource(stringPath)
}

fun findResourceURI(path: Path) : URI? {
    return findResourceURL(path)?.toURI()
}