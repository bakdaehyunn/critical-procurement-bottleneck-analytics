package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.ManagedGraphKind
import com.dcai.semanticservice.graph.ManagedGraphUri
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

enum class OntologyActionLifecycleState(val id: String) {
    REQUESTED("REQUESTED"),
    VALIDATED("VALIDATED"),
    QUEUED("QUEUED"),
    IN_REVIEW("IN_REVIEW"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CLOSED("CLOSED"),
    ;

    companion object {
        fun fromId(id: String): OntologyActionLifecycleState? = entries.firstOrNull { it.id == id }
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
        ManagedGraphUri.requireKind(canonicalGraphUri, ManagedGraphKind.CANONICAL, "canonicalGraphUri")
        ManagedGraphUri.requireKind(provenanceGraphUri, ManagedGraphKind.PROVENANCE, "provenanceGraphUri")
        reasoningGraphUri?.let { ManagedGraphUri.requireKind(it, ManagedGraphKind.REASONING, "reasoningGraphUri") }
        ManagedGraphUri.requireKind(actionAuditGraphUri, ManagedGraphKind.ACTION_AUDIT, "actionAuditGraphUri")
    }

    companion object {
        fun forRelease(
            sourceReleaseId: String,
            reasoningRunId: String?,
            actionAuditReleaseId: String,
        ): OntologyActionGraphUris {
            return OntologyActionGraphUris(
                canonicalGraphUri = ManagedGraphUri.of(ManagedGraphKind.CANONICAL, sourceReleaseId, "sourceReleaseId").value,
                provenanceGraphUri = ManagedGraphUri.of(ManagedGraphKind.PROVENANCE, sourceReleaseId, "sourceReleaseId").value,
                reasoningGraphUri = reasoningRunId?.let { ManagedGraphUri.of(ManagedGraphKind.REASONING, it, "reasoningRunId").value },
                actionAuditGraphUri = ManagedGraphUri.of(ManagedGraphKind.ACTION_AUDIT, actionAuditReleaseId, "actionAuditReleaseId").value,
            )
        }
    }
}

data class OntologyActionAuditPlan(
    val request: OntologyActionRequest,
    val graphs: OntologyActionGraphUris,
)

data class OntologyActionTransitionRequest(
    val transitionId: String,
    val idempotencyKey: String,
    val actorId: String,
    val requestedAt: Instant,
    val targetExecutionUri: String,
    val toState: OntologyActionLifecycleState,
    val transitionReason: String,
) {
    init {
        require(transitionId.isNotBlank()) { "transitionId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(actorId.isNotBlank()) { "actorId must not be blank" }
        require(targetExecutionUri.isNotBlank()) { "targetExecutionUri must not be blank" }
        require(transitionReason.isNotBlank()) { "transitionReason must not be blank" }
    }
}

data class OntologyActionTransitionPlan(
    val request: OntologyActionTransitionRequest,
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

data class OntologyActionTransitionResult(
    val transitioned: Boolean,
    val validation: OntologyActionValidationReport,
    val actionAuditGraphUri: String,
    val currentState: OntologyActionLifecycleState? = null,
    val writtenGraphUris: List<String> = emptyList(),
    val idempotentReplay: Boolean = false,
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)
