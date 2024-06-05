package de.itcr.termite.database.sql

import de.itcr.termite.database.TerminologyStorage
import de.itcr.termite.exception.AmbiguousValueSetVersionException
import de.itcr.termite.exception.CodeSystemException
import de.itcr.termite.exception.ValueSetException
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.*
import java.lang.System
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * Implementation of the ValueSetStorage interface using a relational database
 * @see TerminologyStorage
 */
class TerminologyDatabase constructor(url: String): Database(url), TerminologyStorage {

    companion object {
        private val logger = LogManager.getLogger()
    }

    init {
        //Create tables and indices
        logger.debug("Creating ValueSets table ...")
        super.execute("CREATE TABLE IF NOT EXISTS ValueSets (VS_ID INTEGER PRIMARY KEY, URL TEXT NOT NULL, VERSION TEXT, VERSION_ID INTEGER NOT NULL, LAST_UPDATED TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                          "UNIQUE(URL, VERSION))")
        logger.debug("Creating CodeSystems table ...")
        /*
        super.execute("CREATE TABLE IF NOT EXISTS CodeSystems (CS_ID INTEGER PRIMARY KEY, URL TEXT NOT NULL, VERSION TEXT, VERSION_ID INTEGER NOT NULL, LAST_UPDATED TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP), " +
                          "UNIQUE(URL, VERSION)")
         */
        logger.debug("Creating OperationDefinition table ...")
        super.execute("CREATE TABLE IF NOT EXISTS OperationDefinitions (OD_ID INTEGER PRIMARY KEY, RESOURCE TEXT NOT NULL)")
        logger.debug("Creating Membership table ...")
        super.execute("CREATE TABLE IF NOT EXISTS Membership (VS_ID INTEGER, SYSTEM TEXT NOT NULL, CODE TEXT NOT NULL, DISPLAY TEXT NOT NULL, " +
                          "UNIQUE(VS_ID, SYSTEM, CODE, DISPLAY), " +
                          "FOREIGN KEY (VS_ID) REFERENCES ValueSets(VS_ID))")
        logger.debug("Creating Membership index ...")
        super.execute("CREATE INDEX IF NOT EXISTS Idx_Membership ON Membership(VS_ID, SYSTEM, CODE, DISPLAY)")
    }

