package com.dcai.semanticservice.governance

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
        require(canonicalGraphUri.startsWith(CANONICAL_PREFIX) && canonicalGraphUri.length > CANONICAL_PREFIX.length) {
            "canonicalGraphUri must use $CANONICAL_PREFIX"
        }
        require(provenanceGraphUri.startsWith(PROVENANCE_PREFIX) && provenanceGraphUri.length > PROVENANCE_PREFIX.length) {
            "provenanceGraphUri must use $PROVENANCE_PREFIX"
        }
        require(reasoningGraphUri == null || reasoningGraphUri.startsWith(REASONING_PREFIX) && reasoningGraphUri.length > REASONING_PREFIX.length) {
            "reasoningGraphUri must use $REASONING_PREFIX"
        }
        require(aiAuditGraphUri.startsWith(AI_AUDIT_PREFIX) && aiAuditGraphUri.length > AI_AUDIT_PREFIX.length) {
            "aiAuditGraphUri must use $AI_AUDIT_PREFIX"
        }
    }

    companion object {
        const val CANONICAL_PREFIX = "urn:dcai:graph:canonical:"
        const val PROVENANCE_PREFIX = "urn:dcai:graph:provenance:"
        const val REASONING_PREFIX = "urn:dcai:graph:reasoning:"
        const val AI_AUDIT_PREFIX = "urn:dcai:graph:ai-audit:"

        fun forRelease(
            sourceReleaseId: String,
            reasoningRunId: String?,
            aiAuditReleaseId: String,
        ): AiGovernanceGraphUris {
            requireControlledId(sourceReleaseId, "sourceReleaseId")
            reasoningRunId?.let { requireControlledId(it, "reasoningRunId") }
            requireControlledId(aiAuditReleaseId, "aiAuditReleaseId")
            return AiGovernanceGraphUris(
                canonicalGraphUri = "$CANONICAL_PREFIX$sourceReleaseId",
                provenanceGraphUri = "$PROVENANCE_PREFIX$sourceReleaseId",
                reasoningGraphUri = reasoningRunId?.let { "$REASONING_PREFIX$it" },
                aiAuditGraphUri = "$AI_AUDIT_PREFIX$aiAuditReleaseId",
            )
        }

        private fun requireControlledId(value: String, label: String) {
            require(value.matches(Regex("[A-Za-z0-9._-]+"))) {
                "$label must contain only letters, numbers, dot, underscore, or hyphen"
            }
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
