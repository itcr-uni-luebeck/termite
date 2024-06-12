package de.itcr.termite.database.sql

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.sql.*

/**
 * Provides functionality for controlled interaction with the underlying relational database
 */
open class Database constructor(private val url: String) {

    private val logger : Logger = LogManager.getLogger()
    private var conn: Connection? = null

    init{
        //Establish connection to database
        try {
            conn = DriverManager.getConnection(url)
            logger.debug("Connection established.")
        }
        catch (e: SQLException) {
            logger.error("Connection to URL $url couldn't be established!")
            logger.error(e.stackTraceToString())
        }

        //Add shutdown hook to close connection of instance automatically if not closed already
        Runtime.getRuntime().addShutdownHook(object: Thread() {
            override fun run() {
                close()
            }
        })
    }

    /**
     * Closes the connection to the relational database
     */
    fun close(){
        try{
            if(conn?.isClosed == false){
                conn?.close()
                logger.debug("Connection closed.")
            }
        }
        catch (e: SQLException){
            logger.warn("Connection to URK $url couldn't be closed!")
            logger.debug(e.stackTraceToString())
        }
    }

    /**
     * Executes a SQL statement and returning no result. Thus, this method should only not be used for SELECT statements
     * and with caution in general
     *
     * @param sql String containing the SQL statement to execute
     */
    fun execute(sql: String){
        try {
            conn?.createStatement()?.execute(sql)
                ?: throw SQLException("Connection to URL $url was not established!")
        }
        catch (e: SQLException){
            val message = "Statement couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    /**
     * Executes a SQL query and returning the result. Use this method with caution as the input variables are not
     * checked for malicious content
     *
     * @param sql String containing the SQL query to execute
     *
     * @return ResultSet object containing the result of the query
     */
    fun executeQuery(sql: String): ResultSet{
        try {
            val rs = conn?.createStatement()?.executeQuery(sql)
                ?: throw SQLException("Connection to URL $url was not established!")
            return rs
        } catch (e: SQLException) {
            val message = "Query couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    /**
     * Executes a SQL query using a prepared statement and provided values. NOTE: It is supposed to be used for single
     * queries to which the values are assigned
     *
     * @see TerminologyDatabase for examples of valid transformations
     *
     * @param sql string containing the SQL query to execute
     * @param entries list of values that shall be assigned to the query
     * @param transformation specifies how the values are assigned to the query of the prepared statement
     *
     * @return ResultSet object containing the result of the query
     */
    protected fun <T> executeQuery(sql: String, entries: List<T>, transformation: (stmt: PreparedStatement, entries: List<T>) -> Unit): ResultSet{
        try{
            val stmt = conn?.prepareStatement(sql)
                ?: throw SQLException("Connection to URL $url was not established!")
            transformation.invoke(stmt, entries)
            return stmt.executeQuery()
        }
        catch (e: SQLException){
            val message = "Query couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    /**
     * Executes a SQL query with values assigned using the second argument
     *
     * @param sql string containing the SQL query to execute
     * @param entries list of values that shall be assigned to the query
     */
    protected fun <T> executeQuery(sql: String, entries: List<T>): ResultSet{
        return executeQuery(sql, entries) { stmt: PreparedStatement, entries: List<T> ->
            entries.forEachIndexed { idx, value -> stmt.setString(idx + 1, value.toString()) }
        }
    }

    protected fun <T> executeUpdate(sql: String, entries: List<T>, transformation: (stmt: PreparedStatement, entries: List<T>) -> Unit): Int {
        try{
            val stmt = conn?.prepareStatement(sql)
                ?: throw SQLException("Connection to URL $url was not established!")
            transformation.invoke(stmt, entries)
            return stmt.executeUpdate()
        }
        catch (e: SQLException){
            val message = "Update couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    protected fun <T> executeUpdate(sql: String, entries: List<T>): Int {
        return executeUpdate(sql, entries) { stmt: PreparedStatement, entries: List<T> ->
            entries.forEachIndexed { idx, value -> stmt.setString(idx + 1, value.toString()) }
        }
    }

    protected fun <T> executeBatch(sql: String, entries: List<T>, transformation: (stmt: PreparedStatement, entries: List<T>) -> Unit): List<Pair<Int, ResultSet>>{
        try{
            val stmt = conn?.prepareStatement(sql)
                ?: throw SQLException("Connection to URL $url was not established!")
            transformation.invoke(stmt, entries)
            val resultCodes = stmt.executeBatch()
            val results = mutableListOf<Pair<Int, ResultSet>>()
            var idx = 0
            while(stmt.moreResults){
                results.add(resultCodes[idx] to stmt.resultSet)
                idx++
            }
            return results
        }
        catch (e: Exception){
            val message = "Batch of queries couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    /**
     * Executes SQL INSERT statement in database and returns a ResultSet instance containing the generated keys
     */
    fun <T> insert(sql: String, entries: List<T>, transformation: (stmt: PreparedStatement, entries: List<T>) -> Unit): ResultSet{
        logger.debug("Inserting ${entries.size} elements")
        try {
            val stmt: PreparedStatement = conn?.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                ?: throw SQLException("Connection to URL $url was not established!")
            transformation.invoke(stmt, entries)
            stmt.executeBatch()
            return stmt.generatedKeys
        }
        catch (e: SQLException){
            val message = "Statement couldn't be executed!"
            logger.error(message)
            logger.error(e.stackTraceToString())
            throw Exception(message, e)
        }
    }

    fun getDatabaseMetaData(): DatabaseMetaData?{
        return conn?.metaData
    }

    fun checkIfColumnsAreUnique(catalog: String?, schema: String?, table: String, columns: List<String>): List<Boolean>{
        val dbmd = getDatabaseMetaData()
        val uniqueColumns: MutableSet<String> = mutableSetOf()
        try{
            val rs = dbmd?.getIndexInfo(catalog, schema, table, true, true)
            while(rs?.next() == true){
                val idxName = rs.getString("INDEX_NAME")
                val colName = rs.getString("COLUMN_NAME")
                if (idxName == null || colName == null) continue
                uniqueColumns.add(colName)
            }
        }
        catch (e: SQLException){
            logger.error("Meta data couldn't be retrieved!")
            logger.error(e.stackTraceToString())
            return List(columns.size){false}
        }
        return columns.map{ c -> uniqueColumns.contains(c)}
    }

    fun checkIfColumnsAreUnique(table: String, columns: List<String>): List<Boolean>{
        return checkIfColumnsAreUnique(null, null, table, columns)
    }

    fun checkIfColumnIsUnique(catalog: String?, schema: String?, table: String, column: String): Boolean{
        return checkIfColumnsAreUnique(catalog, schema, table, listOf(column))[0]
    }

    fun checkIfColumnIsUnique(table: String, column: String): Boolean{
        return checkIfColumnIsUnique(null, null, table, column)
    }

    fun getPrimaryKeyColumns(catalog: String?, schema: String?, table: String): List<String>{
        return try{
            val primaryKeys: MutableList<String> = mutableListOf()
            val rs: ResultSet? = getDatabaseMetaData()?.getPrimaryKeys(catalog, schema, table)
            while(rs?.next() == true){
                primaryKeys.add(rs.getString("COLUMN_NAME"))
            }
            primaryKeys
        } catch (e: SQLException){
            logger.error("Meta data couldn't be retrieved!")
            logger.error(e.stackTraceToString())
            emptyList()
        }
    }

    fun getPrimaryKeyColumns(table: String): List<String>{
        return getPrimaryKeyColumns(null, null, table)
    }

}