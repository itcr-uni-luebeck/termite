package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions

interface BatchSupport<KEY, VALUE> {

    fun createBatch(): IBatch<KEY, VALUE>

    fun processBatch(batch: IBatch<KEY, VALUE>)

}

interface IBatch<KEY, VALUE> {

    fun put(partition: FhirIndexPartitions<*, *, *, KEY, *, *>, key: KEY, value: VALUE?)

    fun <T> put(
        partition: FhirIndexPartitions<*, *, *, KEY, *, *>,
        data: Iterable<T>, keySelector: (T) -> KEY,
        valueSelector: (T) -> VALUE?
    )

}