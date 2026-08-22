package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.actions.OntologyActionAuditInspectionPlan
import com.dcai.semanticservice.actions.OntologyActionAuditInspector
import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditService
import com.dcai.semanticservice.actions.OntologyActionGraphUris
import com.dcai.semanticservice.actions.OntologyActionPreconditionValidator
import com.dcai.semanticservice.actions.OntologyActionRequest
import com.dcai.semanticservice.actions.OntologyActionRequestLoader
import com.dcai.semanticservice.actions.OntologyActionRdfMapper
import com.dcai.semanticservice.actions.OntologyActionValidationGate
import com.dcai.semanticservice.connectors.RecordedConnectorSimulationReport
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerationRequest
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerator
import com.dcai.semanticservice.connectors.RecordedSourceScenarioProfile
import com.dcai.semanticservice.connectors.RecordedSourceConnectorSimulationLoader
import com.dcai.semanticservice.dynamic.DynamicPlaybackPlan
import com.dcai.semanticservice.dynamic.DynamicPlaybackRdfMapper
import com.dcai.semanticservice.dynamic.DynamicPlaybackService
import com.dcai.semanticservice.dynamic.DynamicPlaybackValidationGate
import com.dcai.semanticservice.dynamic.LocalDynamicPlaybackScenario
import com.dcai.semanticservice.fixtures.ControlledFixtureGraphLoader
import com.dcai.semanticservice.fixtures.FixtureGraphLoadPlan
import com.dcai.semanticservice.fixtures.FixtureGraphLoader
import com.dcai.semanticservice.fixtures.FixtureValidationGate
import com.dcai.semanticservice.graph.FusekiGraphStoreConfig
import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import com.dcai.semanticservice.graph.FusekiNamedGraphWriter
import com.dcai.semanticservice.graph.JenaFusekiReadOnlyGraphClient
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.graph.ReadOnlyGraphClient
import com.dcai.semanticservice.governance.AiGovernanceGraphUris
import com.dcai.semanticservice.governance.AiGovernanceProposalLoader
import com.dcai.semanticservice.governance.AiGovernanceProposalPlan
import com.dcai.semanticservice.governance.AiGovernanceProposalPreconditionValidator
import com.dcai.semanticservice.governance.AiGovernanceProposalRdfMapper
import com.dcai.semanticservice.governance.AiGovernanceProposalRequest
import com.dcai.semanticservice.governance.AiGovernanceProposalService
import com.dcai.semanticservice.governance.AiGovernanceProposalValidationGate
import com.dcai.semanticservice.ingestion.FileSourceExtractLoader
import com.dcai.semanticservice.ingestion.LocalControlledSourceExtract
import com.dcai.semanticservice.ingestion.SourceExtractBatch
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionPlan
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspector
import com.dcai.semanticservice.promotion.GraphPromotionService
import com.dcai.semanticservice.promotion.ProductionGraphPromotionPlan
import com.dcai.semanticservice.promotion.ProductionGraphUris
import com.dcai.semanticservice.promotion.ProductionGraphValidationGate
import com.dcai.semanticservice.query.ApprovedQueryCatalog
import com.dcai.semanticservice.query.QueryContractRegistry
import com.dcai.semanticservice.query.JenaFusekiReadOnlyQueryExecutor
import com.dcai.semanticservice.query.QueryResultShaper
import com.dcai.semanticservice.query.ReadOnlyQueryExecutor
import com.dcai.semanticservice.reasoning.ReasoningInputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.reasoning.ReasoningOutputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningPromotionPlan
import com.dcai.semanticservice.reasoning.ReasoningPromotionService
import com.dcai.semanticservice.reasoning.ReasoningValidationGate
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists

