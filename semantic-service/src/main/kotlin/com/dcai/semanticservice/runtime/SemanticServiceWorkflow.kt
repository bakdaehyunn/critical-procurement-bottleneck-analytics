package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.actions.OntologyActionAuditInspectionPlan
import com.dcai.semanticservice.actions.OntologyActionAuditInspector
import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.connectors.RecordedConnectorSimulationReport
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerationReport
import com.dcai.semanticservice.contracts.StaticContractValidator
import com.dcai.semanticservice.dynamic.DynamicPlaybackPlan
import com.dcai.semanticservice.dynamic.DynamicPlaybackRunner
import com.dcai.semanticservice.fixtures.FixtureGraphLoadPlan
import com.dcai.semanticservice.fixtures.FixtureGraphLoader
import com.dcai.semanticservice.graph.ReadOnlyGraphClient
import com.dcai.semanticservice.governance.AiGovernanceProposalPlan
import com.dcai.semanticservice.governance.AiGovernanceProposalSubmitter
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionPlan
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspector
import com.dcai.semanticservice.promotion.GraphPromotionResult
import com.dcai.semanticservice.promotion.ProductionGraphPromotionPlan
import com.dcai.semanticservice.promotion.SourceGraphPromoter
import com.dcai.semanticservice.query.QueryResultShaper
import com.dcai.semanticservice.query.ReadOnlyQueryExecutor
import com.dcai.semanticservice.reasoning.ReasoningPromotionPlan
import com.dcai.semanticservice.reasoning.ReasoningPromotionResult
import com.dcai.semanticservice.reasoning.ReasoningRefresher
import java.nio.file.Path

sealed interface SemanticRuntimeOperation {
    data class CheckGraph(val client: ReadOnlyGraphClient) : SemanticRuntimeOperation
    data class LoadFixtures(val loader: FixtureGraphLoader, val plan: FixtureGraphLoadPlan) : SemanticRuntimeOperation
    data class ExecuteQuery(val executor: ReadOnlyQueryExecutor, val shaper: QueryResultShaper, val queryId: String) : SemanticRuntimeOperation
    data class PromoteSource(val promoter: SourceGraphPromoter, val plan: ProductionGraphPromotionPlan) : SemanticRuntimeOperation
    data class RefreshReasoning(val refresher: ReasoningRefresher, val plan: ReasoningPromotionPlan) : SemanticRuntimeOperation
    data class InspectLifecycle(val inspector: GraphLifecycleInspector, val plan: GraphLifecycleInspectionPlan) : SemanticRuntimeOperation
    data class SubmitOntologyAction(val submitter: OntologyActionSubmitter, val plan: OntologyActionAuditPlan) : SemanticRuntimeOperation
    data class InspectActionAudit(val inspector: OntologyActionAuditInspector, val plan: OntologyActionAuditInspectionPlan) : SemanticRuntimeOperation
    data class SubmitAiProposal(val submitter: AiGovernanceProposalSubmitter, val plan: AiGovernanceProposalPlan) : SemanticRuntimeOperation
    data class RunDynamicPlayback(val runner: DynamicPlaybackRunner, val plan: DynamicPlaybackPlan) : SemanticRuntimeOperation
}

data class SemanticRuntimeInputs(
    val recordedConnectorReport: RecordedConnectorSimulationReport? = null,
    val generatedScenarioReport: RecordedSourceScenarioGenerationReport? = null,
)

class SemanticServiceWorkflow(
    private val contractValidator: StaticContractValidator = StaticContractValidator(),
) {
    fun run(
        repoRoot: Path,
        operations: List<SemanticRuntimeOperation>,
        inputs: SemanticRuntimeInputs = SemanticRuntimeInputs(),
    ): SemanticServiceRuntimeReport {
        val validation = contractValidator.validate(repoRoot)
        var graphConnectionCheck: com.dcai.semanticservice.graph.GraphConnectionCheck? = null
        var fixtureLoadSummary: com.dcai.semanticservice.fixtures.FixtureLoadSummary? = null
        var queryExecutionReport: com.dcai.semanticservice.query.QueryExecutionReport? = null
        var queryResultEnvelope: com.dcai.semanticservice.query.QueryResultEnvelope? = null
        var sourcePromotionResult: GraphPromotionResult? = null
        var reasoningPromotionResult: ReasoningPromotionResult? = null
        var lifecycleInspectionResult: com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionResult? = null
        var ontologyActionAuditResult: com.dcai.semanticservice.actions.OntologyActionAuditResult? = null
        var actionAuditInspectionResult: com.dcai.semanticservice.actions.OntologyActionAuditInspectionResult? = null
        var aiGovernanceProposalResult: com.dcai.semanticservice.governance.AiGovernanceProposalResult? = null
        var dynamicPlaybackResult: com.dcai.semanticservice.dynamic.DynamicPlaybackResult? = null

        operations.forEach { operation ->
            when (operation) {
                is SemanticRuntimeOperation.CheckGraph -> graphConnectionCheck = operation.client.checkConnectivity()
                is SemanticRuntimeOperation.LoadFixtures -> fixtureLoadSummary = operation.loader.load(operation.plan)
                is SemanticRuntimeOperation.ExecuteQuery -> {
                    queryExecutionReport = operation.executor.execute(operation.queryId)
                    queryResultEnvelope = operation.shaper.shape(requireNotNull(queryExecutionReport))
                }
                is SemanticRuntimeOperation.PromoteSource -> sourcePromotionResult = operation.promoter.promote(operation.plan)
                is SemanticRuntimeOperation.RefreshReasoning -> {
                    reasoningPromotionResult = if (sourcePromotionResult?.promoted == false) {
                        skippedReasoningRefreshAfterSourceFailure(requireNotNull(sourcePromotionResult))
                    } else {
                        operation.refresher.run(operation.plan)
                    }
                }
                is SemanticRuntimeOperation.InspectLifecycle -> lifecycleInspectionResult = operation.inspector.inspect(operation.plan)
                is SemanticRuntimeOperation.SubmitOntologyAction -> ontologyActionAuditResult = operation.submitter.submit(operation.plan)
                is SemanticRuntimeOperation.InspectActionAudit -> actionAuditInspectionResult = operation.inspector.inspect(operation.plan)
                is SemanticRuntimeOperation.SubmitAiProposal -> aiGovernanceProposalResult = operation.submitter.submit(operation.plan)
                is SemanticRuntimeOperation.RunDynamicPlayback -> dynamicPlaybackResult = operation.runner.run(operation.plan)
            }
        }

        return SemanticServiceRuntimeReport(
            repoRoot = repoRoot,
            contractValidation = validation,
            graphConnectionCheck = graphConnectionCheck,
            fixtureLoadSummary = fixtureLoadSummary,
            queryExecutionReport = queryExecutionReport,
            queryResultEnvelope = queryResultEnvelope,
            sourcePromotionResult = sourcePromotionResult,
            reasoningPromotionResult = reasoningPromotionResult,
            lifecycleInspectionResult = lifecycleInspectionResult,
            recordedConnectorReport = inputs.recordedConnectorReport,
            generatedScenarioReport = inputs.generatedScenarioReport,
            ontologyActionAuditResult = ontologyActionAuditResult,
            actionAuditInspectionResult = actionAuditInspectionResult,
            aiGovernanceProposalResult = aiGovernanceProposalResult,
            dynamicPlaybackResult = dynamicPlaybackResult,
        )
    }
}
