package com.dcai.semanticservice.query

class QueryResultShaper(
    private val manifest: ApprovedQueryManifest,
) {
    fun shape(report: QueryExecutionReport): QueryResultEnvelope {
        val definition = manifest.requireQuery(report.queryId)
        require(report.mode == definition.mode) {
            "Query result mode mismatch for ${report.queryId}: report=${report.mode.value}, manifest=${definition.mode.value}"
        }
        require(report.mode == QueryMode.SELECT) {
            "Query result envelopes are only defined for SELECT results: ${report.queryId}"
        }
        return when (report.queryId) {
            "fixtureNamedGraphInventory" -> shapeNamedGraphInventory(report, definition)
            "fixtureIncidentSummary" -> shapeIncidentSummary(report, definition)
            "fixtureProvenanceSourceRecords" -> shapeProvenanceSourceRecords(report, definition)
            "semanticFollowUpQueueList" -> shapeFollowUpQueue(report, definition)
            "semanticDashboardOverview" -> shapeDashboardOverview(report, definition)
            "semanticPlatformStatus" -> shapePlatformStatus(report, definition)
            "semanticFilterMetadata" -> shapeFilterMetadata(report, definition)
            "semanticFollowUpDetail" -> shapeFollowUpDetail(report, definition)
            "semanticImpactSummary" -> shapeImpactSummary(report, definition)
            "semanticTopologyDependencies" -> shapeTopologyDependencies(report, definition)
            "semanticTrustFindingList" -> shapeTrustFindings(report, definition)
            "semanticStageBottlenecks" -> shapeStageBottlenecks(report, definition)
            "semanticAssetDelaySummary" -> shapeAssetDelaySummary(report, definition)
            "semanticZoneDelaySummary" -> shapeZoneDelaySummary(report, definition)
            "semanticSpareWaitSummary" -> shapeSpareWaitSummary(report, definition)
            "semanticValidationSummary" -> shapeValidationSummary(report, definition)
            "semanticIncidentEvidence" -> shapeIncidentEvidence(report, definition)
            "semanticIncidentTimeline" -> shapeIncidentTimeline(report, definition)
            "semanticDependencyImpactByAsset" -> shapeDependencyImpact(report, definition)
            "semanticBlastRadiusByAsset" -> shapeBlastRadius(report, definition)
            "semanticPromotionReviewQueue",
            "semanticReasoningReviewQueue",
            -> shapeOntologyReviewQueue(report, definition)
            "semanticAvailableActionsByFinding" -> shapeActionAvailability(report, definition)
            "semanticActionAuditHistoryByRelease",
            "semanticActionAuditHistoryByIncident",
            "semanticActionAuditHistoryByTarget",
            -> shapeActionAuditHistory(report, definition)
            "semanticActionNotificationQueueByIncident" -> shapeActionNotificationQueue(report, definition)
            "semanticActionReviewQueueByIncident" -> shapeActionReviewQueue(report, definition)
            "semanticActionTransitionHistoryByIncident" -> shapeActionTransitionHistory(report, definition)
            "semanticActionDispatchQueueByIncident" -> shapeActionDispatchQueue(report, definition)
            "semanticDynamicEventTimelineByIncident",
            "semanticDynamicStateChangesByIncident",
            "semanticDynamicReasoningChangesByIncident",
            "semanticDynamicActionLifecycleByIncident",
            -> shapeDynamicPlayback(report, definition)
            "semanticAiProposalReviewQueue" -> shapeAiProposalReviewQueue(report, definition)
            "semanticAiProposalDetailByIncident" -> shapeAiProposalDetail(report, definition)
            else -> error("No result envelope contract for query id: ${report.queryId}")
        }
    }

    private fun shapeNamedGraphInventory(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): NamedGraphInventoryEnvelope {
        return NamedGraphInventoryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                NamedGraphInventoryRecord(
                    graphUri = row.required("graph"),
                    subjectCount = row.required("subjectCount").toInt(),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeIncidentSummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): IncidentSummaryEnvelope {
        return IncidentSummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                IncidentSummaryRecord(
                    graphUri = row.required("graph"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    assetUri = row.required("asset"),
                    stageUri = row.required("stage"),
                    sourceRecordUri = row.optional("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeProvenanceSourceRecords(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ProvenanceSourceRecordsEnvelope {
        return ProvenanceSourceRecordsEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ProvenanceSourceRecord(
                    graphUri = row.required("graph"),
                    sourceRecordUri = row.required("sourceRecord"),
                    sourceRecordId = row.required("sourceRecordId"),
                    sourceSystemUri = row.required("sourceSystem"),
                    payloadHash = row.required("payloadHash"),
                    activityUri = row.required("activity"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeFollowUpQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): FollowUpQueueEnvelope {
        return FollowUpQueueEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                FollowUpQueueRecord(
                    graphUri = row.required("graph"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    assetUri = row.required("asset"),
                    assetId = row.required("assetId"),
                    zoneUri = row.required("zone"),
                    zoneId = row.required("zoneId"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    sourceRecordUri = row.required("sourceRecord"),
                    priorityRank = row.optionalInt("priorityRank"),
                    requestTitle = row.optional("requestTitle"),
                    currentStatus = row.optional("currentStatus"),
                    hoursInCurrentStage = row.optionalDouble("hoursInCurrentStage"),
                    neededByAt = row.optional("neededByAt"),
                    priorityLevel = row.optional("priorityLevel"),
                    businessImpact = row.optional("businessImpact"),
                    assetCriticalityScore = row.optionalDouble("assetCriticalityScore"),
                    downtimeScore = row.optionalDouble("downtimeScore"),
                    stageDelayScore = row.optionalDouble("stageDelayScore"),
                    infrastructureZoneImpactScore = row.optionalDouble("infrastructureZoneImpactScore"),
                    neededByUrgencyScore = row.optionalDouble("neededByUrgencyScore"),
                    repeatFailureScore = row.optionalDouble("repeatFailureScore"),
                    spareRiskScore = row.optionalDouble("spareRiskScore"),
                    capacityRiskScore = row.optionalDouble("capacityRiskScore"),
                    redundancyRiskScore = row.optionalDouble("redundancyRiskScore"),
                    thermalRiskScore = row.optionalDouble("thermalRiskScore"),
                    vendorEtaRiskScore = row.optionalDouble("vendorEtaRiskScore"),
                    mitigationCreditScore = row.optionalDouble("mitigationCreditScore"),
                    totalPriorityScore = row.optionalDouble("totalPriorityScore"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeDashboardOverview(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): DashboardOverviewEnvelope {
        return DashboardOverviewEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                DashboardOverviewRecord(
                    graphUri = row.required("graph"),
                    totalIncidents = row.requiredInt("totalIncidents"),
                    assetCount = row.requiredInt("assetCount"),
                    zoneCount = row.requiredInt("zoneCount"),
                    impactObservationCount = row.requiredInt("impactObservationCount"),
                    capacityRiskKw = row.requiredDouble("capacityRiskKw"),
                    affectedGpuCount = row.requiredInt("affectedGpuCount"),
                    dependencyEdgeCount = row.requiredInt("dependencyEdgeCount"),
                    trustFindingCount = row.requiredInt("trustFindingCount"),
                    avgDurationHours = row.optionalDouble("avgDurationHours"),
                    totalDurationHours = row.optionalDouble("totalDurationHours"),
                    totalDelayHours = row.optionalDouble("totalDelayHours"),
                    mitigatedIncidentCount = row.optionalInt("mitigatedIncidentCount"),
                    affectedRackCount = row.optionalInt("affectedRackCount"),
                    thermalBreachMinutes = row.optionalInt("thermalBreachMinutes"),
                    redundancyLostIncidentCount = row.optionalInt("redundancyLostIncidentCount"),
                    vendorEtaMissedCount = row.optionalInt("vendorEtaMissedCount"),
                    repeatFailureAssetCount = row.optionalInt("repeatFailureAssetCount"),
                    engineerAssignmentDelayHours = row.optionalDouble("engineerAssignmentDelayHours"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapePlatformStatus(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): PlatformStatusEnvelope {
        return PlatformStatusEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                PlatformStatusRecord(
                    serviceBoundary = row.required("serviceBoundary"),
                    platformVerdict = row.required("platformVerdict"),
                    reasonCode = row.required("reasonCode"),
                    sourceFreshnessStatus = row.required("sourceFreshnessStatus"),
                    latestSourceImportAt = row.optional("latestSourceImportAt"),
                    sourceSystemCount = row.requiredInt("sourceSystemCount"),
                    latestCanonicalReleaseId = row.optional("latestCanonicalReleaseId"),
                    latestPromotionAt = row.optional("latestPromotionAt"),
                    promotionStatus = row.required("promotionStatus"),
                    latestReasoningRunId = row.optional("latestReasoningRunId"),
                    latestAnalysisAt = row.optional("latestAnalysisAt"),
                    analysisStatus = row.required("analysisStatus"),
                    pipelineStatus = row.required("pipelineStatus"),
                    reconciliationStatus = row.required("reconciliationStatus"),
                    graphValidationStatus = row.required("graphValidationStatus"),
                    sourceRecordCount = row.requiredInt("sourceRecordCount"),
                    incidentCount = row.requiredInt("incidentCount"),
                    incidentWithProvenanceCount = row.requiredInt("incidentWithProvenanceCount"),
                    assetCount = row.requiredInt("assetCount"),
                    assetWithProvenanceCount = row.requiredInt("assetWithProvenanceCount"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeFilterMetadata(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): FilterMetadataEnvelope {
        return FilterMetadataEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                FilterMetadataRecord(
                    graphUri = row.required("graph"),
                    filterType = row.required("filterType"),
                    resourceUri = row.required("resource"),
                    id = row.required("id"),
                    label = row.optional("label"),
                    sourceRecordUri = row.optional("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeFollowUpDetail(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): FollowUpDetailEnvelope {
        return FollowUpDetailEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                FollowUpDetailRecord(
                    graphUri = row.required("graph"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    assetUri = row.required("asset"),
                    assetId = row.required("assetId"),
                    zoneUri = row.required("zone"),
                    zoneId = row.required("zoneId"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    sourceRecordUri = row.required("sourceRecord"),
                    impactUri = row.optional("impact"),
                    capacityRiskKw = row.optionalDouble("capacityRiskKw"),
                    affectedGpuCount = row.optionalInt("affectedGpuCount"),
                    followUpDecisionUri = row.optional("followUpDecision"),
                    recommendedAction = row.optional("recommendedAction"),
                    recoveryBlockerUri = row.optional("recoveryBlocker"),
                    blockerSummary = row.optional("blockerSummary"),
                    restoreReadinessUri = row.optional("restoreReadiness"),
                    restoreReadinessSummary = row.optional("restoreReadinessSummary"),
                    trustFindingUri = row.optional("trustFinding"),
                    trustSummary = row.optional("trustSummary"),
                    priorityRank = row.optionalInt("priorityRank"),
                    requestTitle = row.optional("requestTitle"),
                    currentStatus = row.optional("currentStatus"),
                    hoursInCurrentStage = row.optionalDouble("hoursInCurrentStage"),
                    neededByAt = row.optional("neededByAt"),
                    priorityLevel = row.optional("priorityLevel"),
                    businessImpact = row.optional("businessImpact"),
                    assetCriticalityScore = row.optionalDouble("assetCriticalityScore"),
                    downtimeScore = row.optionalDouble("downtimeScore"),
                    stageDelayScore = row.optionalDouble("stageDelayScore"),
                    infrastructureZoneImpactScore = row.optionalDouble("infrastructureZoneImpactScore"),
                    neededByUrgencyScore = row.optionalDouble("neededByUrgencyScore"),
                    repeatFailureScore = row.optionalDouble("repeatFailureScore"),
                    repeatFailureAssetCount = row.optionalInt("repeatFailureAssetCount"),
                    engineerAssignmentDelayHours = row.optionalDouble("engineerAssignmentDelayHours"),
                    spareRiskScore = row.optionalDouble("spareRiskScore"),
                    capacityRiskScore = row.optionalDouble("capacityRiskScore"),
                    redundancyRiskScore = row.optionalDouble("redundancyRiskScore"),
                    thermalRiskScore = row.optionalDouble("thermalRiskScore"),
                    vendorEtaRiskScore = row.optionalDouble("vendorEtaRiskScore"),
                    mitigationCreditScore = row.optionalDouble("mitigationCreditScore"),
                    totalPriorityScore = row.optionalDouble("totalPriorityScore"),
                    redundancyState = row.optional("redundancyState"),
                    affectedRackCount = row.optionalInt("affectedRackCount"),
                    estimatedGpuCapacityRiskPct = row.optionalDouble("estimatedGpuCapacityRiskPct"),
                    thermalBreachMinutes = row.optionalInt("thermalBreachMinutes"),
                    powerRedundancyLost = row.optionalBoolean("powerRedundancyLost"),
                    coolingRedundancyLost = row.optionalBoolean("coolingRedundancyLost"),
                    mitigationStatus = row.optional("mitigationStatus"),
                    vendorEtaAt = row.optional("vendorEtaAt"),
                    vendorStatus = row.optional("vendorStatus"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeImpactSummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ImpactSummaryEnvelope {
        return ImpactSummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ImpactSummaryRecord(
                    graphUri = row.required("graph"),
                    impactObservationCount = row.requiredInt("impactObservationCount"),
                    incidentCount = row.requiredInt("incidentCount"),
                    capacityRiskKw = row.requiredDouble("capacityRiskKw"),
                    affectedGpuCount = row.requiredInt("affectedGpuCount"),
                    trustFindingCount = row.requiredInt("trustFindingCount"),
                    affectedRackCount = row.optionalInt("affectedRackCount"),
                    thermalBreachMinutes = row.optionalInt("thermalBreachMinutes"),
                    redundancyLostIncidentCount = row.optionalInt("redundancyLostIncidentCount"),
                    vendorEtaMissedCount = row.optionalInt("vendorEtaMissedCount"),
                    mitigatedIncidentCount = row.optionalInt("mitigatedIncidentCount"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeTopologyDependencies(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): TopologyDependenciesEnvelope {
        return TopologyDependenciesEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                TopologyDependencyRecord(
                    graphUri = row.required("graph"),
                    dependencyEdgeUri = row.required("dependencyEdge"),
                    dependencyId = row.required("dependencyId"),
                    dependentAssetUri = row.required("dependentAsset"),
                    dependentAssetId = row.required("dependentAssetId"),
                    dependencyAssetUri = row.required("dependencyAsset"),
                    dependencyAssetId = row.required("dependencyAssetId"),
                    dependencyRole = row.required("dependencyRole"),
                    impactScope = row.optional("impactScope"),
                    dependencyPathUri = row.optional("dependencyPath"),
                    pathId = row.optional("pathId"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeTrustFindings(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): TrustFindingsEnvelope {
        val records = report.rows.map { row ->
            TrustFindingRecord(
                graphUri = row.required("graph"),
                trustFindingUri = row.required("trustFinding"),
                trustFindingId = row.optional("trustFindingId"),
                summary = row.required("summary"),
                sourceFactUri = row.required("sourceFact"),
                activityUri = row.optional("activity"),
                severity = row.optional("severity"),
                status = row.optional("status"),
                createdAt = row.optional("createdAt"),
            )
        }.groupBy { record -> record.trustFindingId ?: record.trustFindingUri }
            .values
            .map { versions -> versions.maxWithOrNull(compareBy<TrustFindingRecord> { it.createdAt.orEmpty() }.thenBy { it.graphUri })!! }
            .sortedWith(compareByDescending<TrustFindingRecord> { it.createdAt.orEmpty() }.thenBy { it.trustFindingId ?: it.trustFindingUri })
        return TrustFindingsEnvelope(
            queryId = report.queryId,
            records = records,
            provenance = provenance(definition),
        )
    }

    private fun shapeStageBottlenecks(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): StageBottlenecksEnvelope {
        return StageBottlenecksEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                StageBottleneckRecord(
                    graphUri = row.required("graph"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    incidentCount = row.requiredInt("incidentCount"),
                    delayedCount = row.optionalInt("delayedCount"),
                    avgDurationHours = row.optionalDouble("avgDurationHours"),
                    p90DurationHours = row.optionalDouble("p90DurationHours"),
                    totalDelayHours = row.optionalDouble("totalDelayHours"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeAssetDelaySummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): AssetDelaySummaryEnvelope {
        return AssetDelaySummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                AssetDelaySummaryRecord(
                    graphUri = row.required("graph"),
                    assetUri = row.required("asset"),
                    assetId = row.required("assetId"),
                    zoneUri = row.required("zone"),
                    zoneId = row.required("zoneId"),
                    incidentCount = row.requiredInt("incidentCount"),
                    impactObservationCount = row.requiredInt("impactObservationCount"),
                    capacityRiskKw = row.requiredDouble("capacityRiskKw"),
                    affectedGpuCount = row.requiredInt("affectedGpuCount"),
                    delayedIncidentCount = row.optionalInt("delayedIncidentCount"),
                    repeatFailureCount = row.optionalInt("repeatFailureCount"),
                    totalDurationHours = row.optionalDouble("totalDurationHours"),
                    avgDurationHours = row.optionalDouble("avgDurationHours"),
                    topFailureMode = row.optional("topFailureMode"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeZoneDelaySummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ZoneDelaySummaryEnvelope {
        return ZoneDelaySummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ZoneDelaySummaryRecord(
                    graphUri = row.required("graph"),
                    zoneUri = row.required("zone"),
                    zoneId = row.required("zoneId"),
                    assetCount = row.requiredInt("assetCount"),
                    incidentCount = row.requiredInt("incidentCount"),
                    impactObservationCount = row.requiredInt("impactObservationCount"),
                    capacityRiskKw = row.requiredDouble("capacityRiskKw"),
                    affectedGpuCount = row.requiredInt("affectedGpuCount"),
                    delayedIncidentCount = row.optionalInt("delayedIncidentCount"),
                    criticalIncidentCount = row.optionalInt("criticalIncidentCount"),
                    totalDurationHours = row.optionalDouble("totalDurationHours"),
                    topBottleneckStage = row.optional("topBottleneckStage"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeSpareWaitSummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): SpareWaitSummaryEnvelope {
        return SpareWaitSummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                SpareWaitSummaryRecord(
                    graphUri = row.required("graph"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    incidentCount = row.requiredInt("incidentCount"),
                    recoveryBlockerCount = row.requiredInt("recoveryBlockerCount"),
                    totalWaitHours = row.optionalDouble("totalWaitHours"),
                    avgWaitHours = row.optionalDouble("avgWaitHours"),
                    stockStatus = row.optional("stockStatus"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeValidationSummary(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ValidationSummaryEnvelope {
        return ValidationSummaryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ValidationSummaryRecord(
                    graphUri = row.required("graph"),
                    sourceRecordCount = row.requiredInt("sourceRecordCount"),
                    incidentCount = row.requiredInt("incidentCount"),
                    incidentWithProvenanceCount = row.requiredInt("incidentWithProvenanceCount"),
                    assetCount = row.requiredInt("assetCount"),
                    assetWithProvenanceCount = row.requiredInt("assetWithProvenanceCount"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeIncidentEvidence(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): IncidentEvidenceEnvelope {
        return IncidentEvidenceEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                IncidentEvidenceRecord(
                    graphUri = row.required("graph"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    sourceRecordUri = row.required("sourceRecord"),
                    impactUri = row.optional("impact"),
                    evidenceUri = row.optional("evidence"),
                    evidenceClassUri = row.optional("evidenceClass"),
                    evidenceTimestamp = row.optional("evidenceTimestamp"),
                    confidenceState = row.optional("confidenceState"),
                    metricName = row.optional("metricName"),
                    metricValue = row.optionalDouble("metricValue"),
                    metricUnit = row.optional("metricUnit"),
                    telemetryStatus = row.optional("telemetryStatus"),
                    telemetryAlertId = row.optional("telemetryAlertId"),
                    alertType = row.optional("alertType"),
                    alertSeverity = row.optional("alertSeverity"),
                    alertTriggeredAt = row.optional("alertTriggeredAt"),
                    alertResolvedAt = row.optional("alertResolvedAt"),
                    validationId = row.optional("validationId"),
                    validationStatus = row.optional("validationStatus"),
                    validatorId = row.optional("validatorId"),
                    validationStartedAt = row.optional("validationStartedAt"),
                    validationCompletedAt = row.optional("validationCompletedAt"),
                    failureReason = row.optional("failureReason"),
                    workOrderId = row.optional("workOrderId"),
                    assignedTeam = row.optional("assignedTeam"),
                    assignedEngineerId = row.optional("assignedEngineerId"),
                    workOrderStatus = row.optional("workOrderStatus"),
                    plannedStartAt = row.optional("plannedStartAt"),
                    actualStartAt = row.optional("actualStartAt"),
                    actualCompletedAt = row.optional("actualCompletedAt"),
                    requiredSpareId = row.optional("requiredSpareId"),
                    requiredSpareName = row.optional("requiredSpareName"),
                    stockStatus = row.optional("stockStatus"),
                    trustFindingUri = row.optional("trustFinding"),
                    trustSummary = row.optional("trustSummary"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeIncidentTimeline(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): IncidentTimelineEnvelope {
        return IncidentTimelineEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                IncidentTimelineRecord(
                    graphUri = row.required("graph"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    eventUri = row.required("event"),
                    eventId = row.optional("eventId"),
                    stageUri = row.required("stage"),
                    stageLabel = row.optional("stageLabel"),
                    eventStatus = row.optional("eventStatus"),
                    enteredAt = row.optional("enteredAt"),
                    exitedAt = row.optional("exitedAt"),
                    durationHours = row.optionalDouble("durationHours"),
                    thresholdHours = row.optionalDouble("thresholdHours"),
                    delayHours = row.optionalDouble("delayHours"),
                    sourceRecordUri = row.required("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeDependencyImpact(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): DependencyImpactEnvelope {
        return DependencyImpactEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                DependencyImpactRecord(
                    graphUri = row.required("graph"),
                    assetUri = row.required("asset"),
                    assetId = row.required("assetId"),
                    dependencyEdgeUri = row.optional("dependencyEdge"),
                    dependencyId = row.optional("dependencyId"),
                    dependencyAssetUri = row.optional("dependencyAsset"),
                    dependencyAssetId = row.optional("dependencyAssetId"),
                    dependencyRole = row.optional("dependencyRole"),
                    impactScope = row.optional("impactScope"),
                    findingUri = row.optional("finding"),
                    findingSummary = row.optional("findingSummary"),
                    sourceRecordUri = row.optional("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeBlastRadius(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): BlastRadiusEnvelope {
        return BlastRadiusEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                BlastRadiusRecord(
                    graphUri = row.required("graph"),
                    assetUri = row.required("asset"),
                    assetId = row.required("assetId"),
                    downstreamAssetUri = row.optional("downstreamAsset"),
                    downstreamAssetId = row.optional("downstreamAssetId"),
                    incidentUri = row.optional("incident"),
                    incidentId = row.optional("incidentId"),
                    findingUri = row.optional("finding"),
                    findingSummary = row.optional("findingSummary"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeOntologyReviewQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): OntologyReviewQueueEnvelope {
        val records = report.rows.map { row ->
            OntologyReviewQueueRecord(
                graphUri = row.required("graph"),
                queueId = row.required("queueId"),
                queueKind = row.required("queueKind"),
                reviewActionId = row.required("reviewActionId"),
                reviewActionLabel = row.required("reviewActionLabel"),
                reviewStatus = row.required("reviewStatus"),
                targetUri = row.required("targetUri"),
                targetType = row.required("targetType"),
                targetLabel = row.required("targetLabel"),
                releaseId = row.required("releaseId"),
                sourceGraphUri = row.optional("sourceGraph"),
                canonicalGraphUri = row.optional("canonicalGraph"),
                provenanceGraphUri = row.optional("provenanceGraph"),
                reasoningAuditGraphUri = row.optional("reasoningAuditGraph"),
                reasoningGraphUri = row.optional("reasoningGraph"),
                evidenceSummary = row.required("evidenceSummary"),
                actionStatus = row.required("actionStatus"),
                disabledReason = row.required("disabledReason"),
                incidentCount = row.requiredInt("incidentCount"),
                assetCount = row.requiredInt("assetCount"),
                sourceRecordCount = row.requiredInt("sourceRecordCount"),
                activityCount = row.requiredInt("activityCount"),
                generatedFactCount = row.requiredInt("generatedFactCount"),
                prioritySortOrder = row.requiredInt("prioritySortOrder"),
            )
        }.groupBy { record -> "${record.queueKind}|${record.reviewActionId}|${record.targetUri}|${record.releaseId}" }
            .values
            .map(::mergeOntologyReviewVersions)
            .sortedWith(compareBy<OntologyReviewQueueRecord> { it.prioritySortOrder }.thenBy { it.targetLabel })
        return OntologyReviewQueueEnvelope(
            queryId = report.queryId,
            records = records,
            provenance = provenance(definition),
        )
    }

    private fun shapeActionAuditHistory(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionAuditHistoryEnvelope {
        val records = report.rows.map { row ->
            ActionAuditHistoryRecord(
                graphUri = row.required("graph"),
                actionAuditReleaseId = row.required("actionAuditReleaseId"),
                executionUri = row.required("execution"),
                executionId = row.required("executionId"),
                requestUri = row.required("request"),
                requestId = row.required("requestId"),
                validationReportUri = row.required("validationReport"),
                actionTypeUri = row.required("actionType"),
                actionTypeId = row.required("actionTypeId"),
                actionTypeLabel = row.optional("actionTypeLabel"),
                idempotencyKey = row.required("idempotencyKey"),
                actorId = row.required("actorId"),
                actionReason = row.required("actionReason"),
                actionStatus = row.required("actionStatus"),
                requestedAt = row.required("requestedAt"),
                executedAt = row.required("executedAt"),
                targetObjectUri = row.optional("targetObject"),
                validationStatus = row.required("validationStatus"),
                validationSummary = row.optional("validationSummary"),
                sourceRecordUri = row.optional("sourceRecord"),
                assignedTeam = row.optional("assignedTeam"),
                assigneeId = row.optional("assigneeId"),
                reviewedStatus = row.optional("reviewedStatus"),
                reviewSummary = row.optional("reviewSummary"),
                supportingEvidenceUri = row.optional("supportingEvidence"),
            )
        }.groupBy { it.executionUri }
            .values
            .map { versions -> versions.maxWith(compareBy<ActionAuditHistoryRecord> { it.executedAt }.thenBy { it.requestedAt }) }
            .sortedByDescending { it.requestedAt }
        return ActionAuditHistoryEnvelope(
            queryId = report.queryId,
            records = records,
            provenance = provenance(definition),
        )
    }

    private fun shapeActionAvailability(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionAvailabilityEnvelope {
        val queryRecords = report.rows.mapNotNull { row ->
            if (!row.hasAllActionAvailabilityBindings()) return@mapNotNull null
            val detailRole = row.required("detailRole")
            val rawDetailValue = row.required("detailValue")
            val sourceBackedTarget = if (rawDetailValue == SOURCE_BACKED_ACTION_TARGET) {
                row.resolveSourceBackedActionTarget(detailRole) ?: return@mapNotNull null
            } else {
                null
            }
            row.actionAvailabilityRecord(
                detailKind = row.required("detailKind"),
                detailRole = detailRole,
                detailLabel = sourceBackedTarget?.first ?: row.required("detailLabel"),
                detailValue = sourceBackedTarget?.second ?: rawDetailValue,
                detailSortOrder = row.requiredInt("detailSortOrder"),
            )
        }
        val authoritativeTargets = report.rows.flatMap { row -> row.authoritativeActionTargets() }
        return ActionAvailabilityEnvelope(
            queryId = report.queryId,
            records = (queryRecords + authoritativeTargets).distinctBy {
                listOf(it.graphUri, it.incidentUri, it.actionId, it.uiPlacement, it.detailKind, it.detailRole, it.detailValue)
            },
            provenance = provenance(definition),
        )
    }

    private fun Map<String, String>.authoritativeActionTargets(): List<ActionAvailabilityRecord> {
        if (!hasAllActionAvailabilityBaseBindings()) return emptyList()
        val targets = when (required("actionId")) {
            "AcknowledgeRestoreBlocker" -> listOfNotNull(
                resolveSourceBackedActionTarget("RestoreReadinessFinding")?.let { Triple("RestoreReadinessFinding", it, 100) },
                resolveSourceBackedActionTarget("RecoveryBlocker")?.let { Triple("RecoveryBlocker", it, 101) },
            )
            "AssignEvidenceReview" -> listOfNotNull(
                resolveSourceBackedActionTarget("TrustFinding")?.let { Triple("TrustFinding", it, 100) },
            )
            "RecordValidationReview" -> listOfNotNull(
                resolveSourceBackedActionTarget("ValidationEvidence")?.let { Triple("ValidationEvidence", it, 100) },
            )
            else -> emptyList()
        }
        return targets.map { (role, target, sortOrder) ->
            actionAvailabilityRecord("targetObject", role, target.first, target.second, sortOrder)
        }
    }

    private fun Map<String, String>.actionAvailabilityRecord(
        detailKind: String,
        detailRole: String,
        detailLabel: String,
        detailValue: String,
        detailSortOrder: Int,
    ): ActionAvailabilityRecord {
        return ActionAvailabilityRecord(
            graphUri = required("graph"),
            incidentUri = required("incident"),
            incidentId = required("incidentId"),
            assetUri = required("asset"),
            assetId = required("assetId"),
            sourceRecordUri = required("sourceRecord"),
            actionId = required("actionId"),
            actionLabel = required("actionLabel"),
            actionDescription = required("actionDescription"),
            actionStatus = required("actionStatus"),
            uiPlacement = required("uiPlacement"),
            detailKind = detailKind,
            detailRole = detailRole,
            detailLabel = detailLabel,
            detailValue = detailValue,
            detailSortOrder = detailSortOrder,
        )
    }

    private fun Map<String, String>.resolveSourceBackedActionTarget(role: String): Pair<String, String>? {
        return when (role) {
            "InfrastructureIncident" -> required("incidentId") to required("incident")
            "InfrastructureAsset" -> required("assetId") to required("asset")
            "SourceRecord" -> "Source evidence" to required("sourceRecord")
            "RestoreReadinessFinding" -> (optional("restoreReadinessSummary") ?: "Restore readiness finding") to optional("restoreReadiness")
            "RecoveryBlocker" -> (optional("blockerSummary") ?: "Recovery blocker") to optional("recoveryBlocker")
            "TrustFinding" -> (optional("trustSummary") ?: "Trust finding") to optional("trustFinding")
            "ValidationEvidence" -> (optional("validationStatus") ?: "Validation evidence") to optional("validationEvidence")
            else -> null
        }?.let { (label, value) -> value?.let { label to it } }
    }

    private fun Map<String, String>.hasAllActionAvailabilityBaseBindings(): Boolean {
        return listOf(
            "graph", "incident", "incidentId", "asset", "assetId", "sourceRecord",
            "actionId", "actionLabel", "actionDescription", "actionStatus", "uiPlacement",
        ).all { key -> !this[key].isNullOrBlank() }
    }

    companion object {
        private const val SOURCE_BACKED_ACTION_TARGET = "__SOURCE_BACKED_TARGET__"
    }

    private fun shapeActionNotificationQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionNotificationQueueEnvelope {
        return ActionNotificationQueueEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ActionNotificationQueueRecord(
                    graphUri = row.required("graph"),
                    actionAuditReleaseId = row.required("actionAuditReleaseId"),
                    notificationUri = row.required("notification"),
                    notificationId = row.required("notificationId"),
                    notificationStatus = row.required("notificationStatus"),
                    notificationSummary = row.required("notificationSummary"),
                    executionUri = row.required("execution"),
                    executionId = row.required("executionId"),
                    requestUri = row.required("request"),
                    requestId = row.required("requestId"),
                    actionTypeUri = row.required("actionType"),
                    actionTypeId = row.required("actionTypeId"),
                    actorId = row.required("actorId"),
                    actionReason = row.required("actionReason"),
                    requestedAt = row.required("requestedAt"),
                    generatedAt = row.required("generatedAt"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    targetObjectUri = row.optional("targetObject"),
                    sourceRecordUri = row.optional("sourceRecord"),
                    assignedTeam = row.optional("assignedTeam"),
                    assigneeId = row.optional("assigneeId"),
                    reviewedStatus = row.optional("reviewedStatus"),
                    reviewSummary = row.optional("reviewSummary"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeActionReviewQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionReviewQueueEnvelope {
        val records = report.rows.map { row ->
            ActionReviewQueueRecord(
                graphUri = row.required("graph"),
                actionAuditReleaseId = row.required("actionAuditReleaseId"),
                notificationUri = row.required("notification"),
                notificationId = row.required("notificationId"),
                executionUri = row.required("execution"),
                executionId = row.required("executionId"),
                requestUri = row.required("request"),
                requestId = row.required("requestId"),
                actionTypeUri = row.required("actionType"),
                actionTypeId = row.required("actionTypeId"),
                actorId = row.required("actorId"),
                actionReason = row.required("actionReason"),
                currentState = row.required("currentState"),
                stateGeneratedAt = row.required("stateGeneratedAt"),
                incidentUri = row.required("incident"),
                incidentId = row.required("incidentId"),
                sourceRecordUri = row.optional("sourceRecord"),
            )
        }.groupBy { record -> record.executionUri }
            .values
            .map { versions -> versions.maxWithOrNull(compareBy<ActionReviewQueueRecord> { it.stateGeneratedAt }.thenBy { it.graphUri })!! }
            .sortedWith(compareByDescending<ActionReviewQueueRecord> { it.stateGeneratedAt }.thenBy { it.executionUri })
        return ActionReviewQueueEnvelope(
            queryId = report.queryId,
            records = records,
            provenance = provenance(definition),
        )
    }

    private fun shapeActionTransitionHistory(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionTransitionHistoryEnvelope {
        return ActionTransitionHistoryEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ActionTransitionHistoryRecord(
                    graphUri = row.required("graph"),
                    actionAuditReleaseId = row.required("actionAuditReleaseId"),
                    transitionUri = row.required("transition"),
                    transitionId = row.required("transitionId"),
                    executionUri = row.required("execution"),
                    executionId = row.required("executionId"),
                    requestUri = row.required("request"),
                    requestId = row.required("requestId"),
                    actionTypeUri = row.required("actionType"),
                    actionTypeId = row.required("actionTypeId"),
                    actorId = row.required("actorId"),
                    transitionReason = row.required("transitionReason"),
                    fromState = row.optional("fromState"),
                    toState = row.required("toState"),
                    generatedAt = row.required("generatedAt"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeActionDispatchQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): ActionDispatchQueueEnvelope {
        return ActionDispatchQueueEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                ActionDispatchQueueRecord(
                    graphUri = row.required("graph"),
                    actionAuditReleaseId = row.required("actionAuditReleaseId"),
                    dispatchUri = row.required("dispatch"),
                    dispatchId = row.required("dispatchId"),
                    dispatchChannel = row.required("dispatchChannel"),
                    dispatchStatus = row.required("dispatchStatus"),
                    dispatchLifecycleState = row.required("dispatchLifecycleState"),
                    dispatchSummary = row.required("dispatchSummary"),
                    executionUri = row.required("execution"),
                    executionId = row.required("executionId"),
                    requestUri = row.required("request"),
                    requestId = row.required("requestId"),
                    actionTypeUri = row.required("actionType"),
                    actionTypeId = row.required("actionTypeId"),
                    transitionUri = row.required("transition"),
                    transitionId = row.required("transitionId"),
                    actorId = row.required("actorId"),
                    generatedAt = row.required("generatedAt"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    sourceRecordUri = row.optional("sourceRecord"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeDynamicPlayback(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): DynamicPlaybackEnvelope {
        return DynamicPlaybackEnvelope(
            queryId = report.queryId,
            records = report.rows.map { row ->
                DynamicPlaybackRecord(
                    graphUri = row.required("graph"),
                    actionAuditReleaseId = row.required("actionAuditReleaseId"),
                    eventUri = row.required("event"),
                    eventId = row.required("eventId"),
                    scenarioId = row.required("scenarioId"),
                    playbackBatchId = row.required("playbackBatchId"),
                    playbackStep = row.requiredInt("playbackStep"),
                    incidentUri = row.required("incident"),
                    incidentId = row.required("incidentId"),
                    eventKind = row.required("eventKind"),
                    sourceFamily = row.required("sourceFamily"),
                    occurredAt = row.required("occurredAt"),
                    summary = row.required("summary"),
                    sourceRecordUri = row.required("sourceRecord"),
                    beforeState = row.required("beforeState"),
                    afterState = row.required("afterState"),
                    beforeReasoningState = row.required("beforeReasoningState"),
                    afterReasoningState = row.required("afterReasoningState"),
                    beforeTrustState = row.required("beforeTrustState"),
                    afterTrustState = row.required("afterTrustState"),
                    beforeBlastRadiusCount = row.requiredInt("beforeBlastRadiusCount"),
                    afterBlastRadiusCount = row.requiredInt("afterBlastRadiusCount"),
                    actionLifecycleState = row.required("actionLifecycleState"),
                    canonicalGraphUri = row.optional("canonicalGraph"),
                    provenanceGraphUri = row.optional("provenanceGraph"),
                    reasoningGraphUri = row.optional("reasoningGraph"),
                )
            },
            provenance = provenance(definition),
        )
    }

    private fun shapeAiProposalReviewQueue(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): AiProposalReviewQueueEnvelope {
        return AiProposalReviewQueueEnvelope(
            queryId = report.queryId,
            records = distinctAiProposals(report.rows),
            provenance = provenance(definition),
        )
    }

    private fun shapeAiProposalDetail(
        report: QueryExecutionReport,
        definition: ApprovedQueryDefinition,
    ): AiProposalDetailEnvelope {
        return AiProposalDetailEnvelope(
            queryId = report.queryId,
            records = distinctAiProposals(report.rows),
            provenance = provenance(definition),
        )
    }

    private fun distinctAiProposals(rows: List<Map<String, String>>): List<AiProposalRecord> {
        return rows.map(::aiProposalRecord)
            .groupBy { record -> record.proposalUri }
            .values
            .map { versions -> versions.maxWithOrNull(compareBy<AiProposalRecord> { it.generatedAt }.thenBy { it.targetObjectUri })!! }
            .sortedWith(compareByDescending<AiProposalRecord> { it.generatedAt }.thenBy { it.proposalUri })
    }

    private fun mergeOntologyReviewVersions(versions: List<OntologyReviewQueueRecord>): OntologyReviewQueueRecord {
        val authoritative = versions.maxWithOrNull(
            compareBy<OntologyReviewQueueRecord> { reviewStatusRank(it.reviewStatus) }
                .thenBy { it.activityCount }
                .thenBy { it.generatedFactCount },
        )!!
        return authoritative.copy(
            sourceGraphUri = versions.firstNotNullOfOrNull { it.sourceGraphUri?.takeIf(String::isNotBlank) },
            canonicalGraphUri = versions.firstNotNullOfOrNull { it.canonicalGraphUri?.takeIf(String::isNotBlank) },
            provenanceGraphUri = versions.firstNotNullOfOrNull { it.provenanceGraphUri?.takeIf(String::isNotBlank) },
            reasoningAuditGraphUri = versions.firstNotNullOfOrNull { it.reasoningAuditGraphUri?.takeIf(String::isNotBlank) },
            reasoningGraphUri = versions.firstNotNullOfOrNull { it.reasoningGraphUri?.takeIf(String::isNotBlank) },
            incidentCount = versions.maxOf { it.incidentCount },
            assetCount = versions.maxOf { it.assetCount },
            sourceRecordCount = versions.maxOf { it.sourceRecordCount },
            activityCount = versions.maxOf { it.activityCount },
            generatedFactCount = versions.maxOf { it.generatedFactCount },
            prioritySortOrder = versions.minOf { it.prioritySortOrder },
        )
    }

    private fun reviewStatusRank(status: String): Int {
        val normalized = status.uppercase()
        return when {
            normalized.contains("APPROVED") || normalized.contains("REFRESHED") -> 3
            normalized.contains("REJECTED") || normalized.contains("CLOSED") -> 2
            normalized.contains("PENDING") -> 1
            else -> 0
        }
    }

    private fun aiProposalRecord(row: Map<String, String>): AiProposalRecord {
        return AiProposalRecord(
            graphUri = row.required("graph"),
            aiAuditReleaseId = row.required("aiAuditReleaseId"),
            proposalUri = row.required("proposal"),
            proposalId = row.required("proposalId"),
            proposalType = row.required("proposalType"),
            proposalStatus = row.required("proposalStatus"),
            reviewStatus = row.required("reviewStatus"),
            disabledReason = row.required("disabledReason"),
            summary = row.required("summary"),
            rationale = row.required("rationale"),
            confidenceScore = row.requiredDouble("confidenceScore"),
            riskLevel = row.required("riskLevel"),
            modelId = row.required("modelId"),
            promptId = row.required("promptId"),
            promptHash = row.required("promptHash"),
            actorId = row.required("actorId"),
            generatedAt = row.required("generatedAt"),
            batchUri = row.required("batch"),
            batchId = row.required("batchId"),
            validationReportUri = row.required("validationReport"),
            validationStatus = row.required("validationStatus"),
            validationSummary = row.required("validationSummary"),
            incidentUri = row.required("incident"),
            incidentId = row.required("incidentId"),
            targetObjectUri = row.required("targetObject"),
            sourceRecordUri = row.required("sourceRecord"),
            supportingEvidenceUri = row.required("supportingEvidence"),
        )
    }

    private fun provenance(definition: ApprovedQueryDefinition): QueryResultEnvelopeProvenance {
        return QueryResultEnvelopeProvenance(
            queryId = definition.id,
            graphScope = definition.graphScope,
        )
    }

    private fun Map<String, String>.hasAllActionAvailabilityBindings(): Boolean {
        return listOf(
            "graph",
            "incident",
            "incidentId",
            "asset",
            "assetId",
            "sourceRecord",
            "actionId",
            "actionLabel",
            "actionDescription",
            "actionStatus",
            "uiPlacement",
            "detailKind",
            "detailRole",
            "detailLabel",
            "detailValue",
            "detailSortOrder",
        ).all { key -> !this[key].isNullOrBlank() }
    }

    private fun Map<String, String>.required(key: String): String {
        val value = this[key]
        require(!value.isNullOrBlank()) { "Missing required binding '$key'" }
        return value
    }

    private fun Map<String, String>.optional(key: String): String? {
        return this[key]?.takeIf { it.isNotBlank() }
    }

    private fun Map<String, String>.requiredInt(key: String): Int {
        return required(key).toDouble().toInt()
    }

    private fun Map<String, String>.optionalInt(key: String): Int? {
        return optional(key)?.toDouble()?.toInt()
    }

    private fun Map<String, String>.requiredDouble(key: String): Double {
        return required(key).toDouble()
    }

    private fun Map<String, String>.optionalDouble(key: String): Double? {
        return optional(key)?.toDouble()
    }

    private fun Map<String, String>.optionalBoolean(key: String): Boolean? {
        return optional(key)?.toBooleanStrictOrNull()
    }
}
