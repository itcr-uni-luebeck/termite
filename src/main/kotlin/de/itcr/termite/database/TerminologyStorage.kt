package de.itcr.termite.database

import org.hl7.fhir.r4b.model.*
import java.sql.Timestamp

/**
 * Declares how other objects may interact with an instance that stores value sets and code systems. The methods defined
 * here shall correspond to interactions and operations on ValueSet and CodeSystem instances as described by the
 * HL7 FHIR R4 standard for services conforming to the TerminologyService specification
 */
interface TerminologyStorage {

    /**
     * Adds a value set to the underlying storage medium
     *
     * @param valueSet ValueSet instance to be added
     *
     * @return Triple containing in the following oder the internal ID of the value set, the internal version ID of the
     *         value set and last-updated timestamp
     */
    fun addValueSet(valueSet: ValueSet): Triple<Int, Int, Timestamp>

    fun addCodeSystem(codeSystem: CodeSystem): Triple<Int, Int, Timestamp>

    fun addConceptMap(conceptMap: ConceptMap): Triple<Int, Int, Timestamp>

    /**
     * Validates if the given code is in the given value set
     *
     * @param url URL of the value set
     * @param version Version string of the value set managed by its maintainer
     * @param system Code system to which the code belongs
     * @param code value of the code
     * @param display Display value of the code
     *
     * @return Validation result containing a boolean indicating whether the given code is in the given value set and a
     *         string representing the version of the value set (not the internal version ID!) against which the
     *         validation was performed
     */
    fun validateCodeVS(url: String, version: String?, system: String, code: String, display: String?): Pair<Boolean, String?>

    /**
     * Returns all value sets matching the criteria provided via the arguments
     *
     * @param url URL of the value set
     * @param version Version string of the value set managed by its maintainer
     *
     * @return List containing value sets that match all criteria
     */
    fun searchValueSet(url: String, version: String?): List<ValueSet>

    fun searchCodeSystem(url: String): List<CodeSystem>

    fun searchConceptMap(url: String): List<ConceptMap>

    fun validateCodeCS(code: String, display: String?, url: String): Boolean

    fun expandValueSet(url: String, version: String?): ValueSet

    fun translate(coding: Coding, url: String): List<Pair<ConceptMap.ConceptMapEquivalence, Coding>>

}