object SemanticServiceComposition {
    fun execute(
        options: SemanticServiceRuntimeOptions,
        repoRoot: Path,
    ): SemanticServiceRuntimeReport {
        val graphClient = if (options.checkGraph) {
            JenaFusekiReadOnlyGraphClient(FusekiReadOnlyConfig.fromEnvironment())
        } else {
            null
        }
        val fixtureLoader = if (options.loadFixtures) {
            ControlledFixtureGraphLoader(
                validationGate = FixtureValidationGate(repoRoot),
                writer = FusekiNamedGraphWriter(FusekiGraphStoreConfig.fromEnvironment()),
            )
        } else {
            null
        }
        val approvedQueryManifest = options.queryId?.let {
            ApprovedQueryCatalog(repoRoot).load()
        }
        val queryContractRegistry = approvedQueryManifest?.let { manifest ->
            QueryContractRegistry.fromManifest(manifest, requireCompleteManifest = true)
        }
        val queryExecutor = queryContractRegistry?.let { registry ->
            JenaFusekiReadOnlyQueryExecutor(
                registry = registry,
                config = FusekiReadOnlyConfig.fromEnvironment(),
            )
        }
        val queryResultShaper = queryContractRegistry?.let { registry ->
            QueryResultShaper(registry)
        }
        val generatedScenarioReport = if (options.generateSourceScenarios) {
            val profile = RecordedSourceScenarioProfile.fromValue(options.generatedSourceProfile)
            RecordedSourceScenarioGenerator().generate(
                RecordedSourceScenarioGenerationRequest(
                    profile = profile,
                    seed = options.generatedSourceSeed,
                    outputDirectory = resolveControlledSourceExtractPath(
                        repoRoot = repoRoot,
                        sourceExtractPathArgument = options.generatedSourceOutputDirectory
                            ?: defaultGeneratedSourceScenarioDirectory(profile, options.generatedSourceSeed),
                    ),
                ),
            )
        } else {
            null
        }
        val graphStore: NamedGraphStore? = if (
            options.promoteSource ||
            options.refreshReasoning ||
            options.inspectGraphLifecycle ||
            options.submitOntologyAction ||
            options.submitAiProposal ||
            options.inspectActionAudit ||
            options.runDynamicPlayback
        ) {
            FusekiNamedGraphWriter(FusekiGraphStoreConfig.fromEnvironment())
        } else {
            null
        }
        val sourceExtractInput = if (options.promoteSource) {
            loadSourceExtractInput(
                repoRoot = repoRoot,
                sourceReleaseId = options.sourceReleaseId,
                sourceExtractFile = options.sourceExtractFile,
                sourceExtractDirectory = options.sourceExtractDirectory
                    ?: generatedScenarioReport?.outputDirectory?.toString(),
            )
        } else {
            null
        }
        val sourcePromotionPlan = sourceExtractInput?.let { input ->
            ProductionGraphPromotionPlan(
                batch = input.batch,
                graphs = ProductionGraphUris.forRelease(options.sourceReleaseId),
            )
        }
        val sourcePromoter = sourcePromotionPlan?.let {
            GraphPromotionService(
                mapper = SourceExtractRdfMapper(),
                validationGate = ProductionGraphValidationGate(repoRoot),
                graphStore = requireNotNull(graphStore),
            )
        }
        val reasoningInputReleaseId = options.reasoningInputReleaseId ?: options.sourceReleaseId
        val reasoningPromotionPlan = if (options.refreshReasoning) {
            ReasoningPromotionPlan(
                runId = options.reasoningRunId,
                generatedAt = DEFAULT_REASONING_GENERATED_AT,
                inputGraphs = ReasoningInputGraphUris.forRelease(reasoningInputReleaseId),
                outputGraphs = ReasoningOutputGraphUris.forRun(options.reasoningRunId),
            )
        } else {
            null
        }
        val reasoningRefresher = reasoningPromotionPlan?.let {
            ReasoningPromotionService(
                builder = ReasoningModelBuilder(),
                validationGate = ReasoningValidationGate(repoRoot),
                graphStore = requireNotNull(graphStore),
            )
        }
        val lifecycleInspectionPlan = if (options.inspectGraphLifecycle) {
            GraphLifecycleInspectionPlan(
                releaseId = options.inspectReleaseId ?: options.sourceReleaseId,
                reasoningRunId = options.inspectReasoningRunId ?: options.reasoningRunId,
            )
        } else {
            null
        }
        val lifecycleInspector = lifecycleInspectionPlan?.let {
            GraphLifecycleInspector(requireNotNull(graphStore))
        }
        val ontologyActionRequest = if (options.submitOntologyAction) {
            loadOntologyActionRequest(
                repoRoot = repoRoot,
                actionRequestFile = options.actionRequestFile,
            )
        } else {
            null
        }
        val ontologyActionAuditPlan = ontologyActionRequest?.let { request ->
            OntologyActionAuditPlan(
                request = request,
                graphs = OntologyActionGraphUris.forRelease(
                    sourceReleaseId = options.actionInputReleaseId ?: options.sourceReleaseId,
                    reasoningRunId = options.actionReasoningRunId ?: options.reasoningRunId,
                    actionAuditReleaseId = options.actionAuditReleaseId,
                ),
            )
        }
        val ontologyActionSubmitter = ontologyActionAuditPlan?.let {
            OntologyActionAuditService(
                mapper = OntologyActionRdfMapper(),
                preconditionValidator = OntologyActionPreconditionValidator(),
                validationGate = OntologyActionValidationGate(repoRoot),
                graphStore = requireNotNull(graphStore),
            )
        }
        val actionAuditInspectionPlan = if (options.inspectActionAudit) {
            OntologyActionAuditInspectionPlan(options.inspectActionAuditReleaseId ?: options.actionAuditReleaseId)
        } else {
            null
        }
        val actionAuditInspector = actionAuditInspectionPlan?.let {
            OntologyActionAuditInspector(requireNotNull(graphStore))
        }
        val aiProposalRequest = if (options.submitAiProposal) {
            loadAiGovernanceProposalRequest(
                repoRoot = repoRoot,
                aiProposalFile = options.aiProposalFile,
            )
        } else {
            null
        }
        val aiProposalPlan = aiProposalRequest?.let { request ->
            AiGovernanceProposalPlan(
                request = request,
                graphs = AiGovernanceGraphUris.forRelease(
                    sourceReleaseId = options.aiInputReleaseId ?: options.sourceReleaseId,
                    reasoningRunId = options.aiReasoningRunId ?: options.reasoningRunId,
                    aiAuditReleaseId = options.aiAuditReleaseId,
                ),
            )
        }
        val aiProposalSubmitter = aiProposalPlan?.let {
            AiGovernanceProposalService(
                mapper = AiGovernanceProposalRdfMapper(),
                preconditionValidator = AiGovernanceProposalPreconditionValidator(),
                validationGate = AiGovernanceProposalValidationGate(repoRoot),
                graphStore = requireNotNull(graphStore),
            )
        }
        val dynamicPlaybackPlan = if (options.runDynamicPlayback) {
            DynamicPlaybackPlan(
                scenario = LocalDynamicPlaybackScenario.scenario(
                    scenarioId = options.dynamicPlaybackScenarioId,
                    playbackBatchId = options.dynamicPlaybackBatchId,
                ),
                graphs = OntologyActionGraphUris.forRelease(
                    sourceReleaseId = options.dynamicPlaybackScenarioId,
                    reasoningRunId = "${options.dynamicPlaybackScenarioId}-reasoning-04",
                    actionAuditReleaseId = options.dynamicPlaybackActionAuditReleaseId,
                ),
            )
        } else {
            null
        }
        val dynamicPlaybackRunner = dynamicPlaybackPlan?.let {
            DynamicPlaybackService(
                sourcePromoter = GraphPromotionService(
                    mapper = SourceExtractRdfMapper(),
                    validationGate = ProductionGraphValidationGate(repoRoot),
                    graphStore = requireNotNull(graphStore),
                ),
                reasoningRefresher = ReasoningPromotionService(
                    builder = ReasoningModelBuilder(),
                    validationGate = ReasoningValidationGate(repoRoot),
                    graphStore = requireNotNull(graphStore),
                ),
                mapper = DynamicPlaybackRdfMapper(),
                validationGate = DynamicPlaybackValidationGate(repoRoot),
                graphStore = requireNotNull(graphStore),
            )
        }
        val operations = buildList {
            graphClient?.let { add(SemanticRuntimeOperation.CheckGraph(it)) }
            fixtureLoader?.let { add(SemanticRuntimeOperation.LoadFixtures(it, FixtureGraphLoadPlan.default(repoRoot))) }
            options.queryId?.let { queryId ->
                add(SemanticRuntimeOperation.ExecuteQuery(requireNotNull(queryExecutor), requireNotNull(queryResultShaper), queryId))
            }
            sourcePromotionPlan?.let { add(SemanticRuntimeOperation.PromoteSource(requireNotNull(sourcePromoter), it)) }
            reasoningPromotionPlan?.let { add(SemanticRuntimeOperation.RefreshReasoning(requireNotNull(reasoningRefresher), it)) }
            lifecycleInspectionPlan?.let { add(SemanticRuntimeOperation.InspectLifecycle(requireNotNull(lifecycleInspector), it)) }
            ontologyActionAuditPlan?.let { add(SemanticRuntimeOperation.SubmitOntologyAction(requireNotNull(ontologyActionSubmitter), it)) }
            actionAuditInspectionPlan?.let { add(SemanticRuntimeOperation.InspectActionAudit(requireNotNull(actionAuditInspector), it)) }
            aiProposalPlan?.let { add(SemanticRuntimeOperation.SubmitAiProposal(requireNotNull(aiProposalSubmitter), it)) }
            dynamicPlaybackPlan?.let { add(SemanticRuntimeOperation.RunDynamicPlayback(requireNotNull(dynamicPlaybackRunner), it)) }
        }
        val report = SemanticServiceWorkflow().run(
            repoRoot = repoRoot,
            operations = operations,
            inputs = SemanticRuntimeInputs(
                recordedConnectorReport = sourceExtractInput?.recordedConnectorReport,
                generatedScenarioReport = generatedScenarioReport,
            ),
        )
        return report
    }

