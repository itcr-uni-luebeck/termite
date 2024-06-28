package de.itcr.termite.index.provider.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.Termite
import de.itcr.termite.index.*
import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.util.ResourceUtil
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
    fhirContext: FhirContext,
    dbPath: Path,
    cfDescriptors: List<ColumnFamilyDescriptor>,
    dbOptions: DBOptions? = null
): FhirIndexStore<ByteArray, Function<ByteArray>, ByteArray, Function<ByteArray>> {

    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<String, ColumnFamilyHandle>

    init {
        private val fhirContext = fhirContext
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.database = RocksDB.open(this.dbOptions, dbPath.toAbsolutePath().toString(), cfDescriptors, columnFamilyHandles)
        this.columnFamilyHandleMap = columnFamilyHandles.associateBy { it.name.decodeToString() }
    }

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

        init { RocksDB.loadLibrary() }

        fun open(fhirContext: FhirContext, dbPath: Path, capabilityStmt: CapabilityStatement): RocksDBIndexStore {
            // Column family definition
            val cfOptions = ColumnFamilyOptions().optimizeUniversalStyleCompaction()
            val cfList = createCfDescriptors(fhirContext, capabilityStmt, cfOptions)
            return RocksDBIndexStore(fhirContext, dbPath, cfList)
        }

        private fun createCfDescriptors(
            fhirContext: FhirContext,
            capabilityStmt: CapabilityStatement,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> {
            val list = capabilityStmt.rest[0].resource
                .map { createCfDescriptorsForFhirType(fhirContext, it, cfOptions) }
                .flatten() as MutableList
            list.add(ColumnFamilyDescriptor("default".toByteArray(Charsets.UTF_8), cfOptions))
            return list
        }

        private fun createCfDescriptorsForFhirType(
            fhirContext: FhirContext,
            restResourceComponent: CapabilityStatement.CapabilityStatementRestResourceComponent,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> {
            val classLoader = Termite::class.java.classLoader
            val packageName = "de/itcr/termite/persistence/${fhirContext.version.version.name.lowercase()}"
            val classes = ResourceUtil.findClassesInPackage(packageName, classLoader)
            classes.filter { it.isSealed && it is FhirIndexPartitions<*, *, *, *, *, *> }
                .map { Pair(it.qualifiedName.split(".").) }
            val type = restResourceComponent.type
            return getIndexPartitionsForType(type).map { ColumnFamilyDescriptor(it.bytes(), cfOptions) }
        }

    }

    override fun put(
        partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>,
        key: ByteArray,
        value: ByteArray
    ) = database.put(columnFamilyHandleMap[partition.indexName()], key, value)

    override fun put(batch: List<Pair<ByteArray, ByteArray>>) {
        val writeBatch = WriteBatch()
        batch.forEach { writeBatch.put(it.first, it.second) }
        database.write(writeOptions, writeBatch)
    }

    override fun seek(
        partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>,
        key: ByteArray
    ): ByteArray = database.get(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>, key: ByteArray) =
        database.delete(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(batch: IBatch<ByteArray, ByteArray>) = processBatch(batch)

    override fun createBatch(): IBatch<ByteArray, ByteArray> = Batch()

    override fun processBatch(batch: IBatch<ByteArray, ByteArray>) = database.write(writeOptions, (batch as Batch).batch)

    inner class Batch: IBatch<ByteArray, ByteArray> {

        val batch = WriteBatch()

        override fun put(
            partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>,
            key: ByteArray, value: ByteArray?
        ) = batch.put(columnFamilyHandleMap[partition.indexName()], key, value)

        override fun <T> put(
            partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray,
            valueSelector: (T) -> ByteArray?
        ) = data.forEach {
            batch.put(columnFamilyHandleMap[partition.indexName()], keySelector(it), valueSelector(it))
        }

    }

    inner class Iterator(
        partition: FhirIndexPartitions<*, *, *, ByteArray, *, *>, prefix: ByteArray?
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

    }

}