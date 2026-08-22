package com.dcai.semanticservice.api

object JsonPayloadWriter {
    fun write(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"${value.escapeJson()}\""
            is Number,
            is Boolean,
            -> value.toString()
            is Map<*, *> -> value.entries.joinToString(
                prefix = "{",
                postfix = "}",
            ) { (key, entryValue) ->
                "\"${key.toString().escapeJson()}\":${write(entryValue)}"
            }
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { item -> write(item) }
            else -> "\"${value.toString().escapeJson()}\""
        }
    }

    private fun String.escapeJson(): String {
        return buildString {
            for (char in this@escapeJson) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }
}
