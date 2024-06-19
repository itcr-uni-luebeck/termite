package de.itcr.termite.database.sql

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.exception.*
import de.itcr.termite.util.newBackboneElementParser
import de.itcr.termite.util.r4b.JsonUtil
import org.apache.logging.log4j.LogManager
import org.fhir.ucum.Concept
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.ConceptMap.ConceptMapEquivalence
import org.hl7.fhir.r4b.model.Enumeration
import org.springframework.data.mapping.toDotPath
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.reflect.KClass

/**
 * Implementation of the ValueSetStorage interface using a relational database
 * @see TerminologyStorage
 */
class TerminologyDatabase constructor(private val ctx: FhirContext, url: String): Database(url), TerminologyStorage {

    companion object {
        private val logger = LogManager.getLogger()
    }

    private enum class FhirType constructor(val dbValue: String) {

        CODE_SYSTEM("CS"), VALUE_SET("VS"), CONCEPT_MAP("CM")

    }

    private val jsonParser = ctx.newJsonParser().setDontEncodeElements(setOf(
        "CodeSystem.concept",
        "ValueSet.compose",
        "ValueSet.expansion",
        "ConceptMap.group"
    ))
    private val backboneElementParser = ctx.newBackboneElementParser()

    init {
        //Create tables and indices
        logger.debug("Creating ValueSets table ...")
        super.execute("CREATE TABLE IF NOT EXISTS ValueSets (VS_ID BIGSERIAL PRIMARY KEY, URL TEXT NOT NULL, " +
                "VERSION TEXT, VERSION_ID INTEGER NOT NULL, LAST_UPDATED TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "METADATA JSONB NOT NULL)")
        logger.debug("Creating CodeSystems table ...")
        super.execute("CREATE TABLE IF NOT EXISTS CodeSystems (CS_ID BIGSERIAL PRIMARY KEY, URL TEXT NOT NULL, " +
                "VERSION TEXT, VERSION_ID INTEGER NOT NULL, LAST_UPDATED TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "METADATA JSONB NOT NULL)")
        logger.debug("Creating ConceptMap table ...")
        super.execute("CREATE TABLE IF NOT EXISTS ConceptMaps (CM_ID BIGSERIAL PRIMARY KEY, URL TEXT NOT NULL, " +
                "VERSION TEXT, VERSION_ID INTEGER NOT NULL, LAST_UPDATED TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "METADATA JSONB NOT NULL)")
        logger.debug("Creating OperationDefinition table ...")
        super.execute("CREATE TABLE IF NOT EXISTS OperationDefinitions (OD_ID BIGSERIAL PRIMARY KEY, " +
                "RESOURCE TEXT NOT NULL)")
        logger.debug("Creating Membership table ...")
        super.execute("DO $$ BEGIN CREATE TYPE FHIR_TERM_TYPE AS ENUM ('CS', 'VS', 'CM'); " +
                "EXCEPTION WHEN duplicate_object THEN null; END $$;")
        super.execute("CREATE TABLE IF NOT EXISTS Membership (ID BIGINT, TYPE FHIR_TERM_TYPE, SYSTEM TEXT NOT NULL, " +
                "CODE TEXT NOT NULL, DISPLAY TEXT NOT NULL, UNIQUE(ID, TYPE, SYSTEM, CODE, DISPLAY))")
        logger.debug("Creating Membership index ...")
        super.execute("CREATE INDEX IF NOT EXISTS Idx_Membership ON Membership(ID, TYPE, SYSTEM, CODE, DISPLAY)")
        logger.debug("Creating Translation table ...")
        super.execute("CREATE TABLE IF NOT EXISTS Translation (CM_ID BIGINT REFERENCES ConceptMaps, " +
                "CODE TEXT NOT NULL, DISPLAY TEXT NOT NULL, SOURCE_URL TEXT NOT NULL, SOURCE_VERSION TEXT, " +
                "TARGET_URL TEXT NOT NULL, TARGET_VERSION TEXT, TARGET_CODE TEXT NOT NULL, TARGET_DISPLAY TEXT, " +
                "EQUIVALENCE TEXT NOT NULL, COMMENT TEXT, DEPENDS_ON JSONB)")
        logger.debug("Creating Translation index ...")
        super.execute("CREATE INDEX IF NOT EXISTS Idx_Translation ON Translation(CM_ID, CODE, SOURCE_URL, CODE, " +
                "SOURCE_VERSION, TARGET_URL, TARGET_VERSION, TARGET_CODE, EQUIVALENCE)")
    }

