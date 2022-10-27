package de.itcr.termite.database.nosql

import org.apache.commons.lang3.SerializationUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.rocksdb.*
import java.io.*
import java.nio.file.Path
import kotlin.Exception

open class KeyValueStore constructor(dbPath: Path, columnFamilyNames: List<String> = listOf()) {

    companion object{
        private fun getBytes(a: Serializable): ByteArray {
            return SerializationUtils.serialize(a)
        }

        private fun getBytes(aList: List<Serializable>): List<ByteArray>{
            return aList.map { a -> getBytes(a)}
        }

        private fun getObject(b: ByteArray): Any {
            return SerializationUtils.deserialize(b)
        }

        private fun getObjects(bList: List<ByteArray>): Any {
            return bList.map { b -> getObject(b) }
        }
    }

    private val logger: Logger = LogManager.getLogger(this::class)

    private val dbPath: String = dbPath.toString()
    private val cfOptions: ColumnFamilyOptions
    private val columnFamilyHandleMap : Map<String, ColumnFamilyHandle>
    private val dbOptions : DBOptions
    private val database: RocksDB

    init{
        logger.debug("Initializing key value storage with column families: ${columnFamilyNames.joinToString { ", " }}")
        RocksDB.loadLibrary()

        this.cfOptions = ColumnFamilyOptions().optimizeUniversalStyleCompaction()
        val cfDescriptors: MutableList<ColumnFamilyDescriptor> = mutableListOf(ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOptions))
        cfDescriptors.addAll(columnFamilyNames.map { columnFamilyName ->
            ColumnFamilyDescriptor(columnFamilyName.toByteArray(), cfOptions)
        })
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.dbOptions = DBOptions()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true)
        this.database = RocksDB.open(this.dbOptions, this.dbPath, cfDescriptors, columnFamilyHandles)

        this.columnFamilyHandleMap = columnFamilyHandles.associateBy {
                columnFamilyHandle -> String(columnFamilyHandle.descriptor.name, Charsets.UTF_8)
        }
    }

    fun put(key: Serializable, value: Serializable, columnFamily: String){
        try{
            database.put(columnFamilyHandleMap[columnFamily], getBytes(key), getBytes(value))
        }
        catch (e: Exception){
            throw Exception("Put operation failed", e)
        }
    }

    fun put(key: Serializable, value: Serializable){
        put(key, value, "default")
    }

    fun get(key: Serializable, columnFamily: String): ByteArray?{
        try {
            return database.get(columnFamilyHandleMap[columnFamily], getBytes(key))
        }
        catch (e: Exception){
            throw Exception("Get operation failed", e)
        }
    }

    @JvmName("getT")
    internal inline fun <reified T> get(key: Serializable, columnFamily: String): T?{
        return get(key, columnFamily)?.let { getObject(it) } as T?
    }

    fun get(key: Serializable): ByteArray?{
        return get(key, "default")
    }

    @JvmName("getT")
    internal inline fun <reified T> get(key: Serializable): T?{
        return get(key)?.let { getObject(it) } as T?
    }

    fun delete(key: Serializable, columnFamily: String){
        try{
            database.delete(columnFamilyHandleMap[columnFamily], getBytes(key))
        }
        catch (e: RocksDBException){
            throw Exception("Delete failed", e)
        }
    }

    fun delete(key: Serializable){
        delete(key, "default")
    }

    fun contains(key: Serializable, columnFamilyName: String): Boolean{
        return get(key, columnFamilyName) != null
    }

    fun contains(key: Serializable): Boolean{
        return contains(key, "default")
    }

    fun createColumnFamily(name: ByteArray){
        database.createColumnFamily(ColumnFamilyDescriptor(name, cfOptions))
    }

    fun createColumnFamily(name: String){
        createColumnFamily(getBytes(name))
    }

    fun createColumnFamilies(names: List<String>){
        database.createColumnFamilies(cfOptions, getBytes(names))
    }

    fun close(){
        logger.debug("Closing KeyValueStore instance")
        //NOTE: Closing operations have to be executed in the following order
        this.columnFamilyHandleMap.values.forEach { columnFamilyHandle ->  columnFamilyHandle.close() }
        this.database.close()
        this.dbOptions.close()
        this.cfOptions.close()
    }

}