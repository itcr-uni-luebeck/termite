package de.itcr.termite.index.provider.r4b.rocksdb

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.Termite
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.persistence.PersistenceException
import de.itcr.termite.index.*
import de.itcr.termite.index.annotation.GenerateSearchPartition
import de.itcr.termite.index.partition.*
import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.model.entity.CodeSystemConceptData
import de.itcr.termite.model.entity.VSConceptData
import de.itcr.termite.util.*
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4b.hapi.fhirpath.FhirPathR4B
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.Enumeration
import org.rocksdb.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path
import javax.annotation.PreDestroy
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation

@Qualifier("RocksDB")
class RocksDBIndexStore(
    fhirContext: FhirContext,
    dbPath: Path,
    cfDescriptors: List<ColumnFamilyDescriptor>,
    searchPartitionMap: Map<KClass<out IBaseResource>, List<IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>>,
    dbOptions: DBOptions? = null
): FhirIndexStore<ByteArray, ByteArray> {

    private val fhirContext: FhirContext
    private val dbOptions: DBOptions
    private val writeOptions: WriteOptions
    private val database: RocksDB
    private val columnFamilyHandleMap: Map<String, ColumnFamilyHandle>
    private val searchPartitionMap: Map<KClass<out IBaseResource>, Map<String, IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>>

    init {
        this.fhirContext = fhirContext
        this.dbOptions = dbOptions ?: DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
        this.writeOptions = WriteOptions()
        val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
        this.database = RocksDB.open(this.dbOptions, dbPath.toAbsolutePath().toString(), cfDescriptors, columnFamilyHandles)
        this.columnFamilyHandleMap = columnFamilyHandles.associateBy { it.name.decodeToString() }
        this.searchPartitionMap = searchPartitionMap.mapValues { it.value.associateBy { it.indexName() } }
    }

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

        init { RocksDB.loadLibrary() }

        fun open(fhirContext: FhirContext, dbPath: Path, dbOptions: DBOptions, properties: ApplicationConfig): RocksDBIndexStore {
            val (cfList, pMap) = createCfDescriptorsAndPartitions(fhirContext, properties)
            return RocksDBIndexStore(fhirContext, dbPath, cfList, pMap, dbOptions)
        }

        private fun createCfDescriptorsAndPartitions(
            fhirContext: FhirContext,
            properties: ApplicationConfig
        ): Pair<List<ColumnFamilyDescriptor>, Map<KClass<out IBaseResource>, List<IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>>> {
            val provider = PartitionProvider
            val searchPartitionMap = provider.compileFhirSearchIndexPartitions(fhirContext, properties).toMutableMap()
            val descriptorList = searchPartitionMap.values.flatten().map { searchPartition ->
                val prefixLength = searchPartition.prefixLength()
                val cfOptions = ColumnFamilyOptions()
                    .optimizeUniversalStyleCompaction()
                    .useFixedLengthPrefixExtractor(prefixLength)
                ColumnFamilyDescriptor(searchPartition.bytes(), cfOptions)
            }.toMutableList()
            val partitionLists = provider.compileFhirOperationIndexPartitions().unzip()
            descriptorList.addAll(partitionLists.first.map { opPartition ->
                val prefixLength = opPartition.prefixLength()
                val cfOptions = ColumnFamilyOptions()
                    .optimizeUniversalStyleCompaction()
                    .useFixedLengthPrefixExtractor(prefixLength)
                ColumnFamilyDescriptor(opPartition.bytes(), cfOptions)
            })
            descriptorList.addAll(partitionLists.second.filterNotNull().map { (type, searchPartition) ->
                val prefixLength = searchPartition.prefixLength()
                val cfOptions = ColumnFamilyOptions()
                    .optimizeUniversalStyleCompaction()
                    .useFixedLengthPrefixExtractor(prefixLength)
                (searchPartitionMap[type]!! as MutableList).add(searchPartition)
                ColumnFamilyDescriptor(searchPartition.bytes(), cfOptions)
            })
            // Default Column Family since it is required by RocksDB
            descriptorList.add(
                ColumnFamilyDescriptor("default".toByteArray(Charsets.UTF_8),
                    ColumnFamilyOptions().optimizeUniversalStyleCompaction())
            )
            return descriptorList to searchPartitionMap
        }

        private fun createCfDescriptorsByClass(
            partitions: List<IFhirIndexPartition<IBaseResource, ByteArray, *, *>>
        ): List<ColumnFamilyDescriptor> {
            return partitions.map { partition ->
                val prefixLength = partition.prefixLength()
                val cfOptions = ColumnFamilyOptions()
                    .optimizeUniversalStyleCompaction()
                    .useFixedLengthPrefixExtractor(prefixLength)
                return@map ColumnFamilyDescriptor(partition.bytes(), cfOptions)
            }
        }

        private fun determineLastPossibleKeyForPrefix(prefix: ByteArray, keyLength: Int): ByteArray {
            val arr = ByteArray(keyLength)
            prefix.copyInto(arr)
            for (i in prefix.size ..< keyLength) arr[i] = 0xFF.toByte()
            return arr
        }

    }

    override fun searchPartitionsByType(type: KClass<out IBaseResource>): Map<String, IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>> =
        if (type in this.searchPartitionMap) this.searchPartitionMap[type]!!
        else throw Exception("No search partitions for FHIR type '${type.simpleName}'")

    override fun searchPartitionByTypeAndName(type : KClass<out IBaseResource>, name: String): IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>? {
        if (type in this.searchPartitionMap) {
            val typePartitions = this.searchPartitionMap[type]!!
            if (name in typePartitions) return typePartitions[name]
            else throw Exception("No search partition '$name' for FHIR type '${type.simpleName}'")
        }
        else throw Exception("No search partitions for FHIR type '${type.simpleName}'")
    }

    override fun putCodeSystem(resource: CodeSystem, concepts: Iterable<CodeSystemConceptData>) {
        val batch = createBatch()
        val id = resource.id.toInt()
        // Search indices
        for (partition in searchPartitionsByType(CodeSystem::class).values) {
            val elements = partition.elementPath()(resource)
            for (element in elements) {
                val key = partition.keyGenerator()(element, id)
                batch.put(partition, key, null)
            }
        }
        // Lookup
        for (concept in concepts) {
            val element = Tuple5(resource.url, concept.code, concept.display, resource.version, id)
            // Lookup by system
            var partition: RocksDBOperationPartition<CodeSystem, Tuple5<String, String, String?, String?, Int>, Long> =
                RocksDBOperationPartition.CODE_SYSTEM_LOOKUP_BY_SYSTEM
            var key = partition.keyGenerator()(element)
            val value = partition.valueGenerator()(concept.id)
            batch.put(partition, key, value)
            // Lookup by code
            partition = RocksDBOperationPartition.CODE_SYSTEM_LOOKUP_BY_CODE
            key = partition.keyGenerator()(element)
            batch.put(partition, key, value)
        }
        processBatch(batch)
    }

    override fun deleteCodeSystem(resource: CodeSystem, concepts: Iterable<CodeSystemConceptData>) {
        val batch = createBatch()
        val id = resource.id.toInt()
        // Search indices
        for (partition in searchPartitionsByType(CodeSystem::class).values) {
            val elements = partition.elementPath()(resource)
            for (element in elements) {
                val key = partition.keyGenerator()(element, id)
                batch.put(partition, key, null)
            }
        }
        // Lookup
        for (concept in concepts) {
            val element = Tuple5(resource.url, concept.code, concept.display, resource.version, id)
            // Lookup by system
            var partition: RocksDBOperationPartition<CodeSystem, Tuple5<String, String, String?, String?, Int>, Long> =
                RocksDBOperationPartition.CODE_SYSTEM_LOOKUP_BY_SYSTEM
            var key = partition.keyGenerator()(element)
            batch.delete(partition, key)
            // Lookup by code
            partition = RocksDBOperationPartition.CODE_SYSTEM_LOOKUP_BY_CODE
            key = partition.keyGenerator()(element)
            batch.delete(partition, key)
        }
        processBatch(batch)
    }

    // TODO: Implement short circuiting if resulting set is empty
    override fun search(parameters: Map<String, List<IBase>>, type: KClass<out IBaseResource>): Set<Int> {
        val resultColl = parameters.map { entry -> entry.value.map { search(entry.key, it, type) } }.flatten()
        return if (resultColl.isNotEmpty()) resultColl.reduce { s1: Set<Int>, s2: Set<Int> -> s1 intersect s2 }
        else emptySet()
    }


    override fun search(name: String, value: IBase, type: KClass<out IBaseResource>): Set<Int> {
        val partitionName = "${type.simpleName}.search.${name}"
        val partition = searchPartitionByTypeAndName(type, partitionName)!!
        val prefix = partition.prefixGenerator()(value)
        createIterator(partition).forEach { println(it.first) }
        val iterator = createIterator(partition, prefix)
        val idSet = mutableSetOf<Int>()
        iterator.forEach {
            val key = it.first
            val id = key.sliceArray(key.size - 4 ..< key.size)
            idSet.add(deserializeInt(id))
        }
        iterator.close()
        return idSet
    }

    override fun codeSystemLookup(
        code: String,
        system: String,
        version: String?
    ): Long {
        TODO("Not yet implemented")
    }

    override fun codeSystemLookup(coding: Coding): Long = codeSystemLookup(coding.code, coding.system, coding.version)

    override fun codeSystemValidateCode(
        url: String,
        code: String,
        version: String?,
        display: String?,
        displayLanguage: String?
    ): Triple<Boolean, String, String> {
        TODO("Not yet implemented")
    }

    override fun codeSystemValidateCode(coding: Coding, displayLanguage: String?): Triple<Boolean, String, String> {
        TODO("Not yet implemented")
    }

    override fun codeSystemValidateCode(
        concept: CodeableConcept,
        displayLanguage: String?
    ): Triple<Boolean, String, String> {
        TODO("Not yet implemented")
    }

    override fun putValueSet(resource: ValueSet, concepts: Iterable<VSConceptData>) {
        val batch = createBatch()
        val id = resource.idPart.toInt()
        // Search indices (exclude special parameters as they are indexed differently)
        searchPartitionsByType(ValueSet::class).values
            .filter { !it.parameter().processing.special }
            .forEach { partition ->
                val elements = partition.elementPath()(resource)
                for (element in elements) {
                    val key = partition.keyGenerator()(element, id)
                    batch.put(partition, key, null)
                }
            }
        // Concepts
        // TODO: Ugly and inflexible if more partitions are added. Rework
        for (concept in concepts) {
            val group = concept.vsContentData
            val t1 = Tuple4(group.system!!, concept.code, group.version, resource.id.toInt())
            var key = RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_CODE.keyGenerator()(t1)
            var value = RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_CODE.valueGenerator()(concept.id!!)
            batch.put(RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_CODE, key, value)
            val t2 = Tuple4(resource.id.toInt(), group.system, concept.code, group.version)
            key = RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_ID.keyGenerator()(t2)
            value = RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_ID.valueGenerator()(concept.id)
            batch.put(RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_ID, key, value)
        }
        processBatch(batch)
    }

    override fun deleteValueSet(resource: ValueSet, concepts: Iterable<VSConceptData>) {
        val batch = createBatch()
        val id = resource.id.toInt()
        // Search indices
        for (partition in searchPartitionsByType(ValueSet::class).values) {
            val elements = partition.elementPath()(resource)
            for (element in elements) {
                val key = partition.keyGenerator()(element, id)
                batch.put(partition, key, null)
            }
        }
        processBatch(batch)
    }

    override fun valueSetValidateCode(vsId: Int, system: String, code: String, version: String?): Long? {
        val partition = RocksDBOperationPartition.VALUE_SET_VALIDATE_CODE_BY_ID
        val t = Tuple4(vsId, code, system, version)
        if (version != null) {
            val value = database.get(columnFamilyHandleMap[partition.indexName()], partition.keyGenerator()(t))
            return if (value != null) partition.valueDestructor()(value) else null
        }
        else {
            // FIXME: This assumes that versions adhere to semantic versioning spec!!!
            val prefix = partition.prefixGenerator()(t)
            val it = createIterator(partition, prefix)
            val value = (it as Iterator).last().second
            return partition.valueDestructor()(value)
        }
    }

    override fun valueSetValidateCode(vsId: Int, coding: Coding): Long? =
        valueSetValidateCode(vsId, coding.system, coding.code, coding.version)

    override fun valueSetValidateCode(vsId: Int, concept: CodeableConcept): Long? {
        var conceptId: Long?
        for (coding in concept.coding) {
            conceptId = valueSetValidateCode(vsId, coding)
            if (conceptId != null) break
        }
        return null
    }

    @PreDestroy
    override fun close() {
        this.columnFamilyHandleMap.values.forEach { it.close() }
        this.database.close()
    }

    override fun isClosed(): Boolean = this.database.isClosed

    override fun createBatch(): IBatch<ByteArray, ByteArray> = Batch()

    override fun processBatch(batch: IBatch<ByteArray, ByteArray>) = database.write(writeOptions, (batch as Batch).batch)

    override fun createIterator(partition: IFhirIndexPartition<*, ByteArray, *, *>): IIterator<ByteArray, ByteArray> =
        Iterator(partition, null)

    override fun createIterator(
        partition: IFhirIndexPartition<*, ByteArray, *, *>,
        prefix: ByteArray
    ): IIterator<ByteArray, ByteArray> = Iterator(partition, prefix)

    inner class Batch: IBatch<ByteArray, ByteArray> {

        val batch = WriteBatch()

        override fun put(
            partition: IFhirIndexPartition<*, ByteArray, *, *>,
            key: ByteArray, value: ByteArray?
        ) = batch.put(columnFamilyHandleMap[partition.indexName()], key, value?: byteArrayOf())

        override fun <T> put(
            partition: IFhirIndexPartition<*, ByteArray, *, *>,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray,
            valueSelector: (T) -> ByteArray?
        ) = data.forEach { put(partition, keySelector(it), valueSelector(it)) }

        override fun delete(partition: IFhirIndexPartition<*, ByteArray, *, *>, key: ByteArray) =
            batch.delete(columnFamilyHandleMap[partition.indexName()], key)

        override fun <T> delete(
            partition: IFhirIndexPartition<*, ByteArray, *, *>,
            data: Iterable<T>,
            keySelector: (T) -> ByteArray
        ) = data.forEach { delete(partition, keySelector(it)) }

    }

    inner class Iterator(
        partition: IFhirIndexPartition<*, ByteArray, *, *>, prefix: ByteArray?
    ): IIterator<ByteArray, ByteArray> {

        private val iterator: RocksIterator

        init {
            val it: RocksIterator
            if (prefix != null) {
                val lastKey = determineLastPossibleKeyForPrefix(prefix, partition.keyLength())
                val readOptions = ReadOptions().setIterateUpperBound(Slice(lastKey))
                it = database.newIterator(columnFamilyHandleMap[partition.indexName()], readOptions)
                it.seek(prefix)
            }
            else {
                it = database.newIterator(columnFamilyHandleMap[partition.indexName()])
                it.seekToFirst()
            }
            iterator = it
        }

        override fun next(): Pair<ByteArray, ByteArray> {
            val entry = Pair(iterator.key(), iterator.value())
            iterator.next()
            return entry
        }

        override fun hasNext(): Boolean = iterator.isValid

        override fun close() = iterator.close()

        fun last(): Pair<ByteArray, ByteArray> {
            iterator.seekToLast()
            return Pair(iterator.key(), iterator.value())
        }

    }

    private object PartitionProvider {

        private val logger = LogManager.getLogger(PartitionProvider::class)
        private val classLoader: ClassLoader = Termite::class.java.classLoader

        fun compileFhirSearchIndexPartitions(
            fhirContext: FhirContext,
            config: ApplicationConfig
        ): Map<KClass<out IBaseResource>, List<IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>> {
            logger.info("Compiling search parameter index partitions for provider RocksDB from API documentation")
            val classes = ResourceUtil.findClassesInPackage(config.api.packageName, classLoader)

            val servletClasses = classes.filter { clazz ->
                (clazz.findAnnotation<Controller>() != null || clazz.findAnnotation<RestController>() != null)
                        && (clazz.findAnnotation<ForResource>() != null)
            }
            return servletClasses.associate { servletClass ->
                val directAnn = servletClass.findAnnotation<ForResource>()!!
                val indirectAnns = servletClass.allSuperclasses.mapNotNull { it.findAnnotation<ForResource>() }
                val allParams = mutableListOf<SearchParameter>()
                allParams.addAll(directAnn.searchParam)
                allParams.addAll(indirectAnns.map { it.searchParam.toList() }.flatten())
                @Suppress("UNCHECKED_CAST")
                compileFhirSearchIndexPartitionsForType(directAnn.type, allParams, fhirContext)
                        as Pair<KClass<IBaseResource>, List<IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>>
            }
        }

        fun compileFhirOperationIndexPartitions(): List<Pair<IFhirOperationIndexPartition<*, *, ByteArray, *, ByteArray>, Pair<KClass<out IBaseResource>, IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>?>> {
            return RocksDBOperationPartition::class.sealedSubclasses.mapNotNull { it.objectInstance }
                .map {
                    val ann = it::class.findAnnotation<GenerateSearchPartition>()
                    var searchPartitionPair: Pair<KClass<out IBaseResource > ,IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>>? = null
                    if (ann != null) {
                        // Compile special search partitions resulting from any operation partition
                        val param = ann.param
                        val (prefixGenerator, keyGenerator) = determineGenerators(param.type, param.processing.targetType)
                        val searchPartition = FhirSearchIndexPartition(
                            it.indexName(),
                            it.prefixLength(),
                            it.keyLength(),
                            { _: Nothing -> emptyList() }, // Will not be used anyway
                            prefixGenerator,
                            keyGenerator,
                            param
                        ) as IFhirSearchIndexPartition<IBaseResource, IBase, ByteArray>
                        searchPartitionPair = Pair(ann.target, searchPartition)
                    }
                    return@map it to searchPartitionPair
                }
        }

        private fun compileFhirSearchIndexPartitionsForType(
            resourceType: String,
            searchParameters: List<SearchParameter>,
            fhirContext: FhirContext
        ): Pair<KClass<out IBaseResource>, List<FhirSearchIndexPartition<out IBaseResource, out IBase, ByteArray>>> {
            val resourceClazz = FhirContext.forR4B().getResourceDefinition(resourceType).implementingClass.kotlin
            val groups = searchParameters.groupBy { param -> param.sameAs }
            // Compile index partitions for self contained definitions
            val partitionMap = (if (groups[""] != null) groups[""]!!.filter { !it.processing.special }.associate { param ->
                val partitionName = "$resourceType.search.${param.name}"
                val type = param.type
                val targetType = param.processing.targetType
                val elementPath = determineElementPath(param.processing.elementPath, targetType, fhirContext)
                val prefixLength = determinePrefixLength(targetType)
                val keyLength = determineKeyLength(targetType)
                val (prefixGenerator, keyGenerator) = determineGenerators(type, targetType)
                val partition = FhirSearchIndexPartition(partitionName, prefixLength, keyLength, elementPath, prefixGenerator, keyGenerator, param)
                return@associate param.name to partition
            } else mutableMapOf()).toMutableMap()
            // Assign existing index partitions to parameters referencing definitions
            groups.filter { it.key != "" }.forEach { (paramName, param) ->
                val partition = partitionMap[paramName]
                if (partition != null) param.forEach { partitionMap["$paramName.${it.name}"] = partition }
                else throw PersistenceException("No defining search parameter '$paramName' exists but is referenced")
            }
            return resourceClazz to partitionMap.values.toList()
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
                DateType::class -> 8
                DateTimeType::class -> 8
                Identifier::class -> 8
                CodeType::class -> 8
                Enumeration::class -> 4
                UriType::class -> 4
                CanonicalType::class -> 4
                InstantType::class -> 8
                Coding::class -> 12
                else -> throw PersistenceException("Cannot find prefix length for target type ${targetType.qualifiedName}")
            }
        }

        private fun determineKeyLength(targetType: KClass<out IBase>): Int {
            return when (targetType) {
                IntegerType::class -> 8
                StringType::class -> 8
                DateType::class -> 12
                DateTimeType::class -> 12
                Identifier::class -> 12
                CodeType::class -> 12
                Enumeration::class -> 8
                UriType::class -> 8
                CanonicalType::class -> 8
                InstantType::class -> 12
                Coding::class -> 12
                else -> throw PersistenceException("Cannot find key length for target type ${targetType.qualifiedName}")
            }
        }

        private fun determineGenerators(
            paramType: String, targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (paramType) {
                "number" -> determineGeneratorsForNumberType()
                "date" -> determineGeneratorsForDateType(targetType)
                "string" -> determineGeneratorsForStringType(targetType)
                "token" -> determineGeneratorsForTokenType(targetType)
                "reference" -> determineGeneratorsForStringType(targetType)
                "uri" -> determineGeneratorsForUriType(targetType)
                else -> throw PersistenceException("Cannot find generators for parameter type '$paramType'")
            }
        }

        private fun determineGeneratorsForNumberType(): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return Pair(
                {v: Any -> serialize((v as IntegerType).value) },
                {v: Any, id: Int -> toBytesInOrder((v as IntegerType).value, id) }
            )
        }

        private fun determineGeneratorsForDateType(
            targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (targetType) {
                DateType::class -> Pair(
                    { v: Any -> serialize((v as DateType).value) },
                    { v: Any, id: Int -> toBytesInOrder((v as DateType).value, id) }
                )
                DateTimeType::class -> Pair(
                    { v: Any -> serialize((v as DateTimeType).value) },
                    { v: Any, id: Int -> toBytesInOrder((v as DateTimeType).value, id) }
                )
                InstantType::class -> Pair(
                    { v: Any -> serialize((v as InstantType).value) },
                    { v: Any, id: Int -> toBytesInOrder((v as InstantType).value, id) }
                )
                else -> throw PersistenceException("Cannot find generators for token target type ${targetType.qualifiedName}")
            }
        }

        private fun determineGeneratorsForStringType(
            targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (targetType) {
                StringType::class -> Pair(
                    { v: Any -> serialize((v as StringType).value.hashCode()) },
                    { v: Any, id: Int -> toBytesInOrder((v as StringType).value, id, useHashCode = true) }
                )
                UriType::class -> Pair(
                    { v: Any -> serialize((v as UriType).value.hashCode()) },
                    { v: Any, id: Int -> toBytesInOrder((v as UriType).value, id, useHashCode = true) }
                )
                else -> throw PersistenceException("Cannot find generators for token target type ${targetType.qualifiedName}")
            }
        }

        private fun determineGeneratorsForTokenType(
            targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (targetType) {
                StringType::class -> Pair(
                    { v: Any -> serialize((v as StringType).value.hashCode()) },
                    { v: Any, id: Int -> toBytesInOrder((v as StringType).value, id, useHashCode = true) }
                )
                Enumeration::class -> Pair(
                    { v: Any -> serialize((v as Enumeration<*>).value.ordinal) },
                    { v: Any, id: Int -> toBytesInOrder((v as Enumeration<*>).value.ordinal, id) }
                )
                Identifier::class -> Pair(
                    { v: Any -> with(v as Identifier) { toBytesInOrder(v.system, v.value, useHashCode = true) } },
                    { v: Any, id: Int -> with(v as Identifier) { toBytesInOrder(v.system, v.value, id, useHashCode = true) } }
                )
                CodeType::class -> Pair(
                    { v: Any -> with(v as CodeType) { toBytesInOrder(v.system, v.code, useHashCode = true) } },
                    { v: Any, id: Int -> with(v as CodeType) { toBytesInOrder(v.system, v.code, id, useHashCode = true) } }
                )
                Coding::class -> Pair(
                    { v: Any -> with(v as Coding) { toBytesInOrder(v.system, v.code, useHashCode = true) } },
                    { v: Any, id: Int -> with(v as Coding) { toBytesInOrder(v.system, v.code, id, useHashCode = true) } }
                )
                else -> throw PersistenceException("Cannot find generators for token target type ${targetType.qualifiedName}")
            }
        }

        private fun determineGeneratorsForUriType(
            targetType: KClass<out IBase>
        ): Pair<(Any) -> ByteArray, (Any, Int) -> ByteArray> {
            return when (targetType)  {
                UriType::class -> Pair(
                    { v: Any -> serialize((v as UriType).value.hashCode()) },
                    { v: Any, id: Int -> toBytesInOrder((v as UriType).value, id, useHashCode = true) }
                )
                CanonicalType::class -> Pair(
                    { v: Any -> serialize((v as CanonicalType).value.hashCode()) },
                    { v: Any, id: Int -> toBytesInOrder((v as CanonicalType).value, id, useHashCode = true) }
                )
                else -> throw PersistenceException("Cannot find generators for token target type ${targetType.qualifiedName}")
            }
        }

    }

}