package de.itcr.termite.index.provider.r4b

import de.itcr.termite.index.BatchSupport
import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.index.IBatch
import de.itcr.termite.index.partition.FhirIndexPartitions
import de.itcr.termite.persistence.r4b.codesystem.CodeSystemIndexPartitions
import de.itcr.termite.persistence.r4b.conceptmap.ConceptMapIndexPartitions
import de.itcr.termite.persistence.r4b.valueset.ValueSetIndexPartitions
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.hl7.fhir.r4b.model.StructureDefinition
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.DBOptions
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import org.springframework.beans.factory.annotation.Qualifier
import java.nio.file.Path
import java.util.Collections
import kotlin.io.path.pathString

@Qualifier("RocksDB")
class RocksDBIndexStore(
    dbPath: Path,
    cfDescriptors: List<ColumnFamilyDescriptor>,
    dbOptions: DBOptions? = null
): FhirIndexStore, BatchSupport {

    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<ByteArray, ColumnFamilyHandle>

    init {
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.database = RocksDB.open(this.dbOptions, dbPath.toAbsolutePath().toString(), cfDescriptors, columnFamilyHandles)
        this.columnFamilyHandleMap = columnFamilyHandles.associateBy { it.name }
    }

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

        init { RocksDB.loadLibrary() }

        fun open(dbPath: Path, capabilityStmt: CapabilityStatement): RocksDBIndexStore {
            // Column family definition
            val cfOptions = ColumnFamilyOptions().optimizeUniversalStyleCompaction()
            val cfList = createCfDescriptors(capabilityStmt, cfOptions)
            println(cfList.joinToString(", ") { it.name.decodeToString() })
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
            println(type)
            return getIndexPartitionsForType(type).map { ColumnFamilyDescriptor(it.bytes(), cfOptions) }
        }

    }

    override fun put(partition: FhirIndexPartitions, key: ByteArray, value: ByteArray) {
        database.put(columnFamilyHandleMap[partition.bytes()], key, value)
    }

    // TODO: Could be faster by avoiding creation of Pair objects in the first place
    override fun put(partition: FhirIndexPartitions, batch: List<Pair<ByteArray, ByteArray>>) {
        val writeBatch = WriteBatch()
        batch.forEach { writeBatch.put(it.first, it.second) }
        database.write(writeOptions, writeBatch)
    }

    override fun <T> put(
        partition: FhirIndexPartitions,
        data: Iterable<T>,
        keySelector: (T) -> ByteArray,
        valueSelector: (T) -> ByteArray
    ) {
        val batch = WriteBatch()
        data.forEach { batch.put(columnFamilyHandleMap[partition.bytes()], keySelector(it), valueSelector(it)) }
        database.write(writeOptions, batch)
    }

    override fun search(partition: FhirIndexPartitions, key: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun delete(partition: FhirIndexPartitions, key: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun delete(partition: FhirIndexPartitions, batch: List<ByteArray>) {
        TODO("Not yet implemented")
    }

    override fun createBatch(): IBatch = Batch()

    override fun processBatch(batch: IBatch) = database.write(writeOptions, (batch as Batch).batch)

    inner class Batch: IBatch {

        val batch = WriteBatch()

        override fun put(partition: FhirIndexPartitions, key: ByteArray, value: ByteArray) =
            batch.put(columnFamilyHandleMap[partition.bytes()], key, value)

        override fun <T> put(
            partition: FhirIndexPartitions,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray,
            valueSelector: (T) -> ByteArray
        ) = data.forEach {
            println(partition.bytes().decodeToString())
            batch.put(columnFamilyHandleMap[partition.bytes()], keySelector(it), valueSelector(it))
        }

    }

}