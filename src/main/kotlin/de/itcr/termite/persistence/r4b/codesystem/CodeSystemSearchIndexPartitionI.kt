package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.*
import de.itcr.termite.util.serialize
import org.hl7.fhir.r4b.model.*
import java.nio.ByteBuffer
import java.util.*

sealed class CodeSystemSearchIndexPartitionI<ELEMENT>(
    indexName: String,
    prefixLength: Int,
    elementPath: (CodeSystem) -> Iterable<ELEMENT>,
    prefixGenerator: (ELEMENT) -> ByteArray,
    keyGenerator: (ELEMENT, Int) -> ByteArray
): CodeSystemIndexPartitionI<ELEMENT, Int, (ELEMENT) -> ByteArray, (ELEMENT, Int) -> ByteArray>(indexName, prefixLength, elementPath, prefixGenerator, keyGenerator),
    IFhirSearchIndexPartition<CodeSystem, ELEMENT, ByteArray> {

    data object SEARCH_CONTENT_MODE: CodeSystemSearchIndexPartitionI<CodeSystem.CodeSystemContentMode>(
        "CodeSystem.search.content-mode",
        4,
        { res: CodeSystem -> listOf(res.content) },
        { value: CodeSystem.CodeSystemContentMode -> serialize(value.ordinal) },
        { value: CodeSystem.CodeSystemContentMode, id: Int -> ByteBuffer.allocate(8).putInt(value.ordinal).putInt(id).array() }
    )

    // Using Long (8 bytes) instead of String hash code (4 bytes) to possibly leverage ordering on iteration
    data object SEARCH_DATE: CodeSystemSearchIndexPartitionI<Date>(
        "CodeSystem.search.date",
        8,
        { res: CodeSystem -> listOf(res.date) },
        { value: Date -> serialize(value.time) },
        { value: Date, id: Int -> ByteBuffer.allocate(12).putLong(value.time).putInt(id).array() }
    )

    data object SEARCH_DESCRIPTION: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.description",
        4,
        { res: CodeSystem -> listOf(res.description) },
        { value: String -> serialize(value) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_IDENTIFIER: CodeSystemSearchIndexPartitionI<Identifier>(
        "CodeSystem.search.identifier",
        8,
        { res: CodeSystem -> res.identifier },
        { value: Identifier -> ByteBuffer.allocate(8)
            .putInt(value.system.hashCode())
            .putInt(value.value.hashCode())
            .array() },
        { value: Identifier, id: Int -> ByteBuffer.allocate(12)
            .putInt(value.system.hashCode())
            .putInt(value.value.hashCode())
            .putInt(id)
            .array() }
    )

    data object SEARCH_JURISDICTION: CodeSystemSearchIndexPartitionI<Coding>(
        "CodeSystem.search.jurisdiction",
        8,
        { res: CodeSystem -> res.jurisdiction.map { cc -> cc.coding }.flatten() },
        { value: Coding -> ByteBuffer.allocate(8)
            .putInt(value.system.hashCode())
            .putInt(value.code.hashCode())
            .array()},
        { value: Coding, id: Int -> ByteBuffer.allocate(16)
            .putInt(value.system.hashCode())
            .putInt(value.code.hashCode())
            .putInt(value.display.hashCode())
            .putInt(id)
            .array()}
    )

    data object SEARCH_LANGUAGE: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.language",
        4,
        { cs: CodeSystem -> listOf(cs.language) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_NAME: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.name",
        4,
        { cs: CodeSystem -> listOf(cs.name) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_PUBLISHER: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.publisher",
        4,
        { cs: CodeSystem -> listOf(cs.publisher) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_STATUS: CodeSystemSearchIndexPartitionI<Enumerations.PublicationStatus>(
        "CodeSystem.search.status",
        4,
        { cs: CodeSystem -> listOf(cs.status) },
        { value: Enumerations.PublicationStatus -> serialize(value.ordinal) },
        { value: Enumerations.PublicationStatus, id: Int -> ByteBuffer.allocate(8).putInt(value.ordinal).putInt(id).array() }
    )

    // NOTE: IF a URL is supplied as a reference to the CodeSystem seek into the CodeSystem.search.url partition instead
    data object SEARCH_SUPPLEMENTS: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.supplements",
        4,
        { cs: CodeSystem -> listOf(cs.supplements) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_SYSTEM: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.url",
        4,
        { cs: CodeSystem -> listOf(cs.url) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_TITLE: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.title",
        4,
        { cs: CodeSystem -> listOf(cs.title) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_URL: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.url",
        4,
        { cs: CodeSystem -> listOf(cs.url) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_VERSION: CodeSystemSearchIndexPartitionI<String>(
        "CodeSystem.search.version",
        4,
        { cs: CodeSystem -> listOf(cs.version) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

}