    override fun addValueSet(valueSet: ValueSet): Triple<ValueSet, Int, Timestamp> {
        try{
            //Retrieve ValueSet information and create database entry if not already present
            val url: String = valueSet.url
            val version: String? = valueSet.version
            logger.debug("Adding ValueSet with URL $url and version $version to database")
            val vsIds = insertValueSets(listOf(valueSet))

            if(vsIds.isNotEmpty()){
                //Create database entries for codes if not already present and retrieve keys
                val codes = mutableListOf<Triple<String, String, String>>()
                valueSet.compose.include.forEach { include ->
                    val system = include.system
                    codes.addAll(include.concept.map { concept -> Triple(system, concept.code, concept.display) })
                }
                codes.addAll(valueSet.expansion.contains.distinct().map { concept ->
                    Triple(concept.system, concept.code, concept.display)
                })
                insertCodes(vsIds[0], FhirType.VALUE_SET, codes)
            }
            else{
                logger.warn("ValueSet with URL $url and version $version already exists in database")
            }

            val rs = super.executeQuery("SELECT VERSION_ID, LAST_UPDATED FROM ValueSets WHERE VS_ID = ${vsIds[0]}")
            if(rs.next()){
                val info = Triple(buildValueSet(vsIds[0].toString(), true), rs.getInt(1), rs.getTimestamp(2))
                logger.debug("Finished adding ValueSet with URL $url and version $version to database")
                return info
            }
            else{
                throw ValueSetException("Couldn't retrieve VERSION_ID and LAST_UPDATED for ValueSet with URL $url and version $version from database")
            }
        }
        catch (e: ValueSetException){
            throw e
        }
        catch (e: Exception){
            val message = "Error during addition of ValueSet instance to database"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    /**
     * Inserts value sets in form of entries into the ValueSets table into the database
     *
     * @param entries holds ValueSet instances to be added
     *
     * @return list containing the generated VS_IDs in the order in which the ValueSet instances themselves where
     *         provided
     */
    private fun insertValueSets(entries: List<ValueSet>): List<Int>{
        logger.debug("Inserting ${entries.size} ${if(entries.size == 1) "ValueSet" else "ValueSets"} ...")
        val sql = "INSERT INTO ValueSets (URL, VERSION, VERSION_ID, METADATA) VALUES (?, ?, 0, to_jsonb(?::json)) ON CONFLICT DO NOTHING RETURNING VS_ID"
        val transformation = {stmt: PreparedStatement, valueSets: List<ValueSet> -> valueSets.forEach { vs ->
            stmt.setString(1, vs.url)
            stmt.setString(2, vs.version)
            stmt.setString(3, jsonParser.encodeToString(vs))
            stmt.addBatch()
        }}
        val rs: ResultSet = super.insert(sql, entries, transformation)
        val generatedKeys: MutableList<Int> = mutableListOf()
        while(rs.next()){
            //Returns interesting key values if unique or not null constraint is violated
            generatedKeys.add(rs.getInt(1))
        }
        return generatedKeys
    }

    /**
     * Inserts codes into the Membership table belonging to a certain value set
     *
     * @param id VS_ID by which the value set is represented in the ValueSets table
     * @param entries list containing all codes contained in the expanded form of the value set: the codes represented
     *                as Triple instances with the system as the first, the code as the second and the display value as
     *                the third element
     */
    private fun insertCodes(id: Int, type: FhirType, entries: List<Triple<String, String, String>>) {
        logger.debug("Inserting ${entries.size} concept(s) ...")
        val sql = "INSERT INTO Membership (ID, TYPE, SYSTEM, CODE, DISPLAY) VALUES (?, ?::FHIR_TERM_TYPE, ?, ?, ?) ON CONFLICT DO NOTHING"
        val transformation = { stmt: PreparedStatement, codes: List<Triple<String, String, String>> -> codes.forEach { c ->
            stmt.setInt(1, id)
            stmt.setString(2, type.dbValue)
            stmt.setString(3, c.first)
            stmt.setString(4, c.second)
            stmt.setString(5, c.third)
            stmt.addBatch()
        }}
        super.insert(sql, entries, transformation)
    }

    override fun addCodeSystem(codeSystem: CodeSystem): Triple<CodeSystem, Int, Timestamp> {
        try{
            val url = codeSystem.url
            val version = codeSystem.version
            logger.debug("Adding CodeSystem with URL $url and version $version to database")
            val csIds = insertCodeSystems(listOf(codeSystem))

            if(csIds.isNotEmpty()){
                //Create database entries for codes if not already present and retrieve keys
                val codes = codeSystem.concept.distinct().map { coding -> Triple(url, coding.code, coding.display) }
                insertCodes(csIds[0], FhirType.CODE_SYSTEM, codes)

                val rs = super.executeQuery("SELECT VERSION_ID, LAST_UPDATED FROM CodeSystems WHERE CS_ID = ${csIds[0]}")
                if(rs.next()){
                    val info = Triple(buildCodeSystem(csIds[0].toString(), true), rs.getInt(1), rs.getTimestamp(2))
                    logger.debug("Finished adding ValueSet with URL $url and version $version to database")
                    return info
                }
                else{
                    throw Exception("Couldn't retrieve VERSION_ID and LAST_UPDATED for ValueSet with URL $url and version $version from database")
                }
            }
            else{
                val message = "CodeSystem with URL $url and version $version already exists in database"
                logger.warn(message)
                throw ValueSetException(message)
            }
        }
        catch (e: CodeSystemException){
            throw e
        }
        catch (e: Exception){
            val message = "Couldn't add CodeSystem to database"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(e.message, e)
        }
    }

    /**
     * Inserts value sets in form of entries into the ValueSets table into the database
     *
     * @param entries holds ValueSet instances to be added
     *
     * @return list containing the generated VS_IDs in the order in which the ValueSet instances themselves where
     *         provided
     */
    private fun insertCodeSystems(entries: List<CodeSystem>): List<Int>{
        logger.debug("Inserting ${entries.size} ${if(entries.size == 1) "CodeSystem" else "CodeSystems"} ...")
        val sql = "INSERT INTO CodeSystems (URL, VERSION, VERSION_ID, METADATA) VALUES (?, ?, 0, to_jsonb(?::json)) ON CONFLICT DO NOTHING RETURNING CS_ID"
        val transformation = {stmt: PreparedStatement, codeSystems: List<CodeSystem> -> codeSystems.forEach { cs ->
            stmt.setString(1, cs.url)
            stmt.setString(2, cs.version)
            stmt.setString(3, jsonParser.encodeToString(cs))
            stmt.addBatch()
        }}
        val rs: ResultSet = super.insert(sql, entries, transformation)
        val generatedKeys: MutableList<Int> = mutableListOf()
        while(rs.next()){
            //Returns interesting key values if unique or not null constraint is violated
            generatedKeys.add(rs.getInt(1))
        }
        return generatedKeys
    }

    override fun searchValueSet(url: String, version: String?): List<ValueSet>{
        val query = "SELECT VS_ID FROM ValueSets WHERE URL = ?${if(version != null) "AND VERSION = ?" else ""}"
        val value = mutableListOf(url)
        if(version != null) value.add(version)
        val rs: ResultSet = super.executeQuery(query, value)
        val vsList = mutableListOf<ValueSet>()
        try{
            while(rs.next()){
                val vsId = rs.getString(1)
                vsList.add(buildValueSet(vsId, true))
            }
            return vsList
        } catch (e: Exception){
            val message = "Failed to search for ValueSet instances with URL $url and version $version"
            throw Exception(message, e)
        }
    }

    fun supportsValueSet(url: String, version: String?): Boolean{
        val query = "SELECT VS_ID FROM ValueSets WHERE URL = ?${if(version != null) "AND VERSION = ?" else ""}"
        val value = mutableListOf(url)
        if(version != null) value.add(version)
        val rs: ResultSet = super.executeQuery(query, value)
        try{
            return rs.next()
        } catch (e: Exception){
            val message = "Failed to check if ValueSet with URL $url and version $version is supported!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    override fun validateCodeVS(url: String, version: String?, system: String, code: String, display: String?): Pair<Boolean, String?>{
        return if(version != null){
            validateCodeWithVsVersion(url, version, system, code, display) to version
        } else{
            validateCodeWithoutVsVersion(url, system, code, display)
        }
    }

    private fun validateCodeWithVsVersion(url: String, version: String, system: String, code: String, display: String?): Boolean{
        val query = "SELECT ID FROM Membership " +
                "WHERE ID IN (SELECT VS_ID FROM ValueSets WHERE URL = ? AND VERSION = ?) " +
                "AND TYPE = 'VS' AND SYSTEM = ? AND CODE = ? " + if(display != null) "DISPLAY = ?" else ""
        val value = mutableListOf(url, version, system, code)
        if(display != null) value.add(display)
        val rs: ResultSet = super.executeQuery(query, value)
        try{
            return rs.next()
        } catch (e: Exception){
            val message = "Failed to check if code $code from system $system is in ValueSet with URL $url and version $version"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    private fun validateCodeWithoutVsVersion(url: String, system: String, code: String, display: String?): Pair<Boolean, String?>{
        val query = "SELECT ValueSetsSlice.VS_ID, ValueSetsSlice.VERSION FROM Membership, " +
                "(SELECT VS_ID, VERSION FROM ValueSets WHERE URL = ?) AS ValueSetsSlice " +
                "WHERE Membership.ID = ValueSetsSlice.VS_ID AND Membership.TYPE = 'VS' AND Membership.SYSTEM = ? AND Membership.CODE = ? " +
                if(display != null) "AND Membership.DISPLAY = ?" else ""
        val value = mutableListOf(url, system, code)
        if(display != null) value.add(display)
        val rs: ResultSet = super.executeQuery(query, value)
        try{
            val versions = mutableListOf<String>()
            val result = rs.next()
            //JDBC left me no choice
            var version = rs.getString(2)
            if(version != null) versions.add(version)
            if(result && rs.next()){
                version = rs.getString(2)
                if(version != null) versions.add(version)
                while(rs.next()){
                    version = rs.getString(2)
                    if(version != null) versions.add(version)
                }
                throw AmbiguousValueSetVersionException("More than one version is available for value set [url = $url]", versions)
            }
            return result to if(versions.isEmpty()) null else versions[0]
        } catch (e: Exception){
            val message = "Failed to check if code $code from system $system is in ValueSet with URL $url"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    override fun readValueSet(id: String): ValueSet {
        logger.debug("Retrieving value set with ID $id")
        return buildValueSet(id, true)
    }

    private fun buildValueSet(vsId: String, summarized: Boolean): ValueSet{
        logger.debug("Building value set with internal ID $vsId")
        //Retrieve value set metadata
        var query = "SELECT VS_ID, VERSION_ID, LAST_UPDATED, METADATA FROM ValueSets WHERE VS_ID = ?"
        var rs = super.executeQuery(query, listOf(vsId.toInt())) { preparedStmt, idList -> preparedStmt.setInt(1, idList[0]) }
        val vs: ValueSet
        if(rs.next()) {
            vs = jsonParser.parseResource(rs.getString(4)) as ValueSet
            vs.id = rs.getString(1)
            vs.meta.versionId = rs.getInt(2).toString()
            vs.meta.lastUpdated = rs.getTimestamp(3)
        }
        else{
            throw NotFoundException<ValueSet>(Pair("id", vsId))
        }
        if (summarized) {
            tagAsSummarized(vs)
        }
        else{
            //Retrieve contained codings
            val expansion = vs.expansion
            expansion.timestamp = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
            query = "SELECT SYSTEM, CODE, DISPLAY FROM Membership WHERE TYPE = 'VS' AND ID = ?"
            rs = super.executeQuery(query, listOf(vsId))
            while (rs.next()){
                expansion.addContains(
                    ValueSet.ValueSetExpansionContainsComponent()
                        .setSystem(rs.getString(1))
                        .setCode(rs.getString(2))
                        .setDisplay(rs.getString(3))
                )
            }
        }
        return vs
    }

    private fun tagAsSummarized(canonicalResource: CanonicalResource) {
        canonicalResource.meta.addTag(Coding(
            "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
            "SUBSETTED",
            "subsetted"
        ))
    }

    override fun searchCodeSystem(url: String, version: String?): List<CodeSystem> {
        val query = "SELECT VS_ID FROM CodeSystems WHERE URL = ? ${if(version != null) "AND VERSION = ?" else ""}"
        val value = mutableListOf(url)
        if(version != null) value.add(version)
        val rs: ResultSet = super.executeQuery(query, value)
        val csList = mutableListOf<CodeSystem>()
        try{
            while(rs.next()){
                val csId = rs.getString(1)
                csList.add(buildCodeSystem(csId, true))
            }
            return csList
        } catch (e: Exception){
            val message = "Failed to search for CodeSystem instances with URL $url and version $version"
            throw CodeSystemException(message, e)
        }
    }

    override fun readCodeSystem(id: String): CodeSystem {
        logger.debug("Retrieving code system with ID $id")
        return buildCodeSystem(id, true)
    }

    private fun buildCodeSystem(csId: String, summarized: Boolean): CodeSystem {
        logger.debug("Building code system with internal ID $csId")
        //Retrieve value set metadata
        var query = "SELECT CS_ID, VERSION_ID, LAST_UPDATED, METADATA FROM CodeSystems WHERE CS_ID = ?"
        var rs = super.executeQuery(query, listOf(csId)) { preparedStmt, idList -> preparedStmt.setInt(1, idList[0].toInt()) }
        val cs: CodeSystem
        if(rs.next()) {
            cs = jsonParser.parseResource(rs.getString(4)) as CodeSystem
            cs.id = rs.getString(1)
            cs.meta.versionId = rs.getInt(2).toString()
            cs.meta.lastUpdated = rs.getTimestamp(3)
        }
        else{
            throw NotFoundException<CodeSystem>(Pair("id", csId))
        }
        if (summarized) {
            tagAsSummarized(cs)
        }
        else{
            //Retrieve contained concepts
            val concepts = cs.concept
            query = "SELECT CODE, DISPLAY FROM Membership WHERE TYPE = 'CS' AND ID = ? SYSTEM = ?"
            rs = super.executeQuery(query, listOf(cs.id, cs.url)) { stmt, list ->
                stmt.setInt(0, list[0].toInt())
                stmt.setString(1, list[1])
            }
            while (rs.next()){
                concepts.add(
                    CodeSystem.ConceptDefinitionComponent()
                        .setCode(rs.getString(1))
                        .setDisplay(rs.getString(2))
                )
            }
        }
        return cs
    }

    //TODO: Proper display handling: As of now display value will be ignored
    override fun validateCodeCS(code: String, display: String?, url: String): Boolean{
        logger.debug("Validating if code [code = $code, display = $display] is in code system [url = $url]")
        val query = "SELECT CODE FROM Membership WHERE TYPE = 'CS' SYSTEM = ? AND CODE = ?"
        val parameters = mutableListOf(url, code)
        //if(display != null) parameters.add(display)
        val rs = super.executeQuery(query, parameters)
        return rs.next()
    }

    override fun expandValueSet(url: String, version: String?): ValueSet{
        val query = "SELECT VS_ID FROM ValueSets WHERE URL = ?${if(version != null) "AND VERSION = ?" else ""}"
        val value = mutableListOf(url)
        if(version != null) value.add(version)
        val rs: ResultSet = super.executeQuery(query, value)
        try{
            if(rs.next()) {
                val vsId = rs.getString(1)
                return buildValueSet(vsId, false)
            }
            else {
                throw ValueSetException("No ValueSet found with URL $url and version $version")
            }
        } catch (e: Exception){
            val message = "Failed to search for ValueSet instances with URL $url and version $version"
            throw Exception(message, e)
        }
    }

    override fun addConceptMap(conceptMap: ConceptMap): Triple<ConceptMap, Int, Timestamp> {
        try{
            val url = conceptMap.url
            val version = conceptMap.version
            logger.debug("Adding ConceptMap with URL $url and version $version to database")
            val cmIds = insertConceptMaps(listOf(conceptMap))

            if(cmIds.isNotEmpty()){
                //Create database entries for translation elements
                insertTranslations(cmIds[0], conceptMap.group)

                val rs = super.executeQuery("SELECT VERSION_ID, LAST_UPDATED FROM ConceptMaps WHERE CM_ID = ${cmIds[0]}")
                if(rs.next()){
                    val info = Triple(buildConceptMap(cmIds[0].toString(), true), rs.getInt(1), rs.getTimestamp(2))
                    logger.debug("Finished adding ConceptMap with URL $url and version $version to database")
                    return info
                }
                else{
                    throw Exception("Couldn't retrieve VERSION_ID and LAST_UPDATED for ConceptMap with URL $url and version $version from database")
                }
            }
            else{
                val message = "ConceptMap with URL $url and version $version already exists in database"
                logger.warn(message)
                throw ConceptMapException(message)
            }
        }
        catch (e: CodeSystemException){ throw e }
        catch (e: Exception){
            val message = "Couldn't add CodeSystem to database. Reason: ${e.message}"
            throw Exception(message, e)
        }
    }

    private fun insertConceptMaps(entries: List<ConceptMap>): List<Int> {
        logger.debug("Inserting ${entries.size} ${if(entries.size == 1) "ConceptMap" else "ConceptMap"} ...")
        val sql = "INSERT INTO ConceptMaps (URL, VERSION, VERSION_ID, METADATA) VALUES (?, ?, 0, to_jsonb(?::json)) ON CONFLICT DO NOTHING RETURNING CM_ID"
        val transformation = {stmt: PreparedStatement, conceptMaps: List<ConceptMap> -> conceptMaps.forEach { cm ->
            stmt.setString(1, cm.url)
            stmt.setString(2, cm.version)
            stmt.setString(3, jsonParser.encodeToString(cm))
            stmt.addBatch()
        }}
        val rs: ResultSet = super.insert(sql, entries, transformation)
        val generatedKeys: MutableList<Int> = mutableListOf()
        while(rs.next()){
            //Returns interesting key values if unique or not null constraint is violated
            generatedKeys.add(rs.getInt(1))
        }
        return generatedKeys
    }

    private fun insertTranslations(cmId: Int, entries: List<ConceptMap.ConceptMapGroupComponent>) {
        logger.debug("Inserting ${entries.size} translation group(s) ...")
        val sql = "INSERT INTO Translation (CM_ID, CODE, DISPLAY, SOURCE_URL, SOURCE_VERSION, TARGET_URL, " +
                "TARGET_VERSION, TARGET_CODE, TARGET_DISPLAY, EQUIVALENCE, COMMENT, DEPENDS_ON) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_jsonb(?::json)) ON CONFLICT DO NOTHING"
        val transformation = { stmt: PreparedStatement, groups: List<ConceptMap.ConceptMapGroupComponent> -> groups.forEach { g ->
            val srcUrl = g.source
            val srcVersion = g.sourceVersion
            val tgtUrl = g.target
            val tgtVersion = g.targetVersion
            g.element.forEach { e ->
                e.target.forEach { t ->
                    stmt.setInt(1, cmId)
                    stmt.setString(2, e.code)
                    stmt.setString(3, e.display)
                    stmt.setString(4, srcUrl)
                    stmt.setString(5, srcVersion)
                    stmt.setString(6, tgtUrl)
                    stmt.setString(7, tgtVersion)
                    stmt.setString(8, t.code)
                    stmt.setString(9, t.display)
                    stmt.setString(10, t.equivalence.toCode())
                    stmt.setString(11, t.comment)
                    stmt.setString(12, JsonUtil.serialize(t.dependsOn))
                    stmt.addBatch()
                }
            }
        }}
        super.insert(sql, entries, transformation)
    }

    override fun searchConceptMap(url: String, version: String?): List<ConceptMap> {
        val query = "SELECT VS_ID FROM ConceptMaps WHERE URL = ?${if(version != null) " AND VERSION = ?" else ""}"
        val value = mutableListOf(url)
        if(version != null) value.add(version)
        val rs: ResultSet = super.executeQuery(query, value)
        val cmList = mutableListOf<ConceptMap>()
        try{
            while(rs.next()){
                val cmId = rs.getString(1)
                cmList.add(buildConceptMap(cmId, false))
            }
            return cmList
        } catch (e: Exception){
            val message = "Failed to search for ConceptMap instances with URL $url and version $version"
            throw Exception(message, e)
        }
    }

    private fun buildConceptMap(cmId: String, summarized: Boolean = false): ConceptMap {
        logger.debug("Building ConceptMap instance with internal ID $cmId")
        //Retrieve concept map metadata
        var query = "SELECT CM_ID, VERSION_ID, LAST_UPDATED, METADATA FROM ConceptMaps WHERE CM_ID = ? LIMIT 1"
        var rs = super.executeQuery(query, listOf(cmId.toInt())) { preparedStmt, idList -> preparedStmt.setInt(1, idList[0]) }
        val cm: ConceptMap
        if(rs.next()) {
            cm = jsonParser.parseResource(rs.getString(4)) as ConceptMap
            cm.id = rs.getString(1)
            cm.meta.versionId = rs.getInt(2).toString()
            cm.meta.lastUpdated = rs.getTimestamp(3)
        }
        else{
            throw NotFoundException<ConceptMap>(Pair("id", cmId))
        }
        if (summarized) {
            tagAsSummarized(cm)
        }
        else{
            //Retrieve contained translations
            val groupMap = mutableMapOf<String, Pair<ConceptMap.ConceptMapGroupComponent, MutableMap<String, ConceptMap.TargetElementComponent>>>()
            query = "SELECT CODE, DISPLAY, SOURCE_URL, SOURCE_VERSION, TARGET_URL, TARGET_VERSION, TARGET_CODE, " +
                    "TARGET_DISPLAY, EQUIVALENCE, COMMENT, DEPENDS_ON FROM Membership WHERE CM_ID = ?"
            rs = super.executeQuery(query, listOf(cmId))
            while (rs.next()){
                val code = rs.getString(1)
                val display = rs.getString(2)
                val srcUrl = rs.getString(3)
                val srcVersion = rs.getString(4)
                val tgtUrl = rs.getString(5)
                val tgtVersion = rs.getString(6)
                val tgtCode = rs.getString(7)
                val tgtDisplay = rs.getString(8)
                val tgtEquivalence = rs.getString(9)
                val tgtComment = rs.getString(10)
                val tgtDependsOnJson = rs.getString(11)
                val groupKey = "$srcUrl#$srcVersion#$tgtUrl#$tgtVersion"
                val (group, targetMap) = if (groupKey !in groupMap) Pair(ConceptMap.ConceptMapGroupComponent().apply {
                    source = srcUrl
                    sourceVersion = srcVersion
                    target = tgtUrl
                    targetVersion = tgtVersion
                    cm.addGroup(this)
                }, mutableMapOf<String, ConceptMap.TargetElementComponent>()).also { groupMap[groupKey] = it } else groupMap[groupKey]!!
                val element = group.addElement().apply {
                    this.code = code
                    this.display = display
                    group.addElement(this)
                }
                val target = if (tgtCode !in targetMap) element.addTarget().apply {
                    this.code = tgtCode
                    this.display = tgtDisplay
                    this.equivalence = ConceptMapEquivalence.fromCode(tgtEquivalence)
                    this.comment = tgtComment
                    element.addTarget(this)
                    targetMap[tgtCode] = this
                } else targetMap[tgtCode]!!
                target.dependsOn.addAll(JsonUtil.deserializeList<ConceptMap.OtherElementComponent>(tgtDependsOnJson))
            }
        }
        return cm
    }

    override fun readConceptMap(id: String): ConceptMap {
        logger.debug("Retrieving ConceptMap instance with ID $id")
        return buildConceptMap(id, true)
    }

    override fun deleteConceptMap(id: String) {
        logger.debug("Deleting translations for ConceptMap instance [ID = $id]")
        var query = "DELETE FROM Translation WHERE CM_ID = ?"
        val transformation = { stmt: PreparedStatement, idList: List<String> ->
            stmt.setInt(1, idList[0].toInt())
        }
        var affectedRows = super.executeUpdate(query, listOf(id), transformation)
        logger.trace("Deleted $affectedRows translation(s) from Translation table")

        logger.debug("Deleting ConceptMap instance metadata [ID = $id]")
        query = "DELETE FROM ConceptMaps WHERE CM_ID = ?"
        affectedRows = super.executeUpdate(query, listOf(id), transformation)

        if (affectedRows <= 0) throw NotFoundException<ValueSet>(Pair("id", id))
    }

    override fun translate(coding: Coding, targetSystem: String, recursive: Boolean): List<Pair<ConceptMapEquivalence, Coding>> {
        val system = coding.system
        val code = coding.code
        val version = coding.version
        logger.debug("Searching for translation of concept [URL = $system, code = $code, version = $version] to target system [URL = $targetSystem]")
        var query = "SELECT TARGET_CODE, TARGET_VERSION, EQUIVALENCE FROM Translation WHERE SOURCE_URL = ? AND CODE = ? AND TARGET_URL = ?"
        val params = mutableListOf(system, code, targetSystem)
        if (version != null) {
            query += " AND SOURCE_VERSION = ?"
            params.add(version)
        }
        val rs = super.executeQuery(query, params)
        val results = mutableListOf<Pair<ConceptMapEquivalence, Coding>>()
        while (rs.next()) {
            val targetCoding = Coding(targetSystem, rs.getString(1), rs.getString(2))
            val equivalence = ConceptMapEquivalence.fromCode(rs.getString(3))
            results.add(Pair(equivalence, targetCoding))
        }
        return results
    }

    override fun deleteValueSet(id: String) {
        logger.debug("Deleting concepts for ValueSet instance [ID = $id]")
        var query = "DELETE FROM Membership WHERE TYPE = 'VS' AND ID = ?"
        val transformation = { stmt: PreparedStatement, idList: List<String> ->
            stmt.setInt(1, idList[0].toInt())
        }
        var affectedRows = super.executeUpdate(query, listOf(id), transformation)
        logger.trace("Deleted $affectedRows concept(s) from Membership table")

        logger.debug("Deleting ValueSet instance metadata [ID = $id]")
        query = "DELETE FROM ValueSets WHERE VS_ID = ?"
        affectedRows = super.executeUpdate(query, listOf(id), transformation)

        if (affectedRows <= 0) throw NotFoundException<ValueSet>(Pair("id", id))
    }

    override fun deleteCodeSystem(id: String) {
        logger.debug("Deleting concepts for CodeSystem instance [ID = $id]")
        var query = "DELETE FROM Membership WHERE TYPE = 'CS' AND ID = NULL AND SYSTEM"
        val transformation = { stmt: PreparedStatement, idList: List<String> ->
            stmt.setInt(1, idList[0].toInt())
        }
        var affectedRows = super.executeUpdate(query, listOf(id), transformation)
        logger.trace("Deleted $affectedRows concept(s) from Membership table")

        logger.debug("Deleting ValueSet instance [ID = $id]")
        query = "DELETE FROM ValueSets WHERE VS_ID = ?"
        affectedRows = super.executeUpdate(query, listOf(id), transformation)

        if (affectedRows <= 0) throw NotFoundException<ValueSet>(Pair("id", id))
    }
}