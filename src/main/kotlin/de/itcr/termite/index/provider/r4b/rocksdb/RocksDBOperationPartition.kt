package de.itcr.termite.index.provider.r4b.rocksdb

import de.itcr.termite.index.partition.FhirOperationIndexPartition
import de.itcr.termite.util.*
import org.hl7.fhir.r4b.model.CodeSystem

sealed class RocksDBOperationPartition<FHIR_MODEL, KEY_ELEMENT, VALUE_ELEMENT>(
    indexName: String,
    prefixLength: Int,
    keyLength: Int,
    prefixGenerator: (KEY_ELEMENT) -> ByteArray,
    keyGenerator: (KEY_ELEMENT) -> ByteArray,
    valueGenerator: (VALUE_ELEMENT) -> ByteArray,
    valueDestructor: (ByteArray) -> VALUE_ELEMENT
): FhirOperationIndexPartition<FHIR_MODEL, KEY_ELEMENT, ByteArray, VALUE_ELEMENT, ByteArray>(
    indexName, prefixLength, keyLength, prefixGenerator, keyGenerator, valueGenerator, valueDestructor
) {

    data object CODE_SYSTEM_LOOKUP_BY_SYSTEM: RocksDBOperationPartition<CodeSystem, Tuple5<String, String, String?, String?, Int>, Long>(
        "CodeSystem.\$lookup#system",
        8,
        20,
        { v: Tuple5<String, String, String?, String?, Int> -> serializeInOrder(v.t1, v.t2) },
        { v: Tuple5<String, String, String?, String?, Int> -> serializeInOrder(v.t1, v.t2, v.t3?: "", v.t4?: "", v.t5) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

    data object CODE_SYSTEM_LOOKUP_BY_CODE: RocksDBOperationPartition<CodeSystem, Tuple5<String, String, String?, String?, Int>, Long>(
        "CodeSystem.\$lookup#code",
        8,
        20,
        { v: Tuple5<String, String, String?, String?, Int> -> serializeInOrder(v.t2, v.t1) },
        { v: Tuple5<String, String, String?, String?, Int> -> serializeInOrder(v.t2, v.t1, v.t3?: "", v.t4?: "", v.t5) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

}