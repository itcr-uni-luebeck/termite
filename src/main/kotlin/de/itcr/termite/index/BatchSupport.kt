package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions

interface BatchSupport<KEY, VALUE> {

    fun createBatch(): IBatch<KEY, VALUE>

    fun processBatch(batch: IBatch<KEY, VALUE>)

}

interface IBatch<KEY, VALUE> {

    fun put(partition: FhirIndexPartitions<KEY, *, VALUE, *>, key: ByteArray, value: ByteArray)

    fun <T> put(
        partition: FhirIndexPartitions<KEY, *, VALUE, *>,
        data: Iterable<T>, keySelector: (T) -> ByteArray,
        valueSelector: (T) -> ByteArray
    )

}