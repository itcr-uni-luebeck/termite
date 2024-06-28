package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.serialize
import org.springframework.stereotype.Component

@Component
interface FhirIndexStore<KEY, VALUE>: BatchSupport<KEY, VALUE> {

    fun put(partition: FhirIndexPartitions<*, *, *, KEY, *, *>, key: ByteArray, value: ByteArray)

    fun put(batch: IBatch<KEY, VALUE>)

    fun seek(partition: FhirIndexPartitions<*, *, *, KEY, *, *>, key: KEY): VALUE

    fun delete(partition: FhirIndexPartitions<*, *, *, KEY, *, *>, key: KEY)

    fun delete(batch: IBatch<KEY, VALUE>)

}
