package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.connectors.RecordedSourceScenarioProfile
import com.dcai.semanticservice.dynamic.LocalDynamicPlaybackScenario
import com.dcai.semanticservice.ingestion.LocalControlledSourceExtract

data class SemanticServiceRuntimeOptions(
    val repoRoot: String? = null,
    val checkGraph: Boolean = false,
    val loadFixtures: Boolean = false,
    val queryId: String? = null,
    val promoteSource: Boolean = false,
    val sourceReleaseId: String = LocalControlledSourceExtract.DEFAULT_RELEASE_ID,
    val sourceExtractFile: String? = null,
    val sourceExtractDirectory: String? = null,
    val generateSourceScenarios: Boolean = false,
    val generatedSourceProfile: String = RecordedSourceScenarioProfile.DEMO.value,
    val generatedSourceSeed: Int = 20260610,
    val generatedSourceOutputDirectory: String? = null,
    val refreshReasoning: Boolean = false,
    val reasoningInputReleaseId: String? = null,
    val reasoningRunId: String = "local-controlled-reasoning-v1",
    val inspectGraphLifecycle: Boolean = false,
    val inspectReleaseId: String? = null,
    val inspectReasoningRunId: String? = null,
    val submitOntologyAction: Boolean = false,
    val actionRequestFile: String? = null,
    val actionInputReleaseId: String? = null,
    val actionReasoningRunId: String? = null,
    val actionAuditReleaseId: String = "local-action-audit-v1",
    val submitAiProposal: Boolean = false,
    val aiProposalFile: String? = null,
    val aiInputReleaseId: String? = null,
    val aiReasoningRunId: String? = null,
    val aiAuditReleaseId: String = "local-ai-governance-v1",
    val inspectActionAudit: Boolean = false,
    val inspectActionAuditReleaseId: String? = null,
    val runDynamicPlayback: Boolean = false,
    val dynamicPlaybackScenarioId: String = LocalDynamicPlaybackScenario.DEFAULT_SCENARIO_ID,
    val dynamicPlaybackBatchId: String = LocalDynamicPlaybackScenario.DEFAULT_PLAYBACK_BATCH_ID,
    val dynamicPlaybackActionAuditReleaseId: String = LocalDynamicPlaybackScenario.DEFAULT_ACTION_AUDIT_RELEASE_ID,
    val servePrivateQueryEndpoint: Boolean = false,
    val privateEndpointHost: String = "127.0.0.1",
    val privateEndpointPort: Int = 18080,
) {
    companion object {
        fun fromArgs(args: Array<String>): SemanticServiceRuntimeOptions = SemanticServiceCliParser.parse(args)
    }
}
