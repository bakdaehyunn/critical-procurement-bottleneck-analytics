package com.dcai.semanticservice.actions

import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import kotlin.io.path.inputStream

class OntologyActionRequestLoader {
    fun load(path: Path): OntologyActionRequest {
        val properties = Properties()
        path.inputStream().use(properties::load)
        require(properties.required("format") == FORMAT) {
            "Unsupported ontology action request format: ${properties.getProperty("format")}"
        }
        return OntologyActionRequest(
            requestId = properties.required("request.id"),
            actionType = OntologyActionType.fromId(properties.required("action.id"))
                ?: error("Unsupported ontology action id: ${properties.required("action.id")}"),
            idempotencyKey = properties.required("idempotencyKey"),
            actorId = properties.required("actor.id"),
            requestedAt = Instant.parse(properties.required("requestedAt")),
            incidentUri = properties.required("incidentUri"),
            actionReason = properties.required("actionReason"),
            sourceRecordUri = properties.required("sourceRecordUri"),
            restoreReadinessFindingUri = properties.optional("restoreReadinessFindingUri"),
            recoveryBlockerUri = properties.optional("recoveryBlockerUri"),
            trustFindingUri = properties.optional("trustFindingUri"),
            validationEvidenceUri = properties.optional("validationEvidenceUri"),
            assignedTeam = properties.optional("assignedTeam"),
            assigneeId = properties.optional("assigneeId"),
            reviewedStatus = properties.optional("reviewedStatus"),
            reviewSummary = properties.optional("reviewSummary"),
            supportingEvidenceUri = properties.optional("supportingEvidenceUri"),
        )
    }

    private fun Properties.required(key: String): String {
        return getProperty(key)?.trim()?.takeIf(String::isNotBlank)
            ?: error("Missing required ontology action request property: $key")
    }

    private fun Properties.optional(key: String): String? {
        return getProperty(key)?.trim()?.takeIf(String::isNotBlank)
    }

    private companion object {
        private const val FORMAT = "dcai-ontology-action-request-v1"
    }
}

