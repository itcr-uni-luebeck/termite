package de.itcr.termite.index.provider.r4b

import de.itcr.termite.index.FhirIndexStore
import de.itcr.termite.index.partition.FhirIndexPartitions
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CapabilityStatement
import org.hl7.fhir.r4b.model.StructureDefinition
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.DBOptions
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.WriteBatch
import org.rocksdb.WriteBatchWithIndex
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
): FhirIndexStore {

    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<ByteArray, ColumnFamilyHandle>

    init {
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = listOf<ColumnFamilyHandle>()
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
            return RocksDBIndexStore(dbPath, cfList)
        }

        private fun createCfDescriptors(
            capabilityStmt: CapabilityStatement,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> =
            capabilityStmt.rest[0].resource.map { createCfDescriptorsForFhirType(it, cfOptions) }.flatten()

        private fun createCfDescriptorsForFhirType(
            restResourceComponent: CapabilityStatement.CapabilityStatementRestResourceComponent,
            cfOptions: ColumnFamilyOptions
        ): List<ColumnFamilyDescriptor> {
            val cfList = mutableListOf<ColumnFamilyDescriptor>()
            val type = restResourceComponent.type
            // Add column families for CRUD
            cfList.add(ColumnFamilyDescriptor("$type.crud".toByteArray(), cfOptions))
            // Add column families for supported search parameters
            restResourceComponent.searchParam.forEach { param ->
                cfList.add(ColumnFamilyDescriptor("$type.search.$param".toByteArray(), cfOptions))
            }
            // Delegate creation of type specific indices
            cfList.addAll(when (type) {
                "CodeSystem" -> createCfDescriptorsForCodeSystem(cfOptions)
                "ValueSet" -> createCfDescriptorsForValueSet(cfOptions)
                "ConceptMap" -> createCfDescriptorsForConceptMap(cfOptions)
                else -> emptyList()
            })
            return cfList
        }

        private fun createCfDescriptorsForCodeSystem(cfOptions: ColumnFamilyOptions): List<ColumnFamilyDescriptor> {
            return listOf(
                // CodeSystem-validate-code and CodeSystem-lookup
                ColumnFamilyDescriptor("CodeSystem.lookup".toByteArray(), cfOptions),
            )
        }

        private fun createCfDescriptorsForValueSet(cfOptions: ColumnFamilyOptions): List<ColumnFamilyDescriptor> {
            return listOf(
                // ValueSet-validate-code
                ColumnFamilyDescriptor("ValueSet.validate-code".toByteArray(), cfOptions)
            )
        }

        private fun createCfDescriptorsForConceptMap(cfOptions: ColumnFamilyOptions): List<ColumnFamilyDescriptor> {
            return listOf(
                // ConceptMap-translate
                ColumnFamilyDescriptor("ConceptMap.translate".toByteArray(), cfOptions)
            )
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

    override fun search(partition: FhirIndexPartitions, key: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun delete(partition: FhirIndexPartitions, key: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun delete(partition: FhirIndexPartitions, batch: List<ByteArray>) {
        TODO("Not yet implemented")
    }

}