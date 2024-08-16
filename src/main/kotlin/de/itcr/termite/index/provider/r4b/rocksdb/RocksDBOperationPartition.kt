package de.itcr.termite.index.provider.r4b.rocksdb

import de.itcr.termite.index.annotation.GenerateSearchPartition
import de.itcr.termite.index.partition.FhirOperationIndexPartition
import de.itcr.termite.metadata.annotation.ProcessingHint
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.util.*
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.CodeType
import org.hl7.fhir.r4b.model.ValueSet

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

    @GenerateSearchPartition(
        target = CodeSystem::class,
        param = SearchParameter(
            name = "code",
            type = "token",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "",
                special = true
            )
        )
    )
    data object CODE_SYSTEM_VALIDATE_CODE_BY_CODE: RocksDBOperationPartition<CodeSystem, Tuple4<String, String, String?, Int>, Long>(
        "CodeSystem.search.code",
        8,
        20,
        { v: Tuple4<String, String, String?, Int> -> toBytesInOrder(v.t1, v.t2, useHashCode = true) },
        { v: Tuple4<String, String, String?, Int> -> toBytesInOrder(v.t1, v.t2, v.t3?: "", v.t4, useHashCode = true) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

    data object CODE_SYSTEM_VALIDATE_CODE_BY_ID: RocksDBOperationPartition<CodeSystem, Tuple4<Int, String, String, String?>, Long>(
        "CodeSystem.search.code#id",
        12,
        20,
        { v: Tuple4<Int, String, String, String?> -> toBytesInOrder(v.t1, v.t2, v.t3, useHashCode = true) },
        { v: Tuple4<Int, String, String, String?> -> toBytesInOrder(v.t1, v.t2, v.t3, v.t4?: "", useHashCode = true) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

    @GenerateSearchPartition(
        target = ValueSet::class,
        param = SearchParameter(
            name = "code",
            type = "token",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "",
                special = true
            )
        )
    )
    data object VALUE_SET_VALIDATE_CODE_BY_CODE: RocksDBOperationPartition<ValueSet, Tuple4<String, String, String?, Int>, Long>(
        "ValueSet.search.code",
        8,
        20,
        { v: Tuple4<String, String, String?, Int> -> toBytesInOrder(v.t1, v.t2, useHashCode = true) },
        { v: Tuple4<String, String, String?, Int> -> toBytesInOrder(v.t1, v.t2, v.t3?: "", v.t4, useHashCode = true) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

    data object VALUE_SET_VALIDATE_CODE_BY_ID: RocksDBOperationPartition<ValueSet, Tuple4<Int, String, String, String?>, Long>(
        "ValueSet.search.code#id",
        12,
        20,
        { v: Tuple4<Int, String, String, String?> -> toBytesInOrder(v.t1, v.t2, v.t3, useHashCode = true) },
        { v: Tuple4<Int, String, String, String?> -> toBytesInOrder(v.t1, v.t2, v.t3, v.t4?: "", useHashCode = true) },
        { v: Long -> serialize(v) },
        { b: ByteArray -> deserializeLong(b) }
    )

}