package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.serialize
import org.springframework.stereotype.Component

@Component
interface FhirIndexStore<KEY, KEY_GENERATOR: Function<KEY>, VALUE, VALUE_GENERATOR: Function<VALUE>>: BatchSupport<KEY, VALUE> {

    fun put(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>, key: ByteArray, value: ByteArray)

    fun put(batch: List<Pair<ByteArray, ByteArray>>)

    fun seek(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, *>, key: KEY): VALUE

    fun delete(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, *, *>, key: KEY)

    fun delete(batch: IBatch<KEY, VALUE>)

}
