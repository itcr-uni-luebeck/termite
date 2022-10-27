package de.itcr.termite.database.nosql

import de.itcr.termite.database.TerminologyStorage
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4.model.CodeSystem
import org.hl7.fhir.r4.model.ValueSet
import java.nio.file.Path
import java.sql.Timestamp

class TerminologyStore constructor(dbPath: Path): KeyValueStore(dbPath, listOf("valuesets", "systems", "codes")),
    TerminologyStorage {

    companion object {
        private val logger = LogManager.getLogger()
    }

    init{
        logger.debug("Creating key value store for value sets")
    }

    override fun addValueSet(valueSet: ValueSet): Triple<Int, Int, Timestamp> {
        val url = valueSet.url
        val version = valueSet.version
        logger.debug("Adding value set with url $url and version $version to key value store")
        return Triple(0, 0, Timestamp(0))
    }

    private fun addSystems(systems: List<String>){
        logger.debug("Adding ${systems.size} systems to key value store")

    }

    override fun validateCodeVS(url: String, version: String?, system: String, code: String, display: String?): Pair<Boolean, String> {
        TODO("Not yet implemented")
    }

    override fun searchValueSet(url: String, version: String?): List<ValueSet> {
        TODO("Not yet implemented")
    }

    override fun searchCodeSystem(url: String): List<CodeSystem> {
        TODO("Not yet implemented")
    }

    override fun validateCodeCS(code: String, display: String?, url: String): Boolean {
        TODO("Not yet implemented")
    }

}