    fun locateRepoRoot(start: Path = Path.of("").toAbsolutePath().normalize()): Path {
        var current = start
        while (current.parent != null) {
            if (
                current.resolve("semantic-service/openapi.semantic-service.yaml").exists() &&
                current.resolve("ontology/modules").exists() &&
                Files.isDirectory(current.resolve("semantic-service/src/main/kotlin"))
            ) {
                return current
            }
            current = current.parent
        }
        error("Unable to locate repository root from $start")
    }

    fun resolveControlledSourceExtractPath(repoRoot: Path, sourceExtractPathArgument: String): Path {
        val sourceExtractRoot = repoRoot.resolve("fixtures/source-extracts").toAbsolutePath().normalize()
        val sourceExtractPath = repoRoot.resolve(sourceExtractPathArgument).toAbsolutePath().normalize()
        require(sourceExtractPath.startsWith(sourceExtractRoot)) {
            "--source-extract-file and --source-extract-directory must resolve under fixtures/source-extracts"
        }
        return sourceExtractPath
    }

    fun resolveControlledActionRequestPath(repoRoot: Path, actionRequestPathArgument: String): Path {
        val actionRequestRoot = repoRoot.resolve("fixtures/action-requests").toAbsolutePath().normalize()
        val actionRequestPath = repoRoot.resolve(actionRequestPathArgument).toAbsolutePath().normalize()
        require(actionRequestPath.startsWith(actionRequestRoot)) {
            "--action-request-file must resolve under fixtures/action-requests"
        }
        return actionRequestPath
    }

