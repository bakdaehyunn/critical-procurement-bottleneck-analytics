package com.dcai.semanticservice.api

import com.dcai.semanticservice.graph.ControlledIdentifier
internal object PrivateEndpointPayload {
    fun containsRawSparql(body: String): Boolean {
        val normalized = body.lowercase()
        return normalized.contains("\"sparql\"") ||
            normalized.contains("\"query\"") ||
            RAW_SPARQL_KEYWORD.containsMatchIn(body)
    }

    fun stringObject(
        body: String,
        allowedFields: Set<String>,
        bodyLabel: String,
        fieldLabel: String,
    ): PrivateStringPayload {
        require(body.trim().startsWith("{") && body.trim().endsWith("}")) {
            "$bodyLabel body must be a JSON object with string values."
        }
        val matches = FIELD_PAIR.findAll(body).toList()
        val unmatchedBody = matches.fold(body.trim().removePrefix("{").removeSuffix("}")) { remaining, match ->
            remaining.replace(match.value, "")
        }
        require(unmatchedBody.replace(",", "").isBlank()) {
            "$bodyLabel body must contain only string fields."
        }
        val values = matches.associate { match ->
            val key = match.groupValues[1]
            require(key in allowedFields) { "Unsupported $fieldLabel field: $key" }
            key to match.groupValues[2].unescapeJsonString()
        }
        return PrivateStringPayload(
            values = values,
            fieldLabel = fieldLabel,
        )
    }

    fun parameters(body: String): Map<String, String> {
        if (body.isBlank()) {
            return emptyMap()
        }
        val parameterBody = PARAMETERS_OBJECT.find(body)?.groupValues?.get(1) ?: return emptyMap()
        val matches = FIELD_PAIR.findAll(parameterBody).toList()
        val unmatchedBody = matches.fold(parameterBody) { remaining, match ->
            remaining.replace(match.value, "")
        }
        require(unmatchedBody.replace(",", "").isBlank()) {
            "Parameters must be a JSON object with string values and supported names."
        }
        return matches.associate { match ->
            val key = match.groupValues[1]
            require(PARAMETER_NAME.matches(key)) { "Unsupported query parameter name: $key" }
            key to match.groupValues[2].unescapeJsonString()
        }
    }

    private fun String.unescapeJsonString(): String {
        return replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private val PARAMETERS_OBJECT = Regex(
        pattern = "\"parameters\"\\s*:\\s*\\{([^}]*)}",
        options = setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val FIELD_PAIR = Regex("\"([A-Za-z][A-Za-z0-9_]*)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
    private val PARAMETER_NAME = Regex("[A-Za-z][A-Za-z0-9_]*")
    private val RAW_SPARQL_KEYWORD = Regex(
        pattern = "\\b(select|ask|construct|describe|insert|delete|update|where)\\b",
        options = setOf(RegexOption.IGNORE_CASE),
    )
}

internal data class PrivateStringPayload(
    private val values: Map<String, String>,
    private val fieldLabel: String,
) {
    operator fun get(key: String): String? = values[key]

    fun required(key: String): String {
        return values[key]?.trim()?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing required $fieldLabel field: $key")
    }

    fun requiredControlled(key: String): String {
        return required(key).also { value ->
            ControlledIdentifier.requireLocal(value, key)
        }
    }

    fun optionalControlled(key: String): String? {
        return values[key]?.trim()?.takeIf(String::isNotBlank)?.also { value ->
            ControlledIdentifier.requireLocal(value, key)
        }
    }

    fun requiredUri(key: String): String {
        return required(key).also { value ->
            require(value.startsWith("urn:dcai:")) { "$key must be a controlled DCAI URN" }
        }
    }

    fun optionalUri(key: String): String? {
        return values[key]?.trim()?.takeIf(String::isNotBlank)?.also { value ->
            require(value.startsWith("urn:dcai:")) { "$key must be a controlled DCAI URN" }
        }
    }

}
