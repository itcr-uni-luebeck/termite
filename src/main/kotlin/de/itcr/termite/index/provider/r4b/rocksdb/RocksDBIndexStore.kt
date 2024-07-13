package de.itcr.termite.index.provider.r4b.rocksdb

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.Termite
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.*
import de.itcr.termite.index.partition.FhirSearchIndexPartition
import de.itcr.termite.index.partition.IFhirIndexPartition
import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.util.ResourceUtil
import de.itcr.termite.util.serialize
import de.itcr.termite.util.serializeInOrder
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.hapi.fhirpath.FhirPathR4B
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.Enumeration
import org.hl7.fhir.r4b.model.Identifier
import org.hl7.fhir.r4b.model.IntegerType
import org.hl7.fhir.r4b.model.StringType
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.DBOptions
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

@Qualifier("RocksDB")
class RocksDBIndexStore(
    fhirContext: FhirContext,
    dbPath: Path,
    cfDescriptors: List<ColumnFamilyDescriptor>,
    dbOptions: DBOptions? = null
): FhirIndexStore<ByteArray, ByteArray> {

    private val fhirContext: FhirContext
    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<String, ColumnFamilyHandle>

    init {
        this.fhirContext = fhirContext
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.database = RocksDB.open(this.dbOptions, dbPath.toAbsolutePath().toString(), cfDescriptors, columnFamilyHandles)
        this.columnFamilyHandleMap = columnFamilyHandles.associateBy { it.name.decodeToString() }
    }

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

        init { RocksDB.loadLibrary() }

        fun open(fhirContext: FhirContext, dbPath: Path, dbOptions: DBOptions, properties: ApplicationConfig): RocksDBIndexStore {
            val cfList = createCfDescriptors(fhirContext, properties)
            return RocksDBIndexStore(fhirContext, dbPath, cfList, dbOptions)
        }

        private fun createCfDescriptors(
            fhirContext: FhirContext,
            properties: ApplicationConfig
        ): List<ColumnFamilyDescriptor> {
            val provider = PartitionProvider
            val list = provider.compileFhirSearchIndexPartitions(fhirContext, properties)
                .map {
                    val partitions = it.value
                    return@map partitions.map { partition ->
                        val prefixLength = partition.prefixLength()
                        val cfOptions = ColumnFamilyOptions()
                            .optimizeUniversalStyleCompaction()
                            .useFixedLengthPrefixExtractor(prefixLength)
                        ColumnFamilyDescriptor(partition.bytes(), cfOptions)
                    }
                }
                .flatten()
            return list
        }

        private fun createCfDescriptorsByClass(
            partitions: List<IFhirIndexPartition<*, *, Int, ByteArray, (Any) -> ByteArray, (Any, Int) -> ByteArray>>
        ): List<ColumnFamilyDescriptor> {
            return partitions.map { partition ->
                val prefixLength = partition.prefixLength()
                val cfOptions = ColumnFamilyOptions()
                    .optimizeUniversalStyleCompaction()
                    .useFixedLengthPrefixExtractor(prefixLength)
                return@map ColumnFamilyDescriptor(partition.bytes(), cfOptions)
            }
        }

    }

    override fun put(
        partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>,
        key: ByteArray,
        value: ByteArray
    ) = database.put(columnFamilyHandleMap[partition.indexName()], key, value)

    override fun put(batch: IBatch<ByteArray, ByteArray>) = processBatch(batch)

    override fun seek(
        partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>,
        key: ByteArray
    ): ByteArray = database.get(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>, key: ByteArray) =
        database.delete(columnFamilyHandleMap[partition.indexName()], key)

    override fun delete(batch: IBatch<ByteArray, ByteArray>) = processBatch(batch)

    override fun createBatch(): IBatch<ByteArray, ByteArray> = Batch()

    override fun processBatch(batch: IBatch<ByteArray, ByteArray>) = database.write(writeOptions, (batch as Batch).batch)

    inner class Batch: IBatch<ByteArray, ByteArray> {

        val batch = WriteBatch()

        override fun put(
            partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>,
            key: ByteArray, value: ByteArray?
        ) = batch.put(columnFamilyHandleMap[partition.indexName()], key, value)

        override fun <T> put(
            partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray,
            valueSelector: (T) -> ByteArray?
        ) = data.forEach {
            batch.put(columnFamilyHandleMap[partition.indexName()], keySelector(it), valueSelector(it))
        }

    }

    inner class Iterator(
        partition: IFhirIndexPartition<*, *, *, ByteArray, *, *>, prefix: ByteArray?
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

    private object PartitionProvider {

        private val logger = LogManager.getLogger(PartitionProvider::class)
        private val classLoader: ClassLoader = Termite::class.java.classLoader

        fun compileFhirSearchIndexPartitions(
            fhirContext: FhirContext,
            config: ApplicationConfig
        ): Map<KClass<*>, List<IFhirIndexPartition<*, *, Int, ByteArray, (Any) -> ByteArray, (Any, Int) -> ByteArray>>> {
            logger.info("Compiling search parameter index partitions for provider RocksDB from API documentation")
            val classes = ResourceUtil.findClassesInPackage(config.api.packageName, classLoader)

            val servletClasses = classes.filter { clazz ->
                clazz.findAnnotation<Controller>() != null || clazz.findAnnotation<RestController>() != null
            }
            return servletClasses.mapNotNull { servletClass -> servletClass.findAnnotation<ForResource>() }
                .associate { forResource ->
                    @Suppress("UNCHECKED_CAST")
                    compileFhirSearchIndexPartitionsForType(forResource.type, forResource.searchParam, fhirContext)
                            as Pair<KClass<*>, List<IFhirIndexPartition<*, *, Int, ByteArray, (Any) -> ByteArray, (Any, Int) -> ByteArray>>>
                }
        }

        private fun compileFhirSearchIndexPartitionsForType(
            resourceType: String,
            searchParameters: Array<SearchParameter>,
            fhirContext: FhirContext
        ): Pair<KClass<*>, List<FhirSearchIndexPartition<out IBaseResource, out IBase, ByteArray>>> {
            val resourceClazz = FhirContext.forR4B().getResourceDefinition(resourceType).implementingClass.kotlin
            val partitions = searchParameters.map { param ->
                val partitionName = "$resourceType.search.${param.name}"
                val type = param.type
                val targetType = param.processing.targetType
                val elementPath = determineElementPath(param.processing.elementPath, targetType, fhirContext)
                val prefixLength = determinePrefixLength(targetType)
                val (prefixGenerator, keyGenerator) = determineGenerators(type, targetType)
                val partition = FhirSearchIndexPartition(partitionName, prefixLength, elementPath, prefixGenerator, keyGenerator)
                return@map partition
            }
            return resourceClazz to partitions
        }

        // TODO: Check whether many instantiations of FhirPath classes causes issues
        private fun determineElementPath(
            elementPath: String,
            returnType: KClass<out IBase>,
            fhirContext: FhirContext
        ): (IBaseResource) -> Iterable<IBase> {
            val evaluator = FhirPathR4B(fhirContext)
            val parsedExpr = evaluator.parse(elementPath)
            return { r: IBaseResource -> evaluator.evaluate(r, parsedExpr, returnType.java) }
        }

        private fun determinePrefixLength(targetType: KClass<out IBase>): Int {
            return when (targetType) {
                IntegerType::class -> 4
                StringType::class -> 4
                Date::class -> 8
                Identifier::class -> 8
                Coding::class -> 8
                Enumeration::class -> 4
                else -> throw PersistenceException("Cannot find prefix length for target type ${targetType.qualifiedName}")
            }
        }

        private fun determineGenerators(
            paramType: String, targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (paramType) {
                "number" -> determineGeneratorsForNumberType()
                "date" -> determineGeneratorsForDateType()
                "string" -> determineGeneratorsForStringType()
                "token" -> determineGeneratorsForTokenType(targetType)
                "reference" -> determineGeneratorsForStringType()
                "uri" -> determineGeneratorsForStringType()
                else -> throw PersistenceException("Cannot find generators for parameter type '$paramType'")
            }
        }

        private fun determineGeneratorsForNumberType(): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return Pair(
                {v: Any -> serialize((v as IntegerType).value) },
                {v: Any, id: Int -> serializeInOrder((v as IntegerType).value, id) }
            )
        }

        private fun determineGeneratorsForDateType(): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return Pair({ v: Any -> serialize(v as Date) }, { v: Any, id: Int -> serializeInOrder(v as Date, id) })
        }

        private fun determineGeneratorsForStringType(): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return Pair({ v: Any -> serialize((v as StringType).value) }, { v: Any, id: Int -> serializeInOrder((v as StringType).value, id) })
        }

        private fun determineGeneratorsForTokenType(
            targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (targetType) {
                StringType::class -> Pair({ v: Any -> serialize((v as StringType).value) }, { v: Any, id: Int -> serializeInOrder((v as StringType).value, id) })
                // Instances of Enumeration class are also 'instances' of Enum interface
                Enum::class -> Pair({ v: Any -> serialize(v as Enum<*>) }, { v: Any, id: Int -> serializeInOrder(v as Enum<*>, id) })
                Identifier::class -> Pair(
                    { v: Any -> with(v as Identifier) { serializeInOrder(v.system, v.value) } },
                    { v: Any, id: Int -> with(v as Identifier) { serializeInOrder(v.system, v.value, id) } }
                )
                Coding::class -> Pair(
                    { v: Any -> with(v as Coding) { serializeInOrder(v.system, v.code) } },
                    { v: Any, id: Int -> with(v as Coding) { serializeInOrder(v.system, v.code, id) } }
                )
                else -> throw PersistenceException("Cannot find generators for token target type ${targetType.qualifiedName}")
            }
        }

    }

}