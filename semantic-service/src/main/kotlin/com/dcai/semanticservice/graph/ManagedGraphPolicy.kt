package com.dcai.semanticservice.graph

enum class ManagedGraphKind(val prefix: String) {
    FIXTURE("urn:dcai:graph:fixture:"),
    SOURCE("urn:dcai:graph:source:"),
    CANONICAL("urn:dcai:graph:canonical:"),
    PROVENANCE("urn:dcai:graph:provenance:"),
    REASONING_AUDIT("urn:dcai:graph:reasoning-audit:"),
    REASONING("urn:dcai:graph:reasoning:"),
    ACTION_AUDIT("urn:dcai:graph:action-audit:"),
    AI_AUDIT("urn:dcai:graph:ai-audit:"),
    ;

    companion object {
        fun fromUri(uri: String): ManagedGraphKind? = entries.firstOrNull { kind -> uri.startsWith(kind.prefix) }
    }
}

@JvmInline
value class ManagedGraphUri private constructor(val value: String) {
    val kind: ManagedGraphKind
        get() = requireNotNull(ManagedGraphKind.fromUri(value))

    companion object {
        fun parse(value: String): ManagedGraphUri {
            val kind = ManagedGraphKind.fromUri(value)
            require(kind != null && value.length > kind.prefix.length) { "Only controlled DCAI graph URIs are allowed" }
            ControlledIdentifier.requireRelease(value.removePrefix(kind.prefix), "graph URI suffix")
            return ManagedGraphUri(value)
        }

        fun of(kind: ManagedGraphKind, id: String, label: String): ManagedGraphUri {
            return ManagedGraphUri(kind.prefix + ControlledIdentifier.requireRelease(id, label))
        }

        fun requireKind(value: String, kind: ManagedGraphKind, label: String): ManagedGraphUri {
            val uri = parse(value)
            require(uri.kind == kind) { "$label must use ${kind.prefix}" }
            return uri
        }
    }

    override fun toString(): String = value
}

object ControlledIdentifier {
    private val RELEASE_ID = Regex("[A-Za-z0-9._-]+")
    private val LOCAL_ID = Regex("[A-Za-z0-9._:-]+")

    fun requireRelease(value: String, label: String): String {
        require(RELEASE_ID.matches(value)) { "$label must contain only letters, numbers, dot, underscore, or hyphen" }
        return value
    }

    fun isLocal(value: String): Boolean = LOCAL_ID.matches(value)

    fun requireLocal(value: String, label: String): String {
        require(isLocal(value)) { "$label must use the controlled local identifier vocabulary" }
        return value
    }
}
