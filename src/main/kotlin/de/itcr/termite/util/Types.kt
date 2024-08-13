package de.itcr.termite.util

import kotlin.contracts.contract

data class Either<out LEFT, out RIGHT> (val left: LEFT?, val right: RIGHT?) {

    fun hasLeft(): Boolean = left != null

    fun hasRight(): Boolean = right != null

}

fun <LEFT> Leither(left: LEFT) = Either<LEFT, Nothing>(left, null)

fun <RIGHT> Reither(right: RIGHT) = Either<Nothing, RIGHT>(null, right)

fun Neither() = Either<Nothing, Nothing>(null, null)