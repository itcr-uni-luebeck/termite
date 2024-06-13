package de.itcr.termite.index.definition

import ca.uhn.fhir.context.FhirVersionEnum

class IndexDefinition(
    val fhirVersion: FhirVersionEnum,
    val content: List<Node>
) {

    class PathNode(private val name: String, private val children: List<Node>): Node {

        override fun name(): String = name

        override fun children(): List<Node> = children

    }

    class IndexNode(private val name: String): Node {

        override fun name(): String = name

        override fun children(): List<Node> = emptyList()

    }

}

fun buildIndexDefinition(filePath: String): IndexDefinition {
    TODO()
}