package com.dcai.semanticservice.runtime

object SemanticServiceReportPrinter {
    fun print(
        report: SemanticServiceRuntimeReport,
        emit: (String) -> Unit = ::println,
    ) {
        emit("DCAI Semantic Service")
        emit("mode=${report.mode}")
        emit("repoRoot=${report.repoRoot}")
        emit("status=${report.status}")
        emit("checkedContracts=${report.contractValidation.checkedArtifacts.size}")
        emit("graphExecutionEnabled=${report.graphExecutionEnabled}")
        emit("httpEndpointsEnabled=${report.httpEndpointsEnabled}")
        emit("fixtureLoadingEnabled=${report.fixtureLoadingEnabled}")
        emit("queryExecutionEnabled=${report.queryExecutionEnabled}")
        emit("sourcePromotionEnabled=${report.sourcePromotionEnabled}")
        emit("reasoningRefreshEnabled=${report.reasoningRefreshEnabled}")
        emit("graphLifecycleInspectionEnabled=${report.graphLifecycleInspectionEnabled}")
        emit("sourceScenarioGenerationEnabled=${report.sourceScenarioGenerationEnabled}")
        emit("ontologyActionAuditEnabled=${report.ontologyActionAuditEnabled}")
        emit("actionAuditInspectionEnabled=${report.actionAuditInspectionEnabled}")
        emit("aiGovernanceProposalEnabled=${report.aiGovernanceProposalEnabled}")
        emit("dynamicPlaybackEnabled=${report.dynamicPlaybackEnabled}")
        report.graphConnectionCheck?.let { check ->
            emit("graphReachable=${check.reachable}")
            emit("graphDatasetUrl=${check.datasetUrl}")
            emit("graphQueryEndpointUrl=${check.queryEndpointUrl}")
            emit("namedGraphCount=${check.namedGraphCount ?: "unknown"}")
            emit("graphMessage=${check.message}")
        }
        report.fixtureLoadSummary?.let { summary ->
            emit("fixtureLoadSucceeded=${summary.succeeded}")
            emit("fixtureLoadAttempted=${summary.attemptedCount}")
            emit("fixtureGraphsPromoted=${summary.promotedCount}")
        }
        report.queryExecutionReport?.let { queryReport ->
            emit("queryId=${queryReport.queryId}")
            emit("queryMode=${queryReport.mode.value}")
            emit("queryRows=${queryReport.rowCount}")
            queryReport.askResult?.let { result -> emit("queryAskResult=$result") }
        }
        report.queryResultEnvelope?.let { envelope ->
            emit("queryResultType=${envelope.resultType.value}")
            emit("queryResultRecords=${envelope.recordCount}")
            emit("queryResultContract=${envelope.provenance.contractVersion}")
        }
        report.sourcePromotionResult?.let { result ->
            emit("sourcePromotionSucceeded=${result.promoted}")
            emit("sourcePromotionWrittenGraphs=${result.writtenGraphUris.size}")
            result.releaseManifest?.let { manifest -> emit("sourcePromotionRelease=${manifest.releaseId}") }
        }
        report.generatedScenarioReport?.let { generationReport ->
            emit("sourceScenarioProfile=${generationReport.profile.value}")
            emit("sourceScenarioSeed=${generationReport.seed}")
            emit("sourceScenarioBatch=${generationReport.batchId}")
            emit("sourceScenarioOutputDirectory=${generationReport.outputDirectory}")
            emit("sourceScenarioCount=${generationReport.scenarioCount}")
            emit("sourceScenarioRows=${generationReport.totalRows}")
            emit("sourceScenarioInvalidIncidentRows=${generationReport.invalidIncidentRows}")
            emit("sourceScenarioDuplicateWorkflowRows=${generationReport.duplicateWorkflowRows}")
        }
        report.recordedConnectorReport?.let { connectorReport ->
            emit("recordedConnectorBatch=${connectorReport.batchId}")
            emit("recordedConnectorSourceSystem=${connectorReport.sourceSystemId}")
            connectorReport.connectorContractId?.let { emit("recordedConnectorContract=$it") }
            connectorReport.connectorContractVersion?.let { emit("recordedConnectorContractVersion=$it") }
            connectorReport.scenarioProfile?.let { emit("recordedConnectorScenarioProfile=$it") }
            connectorReport.scenarioSeed?.let { emit("recordedConnectorScenarioSeed=$it") }
            emit("recordedConnectorTotalRows=${connectorReport.totalRows}")
            emit("recordedConnectorAcceptedRows=${connectorReport.acceptedRows}")
            emit("recordedConnectorRejectedRows=${connectorReport.rejectedRowCount}")
            emit("recordedConnectorBatchHistory=${connectorReport.batchHistoryEntry}")
        }
        report.reasoningPromotionResult?.let { result ->
            emit("reasoningRefreshSucceeded=${result.promoted}")
            emit("reasoningFindingCount=${result.findingCount}")
            emit("reasoningWrittenGraphs=${result.writtenGraphUris.size}")
            result.releaseManifest?.let { manifest -> emit("reasoningRun=${manifest.runId}") }
        }
        report.lifecycleInspectionResult?.let { result ->
            emit("lifecycleInspectionSucceeded=${result.inspected}")
            emit("lifecycleRelease=${result.releaseId}")
            emit("lifecycleStatus=${result.lifecycleStatus}")
            emit("lifecycleReasoningStatus=${result.reasoningStatus}")
            result.sourceGraph?.let { emit("lifecycleSourceGraphExists=${it.exists}") }
            result.canonicalGraph?.let { graph ->
                emit("lifecycleCanonicalGraphExists=${graph.exists}")
                emit("lifecycleCanonicalIncidents=${graph.incidentCount}")
                emit("lifecycleCanonicalAssets=${graph.assetCount}")
                emit("lifecycleCanonicalDependencies=${graph.dependencyEdgeCount}")
            }
            result.provenanceGraph?.let { graph ->
                emit("lifecycleProvenanceGraphExists=${graph.exists}")
                emit("lifecycleSourceRecords=${graph.sourceRecordCount}")
                emit("lifecyclePromotionActivities=${graph.promotionActivityCount}")
                emit("lifecycleGeneratedFacts=${graph.generatedFactCount}")
            }
            result.reasoningGraph?.let { graph ->
                emit("lifecycleReasoningGraphExists=${graph.exists}")
                emit("lifecycleReasoningActivities=${graph.reasoningActivityCount}")
                emit("lifecycleReasoningFindings=${graph.findingCount}")
                emit("lifecycleRestoreReadinessFindings=${graph.restoreReadinessFindingCount}")
                emit("lifecycleTrustFindings=${graph.trustFindingCount}")
            }
        }
        report.ontologyActionAuditResult?.let { result ->
            emit("ontologyActionAuditSucceeded=${result.audited}")
            emit("ontologyActionAuditGraph=${result.actionAuditGraphUri}")
            emit("ontologyActionAuditWrittenGraphs=${result.writtenGraphUris.size}")
            emit("ontologyActionAuditIdempotentReplay=${result.idempotentReplay}")
        }
        report.actionAuditInspectionResult?.let { result ->
            emit("actionAuditInspectionSucceeded=${result.inspected}")
            emit("actionAuditRelease=${result.actionAuditReleaseId}")
            emit("actionAuditGraph=${result.actionAuditGraphUri}")
            emit("actionAuditGraphExists=${result.exists}")
            emit("actionAuditExecutions=${result.executionCount}")
            emit("actionAuditRequests=${result.requestCount}")
            emit("actionAuditValidationReports=${result.validationReportCount}")
            emit("actionAuditIdempotencyKeys=${result.idempotencyKeyCount}")
            result.latestGeneratedAt?.let { emit("actionAuditLatestGeneratedAt=$it") }
            result.actionTypeCounts.toSortedMap().forEach { (actionType, count) ->
                emit("actionAuditTypeCount.$actionType=$count")
            }
        }
        report.aiGovernanceProposalResult?.let { result ->
            emit("aiGovernanceProposalSucceeded=${result.proposed}")
            emit("aiGovernanceProposalGraph=${result.aiAuditGraphUri}")
            emit("aiGovernanceProposalWrittenGraphs=${result.writtenGraphUris.size}")
            emit("aiGovernanceProposalIdempotentReplay=${result.idempotentReplay}")
            emit("aiGovernanceCanonicalGraphMutation=false")
            emit("aiGovernanceReasoningGraphMutation=false")
            emit("aiGovernanceOperationsGraphMutation=false")
            emit("aiGovernanceExternalSystemMutation=false")
        }
        report.dynamicPlaybackResult?.let { result ->
            emit("dynamicPlaybackSucceeded=${result.played}")
            emit("dynamicPlaybackScenario=${result.scenarioId}")
            emit("dynamicPlaybackBatch=${result.playbackBatchId}")
            emit("dynamicPlaybackActionAuditGraph=${result.actionAuditGraphUri}")
            emit("dynamicPlaybackSteps=${result.stepResults.size}")
            emit("dynamicPlaybackWrittenGraphs=${result.writtenGraphUris.size}")
        }

        if (!report.isReady) {
            report.contractValidation.errors.forEach { error -> emit("error=$error") }
            report.graphConnectionCheck
                ?.takeUnless { it.reachable }
                ?.let { emit("error=${it.message}") }
            report.fixtureLoadSummary?.errors?.forEach { error -> emit("error=$error") }
            report.sourcePromotionResult?.errors?.forEach { error -> emit("error=$error") }
            report.reasoningPromotionResult?.errors?.forEach { error -> emit("error=$error") }
            report.lifecycleInspectionResult?.errors?.forEach { error -> emit("error=$error") }
            report.ontologyActionAuditResult?.errors?.forEach { error -> emit("error=$error") }
            report.actionAuditInspectionResult?.errors?.forEach { error -> emit("error=$error") }
            report.aiGovernanceProposalResult?.errors?.forEach { error -> emit("error=$error") }
            report.dynamicPlaybackResult?.errors?.forEach { error -> emit("error=$error") }
        }
    }
}