    fun resolveControlledAiProposalPath(repoRoot: Path, aiProposalPathArgument: String): Path {
        val aiProposalRoot = repoRoot.resolve("fixtures/ai-proposals").toAbsolutePath().normalize()
        val aiProposalPath = repoRoot.resolve(aiProposalPathArgument).toAbsolutePath().normalize()
        require(aiProposalPath.startsWith(aiProposalRoot)) {
            "--ai-proposal-file must resolve under fixtures/ai-proposals"
        }
        return aiProposalPath
    }

    fun defaultGeneratedSourceScenarioDirectory(
        profile: RecordedSourceScenarioProfile,
        seed: Int,
    ): String {
        return "fixtures/source-extracts/generated-scenarios/${profile.value}-seed-$seed"
    }

    fun loadSourceExtractInput(
        repoRoot: Path,
        sourceReleaseId: String,
        sourceExtractFile: String?,
        sourceExtractDirectory: String?,
    ): SourceExtractInput {
        require(sourceExtractFile == null || sourceExtractDirectory == null) {
            "Use either --source-extract-file or --source-extract-directory, not both"
        }

        return when {
            sourceExtractFile != null -> {
                val batch = FileSourceExtractLoader().load(resolveControlledSourceExtractPath(repoRoot, sourceExtractFile))
                require(batch.batchId == sourceReleaseId) {
                    "--source-release-id must match source extract batch.id for file-backed promotion"
                }
                SourceExtractInput(batch = batch)
            }
            sourceExtractDirectory != null -> {
                val simulation = RecordedSourceConnectorSimulationLoader()
                    .load(resolveControlledSourceExtractPath(repoRoot, sourceExtractDirectory))
                require(simulation.batch.batchId == sourceReleaseId) {
                    "--source-release-id must match recorded connector batch.id for directory-backed promotion"
                }
                SourceExtractInput(
                    batch = simulation.batch,
                    recordedConnectorReport = simulation.report,
                )
            }
            else -> SourceExtractInput(batch = LocalControlledSourceExtract.batch(sourceReleaseId))
        }
    }

    fun loadSourceExtractBatch(
        repoRoot: Path,
        sourceReleaseId: String,
        sourceExtractFile: String?,
    ): SourceExtractBatch {
        return loadSourceExtractInput(
            repoRoot = repoRoot,
            sourceReleaseId = sourceReleaseId,
            sourceExtractFile = sourceExtractFile,
            sourceExtractDirectory = null,
        ).batch
    }

    fun loadOntologyActionRequest(
        repoRoot: Path,
        actionRequestFile: String?,
    ): OntologyActionRequest {
        val path = actionRequestFile
            ?: error("--action-request-file is required when --submit-ontology-action is set")
        return OntologyActionRequestLoader().load(resolveControlledActionRequestPath(repoRoot, path))
    }

    fun loadAiGovernanceProposalRequest(
        repoRoot: Path,
        aiProposalFile: String?,
    ): AiGovernanceProposalRequest {
        val path = aiProposalFile
            ?: error("--ai-proposal-file is required when --submit-ai-proposal is set")
        return AiGovernanceProposalLoader().load(resolveControlledAiProposalPath(repoRoot, path))
    }
data class SourceExtractInput(
    val batch: SourceExtractBatch,
    val recordedConnectorReport: RecordedConnectorSimulationReport? = null,
)

private val DEFAULT_REASONING_GENERATED_AT: Instant = Instant.parse("2026-06-09T01:00:00Z")
}
