package de.itcr.termite.index

import de.itcr.termite.index.partition.IFhirIndexPartition

interface IteratorSupport<KEY, VALUE> {

    fun createIterator(partition: IFhirIndexPartition<*, KEY, *, *>): IIterator<KEY, VALUE>

    fun createIterator(partition: IFhirIndexPartition<*, KEY, *, *>, prefix: ByteArray): IIterator<KEY, VALUE>

}

interface IIterator<KEY, VALUE>: Iterator<Pair<KEY, VALUE?>>, AutoCloseable {

    override fun next(): Pair<KEY, VALUE?>

    override fun hasNext(): Boolean

    override fun close()

}