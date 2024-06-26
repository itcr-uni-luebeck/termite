package de.itcr.termite.index.provider.r4b

import de.itcr.termite.index.*
import de.itcr.termite.index.partition.FhirIndexPartitions
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.DBOptions
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import org.springframework.beans.factory.annotation.Qualifier
import java.nio.file.Path

@Qualifier("RocksDB")
class RocksDBIndexStore(
    dbPath: Path,
    cfDescriptors: List<ColumnFamilyDescriptor>,
    dbOptions: DBOptions? = null
): FhirIndexStore<ByteArray, Function<ByteArray>, ByteArray, Function<ByteArray>> {

    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<String, ColumnFamilyHandle>

    init {
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.database = RocksDB.open(this.dbOptions, dbPath.toAbsolutePath().toString(), cfDescriptors, columnFamilyHandles)
        this.columnFamilyHandleMap = columnFamilyHandles.associateBy { it.name.decodeToString() }
    }

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

        init { RocksDB.loadLibrary() }

        fun open(dbPath: Path, capabilityStmt: CapabilityStatement): RocksDBIndexStore {
            // Column family definition
            val cfOptions = ColumnFamilyOptions().optimizeUniversalStyleCompaction()
            val cfList = createCfDescriptors(capabilityStmt, cfOptions)
            return RocksDBIndexStore(dbPath, cfList)
        }

        private fun createCfDescriptors(
            capabilityStmt: CapabilityStatement,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> {
            val list = capabilityStmt.rest[0].resource
                .map { createCfDescriptorsForFhirType(it, cfOptions) }
                .flatten() as MutableList
            list.add(ColumnFamilyDescriptor("default".toByteArray(Charsets.UTF_8), cfOptions))
            return list
        }

        private fun createCfDescriptorsForFhirType(
            restResourceComponent: CapabilityStatement.CapabilityStatementRestResourceComponent,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> {
            val type = restResourceComponent.type
            return getIndexPartitionsForType(type).map { ColumnFamilyDescriptor(it.bytes(), cfOptions) }
        }

    }

    override fun put(
        partition: FhirIndexPartitions<ByteArray, Function<ByteArray>, ByteArray, Function<ByteArray>>,
        key: ByteArray,
        value: ByteArray
    ) = database.put(columnFamilyHandleMap[partition.indexName()], key, value)

    override fun put(batch: List<Pair<ByteArray, ByteArray>>) {
        val writeBatch = WriteBatch()
        batch.forEach { writeBatch.put(it.first, it.second) }
        database.write(writeOptions, writeBatch)
    }

    override fun seek(
        partition: FhirIndexPartitions<ByteArray, Function<ByteArray>, ByteArray, *>,
        key: ByteArray
    ): ByteArray = database.get(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(partition: FhirIndexPartitions<ByteArray, Function<ByteArray>, *, *>, key: ByteArray) =
        database.delete(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(batch: IBatch<ByteArray, ByteArray>) = processBatch(batch)

    override fun createBatch(): IBatch<ByteArray, ByteArray> = Batch()

    override fun processBatch(batch: IBatch<ByteArray, ByteArray>) = database.write(writeOptions, (batch as Batch).batch)

    inner class Batch: IBatch<ByteArray, ByteArray> {

        val batch = WriteBatch()

        override fun put(
            partition: FhirIndexPartitions<ByteArray, *, ByteArray, *>,
            key: ByteArray, value: ByteArray
        ) = batch.put(columnFamilyHandleMap[partition.indexName()], key, value)

        override fun <T> put(
            partition: FhirIndexPartitions<ByteArray, *, ByteArray, *>,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray,
            valueSelector: (T) -> ByteArray
        ) = data.forEach {
            batch.put(columnFamilyHandleMap[partition.indexName()], keySelector(it), valueSelector(it))
        }

    }

    inner class Iterator(
        partition: FhirIndexPartitions<ByteArray, *, ByteArray, *>, prefix: ByteArray?
    ): IIterator<ByteArray, ByteArray> {

        private val iterator: RocksIterator

        init {
            val it = database.newIterator(columnFamilyHandleMap[partition.indexName()])
            if (prefix != null) it.seek(prefix) else it.seekToFirst()
            iterator = it
        }

        override fun next(): Pair<ByteArray, ByteArray> {
            iterator.next()
            return Pair(iterator.key(), iterator.value())
        }

        override fun hasNext(): Boolean = iterator.isValid

        override fun close() = iterator.close()

        override fun key(): ByteArray = iterator.key()

        override fun value(): ByteArray = iterator.value()

    }

}