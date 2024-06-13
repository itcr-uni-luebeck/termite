package de.itcr.termite.index

import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.serialize

interface FhirIndexStore {

    fun put(partition: FhirIndexPartitions, key: ByteArray, value: ByteArray)

    fun search(partition: FhirIndexPartitions, key: ByteArray)

    fun delete(partition: FhirIndexPartitions, key: ByteArray)

}

inline fun <reified KEY, reified VALUE> FhirIndexStore.put(partition: FhirIndexPartitions, key: KEY, value: VALUE) =
    put(partition, serialize(key), serialize(value))

inline fun <reified KEY> FhirIndexStore.search(partition: FhirIndexPartitions, key: KEY) =
    search(partition, serialize(key))

inline fun <reified KEY> FhirIndexStore.delete(partition: FhirIndexPartitions, key: KEY) =
    delete(partition, serialize(key))