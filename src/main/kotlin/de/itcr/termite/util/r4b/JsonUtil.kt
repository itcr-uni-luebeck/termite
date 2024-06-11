package de.itcr.termite.util.r4b

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.exception.parsing.JsonParsingException
import de.itcr.termite.parser.r4b.BackboneElementParser
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4b.formats.JsonParser
import org.hl7.fhir.r4b.model.DataType

class JsonUtil {

    companion object {

        private val context = FhirContext.forR4B()
        private val jsonEncoder = context.newJsonParser()
        private val jsonParser = JsonParser(false)
        private val backboneElementParser = BackboneElementParser()

        fun <TYPE: IBase> serialize(type: TYPE?): String? = if (type != null) jsonEncoder.encodeToString(type) else null

        fun <TYPE: IBase> serialize(list: List<TYPE?>?): String? = if (list != null) "[${list.filterNotNull().joinToString(",") { serialize(it)!! }}]" else null

        fun deserialize(string: String?, typeName: String): DataType? {
            return if (string == null) null
            else if (BackboneElementParser.supportsType(typeName)) backboneElementParser.parse(string, typeName)
            else jsonParser.parseType(string, typeName)
        }

        fun deserializeList(string: String?, typeName: String): List<DataType?> {
            if (string == null) return emptyList()
            val jsonList = splitJsonArrayString(string)
            return when (jsonList.size) {
                0 -> emptyList()
                1 -> if (jsonList[0].all { it == ' ' }) emptyList() else listOf(deserialize(jsonList[0], typeName))
                else -> jsonList.map { deserialize(it, typeName) }
            }
        }

        fun deserializeStringList(string: String?): List<String> {
            if (string == null) return emptyList()
            val jsonList = splitJsonArrayString(string)
            // Assuming this method is only used to parse JSON string from the database, this should suffice
            return if (jsonList.isEmpty()) emptyList()
            else jsonList.map { it.substring(1, it.length - 1) }
        }

        fun splitJsonArrayString(string: String): List<String> = splitJsonObjectOrArrayString(string, '[', ']')

        fun splitJsonObjectString(string: String) =
            splitJsonObjectOrArrayString(string, '{', '}').associate { splitJsonKeyValueString(it) }

        // FIXME: Inefficient for deeply nested structures as many substrings will be read multiple times over multiple
        //        uses of this method on smaller substrings of the original string
        private fun splitJsonObjectOrArrayString(string: String, prefixChar: Char, suffixChar: Char): List<String> {
            val trimmedString = string.trim().removeSurrounding("$prefixChar", "$suffixChar")
            if (trimmedString.isEmpty()) return emptyList()
            val list = mutableListOf<String>()
            var depthCurlyBraces = 0
            var depthBrackets = 0
            var depthDoubleQuotes = 0
            var prevIdx = 0
            var idx = 0
            while (idx < trimmedString.length) {
                val c = trimmedString[idx]
                when (c) {
                    '{' -> if (depthBrackets == 0 && depthDoubleQuotes == 0) depthCurlyBraces++
                    '}' -> if (depthBrackets == 0 && depthDoubleQuotes == 0) depthCurlyBraces--
                    '[' -> if (depthCurlyBraces == 0 && depthDoubleQuotes == 0) depthBrackets++
                    ']' -> if (depthCurlyBraces == 0 && depthDoubleQuotes == 0) depthBrackets--
                    '"' -> if (depthCurlyBraces == 0 && depthBrackets == 0) depthDoubleQuotes = 1 - depthDoubleQuotes
                    '\\' -> if (depthDoubleQuotes >= 1 && trimmedString[idx + 1] == '"' && depthDoubleQuotes == 1) idx++
                    ',' -> if (depthCurlyBraces == 0 && depthBrackets == 0 && depthDoubleQuotes == 0) {
                        list.add(trimmedString.slice(prevIdx..<idx))
                        prevIdx = idx + 1
                    }
                }
                idx++
            }
            list.add(trimmedString.substring(prevIdx))
            return list
        }

        fun splitJsonKeyValueString(string: String, trimInput: Boolean = true): Pair<String, String> {
            val trimmedString = if (trimInput) string.trim() else string
            if (trimmedString[0] != '"') throw JsonParsingException("Key in JSON string '$string' is not of type string")
            var idx = 1
            while (idx < trimmedString.length) {
                val c = trimmedString[idx]
                if (c == '\\' && trimmedString[idx + 1] == '"') idx++
                else if (c == '"') break
                idx++
            }
            if (idx >= trimmedString.length) throw JsonParsingException("No value in JSON string '$string' for key")
            val splitIdx = trimmedString.substring(idx + 1).indexOfFirst { it == ':' } + idx + 1
            return Pair(
                trimmedString.substring(0, splitIdx).trimEnd().removeSurrounding("\""),
                trimmedString.substring(splitIdx + 1).trimStart().removeSurrounding("\"")
            )
        }

    }

}