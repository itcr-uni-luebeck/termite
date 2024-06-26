package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions

interface IteratorSupport<KEY, VALUE> {

    fun createIterator(partition: FhirIndexPartitions<KEY, *, VALUE, *>): IIterator<KEY, VALUE>

    fun createIterator(partition: FhirIndexPartitions<KEY, *, VALUE, *>, prefix: ByteArray): IIterator<KEY, VALUE>

}

interface IIterator<KEY, VALUE>: Iterator<Pair<KEY, VALUE>>, AutoCloseable {

    override fun next(): Pair<KEY, VALUE>

    override fun hasNext(): Boolean

    override fun close()

    fun key(): KEY

    fun value(): VALUE

}