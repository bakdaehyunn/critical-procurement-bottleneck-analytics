package com.dcai.semanticservice.governance

import com.dcai.semanticservice.graph.ManagedGraphKind
import com.dcai.semanticservice.graph.ManagedGraphUri
import java.time.Instant

enum class AiProposalType(val id: String) {
    REASONING_FINDING_SUGGESTION("REASONING_FINDING_SUGGESTION"),
    ACTION_RECOMMENDATION("ACTION_RECOMMENDATION"),
    EVIDENCE_SUMMARY("EVIDENCE_SUMMARY"),
    ;

    companion object {
        fun fromId(id: String): AiProposalType? = entries.firstOrNull { it.id == id }
    }
}

enum class AiProposalRiskLevel(val id: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL"),
    ;

    companion object {
        fun fromId(id: String): AiProposalRiskLevel? = entries.firstOrNull { it.id == id }
    }
}

data class AiGovernanceProposalRequest(
    val proposalId: String,
    val proposalType: AiProposalType,
    val batchId: String,
    val idempotencyKey: String,
    val actorId: String,
    val generatedAt: Instant,
    val incidentUri: String,
    val sourceRecordUri: String,
    val supportingEvidenceUri: String,
    val targetObjectUri: String,
    val summary: String,
    val rationale: String,
    val confidenceScore: Double,
    val riskLevel: AiProposalRiskLevel,
    val modelId: String,
    val promptId: String,
    val promptHash: String,
) {
    init {
        require(proposalId.isNotBlank()) { "proposalId must not be blank" }
        require(batchId.isNotBlank()) { "batchId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(actorId.isNotBlank()) { "actorId must not be blank" }
        require(incidentUri.isNotBlank()) { "incidentUri must not be blank" }
        require(sourceRecordUri.isNotBlank()) { "sourceRecordUri must not be blank" }
        require(supportingEvidenceUri.isNotBlank()) { "supportingEvidenceUri must not be blank" }
        require(targetObjectUri.isNotBlank()) { "targetObjectUri must not be blank" }
        require(summary.isNotBlank()) { "summary must not be blank" }
        require(rationale.isNotBlank()) { "rationale must not be blank" }
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(promptId.isNotBlank()) { "promptId must not be blank" }
        require(promptHash.isNotBlank()) { "promptHash must not be blank" }
    }
}

data class AiGovernanceGraphUris(
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
    val reasoningGraphUri: String?,
    val aiAuditGraphUri: String,
) {
    init {
        ManagedGraphUri.requireKind(canonicalGraphUri, ManagedGraphKind.CANONICAL, "canonicalGraphUri")
        ManagedGraphUri.requireKind(provenanceGraphUri, ManagedGraphKind.PROVENANCE, "provenanceGraphUri")
        reasoningGraphUri?.let { ManagedGraphUri.requireKind(it, ManagedGraphKind.REASONING, "reasoningGraphUri") }
        ManagedGraphUri.requireKind(aiAuditGraphUri, ManagedGraphKind.AI_AUDIT, "aiAuditGraphUri")
    }

    companion object {
        fun forRelease(
            sourceReleaseId: String,
            reasoningRunId: String?,
            aiAuditReleaseId: String,
        ): AiGovernanceGraphUris {
            return AiGovernanceGraphUris(
                canonicalGraphUri = ManagedGraphUri.of(ManagedGraphKind.CANONICAL, sourceReleaseId, "sourceReleaseId").value,
                provenanceGraphUri = ManagedGraphUri.of(ManagedGraphKind.PROVENANCE, sourceReleaseId, "sourceReleaseId").value,
                reasoningGraphUri = reasoningRunId?.let { ManagedGraphUri.of(ManagedGraphKind.REASONING, it, "reasoningRunId").value },
                aiAuditGraphUri = ManagedGraphUri.of(ManagedGraphKind.AI_AUDIT, aiAuditReleaseId, "aiAuditReleaseId").value,
            )
        }
    }
}

data class AiGovernanceProposalPlan(
    val request: AiGovernanceProposalRequest,
    val graphs: AiGovernanceGraphUris,
)

data class AiGovernanceValidationReport(
    val conforms: Boolean,
    val tripleCount: Int = 0,
    val errors: List<String> = emptyList(),
)

data class AiGovernanceProposalResult(
    val proposed: Boolean,
    val validation: AiGovernanceValidationReport,
    val aiAuditGraphUri: String,
    val writtenGraphUris: List<String> = emptyList(),
    val idempotentReplay: Boolean = false,
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)
