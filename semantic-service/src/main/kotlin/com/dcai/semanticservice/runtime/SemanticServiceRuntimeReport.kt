package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.actions.OntologyActionAuditInspectionResult
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.connectors.RecordedConnectorSimulationReport
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerationReport
import com.dcai.semanticservice.contracts.ContractValidationReport
import com.dcai.semanticservice.dynamic.DynamicPlaybackResult
import com.dcai.semanticservice.fixtures.FixtureLoadSummary
import com.dcai.semanticservice.graph.GraphConnectionCheck
import com.dcai.semanticservice.governance.AiGovernanceProposalResult
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionResult
import com.dcai.semanticservice.promotion.GraphPromotionResult
import com.dcai.semanticservice.query.QueryExecutionReport
import com.dcai.semanticservice.query.QueryResultEnvelope
import com.dcai.semanticservice.reasoning.ReasoningPromotionResult
import java.nio.file.Path

internal fun skippedReasoningRefreshAfterSourceFailure(
    sourcePromotionResult: GraphPromotionResult,
): ReasoningPromotionResult {
    val message = "Reasoning refresh skipped because source promotion failed."
    return ReasoningPromotionResult(
        promoted = false,
        validation = com.dcai.semanticservice.reasoning.ReasoningValidationReport(
            conforms = false,
            tripleCount = 0,
            errors = listOf(message),
        ),
        errors = listOf(message) + sourcePromotionResult.errors,
    )
}

data class SemanticServiceRuntimeReport(
    val repoRoot: Path,
    val contractValidation: ContractValidationReport,
    val graphConnectionCheck: GraphConnectionCheck? = null,
    val fixtureLoadSummary: FixtureLoadSummary? = null,
    val queryExecutionReport: QueryExecutionReport? = null,
    val queryResultEnvelope: QueryResultEnvelope? = null,
    val sourcePromotionResult: GraphPromotionResult? = null,
    val reasoningPromotionResult: ReasoningPromotionResult? = null,
    val lifecycleInspectionResult: GraphLifecycleInspectionResult? = null,
    val recordedConnectorReport: RecordedConnectorSimulationReport? = null,
    val generatedScenarioReport: RecordedSourceScenarioGenerationReport? = null,
    val ontologyActionAuditResult: OntologyActionAuditResult? = null,
    val actionAuditInspectionResult: OntologyActionAuditInspectionResult? = null,
    val aiGovernanceProposalResult: AiGovernanceProposalResult? = null,
    val dynamicPlaybackResult: DynamicPlaybackResult? = null,
) {
    val mode: String = "contract-validation-runtime"
    val isReady: Boolean = contractValidation.isValid &&
        (graphConnectionCheck == null || graphConnectionCheck.reachable) &&
        (fixtureLoadSummary == null || fixtureLoadSummary.succeeded) &&
        (sourcePromotionResult == null || sourcePromotionResult.promoted) &&
        (reasoningPromotionResult == null || reasoningPromotionResult.promoted) &&
        (lifecycleInspectionResult == null || lifecycleInspectionResult.inspected) &&
        (ontologyActionAuditResult == null || ontologyActionAuditResult.audited) &&
        (actionAuditInspectionResult == null || actionAuditInspectionResult.inspected) &&
        (aiGovernanceProposalResult == null || aiGovernanceProposalResult.proposed) &&
        (dynamicPlaybackResult == null || dynamicPlaybackResult.played)
    val status: String = if (isReady) "ready" else "blocked"
    val graphExecutionEnabled: Boolean = sourcePromotionResult != null ||
        reasoningPromotionResult != null ||
        ontologyActionAuditResult != null ||
        aiGovernanceProposalResult != null ||
        dynamicPlaybackResult != null
    val httpEndpointsEnabled: Boolean = false
    val fixtureLoadingEnabled: Boolean = fixtureLoadSummary != null
    val queryExecutionEnabled: Boolean = queryExecutionReport != null
    val sourcePromotionEnabled: Boolean = sourcePromotionResult != null
    val reasoningRefreshEnabled: Boolean = reasoningPromotionResult != null
    val graphLifecycleInspectionEnabled: Boolean = lifecycleInspectionResult != null
    val sourceScenarioGenerationEnabled: Boolean = generatedScenarioReport != null
    val ontologyActionAuditEnabled: Boolean = ontologyActionAuditResult != null
    val actionAuditInspectionEnabled: Boolean = actionAuditInspectionResult != null
    val aiGovernanceProposalEnabled: Boolean = aiGovernanceProposalResult != null
    val dynamicPlaybackEnabled: Boolean = dynamicPlaybackResult != null
}
