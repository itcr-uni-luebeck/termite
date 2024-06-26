package de.itcr.termite.index.partition

fun interface Generator1<V1, KEY> {

    fun invoke(v1: V1): KEY

}

fun interface Generator2<V1, V2, KEY> {

    fun invoke(v1: V1, v2: V2): KEY

}

fun interface Generator3<V1, V2, V3, KEY> {

    fun invoke(v1: V1, v2: V2, v3: V3): KEY

}

fun interface Generator4<V1, V2, V3, V4, KEY> {

    fun invoke(v1: V1, v2: V2, v3: V3, v4: V4): KEY

}

fun interface Generator<KEY> {

    fun invoke(v: Array<Any>): KEY

}