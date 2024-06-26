package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.serialize
import org.springframework.stereotype.Component

@Component
interface FhirIndexStore<KEY, KEY_GENERATOR: Function<KEY>, VALUE, VALUE_GENERATOR: Function<VALUE>>: BatchSupport<KEY, VALUE> {

    fun put(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>, key: ByteArray, value: ByteArray)

    fun put(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>, batch: List<Pair<ByteArray, ByteArray>>)

    fun <T> put(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>, data: Iterable<T>, keySelector: (T) -> ByteArray, valueSelector: (T) -> ByteArray)

    fun seek(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, *>, key: ByteArray): VALUE

    fun delete(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, *, *>, key: ByteArray)

    fun delete(partition: FhirIndexPartitions<KEY, KEY_GENERATOR, *, *>, batch: List<ByteArray>)

}

inline fun <reified KEY, KEY_GENERATOR: Function<KEY>, reified VALUE, VALUE_GENERATOR: Function<VALUE>> FhirIndexStore<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>.put(
    partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>,
    key: KEY,
    value: VALUE
) = put(partition, serialize(key), serialize(value))

inline fun <reified KEY, KEY_GENERATOR: Function<KEY>, reified VALUE, VALUE_GENERATOR: Function<VALUE>> FhirIndexStore<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>.put(
    partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, VALUE_GENERATOR>,
    key: KEY,
    value: VALUE,
    batch: List<Pair<KEY, VALUE>>
) = put(partition, batch.map { (key, value) -> Pair(serialize(key), serialize(value)) })

inline fun <reified KEY, KEY_GENERATOR: Function<KEY>, VALUE> FhirIndexStore<KEY, KEY_GENERATOR, VALUE, *>.seek(
    partition: FhirIndexPartitions<KEY, KEY_GENERATOR, VALUE, *>,
    key: KEY
) = seek(partition, serialize(key))

inline fun <reified KEY, KEY_GENERATOR: Function<KEY>> FhirIndexStore<KEY, KEY_GENERATOR, *, *>.delete(
    partition: FhirIndexPartitions<KEY, KEY_GENERATOR, *, *>,
    key: KEY
) = delete(partition, serialize(key))

inline fun <reified KEY, KEY_GENERATOR: Function<KEY>> FhirIndexStore<KEY, KEY_GENERATOR, *, *>.delete(
    partition: FhirIndexPartitions<KEY, KEY_GENERATOR, *, *>,
    batch: List<KEY>
) = delete(partition, batch.map { serialize(it) })