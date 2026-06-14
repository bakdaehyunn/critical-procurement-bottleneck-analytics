package com.dcai.semanticservice.actions

import java.time.Instant

enum class OntologyActionType(val id: String) {
    ACKNOWLEDGE_RESTORE_BLOCKER("AcknowledgeRestoreBlocker"),
    ASSIGN_EVIDENCE_REVIEW("AssignEvidenceReview"),
    RECORD_VALIDATION_REVIEW("RecordValidationReview"),
    ;

    companion object {
        fun fromId(id: String): OntologyActionType? = entries.firstOrNull { it.id == id }
    }
}

data class OntologyActionRequest(
    val requestId: String,
    val actionType: OntologyActionType,
    val idempotencyKey: String,
    val actorId: String,
    val requestedAt: Instant,
    val incidentUri: String,
    val actionReason: String,
    val sourceRecordUri: String,
    val restoreReadinessFindingUri: String? = null,
    val recoveryBlockerUri: String? = null,
    val trustFindingUri: String? = null,
    val validationEvidenceUri: String? = null,
    val assignedTeam: String? = null,
    val assigneeId: String? = null,
    val reviewedStatus: String? = null,
    val reviewSummary: String? = null,
    val supportingEvidenceUri: String? = null,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(actorId.isNotBlank()) { "actorId must not be blank" }
        require(incidentUri.isNotBlank()) { "incidentUri must not be blank" }
        require(actionReason.isNotBlank()) { "actionReason must not be blank" }
        require(sourceRecordUri.isNotBlank()) { "sourceRecordUri must not be blank" }
    }
}

data class OntologyActionGraphUris(
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
    val reasoningGraphUri: String?,
    val actionAuditGraphUri: String,
) {
    init {
        require(canonicalGraphUri.startsWith(CANONICAL_PREFIX) && canonicalGraphUri.length > CANONICAL_PREFIX.length) {
            "canonicalGraphUri must use $CANONICAL_PREFIX"
        }
        require(provenanceGraphUri.startsWith(PROVENANCE_PREFIX) && provenanceGraphUri.length > PROVENANCE_PREFIX.length) {
            "provenanceGraphUri must use $PROVENANCE_PREFIX"
        }
        require(reasoningGraphUri == null || reasoningGraphUri.startsWith(REASONING_PREFIX) && reasoningGraphUri.length > REASONING_PREFIX.length) {
            "reasoningGraphUri must use $REASONING_PREFIX"
        }
        require(actionAuditGraphUri.startsWith(ACTION_AUDIT_PREFIX) && actionAuditGraphUri.length > ACTION_AUDIT_PREFIX.length) {
            "actionAuditGraphUri must use $ACTION_AUDIT_PREFIX"
        }
    }

    companion object {
        const val CANONICAL_PREFIX = "urn:dcai:graph:canonical:"
        const val PROVENANCE_PREFIX = "urn:dcai:graph:provenance:"
        const val REASONING_PREFIX = "urn:dcai:graph:reasoning:"
        const val ACTION_AUDIT_PREFIX = "urn:dcai:graph:action-audit:"

        fun forRelease(
            sourceReleaseId: String,
            reasoningRunId: String?,
            actionAuditReleaseId: String,
        ): OntologyActionGraphUris {
            requireControlledId(sourceReleaseId, "sourceReleaseId")
            reasoningRunId?.let { requireControlledId(it, "reasoningRunId") }
            requireControlledId(actionAuditReleaseId, "actionAuditReleaseId")
            return OntologyActionGraphUris(
                canonicalGraphUri = "$CANONICAL_PREFIX$sourceReleaseId",
                provenanceGraphUri = "$PROVENANCE_PREFIX$sourceReleaseId",
                reasoningGraphUri = reasoningRunId?.let { "$REASONING_PREFIX$it" },
                actionAuditGraphUri = "$ACTION_AUDIT_PREFIX$actionAuditReleaseId",
            )
        }

        private fun requireControlledId(value: String, label: String) {
            require(value.matches(Regex("[A-Za-z0-9._-]+"))) {
                "$label must contain only letters, numbers, dot, underscore, or hyphen"
            }
        }
    }
}

data class OntologyActionAuditPlan(
    val request: OntologyActionRequest,
    val graphs: OntologyActionGraphUris,
)

data class OntologyActionValidationReport(
    val conforms: Boolean,
    val tripleCount: Int = 0,
    val errors: List<String> = emptyList(),
)

data class OntologyActionAuditResult(
    val audited: Boolean,
    val validation: OntologyActionValidationReport,
    val actionAuditGraphUri: String,
    val writtenGraphUris: List<String> = emptyList(),
    val idempotentReplay: Boolean = false,
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)

