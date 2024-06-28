package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.*
import de.itcr.termite.model.entity.FhirCodeSystemMetadata
import de.itcr.termite.util.serialize
import org.hl7.fhir.r4b.model.*
import java.nio.ByteBuffer
import java.util.*

sealed class CodeSystemSearchIndexPartitions<ELEMENT>(
    indexName: String,
    prefixLength: Int,
    elementPath: (CodeSystem) -> Iterable<ELEMENT>,
    prefixGenerator: (ELEMENT) -> ByteArray,
    keyGenerator: (ELEMENT, Int) -> ByteArray
): CodeSystemIndexPartitions<ELEMENT, Int, (ELEMENT) -> ByteArray, (ELEMENT, Int) -> ByteArray>(indexName, prefixLength, elementPath, prefixGenerator, keyGenerator),
    FhirSearchIndexPartitions<CodeSystem, ELEMENT, ByteArray> {

    data object SEARCH_CONTENT_MODE: CodeSystemSearchIndexPartitions<CodeSystem.CodeSystemContentMode>(
        "CodeSystem.search.content-mode",
        4,
        { res: CodeSystem -> listOf(res.content) },
        { value: CodeSystem.CodeSystemContentMode -> serialize(value.ordinal) },
        { value: CodeSystem.CodeSystemContentMode, id: Int -> ByteBuffer.allocate(8).putInt(value.ordinal).putInt(id).array() }
    )

    // Using Long (8 bytes) instead of String hash code (4 bytes) to possibly leverage ordering on iteration
    data object SEARCH_DATE: CodeSystemSearchIndexPartitions<Date>(
        "CodeSystem.search.date",
        8,
        { res: CodeSystem -> listOf(res.date) },
        { value: Date -> serialize(value.time) },
        { value: Date, id: Int -> ByteBuffer.allocate(12).putLong(value.time).putInt(id).array() }
    )

    data object SEARCH_DESCRIPTION: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.description",
        4,
        { res: CodeSystem -> listOf(res.description) },
        { value: String -> serialize(value) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_IDENTIFIER: CodeSystemSearchIndexPartitions<Identifier>(
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

    data object SEARCH_JURISDICTION: CodeSystemSearchIndexPartitions<Coding>(
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

    data object SEARCH_LANGUAGE: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.language",
        4,
        { cs: CodeSystem -> listOf(cs.language) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_NAME: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.name",
        4,
        { cs: CodeSystem -> listOf(cs.name) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_PUBLISHER: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.publisher",
        4,
        { cs: CodeSystem -> listOf(cs.publisher) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_STATUS: CodeSystemSearchIndexPartitions<Enumerations.PublicationStatus>(
        "CodeSystem.search.status",
        4,
        { cs: CodeSystem -> listOf(cs.status) },
        { value: Enumerations.PublicationStatus -> serialize(value.ordinal) },
        { value: Enumerations.PublicationStatus, id: Int -> ByteBuffer.allocate(8).putInt(value.ordinal).putInt(id).array() }
    )

    // NOTE: IF a URL is supplied as a reference to the CodeSystem seek into the CodeSystem.search.url partition instead
    data object SEARCH_SUPPLEMENTS: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.supplements",
        4,
        { cs: CodeSystem -> listOf(cs.supplements) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_SYSTEM: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.url",
        4,
        { cs: CodeSystem -> listOf(cs.url) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_TITLE: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.title",
        4,
        { cs: CodeSystem -> listOf(cs.title) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_URL: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.url",
        4,
        { cs: CodeSystem -> listOf(cs.url) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

    data object SEARCH_VERSION: CodeSystemSearchIndexPartitions<String>(
        "CodeSystem.search.version",
        4,
        { cs: CodeSystem -> listOf(cs.version) },
        { value: String -> serialize(value.hashCode()) },
        { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() }
    )

}