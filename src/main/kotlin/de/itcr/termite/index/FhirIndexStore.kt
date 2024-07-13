package de.itcr.termite.index

import de.itcr.termite.index.partition.IFhirIndexPartition
import org.springframework.stereotype.Component

@Component
interface FhirIndexStore<KEY, VALUE>: BatchSupport<KEY, VALUE> {

    fun put(partition: IFhirIndexPartition<*, *, *, KEY, *, *>, key: ByteArray, value: ByteArray)

    fun put(batch: IBatch<KEY, VALUE>)

    fun seek(partition: IFhirIndexPartition<*, *, *, KEY, *, *>, key: KEY): VALUE

    fun delete(partition: IFhirIndexPartition<*, *, *, KEY, *, *>, key: KEY)

    fun delete(batch: IBatch<KEY, VALUE>)

}
