package de.itcr.termite.parser

import de.itcr.termite.util.simpleQualifiedName
import org.hl7.fhir.instance.model.api.IBackboneElement

interface IBackboneElementParser {

    fun parse(string: String, typeName: String): IBackboneElement

}

inline fun <reified TYPE: IBackboneElement> IBackboneElementParser.parse(string: String) =
    this.parse(string, TYPE::class.simpleQualifiedName()) as TYPE