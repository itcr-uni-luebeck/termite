package de.itcr.termite.index

import de.itcr.termite.index.partition.IFhirIndexPartition

interface BatchSupport<KEY, VALUE> {

    fun createBatch(): IBatch<KEY, VALUE>

    fun processBatch(batch: IBatch<KEY, VALUE>)

}

interface IBatch<KEY, VALUE> {

    fun put(partition: IFhirIndexPartition<*, *, *, KEY, *, *>, key: KEY, value: VALUE?)

    fun <T> put(
        partition: IFhirIndexPartition<*, *, *, KEY, *, *>,
        data: Iterable<T>, keySelector: (T) -> KEY,
        valueSelector: (T) -> VALUE?
    )

}