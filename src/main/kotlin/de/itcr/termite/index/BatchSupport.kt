package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions

interface BatchSupport {

    fun createBatch(): IBatch

    fun processBatch(batch: IBatch)

}

interface IBatch {

    fun put(partition: FhirIndexPartitions, key: ByteArray, value: ByteArray)

    fun <T> put(partition: FhirIndexPartitions, data: Iterable<T>, keySelector: (T) -> ByteArray, valueSelector: (T) -> ByteArray)

}