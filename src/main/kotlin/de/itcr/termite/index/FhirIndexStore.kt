package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.serialize
import org.springframework.stereotype.Component

@Component
interface FhirIndexStore: BatchSupport {

    fun put(partition: FhirIndexPartitions, key: ByteArray, value: ByteArray)

    fun put(partition: FhirIndexPartitions, batch: List<Pair<ByteArray, ByteArray>>)

    fun <T> put(partition: FhirIndexPartitions, data: Iterable<T>, keySelector: (T) -> ByteArray, valueSelector: (T) -> ByteArray)

    fun search(partition: FhirIndexPartitions, key: ByteArray)

    fun delete(partition: FhirIndexPartitions, key: ByteArray)

    fun delete(partition: FhirIndexPartitions, batch: List<ByteArray>)

}

inline fun <reified KEY, reified VALUE> FhirIndexStore.put(partition: FhirIndexPartitions, key: KEY, value: VALUE) =
    put(partition, serialize(key), serialize(value))

inline fun <reified KEY, reified VALUE> FhirIndexStore.put(partition: FhirIndexPartitions, batch: List<Pair<KEY, VALUE>>) =
    put(partition, batch.map { (key, value) -> Pair(serialize(key), serialize(value)) })

inline fun <reified KEY> FhirIndexStore.search(partition: FhirIndexPartitions, key: KEY) =
    search(partition, serialize(key))

inline fun <reified KEY> FhirIndexStore.delete(partition: FhirIndexPartitions, key: KEY) =
    delete(partition, serialize(key))

inline fun <reified  KEY> FhirIndexStore.delete(partition: FhirIndexPartitions, batch: List<KEY>) =
    delete(partition, batch.map { serialize(it) })