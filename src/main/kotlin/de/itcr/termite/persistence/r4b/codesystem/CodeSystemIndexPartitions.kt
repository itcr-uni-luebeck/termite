package de.itcr.termite.persistence.r4b.codesystem

import de.itcr.termite.index.partition.*
import de.itcr.termite.util.serialize
import java.nio.ByteBuffer

sealed class CodeSystemIndexPartitions<KEY_GENERATOR: Function<ByteArray>, VALUE_GENERATOR: Function<ByteArray>>(
    private val indexName: String,
    private val keyGenerator: KEY_GENERATOR,
    private val valueGenerator: VALUE_GENERATOR
) : FhirIndexPartitions<ByteArray, KEY_GENERATOR, ByteArray, VALUE_GENERATOR> {

    private val bytes = indexName.toByteArray(Charsets.UTF_8)

    data object SEARCH_CODE: CodeSystemIndexPartitions<Generator4<String, String, String, Int, ByteArray>, Generator1<Int, ByteArray>> (
        "CodeSystem.lookup",
        Generator4 { system: String, code: String, display: String, id: Int -> ByteBuffer.allocate(16)
            .putInt(system.hashCode())
            .putInt(code.hashCode())
            .putInt(display.hashCode())
            .putInt(id)
            .array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_CONTENT_MODE: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.content-mode",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_DATE: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.date",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_DESCRIPTION: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.description",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_IDENTIFIER: CodeSystemIndexPartitions<Generator3<String, String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.identifier",
        Generator3 {system: String, value:String, id: Int -> ByteBuffer.allocate(12)
            .putInt(system.hashCode())
            .putInt(value.hashCode())
            .putInt(id)
            .array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_JURISDICTION: CodeSystemIndexPartitions<Generator4<String, String, String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.jurisdiction",
        Generator4 { system: String, code: String, display: String, id: Int -> ByteBuffer.allocate(16)
            .putInt(system.hashCode())
            .putInt(code.hashCode())
            .putInt(display.hashCode())
            .putInt(id)
            .array()},
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_LANGUAGE: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.language",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_NAME: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.name",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_PUBLISHER: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.publisher",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_STATUS: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
    "CodeSystem.search.status",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    // NOTE: IF a URL is supplied as a reference to the CodeSystem seek into the CodeSystem.search.url partition instead
    data object SEARCH_SUPPLEMENTS: CodeSystemIndexPartitions<Generator2<Int, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.supplements",
        Generator2 { csId: Int, id: Int -> ByteBuffer.allocate(8).putInt(csId).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_SYSTEM: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.url",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_TITLE: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.title",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_URL: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.url",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object SEARCH_VERSION: CodeSystemIndexPartitions<Generator2<String, Int, ByteArray>, Generator1<Int, ByteArray>>(
        "CodeSystem.search.version",
        Generator2 { value: String, id: Int -> ByteBuffer.allocate(8).putInt(value.hashCode()).putInt(id).array() },
        Generator1 { id: Int -> serialize(id) }
    )

    data object LOOKUP: CodeSystemIndexPartitions<Generator4<String, String, String, Int, ByteArray>, Generator1<Int, ByteArray>> (
        "CodeSystem.lookup",
        Generator4 { system: String, code: String, display: String, id: Int -> ByteBuffer.allocate(16)
            .putInt(system.hashCode())
            .putInt(code.hashCode())
            .putInt(display.hashCode())
            .putInt(id)
            .array() },
        Generator1 { id: Int -> serialize(id) }
    )


    override fun indexName(): String = indexName

    override fun bytes(): ByteArray = bytes

    override fun keyGenerator(): KEY_GENERATOR = keyGenerator

    override fun valueGenerator(): VALUE_GENERATOR = valueGenerator

}