package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.connectors.RecordedSourceScenarioProfile
import com.dcai.semanticservice.dynamic.LocalDynamicPlaybackScenario
import com.dcai.semanticservice.ingestion.LocalControlledSourceExtract

object SemanticServiceCliParser {
    fun parse(args: Array<String>): SemanticServiceRuntimeOptions {
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
