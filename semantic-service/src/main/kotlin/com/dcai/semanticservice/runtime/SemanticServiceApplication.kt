package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.api.PrivateSemanticQueryEndpointServer
import com.dcai.semanticservice.api.PrivateSemanticQueryEndpointServerConfig
import com.dcai.semanticservice.actions.OntologyActionAuditInspectionPlan
import com.dcai.semanticservice.actions.OntologyActionAuditInspectionResult
import com.dcai.semanticservice.actions.OntologyActionAuditInspector
import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.actions.OntologyActionAuditService
import com.dcai.semanticservice.actions.OntologyActionGraphUris
import com.dcai.semanticservice.actions.OntologyActionPreconditionValidator
import com.dcai.semanticservice.actions.OntologyActionRequest
import com.dcai.semanticservice.actions.OntologyActionRequestLoader
import com.dcai.semanticservice.actions.OntologyActionRdfMapper
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.actions.OntologyActionValidationGate
import com.dcai.semanticservice.connectors.RecordedConnectorSimulationReport
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerationReport
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerationRequest
import com.dcai.semanticservice.connectors.RecordedSourceScenarioGenerator
import com.dcai.semanticservice.connectors.RecordedSourceScenarioProfile
import com.dcai.semanticservice.connectors.RecordedSourceConnectorSimulationLoader
import com.dcai.semanticservice.contracts.ContractValidationReport
import com.dcai.semanticservice.contracts.StaticContractValidator
import com.dcai.semanticservice.dynamic.DynamicPlaybackPlan
import com.dcai.semanticservice.dynamic.DynamicPlaybackResult
import com.dcai.semanticservice.dynamic.DynamicPlaybackRunner
import com.dcai.semanticservice.dynamic.DynamicPlaybackRdfMapper
import com.dcai.semanticservice.dynamic.DynamicPlaybackService
import com.dcai.semanticservice.dynamic.DynamicPlaybackValidationGate
import com.dcai.semanticservice.dynamic.LocalDynamicPlaybackScenario
import com.dcai.semanticservice.fixtures.ControlledFixtureGraphLoader
import com.dcai.semanticservice.fixtures.FixtureGraphLoadPlan
import com.dcai.semanticservice.fixtures.FixtureGraphLoader
import com.dcai.semanticservice.fixtures.FixtureLoadSummary
import com.dcai.semanticservice.fixtures.FixtureValidationGate
import com.dcai.semanticservice.graph.FusekiGraphStoreConfig
import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import com.dcai.semanticservice.graph.FusekiNamedGraphWriter
import com.dcai.semanticservice.graph.GraphConnectionCheck
import com.dcai.semanticservice.graph.JenaFusekiReadOnlyGraphClient
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.graph.ReadOnlyGraphClient
import com.dcai.semanticservice.governance.AiGovernanceGraphUris
import com.dcai.semanticservice.governance.AiGovernanceProposalLoader
import com.dcai.semanticservice.governance.AiGovernanceProposalPlan
import com.dcai.semanticservice.governance.AiGovernanceProposalPreconditionValidator
import com.dcai.semanticservice.governance.AiGovernanceProposalRdfMapper
import com.dcai.semanticservice.governance.AiGovernanceProposalRequest
import com.dcai.semanticservice.governance.AiGovernanceProposalResult
import com.dcai.semanticservice.governance.AiGovernanceProposalService
import com.dcai.semanticservice.governance.AiGovernanceProposalSubmitter
import com.dcai.semanticservice.governance.AiGovernanceProposalValidationGate
import com.dcai.semanticservice.ingestion.FileSourceExtractLoader
import com.dcai.semanticservice.ingestion.LocalControlledSourceExtract
import com.dcai.semanticservice.ingestion.SourceExtractBatch
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionPlan
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionResult
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspector
import com.dcai.semanticservice.promotion.GraphPromotionResult
import com.dcai.semanticservice.promotion.GraphPromotionService
import com.dcai.semanticservice.promotion.ProductionGraphPromotionPlan
import com.dcai.semanticservice.promotion.ProductionGraphUris
import com.dcai.semanticservice.promotion.ProductionGraphValidationGate
import com.dcai.semanticservice.promotion.SourceGraphPromoter
import com.dcai.semanticservice.query.ApprovedQueryCatalog
import com.dcai.semanticservice.query.JenaFusekiReadOnlyQueryExecutor
import com.dcai.semanticservice.query.QueryExecutionReport
import com.dcai.semanticservice.query.QueryResultEnvelope
import com.dcai.semanticservice.query.QueryResultShaper
import com.dcai.semanticservice.query.ReadOnlyQueryExecutor
import com.dcai.semanticservice.reasoning.ReasoningInputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.reasoning.ReasoningOutputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningPromotionPlan
import com.dcai.semanticservice.reasoning.ReasoningPromotionResult
import com.dcai.semanticservice.reasoning.ReasoningPromotionService
import com.dcai.semanticservice.reasoning.ReasoningRefresher
import com.dcai.semanticservice.reasoning.ReasoningValidationGate
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.system.exitProcess

object SemanticServiceApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        val options = SemanticServiceRuntimeOptions.fromArgs(args)
        val repoRoot = options.repoRoot?.let { Path.of(it).toAbsolutePath().normalize() }
            ?: locateRepoRoot()
        if (options.servePrivateQueryEndpoint) {
            val server = PrivateSemanticQueryEndpointServer
                .fromRepoRoot(
                    repoRoot = repoRoot,
                    config = PrivateSemanticQueryEndpointServerConfig(
                        host = options.privateEndpointHost,
                        port = options.privateEndpointPort,
                    ),
                )
                .start()
            println("DCAI Semantic Service")
            println("mode=private-semantic-query-endpoint")
            println("repoRoot=$repoRoot")
            println("privateEndpointUrl=http://${server.address.hostString}:${server.address.port}/semantic/query/{queryId}")
            println("publicEndpointExposure=false")
            Thread.currentThread().join()
            return
        }
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
        val queryExecutor = approvedQueryManifest?.let { manifest ->
            JenaFusekiReadOnlyQueryExecutor(
                manifest = manifest,
                config = FusekiReadOnlyConfig.fromEnvironment(),
            )
        }
        val queryResultShaper = approvedQueryManifest?.let { manifest ->
            QueryResultShaper(manifest)
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
        val report = run(
            repoRoot = repoRoot,
            graphClient = graphClient,
            fixtureLoader = fixtureLoader,
            queryExecutor = queryExecutor,
            queryId = options.queryId,
            queryResultShaper = queryResultShaper,
            sourcePromoter = sourcePromoter,
            sourcePromotionPlan = sourcePromotionPlan,
            reasoningRefresher = reasoningRefresher,
            reasoningPromotionPlan = reasoningPromotionPlan,
            lifecycleInspector = lifecycleInspector,
            lifecycleInspectionPlan = lifecycleInspectionPlan,
            recordedConnectorReport = sourceExtractInput?.recordedConnectorReport,
            generatedScenarioReport = generatedScenarioReport,
            ontologyActionSubmitter = ontologyActionSubmitter,
            ontologyActionAuditPlan = ontologyActionAuditPlan,
            actionAuditInspector = actionAuditInspector,
            actionAuditInspectionPlan = actionAuditInspectionPlan,
            aiProposalSubmitter = aiProposalSubmitter,
            aiProposalPlan = aiProposalPlan,
            dynamicPlaybackRunner = dynamicPlaybackRunner,
            dynamicPlaybackPlan = dynamicPlaybackPlan,
        )

        println("DCAI Semantic Service")
        println("mode=${report.mode}")
        println("repoRoot=${report.repoRoot}")
        println("status=${report.status}")
        println("checkedContracts=${report.contractValidation.checkedArtifacts.size}")
        println("graphExecutionEnabled=${report.graphExecutionEnabled}")
        println("httpEndpointsEnabled=${report.httpEndpointsEnabled}")
        println("fixtureLoadingEnabled=${report.fixtureLoadingEnabled}")
        println("queryExecutionEnabled=${report.queryExecutionEnabled}")
        println("sourcePromotionEnabled=${report.sourcePromotionEnabled}")
        println("reasoningRefreshEnabled=${report.reasoningRefreshEnabled}")
        println("graphLifecycleInspectionEnabled=${report.graphLifecycleInspectionEnabled}")
        println("sourceScenarioGenerationEnabled=${report.sourceScenarioGenerationEnabled}")
        println("ontologyActionAuditEnabled=${report.ontologyActionAuditEnabled}")
        println("actionAuditInspectionEnabled=${report.actionAuditInspectionEnabled}")
        println("aiGovernanceProposalEnabled=${report.aiGovernanceProposalEnabled}")
        println("dynamicPlaybackEnabled=${report.dynamicPlaybackEnabled}")
        report.graphConnectionCheck?.let { check ->
            println("graphReachable=${check.reachable}")
            println("graphDatasetUrl=${check.datasetUrl}")
            println("graphQueryEndpointUrl=${check.queryEndpointUrl}")
            println("namedGraphCount=${check.namedGraphCount ?: "unknown"}")
            println("graphMessage=${check.message}")
        }
        report.fixtureLoadSummary?.let { summary ->
            println("fixtureLoadSucceeded=${summary.succeeded}")
            println("fixtureLoadAttempted=${summary.attemptedCount}")
            println("fixtureGraphsPromoted=${summary.promotedCount}")
        }
        report.queryExecutionReport?.let { queryReport ->
            println("queryId=${queryReport.queryId}")
            println("queryMode=${queryReport.mode.value}")
            println("queryRows=${queryReport.rowCount}")
            queryReport.askResult?.let { result -> println("queryAskResult=$result") }
        }
        report.queryResultEnvelope?.let { envelope ->
            println("queryResultType=${envelope.resultType.value}")
            println("queryResultRecords=${envelope.recordCount}")
            println("queryResultContract=${envelope.provenance.contractVersion}")
        }
        report.sourcePromotionResult?.let { result ->
            println("sourcePromotionSucceeded=${result.promoted}")
            println("sourcePromotionWrittenGraphs=${result.writtenGraphUris.size}")
            result.releaseManifest?.let { manifest -> println("sourcePromotionRelease=${manifest.releaseId}") }
        }
        report.generatedScenarioReport?.let { generationReport ->
            println("sourceScenarioProfile=${generationReport.profile.value}")
            println("sourceScenarioSeed=${generationReport.seed}")
            println("sourceScenarioBatch=${generationReport.batchId}")
            println("sourceScenarioOutputDirectory=${generationReport.outputDirectory}")
            println("sourceScenarioCount=${generationReport.scenarioCount}")
            println("sourceScenarioRows=${generationReport.totalRows}")
            println("sourceScenarioInvalidIncidentRows=${generationReport.invalidIncidentRows}")
            println("sourceScenarioDuplicateWorkflowRows=${generationReport.duplicateWorkflowRows}")
        }
        report.recordedConnectorReport?.let { connectorReport ->
            println("recordedConnectorBatch=${connectorReport.batchId}")
            println("recordedConnectorSourceSystem=${connectorReport.sourceSystemId}")
            connectorReport.connectorContractId?.let { println("recordedConnectorContract=$it") }
            connectorReport.connectorContractVersion?.let { println("recordedConnectorContractVersion=$it") }
            connectorReport.scenarioProfile?.let { println("recordedConnectorScenarioProfile=$it") }
            connectorReport.scenarioSeed?.let { println("recordedConnectorScenarioSeed=$it") }
            println("recordedConnectorTotalRows=${connectorReport.totalRows}")
            println("recordedConnectorAcceptedRows=${connectorReport.acceptedRows}")
            println("recordedConnectorRejectedRows=${connectorReport.rejectedRowCount}")
            println("recordedConnectorBatchHistory=${connectorReport.batchHistoryEntry}")
        }
        report.reasoningPromotionResult?.let { result ->
            println("reasoningRefreshSucceeded=${result.promoted}")
            println("reasoningFindingCount=${result.findingCount}")
            println("reasoningWrittenGraphs=${result.writtenGraphUris.size}")
            result.releaseManifest?.let { manifest -> println("reasoningRun=${manifest.runId}") }
        }
        report.lifecycleInspectionResult?.let { result ->
            println("lifecycleInspectionSucceeded=${result.inspected}")
            println("lifecycleRelease=${result.releaseId}")
            println("lifecycleStatus=${result.lifecycleStatus}")
            println("lifecycleReasoningStatus=${result.reasoningStatus}")
            result.sourceGraph?.let { println("lifecycleSourceGraphExists=${it.exists}") }
            result.canonicalGraph?.let { graph ->
                println("lifecycleCanonicalGraphExists=${graph.exists}")
                println("lifecycleCanonicalIncidents=${graph.incidentCount}")
                println("lifecycleCanonicalAssets=${graph.assetCount}")
                println("lifecycleCanonicalDependencies=${graph.dependencyEdgeCount}")
            }
            result.provenanceGraph?.let { graph ->
                println("lifecycleProvenanceGraphExists=${graph.exists}")
                println("lifecycleSourceRecords=${graph.sourceRecordCount}")
                println("lifecyclePromotionActivities=${graph.promotionActivityCount}")
                println("lifecycleGeneratedFacts=${graph.generatedFactCount}")
            }
            result.reasoningGraph?.let { graph ->
                println("lifecycleReasoningGraphExists=${graph.exists}")
                println("lifecycleReasoningActivities=${graph.reasoningActivityCount}")
                println("lifecycleReasoningFindings=${graph.findingCount}")
                println("lifecycleRestoreReadinessFindings=${graph.restoreReadinessFindingCount}")
                println("lifecycleTrustFindings=${graph.trustFindingCount}")
            }
        }
        report.ontologyActionAuditResult?.let { result ->
            println("ontologyActionAuditSucceeded=${result.audited}")
            println("ontologyActionAuditGraph=${result.actionAuditGraphUri}")
            println("ontologyActionAuditWrittenGraphs=${result.writtenGraphUris.size}")
            println("ontologyActionAuditIdempotentReplay=${result.idempotentReplay}")
        }
        report.actionAuditInspectionResult?.let { result ->
            println("actionAuditInspectionSucceeded=${result.inspected}")
            println("actionAuditRelease=${result.actionAuditReleaseId}")
            println("actionAuditGraph=${result.actionAuditGraphUri}")
            println("actionAuditGraphExists=${result.exists}")
            println("actionAuditExecutions=${result.executionCount}")
            println("actionAuditRequests=${result.requestCount}")
            println("actionAuditValidationReports=${result.validationReportCount}")
            println("actionAuditIdempotencyKeys=${result.idempotencyKeyCount}")
            result.latestGeneratedAt?.let { println("actionAuditLatestGeneratedAt=$it") }
            result.actionTypeCounts.toSortedMap().forEach { (actionType, count) ->
                println("actionAuditTypeCount.$actionType=$count")
            }
        }
        report.aiGovernanceProposalResult?.let { result ->
            println("aiGovernanceProposalSucceeded=${result.proposed}")
            println("aiGovernanceProposalGraph=${result.aiAuditGraphUri}")
            println("aiGovernanceProposalWrittenGraphs=${result.writtenGraphUris.size}")
            println("aiGovernanceProposalIdempotentReplay=${result.idempotentReplay}")
            println("aiGovernanceCanonicalGraphMutation=false")
            println("aiGovernanceReasoningGraphMutation=false")
            println("aiGovernanceOperationsGraphMutation=false")
            println("aiGovernanceExternalSystemMutation=false")
        }
        report.dynamicPlaybackResult?.let { result ->
            println("dynamicPlaybackSucceeded=${result.played}")
            println("dynamicPlaybackScenario=${result.scenarioId}")
            println("dynamicPlaybackBatch=${result.playbackBatchId}")
            println("dynamicPlaybackActionAuditGraph=${result.actionAuditGraphUri}")
            println("dynamicPlaybackSteps=${result.stepResults.size}")
            println("dynamicPlaybackWrittenGraphs=${result.writtenGraphUris.size}")
        }

        if (!report.isReady) {
            report.contractValidation.errors.forEach { error -> println("error=$error") }
            report.graphConnectionCheck
                ?.takeUnless { it.reachable }
                ?.let { println("error=${it.message}") }
            report.fixtureLoadSummary?.errors?.forEach { error -> println("error=$error") }
            report.sourcePromotionResult?.errors?.forEach { error -> println("error=$error") }
            report.reasoningPromotionResult?.errors?.forEach { error -> println("error=$error") }
            report.lifecycleInspectionResult?.errors?.forEach { error -> println("error=$error") }
            report.ontologyActionAuditResult?.errors?.forEach { error -> println("error=$error") }
            report.actionAuditInspectionResult?.errors?.forEach { error -> println("error=$error") }
            report.aiGovernanceProposalResult?.errors?.forEach { error -> println("error=$error") }
            report.dynamicPlaybackResult?.errors?.forEach { error -> println("error=$error") }
            exitProcess(1)
        }
    }

    fun run(
        repoRoot: Path = locateRepoRoot(),
        graphClient: ReadOnlyGraphClient? = null,
        fixtureLoader: FixtureGraphLoader? = null,
        fixtureLoadPlan: FixtureGraphLoadPlan? = null,
        queryExecutor: ReadOnlyQueryExecutor? = null,
        queryId: String? = null,
        queryResultShaper: QueryResultShaper? = null,
        sourcePromoter: SourceGraphPromoter? = null,
        sourcePromotionPlan: ProductionGraphPromotionPlan? = null,
        reasoningRefresher: ReasoningRefresher? = null,
        reasoningPromotionPlan: ReasoningPromotionPlan? = null,
        lifecycleInspector: GraphLifecycleInspector? = null,
        lifecycleInspectionPlan: GraphLifecycleInspectionPlan? = null,
        recordedConnectorReport: RecordedConnectorSimulationReport? = null,
        generatedScenarioReport: RecordedSourceScenarioGenerationReport? = null,
        ontologyActionSubmitter: OntologyActionSubmitter? = null,
        ontologyActionAuditPlan: OntologyActionAuditPlan? = null,
        actionAuditInspector: OntologyActionAuditInspector? = null,
        actionAuditInspectionPlan: OntologyActionAuditInspectionPlan? = null,
        aiProposalSubmitter: AiGovernanceProposalSubmitter? = null,
        aiProposalPlan: AiGovernanceProposalPlan? = null,
        dynamicPlaybackRunner: DynamicPlaybackRunner? = null,
        dynamicPlaybackPlan: DynamicPlaybackPlan? = null,
    ): SemanticServiceRuntimeReport {
        val validation = StaticContractValidator().validate(repoRoot)
        val graphConnectionCheck = graphClient?.checkConnectivity()
        val fixtureLoadSummary = fixtureLoader?.load(
            fixtureLoadPlan ?: FixtureGraphLoadPlan.default(repoRoot),
        )
        val queryExecutionReport = queryId?.let { id ->
            requireNotNull(queryExecutor) { "queryExecutor is required when queryId is provided" }
                .execute(id)
        }
        val queryResultEnvelope = queryExecutionReport?.let { report ->
            requireNotNull(queryResultShaper) { "queryResultShaper is required when query execution is enabled" }
                .shape(report)
        }
        val sourcePromotionResult = sourcePromotionPlan?.let { plan ->
            requireNotNull(sourcePromoter) { "sourcePromoter is required when sourcePromotionPlan is provided" }
                .promote(plan)
        }
        val reasoningPromotionResult = reasoningPromotionPlan?.let { plan ->
            if (sourcePromotionResult != null && !sourcePromotionResult.promoted) {
                skippedReasoningRefreshAfterSourceFailure(sourcePromotionResult)
            } else {
                requireNotNull(reasoningRefresher) { "reasoningRefresher is required when reasoningPromotionPlan is provided" }
                    .run(plan)
            }
        }
        val lifecycleInspectionResult = lifecycleInspectionPlan?.let { plan ->
            requireNotNull(lifecycleInspector) { "lifecycleInspector is required when lifecycleInspectionPlan is provided" }
                .inspect(plan)
        }
        val ontologyActionAuditResult = ontologyActionAuditPlan?.let { plan ->
            requireNotNull(ontologyActionSubmitter) { "ontologyActionSubmitter is required when ontologyActionAuditPlan is provided" }
                .submit(plan)
        }
        val actionAuditInspectionResult = actionAuditInspectionPlan?.let { plan ->
            requireNotNull(actionAuditInspector) { "actionAuditInspector is required when actionAuditInspectionPlan is provided" }
                .inspect(plan)
        }
        val aiGovernanceProposalResult = aiProposalPlan?.let { plan ->
            requireNotNull(aiProposalSubmitter) { "aiProposalSubmitter is required when aiProposalPlan is provided" }
                .submit(plan)
        }
        val dynamicPlaybackResult = dynamicPlaybackPlan?.let { plan ->
            requireNotNull(dynamicPlaybackRunner) { "dynamicPlaybackRunner is required when dynamicPlaybackPlan is provided" }
                .run(plan)
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
            recordedConnectorReport = recordedConnectorReport,
            generatedScenarioReport = generatedScenarioReport,
            ontologyActionAuditResult = ontologyActionAuditResult,
            actionAuditInspectionResult = actionAuditInspectionResult,
            aiGovernanceProposalResult = aiGovernanceProposalResult,
            dynamicPlaybackResult = dynamicPlaybackResult,
        )
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
}

