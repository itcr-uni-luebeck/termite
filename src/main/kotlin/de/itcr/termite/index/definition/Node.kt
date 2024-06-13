package de.itcr.termite.index.definition

interface Node {

    fun name(): String

    fun children(): List<Node>

}