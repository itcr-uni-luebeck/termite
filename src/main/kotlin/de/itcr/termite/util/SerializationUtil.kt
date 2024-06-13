package de.itcr.termite.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.ByteBuffer

private val anyArraySerializer = serializer<Array<Any>>()
private val stringArraySerializer = serializer<Array<String>>()

/**
 * Serialize instance of generic type T to ByteArray
 * @param t instance of generic type T
 * @return ByteArray containing serialized object
 */
@JvmName("serializeT")
inline fun <reified T> serialize(t: T): ByteArray {
    return when (T::class) {
        Int::class -> serialize(t as Int)
        Long::class -> serialize(t as Long)
        String::class -> serialize(t as String)
        else -> serialize(t as Any)
    }
}

/**
 * Serializes any serializable object (implements the Serializable interface)
 * @param a Any object
 * @return ByteArray containing serialized object
 */
fun serialize(a: Any): ByteArray {
    return Json.encodeToString(a).toByteArray(Charsets.UTF_8)
}

fun serializeAnyArray(anyArr: Array<Any>): ByteArray {
    return Json.encodeToString(anyArraySerializer, anyArr).toByteArray()
}

/**
 * Serializes String objects
 * @param a String object
 * @return ByteArray containing the String objects byte array encoded using UTF-8 charset
 */
fun serialize(a: String): ByteArray {
    return a.toByteArray(Charsets.UTF_8)
}

fun serializeStringArray(stringArr: Array<String>): ByteArray {
    return Json.encodeToString(stringArraySerializer, stringArr).toByteArray()
}

/**
 * Serializes integers
 * @param i integer
 * @return ByteArray containing serialized integer in big-endian format
 */
fun serialize(i: Int): ByteArray {
    val buffer = ByteArray(4)
    buffer[0] = (i shl 24).toByte()
    buffer[1] = (i shl 16).toByte()
    buffer[2] = (i shl 8).toByte()
    buffer[3] = i.toByte()
    return buffer
}

// TODO: Check if using ByteBuffer has measurable performance impact instead of manually inserting into ByteArray
fun serializeIntArray(intArr: IntArray): ByteArray {
    val buffer = ByteBuffer.allocate(intArr.size * 4)
    for (i in intArr) buffer.putInt(i)
    return buffer.array()
}

/**
 * Serializes longs
 * @return ByteArray containing serialized long in big-endian format
 */
fun serialize(a: Long): ByteArray {
    val buffer = ByteArray(8)
    buffer[0] = (a shl 56).toByte()
    buffer[1] = (a shl 48).toByte()
    buffer[2] = (a shl 40).toByte()
    buffer[3] = (a shl 32).toByte()
    buffer[4] = (a shl 24).toByte()
    buffer[5] = (a shl 16).toByte()
    buffer[6] = (a shl 8).toByte()
    buffer[7] = a.toByte()
    return buffer
}

// TODO: Check if using ByteBuffer has measurable performance impact instead of manually inserting into ByteArray
fun serializeLongArray(longArr: LongArray): ByteArray {
    val buffer = ByteBuffer.allocate(longArr.size * 8)
    for (l in longArr) buffer.putLong(l)
    return buffer.array()
}

/**
 * Deserializes ByteArray to object of specified type T
 * @param b ByteArray
 * @return object of type T deserialized from the byte array
 */
@JvmName("deserializeT")
inline fun <reified T> deserialize(b: ByteArray): T {
    return when (T::class) {
        Int::class -> deserializeInt(b) as T
        Long::class -> deserializeLong(b) as T
        String::class -> deserializeString(b) as T
        else -> deserialize(b) as T
    }
}

/**
 * Deserializes ByteArray to Any
 * @param b ByteArray
 * @return Any object deserialized from the byte array
 */
fun deserialize(b: ByteArray): Any {
    return Json.decodeFromString(String(b))
}

fun deserializeAnyArray(b: ByteArray): Array<Any> {
    return Json.decodeFromString(anyArraySerializer, String(b))
}

/**
 * Deserializes ByteArray to String object
 * @param b ByteArray
 * @return String object deserialized from the byte array
 */
fun deserializeString(b: ByteArray): String {
    return String(b)
}

fun deserializeStringArray(b: ByteArray): Array<String> {
    return Json.decodeFromString(stringArraySerializer, String(b))
}

/**
 * Deserializes ByteArray to integer
 * NOTE: Only the first 4 bytes are used during deserialization
 * @param b ByteArray
 * @return integer deserialized from the byte array
 */
fun deserializeInt(b: ByteArray): Int {
    return (b[0].toInt() and 0xFF shl 24) or
            (b[1].toInt() and 0xFF shl 16) or
            (b[2].toInt() and 0xFF shl 8) or
            (b[3].toInt() and 0xFF)
}

fun deserializeIntArray(b: ByteArray): IntArray {
    val intArr = IntArray(b.size/4)
    for (idx in b.indices) intArr[idx/4] = deserializeInt(b.sliceArray(idx..<idx + 4))
    return intArr
}

/**
 * Deserializes ByteArray to long
 * NOTE: Only the first 8 bytes are used during deserialization
 * @param b ByteArray
 * @return long deserialized from the byte array
 */
fun deserializeLong(b: ByteArray): Long {
    return (b[0].toLong() and 0xFF shl 56) or
            (b[1].toLong() and 0xFF shl 48) or
            (b[2].toLong() and 0xFF shl 40) or
            (b[3].toLong() and 0xFF shl 32) or
            (b[4].toLong() and 0xFF shl 24) or
            (b[5].toLong() and 0xFF shl 16) or
            (b[6].toLong() and 0xFF shl 8) or
            (b[7].toLong() and 0xFF)
}

fun deserializeLongArray(b: ByteArray): LongArray {
    val longArr = LongArray(b.size/8)
    for (idx in b.indices) longArr[idx/8] = deserializeLong(b.sliceArray(idx..<idx + 8))
    return longArr
}