    override fun addValueSet(valueSet: ValueSet): Triple<Int, Int, Timestamp> {
        try{
            //Retrieve ValueSet information and create database entry if not already present
            val url: String = valueSet.url
            val version: String? = valueSet.version
            logger.debug("Adding ValueSet with URL $url and version $version to database")
            val vsIds = insertValueSets(listOf(valueSet))

            if(vsIds.isNotEmpty()){
                //Create database entries for codes if not already present and retrieve keys
                if(valueSet.expansion.contains.isEmpty()) throw ValueSetException("Value set is empty or not expanded")
                val codes = valueSet.expansion.contains.distinct()
                    .map { coding -> Triple(coding.system, coding.code, coding.display) }
                insertCodes(vsIds[0], codes)
            }
            else{
                logger.warn("ValueSet with URL $url and version $version already exists in database")
            }

            val rs = super.executeQuery("SELECT VERSION_ID, LAST_UPDATED FROM ValueSets WHERE VS_ID = ${vsIds[0]}")
            if(rs.next()){
                val info = Triple(vsIds[0], rs.getInt(1), rs.getTimestamp(2))
                logger.debug("Finished adding ValueSet with URL $url and version $version to database")
                return info
            }
            else{
                throw Exception("Couldn't retrieve VERSION_ID and LAST_UPDATED for ValueSet with URL $url and version $version from database")
            }
        }
        catch (e: ValueSetException){
            throw e
        }
        catch (e: Exception){
            val message = "Couldn't add ValueSet to database"
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
        val sql = "INSERT OR IGNORE INTO ValueSets (URL, VERSION, VERSION_ID) VALUES (?, ?, 0)"
        val transformation = {stmt: PreparedStatement, valueSets: List<ValueSet> -> valueSets.forEach { vs ->
            stmt.setString(1, vs.url)
            stmt.setString(2, vs.version)
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
     * @param vsId VS_ID by which the value set is represented in the ValueSets table
     * @param entries list containing all codes contained in the expanded form of the value set: the codes represented
     *                as Triple instances with the system as the first, the code as the second and the display value as
     *                the third element
     */
    private fun insertCodes(vsId: Int?, entries: List<Triple<String, String, String>>) {
        logger.debug("Inserting ${entries.size} ${if(entries.size == 1) "Code" else "Codes"} ...")
        val sql = "INSERT OR IGNORE INTO Membership (VS_ID, SYSTEM, CODE, DISPLAY) VALUES (?, ?, ?, ?)"
        val transformation = { stmt: PreparedStatement, codes: List<Triple<String, String, String>> -> codes.forEach { c ->
            if (vsId != null) stmt.setInt(1, vsId) else stmt.setNull(1, java.sql.Types.INTEGER)
            stmt.setString(2, c.first)
            stmt.setString(3, c.second)
            stmt.setString(4, c.third)
            stmt.addBatch()
        }}
        super.insert(sql, entries, transformation)
    }

    override fun addCodeSystem(codeSystem: CodeSystem): Triple<Int, Int, Timestamp> {
        try{
            val url = codeSystem.url
            logger.debug("Adding CodeSystem with URL $url and version to database")

            //Create database entries for codes if not already present and retrieve keys
            if(codeSystem.concept.isEmpty()) throw CodeSystemException("Code system is empty")
            val codes = codeSystem.concept.distinct().map { coding -> Triple(url, coding.code, coding.display) }
            insertCodes(null, codes)

            val info = Triple(0, 0, Timestamp(System.currentTimeMillis()))
            logger.debug("Finished adding CodeSystem with URL $url to database")
            return info
        }
        catch (e: CodeSystemException){
            throw e
        }
        catch (e: Exception){
            val message = "Couldn't add CodeSystem to database"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
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
        val query = "SELECT VS_ID FROM Membership " +
                "WHERE VS_ID = (SELECT VS_ID FROM ValueSets WHERE URL = ? AND VERSION = ?) " +
                "AND SYSTEM = ? AND CODE = ? " + if(display != null) "DISPLAY = ?" else ""
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
                "WHERE Membership.VS_ID = ValueSetsSlice.VS_ID AND Membership.SYSTEM = ? AND Membership.CODE = ? " +
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

    //TODO: Implement function (Optional for functionality: Lower Priority)
    fun getValueSet(vsId: String, versionId: Int): ValueSet{
        logger.debug("Retrieving ValueSet instance with internal ID $vsId and internal version ID $versionId ...")
        try{
            val rs: ResultSet = super.executeQuery(
                "SELECT * FROM ValueSets WHERE VS_ID = ? AND VERSION_ID = ?",
                listOf(vsId, versionId)
            )
            if(rs.next()){
                val vs = ValueSet()
                vs.id = rs.getInt(1).toString()
                vs.url = rs.getString(2)
                vs.version = rs.getString(3)
                return vs
            }
            else{
                throw Exception("ResultSet instance was empty")
            }
        }
        catch (e: Exception){
            val message = "Retrieving ValueSet instance with internal ID $vsId and internal version ID $versionId failed"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    //TODO: Implement getValueSet function on which this function relies
    fun getValueSet(vsId: String): ValueSet{
        return getValueSet(vsId, 0)
    }

    private fun buildValueSet(vsId: String, summarized: Boolean): ValueSet{
        logger.debug("Building value set with internal ID $vsId")
        //Retrieve value set metadata
        var query = "SELECT VS_ID, URL, VERSION, VERSION_ID, LAST_UPDATED FROM ValueSets WHERE VS_ID = ?"
        var rs = super.executeQuery(query, listOf(vsId))
        val vs = ValueSet()
        if(rs.next()) {
            vs.id = rs.getString(1)
            vs.url = rs.getString(2)
            vs.version = rs.getString(3)
            vs.meta.versionId = rs.getInt(4).toString()
            vs.meta.lastUpdated = rs.getTimestamp(5)
        }
        else{
            throw Exception("Value set corresponding to internal ID $vsId doesn't exist")
        }
        if (summarized) {
            tagAsSummarized(vs)
        }
        else{
            //Retrieve contained codings
            val expansion = vs.expansion
            expansion.timestamp = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
            query = "SELECT SYSTEM, CODE, DISPLAY FROM Membership WHERE VS_ID = ?"
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

    override fun searchCodeSystem(url: String): List<CodeSystem> {
        logger.debug("Retrieving fragment of code system [url = $url]")
        val query = "SELECT CODE, DISPLAY FROM Membership WHERE SYSTEM = ? GROUP BY CODE, DISPLAY LIMIT 1"
        val rs = super.executeQuery(query, listOf(url))
        if (!rs.isBeforeFirst) return listOf()
        val cs = CodeSystem()
        cs.url = url
        cs.status = Enumerations.PublicationStatus.UNKNOWN
        cs.content = CodeSystem.CodeSystemContentMode.FRAGMENT
        tagAsSummarized(cs)
        return listOf(cs)
    }

    //TODO: Proper display handling: As of now display value will be ignored
    override fun validateCodeCS(code: String, display: String?, url: String): Boolean{
        logger.debug("Validating if code [code = $code, display = $display] is in code system [url = $url]")
        //val query = "SELECT CODE FROM Membership WHERE SYSTEM = ? AND CODE = ?${if(display != null) " AND DISPLAY = ?" else ""}"
        val query = "SELECT CODE FROM Membership WHERE SYSTEM = ? AND CODE = ?"
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

    override fun addConceptMap(conceptMap: ConceptMap): Triple<Int, Int, Timestamp> {
        TODO("Not yet implemented")
    }

    override fun searchConceptMap(url: String): List<ConceptMap> {
        TODO("Not yet implemented")
    }

    override fun translate(coding: Coding, url: String): List<Pair<ConceptMap.ConceptMapEquivalence, Coding>> {
        TODO("Not yet implemented")
    }
}

data class System(val url: String)

data class Code(val system: String, val code: String, val display: String)