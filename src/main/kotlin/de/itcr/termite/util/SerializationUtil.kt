package de.itcr.termite.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.ByteBuffer
import java.util.Date
import java.io.Serializable

private val versionRegex = ("^(?<major>25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)" +
        "(?:\\.(?<minor>25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d))?" +
        "(?:\\.(?<patch>25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d))?" +
        "(?:[.:,;+\\-|#](?<text>.+))?\$").toRegex()

//private val anyArraySerializer = serializer<Array<*>>()
private val stringArraySerializer = serializer<Array<String>>()

/**
 * Serialize instance of generic type T to ByteArray
 * @param t instance of generic type T
 * @return ByteArray containing serialized object
 */
@JvmName("serializeT")
inline fun <reified T: Serializable> serialize(t: T): ByteArray {
    return when (T::class) {
        Int::class -> serialize(t as Int)
        Long::class -> serialize(t as Long)
        String::class -> serialize(t as String)
        else -> serialize(t)
    }
}

/**
 * Serializes any serializable object (implements the Serializable interface)
 * @param a Any object
 * @return ByteArray containing serialized object
 */
fun serialize(a: Serializable): ByteArray {
    return Json.encodeToString(a).toByteArray(Charsets.UTF_8)
}

//fun serializeAnyArray(anyArr: Array<Any>): ByteArray {
//    return Json.encodeToString(anyArraySerializer, anyArr).toByteArray()
//}

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
    buffer[0] = (i shr 24).toByte()
    buffer[1] = (i shr 16).toByte()
    buffer[2] = (i shr 8).toByte()
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
    buffer[0] = (a shr 56).toByte()
    buffer[1] = (a shr 48).toByte()
    buffer[2] = (a shr 40).toByte()
    buffer[3] = (a shr 32).toByte()
    buffer[4] = (a shr 24).toByte()
    buffer[5] = (a shr 16).toByte()
    buffer[6] = (a shr 8).toByte()
    buffer[7] = a.toByte()
    return buffer
}

// TODO: Check if using ByteBuffer has measurable performance impact instead of manually inserting into ByteArray
fun serializeLongArray(longArr: LongArray): ByteArray {
    val buffer = ByteBuffer.allocate(longArr.size * 8)
    for (l in longArr) buffer.putLong(l)
    return buffer.array()
}

fun serialize(a: Date): ByteArray = serialize(a.time)

fun serialize(a: Enum<*>): ByteArray = serialize(a.ordinal)

/**
 * Attempts to parse version string to byte array of length 4 (bytes, i.e. 32 bits) according to the Semantic Versioning
 * specification (@see <a href="https://semver.org/">SemVer</a>) where the version string shall conform to
 * <major>.<minor>.<patch>[.:-+#~|]<text> and major, minor, and patch should be in range 0 - 255. Additional text at the end of
 * the string is converted into a number according to lexical order and compressed into the range 0 - 255. This part of
 * the string cannot be reliably used to order version strings. If the version string does not match this pattern the
 * method indicates this via returning null.
 */
fun serializeVersion(v: String): ByteArray? {
    val results = versionRegex.matchEntire(v) ?: return null
    val arr = ByteArray(4) { 0x00 }
    if (results.groups["text"] != null) arr[3] = results.groups["text"]!!.value[0].code.toByte()
    if (results.groups["patch"] != null) arr[2] = results.groups["patch"]!!.value.toInt().toByte()
    if (results.groups["minor"] != null) arr[1] = results.groups["minor"]!!.value.toInt().toByte()
    arr[0] = results.groups["major"]!!.value.toInt().toByte()
    return arr
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
        Date::class -> deserializeDate(b) as T
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

//fun deserializeAnyArray(b: ByteArray): Array<*> {
//    return Json.decodeFromString(anyArraySerializer, String(b))
//}

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
    return (b[0].toInt() shl 24) or
            (b[1].toInt() shl 16) or
            (b[2].toInt() shl 8) or
            b[3].toInt()
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
    return (b[0].toLong() shl 56) or
            (b[1].toLong() shl 48) or
            (b[2].toLong() shl 40) or
            (b[3].toLong() shl 32) or
            (b[4].toLong() shl 24) or
            (b[5].toLong() shl 16) or
            (b[6].toLong() shl 8) or
            b[7].toLong()
}

fun deserializeLongArray(b: ByteArray): LongArray {
    val longArr = LongArray(b.size/8)
    for (idx in b.indices) longArr[idx/8] = deserializeLong(b.sliceArray(idx..<idx + 8))
    return longArr
}

fun deserializeDate(b: ByteArray): Date = Date(deserializeLong(b))

inline fun <reified T: Enum<T>> deserializeEnum(b: ByteArray): Enum<T> = enumValues<T>()[deserializeInt(b)]

fun toBytesInOrder(vararg args: String): ByteArray = args.mapToBytes()

// TODO: Check performance
fun toBytesInOrder(vararg args: Serializable, useHashCode: Boolean = false): ByteArray =
    if (useHashCode) hashInOrder(*args) else serializeInOrder(*args)


fun serializeInOrder(vararg args: Serializable): ByteArray {
    val buffer = ArrayList<Byte>(args.size * 4)
    args.forEach { arg ->
        when (arg) {
            is String -> buffer.addAll(serialize(arg).asIterable())
            is Int -> buffer.addAll(serialize(arg).asIterable())
            is Long -> buffer.addAll(serialize(arg).asIterable())
            is Date -> buffer.addAll(serialize(arg).asIterable())
            is Enum<*> -> buffer.addAll(serialize(arg).asIterable())
            else -> buffer.addAll(serialize(arg.hashCode()).asIterable())
        }
    }
    return buffer.toByteArray()
}

fun hashInOrder(vararg args: Serializable): ByteArray {
    val buffer = ArrayList<Byte>(args.size * 4)
    args.forEach { arg ->
        when (arg) {
            is Int -> buffer.addAll(serialize(arg).asIterable())
            is Long -> buffer.addAll(serialize(arg).asIterable())
            is Date -> buffer.addAll(serialize(arg).asIterable())
            is Enum<*> -> buffer.addAll(serialize(arg).asIterable())
            else -> buffer.addAll(serialize(arg.hashCode()).asIterable())
        }
    }
    return buffer.toByteArray()
}