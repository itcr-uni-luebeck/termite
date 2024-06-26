package de.itcr.termite.index.partition


fun interface Generator1<V1, R>: Function<R> {

    fun invoke(v1: V1): R

}

fun interface Generator2<V1, V2, R>: Function<R> {

    fun invoke(v1: V1, v2: V2): R

}

fun interface Generator3<V1, V2, V3, R>: Function<R> {

    fun invoke(v1: V1, v2: V2, v3: V3): R

}

fun interface Generator4<V1, V2, V3, V4, R>: Function<R> {

    fun invoke(v1: V1, v2: V2, v3: V3, v4: V4): R

}