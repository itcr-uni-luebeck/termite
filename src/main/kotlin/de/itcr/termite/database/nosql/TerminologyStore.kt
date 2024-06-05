package de.itcr.termite.database.nosql

import de.itcr.termite.database.TerminologyStorage
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CodeSystem
import org.hl7.fhir.r4b.model.Coding
import org.hl7.fhir.r4b.model.ConceptMap
import org.hl7.fhir.r4b.model.ValueSet
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

    override fun addCodeSystem(codeSystem: CodeSystem): Triple<Int, Int, Timestamp> {
        TODO("Not yet implemented")
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

    override fun expandValueSet(url: String, version: String?): ValueSet {
        TODO("Not yet implemented")
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