data class SourceExtractInput(
    val batch: SourceExtractBatch,
    val recordedConnectorReport: RecordedConnectorSimulationReport? = null,
)

private val DEFAULT_REASONING_GENERATED_AT: Instant = Instant.parse("2026-06-09T01:00:00Z")

private fun skippedReasoningRefreshAfterSourceFailure(
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
        fun fromArgs(args: Array<String>): SemanticServiceRuntimeOptions {
            var repoRoot: String? = null
            var checkGraph = false
            var loadFixtures = false
            var queryId: String? = null
            var promoteSource = false
            var sourceReleaseId = LocalControlledSourceExtract.DEFAULT_RELEASE_ID
            var sourceExtractFile: String? = null
            var sourceExtractDirectory: String? = null
            var generateSourceScenarios = false
            var generatedSourceProfile = RecordedSourceScenarioProfile.DEMO.value
            var generatedSourceSeed = 20260610
            var generatedSourceOutputDirectory: String? = null
            var refreshReasoning = false
            var reasoningInputReleaseId: String? = null
            var reasoningRunId = "local-controlled-reasoning-v1"
            var inspectGraphLifecycle = false
            var inspectReleaseId: String? = null
            var inspectReasoningRunId: String? = null
            var submitOntologyAction = false
            var actionRequestFile: String? = null
            var actionInputReleaseId: String? = null
            var actionReasoningRunId: String? = null
            var actionAuditReleaseId = "local-action-audit-v1"
            var submitAiProposal = false
            var aiProposalFile: String? = null
            var aiInputReleaseId: String? = null
            var aiReasoningRunId: String? = null
            var aiAuditReleaseId = "local-ai-governance-v1"
            var inspectActionAudit = false
            var inspectActionAuditReleaseId: String? = null
            var runDynamicPlayback = false
            var dynamicPlaybackScenarioId = LocalDynamicPlaybackScenario.DEFAULT_SCENARIO_ID
            var dynamicPlaybackBatchId = LocalDynamicPlaybackScenario.DEFAULT_PLAYBACK_BATCH_ID
            var dynamicPlaybackActionAuditReleaseId = LocalDynamicPlaybackScenario.DEFAULT_ACTION_AUDIT_RELEASE_ID
            var servePrivateQueryEndpoint = false
            var privateEndpointHost = "127.0.0.1"
            var privateEndpointPort = 18080

            for (arg in args) {
                when {
                    arg == "--check-graph" -> checkGraph = true
                    arg == "--load-fixtures" -> loadFixtures = true
                    arg == "--promote-source" -> promoteSource = true
                    arg == "--generate-source-scenarios" -> generateSourceScenarios = true
                    arg == "--refresh-reasoning" -> refreshReasoning = true
                    arg == "--inspect-graph-lifecycle" -> inspectGraphLifecycle = true
                    arg == "--submit-ontology-action" -> submitOntologyAction = true
                    arg == "--submit-ai-proposal" -> submitAiProposal = true
                    arg == "--inspect-action-audit" -> inspectActionAudit = true
                    arg == "--run-dynamic-playback" -> runDynamicPlayback = true
                    arg == "--serve-private-query-endpoint" -> servePrivateQueryEndpoint = true
                    arg.startsWith("--source-extract-file=") -> {
                        sourceExtractFile = arg.substringAfter("=")
                        require(sourceExtractFile.isNotBlank()) { "--source-extract-file requires a value" }
                    }
                    arg.startsWith("--source-extract-directory=") -> {
                        sourceExtractDirectory = arg.substringAfter("=")
                        require(sourceExtractDirectory.isNotBlank()) { "--source-extract-directory requires a value" }
                    }
                    arg.startsWith("--generated-source-profile=") -> {
                        generatedSourceProfile = arg.substringAfter("=")
                        require(generatedSourceProfile.isNotBlank()) { "--generated-source-profile requires a value" }
                    }
                    arg.startsWith("--generated-source-seed=") -> {
                        generatedSourceSeed = arg.substringAfter("=").toInt()
                        require(generatedSourceSeed >= 0) { "--generated-source-seed must be non-negative" }
                    }
                    arg.startsWith("--generated-source-output-directory=") -> {
                        generatedSourceOutputDirectory = arg.substringAfter("=")
                        require(generatedSourceOutputDirectory.isNotBlank()) {
                            "--generated-source-output-directory requires a value"
                        }
                    }
                    arg.startsWith("--source-release-id=") -> {
                        sourceReleaseId = arg.substringAfter("=")
                        require(sourceReleaseId.isNotBlank()) { "--source-release-id requires a value" }
                    }
                    arg.startsWith("--inspect-release-id=") -> {
                        inspectReleaseId = arg.substringAfter("=")
                        require(inspectReleaseId.isNotBlank()) { "--inspect-release-id requires a value" }
                    }
                    arg.startsWith("--inspect-reasoning-run-id=") -> {
                        inspectReasoningRunId = arg.substringAfter("=")
                        require(inspectReasoningRunId.isNotBlank()) { "--inspect-reasoning-run-id requires a value" }
                    }
                    arg.startsWith("--action-request-file=") -> {
                        actionRequestFile = arg.substringAfter("=")
                        require(actionRequestFile.isNotBlank()) { "--action-request-file requires a value" }
                    }
                    arg.startsWith("--action-input-release-id=") -> {
                        actionInputReleaseId = arg.substringAfter("=")
                        require(actionInputReleaseId.isNotBlank()) { "--action-input-release-id requires a value" }
                    }
                    arg.startsWith("--action-reasoning-run-id=") -> {
                        actionReasoningRunId = arg.substringAfter("=")
                        require(actionReasoningRunId.isNotBlank()) { "--action-reasoning-run-id requires a value" }
                    }
                    arg.startsWith("--action-audit-release-id=") -> {
                        actionAuditReleaseId = arg.substringAfter("=")
                        require(actionAuditReleaseId.isNotBlank()) { "--action-audit-release-id requires a value" }
                    }
                    arg.startsWith("--ai-proposal-file=") -> {
                        aiProposalFile = arg.substringAfter("=")
                        require(aiProposalFile.isNotBlank()) { "--ai-proposal-file requires a value" }
                    }
                    arg.startsWith("--ai-input-release-id=") -> {
                        aiInputReleaseId = arg.substringAfter("=")
                        require(aiInputReleaseId.isNotBlank()) { "--ai-input-release-id requires a value" }
                    }
                    arg.startsWith("--ai-reasoning-run-id=") -> {
                        aiReasoningRunId = arg.substringAfter("=")
                        require(aiReasoningRunId.isNotBlank()) { "--ai-reasoning-run-id requires a value" }
                    }
                    arg.startsWith("--ai-audit-release-id=") -> {
                        aiAuditReleaseId = arg.substringAfter("=")
                        require(aiAuditReleaseId.isNotBlank()) { "--ai-audit-release-id requires a value" }
                    }
                    arg.startsWith("--inspect-action-audit-release-id=") -> {
                        inspectActionAuditReleaseId = arg.substringAfter("=")
                        require(inspectActionAuditReleaseId.isNotBlank()) {
                            "--inspect-action-audit-release-id requires a value"
                        }
                    }
                    arg.startsWith("--dynamic-playback-scenario-id=") -> {
                        dynamicPlaybackScenarioId = arg.substringAfter("=")
                        require(dynamicPlaybackScenarioId.isNotBlank()) {
                            "--dynamic-playback-scenario-id requires a value"
                        }
                    }
                    arg.startsWith("--dynamic-playback-batch-id=") -> {
                        dynamicPlaybackBatchId = arg.substringAfter("=")
                        require(dynamicPlaybackBatchId.isNotBlank()) {
                            "--dynamic-playback-batch-id requires a value"
                        }
                    }
                    arg.startsWith("--dynamic-playback-action-audit-release-id=") -> {
                        dynamicPlaybackActionAuditReleaseId = arg.substringAfter("=")
                        require(dynamicPlaybackActionAuditReleaseId.isNotBlank()) {
                            "--dynamic-playback-action-audit-release-id requires a value"
                        }
                    }
                    arg.startsWith("--reasoning-input-release-id=") -> {
                        reasoningInputReleaseId = arg.substringAfter("=")
                        require(reasoningInputReleaseId.isNotBlank()) { "--reasoning-input-release-id requires a value" }
                    }
                    arg.startsWith("--reasoning-run-id=") -> {
                        reasoningRunId = arg.substringAfter("=")
                        require(reasoningRunId.isNotBlank()) { "--reasoning-run-id requires a value" }
                    }
                    arg.startsWith("--private-endpoint-host=") -> {
                        privateEndpointHost = arg.substringAfter("=")
                        require(privateEndpointHost.isNotBlank()) { "--private-endpoint-host requires a value" }
                    }
                    arg.startsWith("--private-endpoint-port=") -> {
                        privateEndpointPort = arg.substringAfter("=").toInt()
                    }
                    arg.startsWith("--run-query=") -> {
                        queryId = arg.substringAfter("=")
                        require(queryId.isNotBlank()) { "--run-query requires a query id" }
                    }
                    arg.startsWith("--repo-root=") -> repoRoot = arg.substringAfter("=")
                    repoRoot == null -> repoRoot = arg
                    else -> error("Unknown argument: $arg")
                }
            }

            return SemanticServiceRuntimeOptions(
                repoRoot = repoRoot,
                checkGraph = checkGraph,
                loadFixtures = loadFixtures,
                queryId = queryId,
                promoteSource = promoteSource,
                sourceReleaseId = sourceReleaseId,
                sourceExtractFile = sourceExtractFile,
                sourceExtractDirectory = sourceExtractDirectory,
                generateSourceScenarios = generateSourceScenarios,
                generatedSourceProfile = generatedSourceProfile,
                generatedSourceSeed = generatedSourceSeed,
                generatedSourceOutputDirectory = generatedSourceOutputDirectory,
                refreshReasoning = refreshReasoning,
                reasoningInputReleaseId = reasoningInputReleaseId,
                reasoningRunId = reasoningRunId,
                inspectGraphLifecycle = inspectGraphLifecycle,
                inspectReleaseId = inspectReleaseId,
                inspectReasoningRunId = inspectReasoningRunId,
                submitOntologyAction = submitOntologyAction,
                actionRequestFile = actionRequestFile,
                actionInputReleaseId = actionInputReleaseId,
                actionReasoningRunId = actionReasoningRunId,
                actionAuditReleaseId = actionAuditReleaseId,
                submitAiProposal = submitAiProposal,
                aiProposalFile = aiProposalFile,
                aiInputReleaseId = aiInputReleaseId,
                aiReasoningRunId = aiReasoningRunId,
                aiAuditReleaseId = aiAuditReleaseId,
                inspectActionAudit = inspectActionAudit,
                inspectActionAuditReleaseId = inspectActionAuditReleaseId,
                runDynamicPlayback = runDynamicPlayback,
                dynamicPlaybackScenarioId = dynamicPlaybackScenarioId,
                dynamicPlaybackBatchId = dynamicPlaybackBatchId,
                dynamicPlaybackActionAuditReleaseId = dynamicPlaybackActionAuditReleaseId,
                servePrivateQueryEndpoint = servePrivateQueryEndpoint,
                privateEndpointHost = privateEndpointHost,
                privateEndpointPort = privateEndpointPort,
            )
        }
    }
}
