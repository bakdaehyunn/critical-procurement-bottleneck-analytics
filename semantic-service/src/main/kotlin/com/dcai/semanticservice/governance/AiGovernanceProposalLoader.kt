package com.dcai.semanticservice.governance

import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import kotlin.io.path.inputStream

class AiGovernanceProposalLoader {
    fun load(path: Path): AiGovernanceProposalRequest {
        val properties = Properties()
        path.inputStream().use(properties::load)
        require(properties.required("format") == FORMAT) {
            "Unsupported AI governance proposal format: ${properties.getProperty("format")}"
        }
        val proposalType = AiProposalType.fromId(properties.required("proposal.type"))
            ?: error("Unsupported AI proposal type: ${properties.required("proposal.type")}")
        val riskLevel = AiProposalRiskLevel.fromId(properties.required("riskLevel"))
            ?: error("Unsupported AI proposal risk level: ${properties.required("riskLevel")}")
        return AiGovernanceProposalRequest(
            proposalId = properties.required("proposal.id"),
            proposalType = proposalType,
            batchId = properties.required("batch.id"),
            idempotencyKey = properties.required("idempotencyKey"),
            actorId = properties.required("actor.id"),
            generatedAt = Instant.parse(properties.required("generatedAt")),
            incidentUri = properties.required("incidentUri"),
            sourceRecordUri = properties.required("sourceRecordUri"),
            supportingEvidenceUri = properties.required("supportingEvidenceUri"),
            targetObjectUri = properties.required("targetObjectUri"),
            summary = properties.required("summary"),
            rationale = properties.required("rationale"),
            confidenceScore = properties.required("confidenceScore").toDouble(),
            riskLevel = riskLevel,
            modelId = properties.required("model.id"),
            promptId = properties.required("prompt.id"),
            promptHash = properties.required("prompt.hash"),
        )
    }

    private fun Properties.required(key: String): String {
        return getProperty(key)?.trim()?.takeIf(String::isNotBlank)
            ?: error("Missing required AI governance proposal property: $key")
    }

    private companion object {
        private const val FORMAT = "dcai-ai-proposal-v1"
    }
}
