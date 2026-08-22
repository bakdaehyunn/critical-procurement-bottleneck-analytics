package com.dcai.semanticservice.response

import com.dcai.semanticservice.query.AssetDelaySummaryEnvelope
import com.dcai.semanticservice.query.ActionAuditHistoryEnvelope
import com.dcai.semanticservice.query.ActionNotificationQueueEnvelope
import com.dcai.semanticservice.query.ActionReviewQueueEnvelope
import com.dcai.semanticservice.query.ActionTransitionHistoryEnvelope
import com.dcai.semanticservice.query.ActionDispatchQueueEnvelope
import com.dcai.semanticservice.query.ActionAvailabilityEnvelope
import com.dcai.semanticservice.query.AiProposalDetailEnvelope
import com.dcai.semanticservice.query.AiProposalRecord
import com.dcai.semanticservice.query.AiProposalReviewQueueEnvelope
import com.dcai.semanticservice.query.BlastRadiusEnvelope
import com.dcai.semanticservice.query.DashboardOverviewEnvelope
import com.dcai.semanticservice.query.DependencyImpactEnvelope
import com.dcai.semanticservice.query.DynamicPlaybackEnvelope
import com.dcai.semanticservice.query.FilterMetadataEnvelope
import com.dcai.semanticservice.query.FollowUpDetailEnvelope
import com.dcai.semanticservice.query.IncidentSummaryEnvelope
import com.dcai.semanticservice.query.FollowUpQueueEnvelope
import com.dcai.semanticservice.query.ImpactSummaryEnvelope
import com.dcai.semanticservice.query.IncidentEvidenceEnvelope
import com.dcai.semanticservice.query.IncidentTimelineEnvelope
import com.dcai.semanticservice.query.NamedGraphInventoryEnvelope
import com.dcai.semanticservice.query.OntologyReviewQueueEnvelope
import com.dcai.semanticservice.query.PlatformStatusEnvelope
import com.dcai.semanticservice.query.ProvenanceSourceRecordsEnvelope
import com.dcai.semanticservice.query.QueryResultEnvelope
import com.dcai.semanticservice.query.QueryResultEnvelopeProvenance
import com.dcai.semanticservice.query.QueryPageResult
import com.dcai.semanticservice.query.SpareWaitSummaryEnvelope
import com.dcai.semanticservice.query.StageBottlenecksEnvelope
import com.dcai.semanticservice.query.TopologyDependenciesEnvelope
import com.dcai.semanticservice.query.TrustFindingsEnvelope
import com.dcai.semanticservice.query.ValidationSummaryEnvelope
import com.dcai.semanticservice.query.ZoneDelaySummaryEnvelope

class SemanticResponseSerializer {
    fun serialize(
        envelope: QueryResultEnvelope,
        page: QueryPageResult? = null,
    ): Map<String, Any> {
        val records = when (envelope) {
            is NamedGraphInventoryEnvelope -> envelope.records.map { record ->
                mapOf(
                    "graphUri" to record.graphUri,
                    "subjectCount" to record.subjectCount,
                )
            }
            is IncidentSummaryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("assetUri", record.assetUri)
                    put("stageUri", record.stageUri)
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                }
            }
            is ProvenanceSourceRecordsEnvelope -> envelope.records.map { record ->
                mapOf(
                    "graphUri" to record.graphUri,
                    "sourceRecordUri" to record.sourceRecordUri,
                    "sourceRecordId" to record.sourceRecordId,
                    "sourceSystemUri" to record.sourceSystemUri,
                    "payloadHash" to record.payloadHash,
                    "activityUri" to record.activityUri,
                )
            }
            is FollowUpQueueEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("assetUri", record.assetUri)
                    put("assetId", record.assetId)
                    put("zoneUri", record.zoneUri)
                    put("zoneId", record.zoneId)
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                    record.priorityRank?.let { put("priorityRank", it) }
                    record.requestTitle?.let { put("requestTitle", it) }
                    record.currentStatus?.let { put("currentStatus", it) }
                    record.hoursInCurrentStage?.let { put("hoursInCurrentStage", it) }
                    record.neededByAt?.let { put("neededByAt", it) }
                    record.priorityLevel?.let { put("priorityLevel", it) }
                    record.businessImpact?.let { put("businessImpact", it) }
                    record.assetCriticalityScore?.let { put("assetCriticalityScore", it) }
                    record.downtimeScore?.let { put("downtimeScore", it) }
                    record.stageDelayScore?.let { put("stageDelayScore", it) }
                    record.infrastructureZoneImpactScore?.let { put("infrastructureZoneImpactScore", it) }
                    record.neededByUrgencyScore?.let { put("neededByUrgencyScore", it) }
                    record.repeatFailureScore?.let { put("repeatFailureScore", it) }
                    record.spareRiskScore?.let { put("spareRiskScore", it) }
                    record.capacityRiskScore?.let { put("capacityRiskScore", it) }
                    record.redundancyRiskScore?.let { put("redundancyRiskScore", it) }
                    record.thermalRiskScore?.let { put("thermalRiskScore", it) }
                    record.vendorEtaRiskScore?.let { put("vendorEtaRiskScore", it) }
                    record.mitigationCreditScore?.let { put("mitigationCreditScore", it) }
                    record.totalPriorityScore?.let { put("totalPriorityScore", it) }
                }
            }
            is DashboardOverviewEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("totalIncidents", record.totalIncidents)
                    put("assetCount", record.assetCount)
                    put("zoneCount", record.zoneCount)
                    put("impactObservationCount", record.impactObservationCount)
                    put("capacityRiskKw", record.capacityRiskKw)
                    put("affectedGpuCount", record.affectedGpuCount)
                    put("dependencyEdgeCount", record.dependencyEdgeCount)
                    put("trustFindingCount", record.trustFindingCount)
                    record.avgDurationHours?.let { put("avgDurationHours", it) }
                    record.totalDurationHours?.let { put("totalDurationHours", it) }
                    record.totalDelayHours?.let { put("totalDelayHours", it) }
                    record.mitigatedIncidentCount?.let { put("mitigatedIncidentCount", it) }
                    record.affectedRackCount?.let { put("affectedRackCount", it) }
                    record.thermalBreachMinutes?.let { put("thermalBreachMinutes", it) }
                    record.redundancyLostIncidentCount?.let { put("redundancyLostIncidentCount", it) }
                    record.vendorEtaMissedCount?.let { put("vendorEtaMissedCount", it) }
                    record.repeatFailureAssetCount?.let { put("repeatFailureAssetCount", it) }
                    record.engineerAssignmentDelayHours?.let { put("engineerAssignmentDelayHours", it) }
                }
            }
            is PlatformStatusEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("serviceBoundary", record.serviceBoundary)
                    put("platformVerdict", record.platformVerdict)
                    put("reasonCode", record.reasonCode)
                    put("sourceFreshnessStatus", record.sourceFreshnessStatus)
                    record.latestSourceImportAt?.let { put("latestSourceImportAt", it) }
                    put("sourceSystemCount", record.sourceSystemCount)
                    record.latestCanonicalReleaseId?.let { put("latestCanonicalReleaseId", it) }
                    record.latestPromotionAt?.let { put("latestPromotionAt", it) }
                    put("promotionStatus", record.promotionStatus)
                    record.latestReasoningRunId?.let { put("latestReasoningRunId", it) }
                    record.latestAnalysisAt?.let { put("latestAnalysisAt", it) }
                    put("analysisStatus", record.analysisStatus)
                    put("pipelineStatus", record.pipelineStatus)
                    put("reconciliationStatus", record.reconciliationStatus)
                    put("graphValidationStatus", record.graphValidationStatus)
                    put("sourceRecordCount", record.sourceRecordCount)
                    put("incidentCount", record.incidentCount)
                    put("incidentWithProvenanceCount", record.incidentWithProvenanceCount)
                    put("assetCount", record.assetCount)
                    put("assetWithProvenanceCount", record.assetWithProvenanceCount)
                }
            }
            is FilterMetadataEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("filterType", record.filterType)
                    put("resourceUri", record.resourceUri)
                    put("id", record.id)
                    record.label?.let { put("label", it) }
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                }
            }
            is FollowUpDetailEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("assetUri", record.assetUri)
                    put("assetId", record.assetId)
                    put("zoneUri", record.zoneUri)
                    put("zoneId", record.zoneId)
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                    record.impactUri?.let { put("impactUri", it) }
                    record.capacityRiskKw?.let { put("capacityRiskKw", it) }
                    record.affectedGpuCount?.let { put("affectedGpuCount", it) }
                    record.followUpDecisionUri?.let { put("followUpDecisionUri", it) }
                    record.recommendedAction?.let { put("recommendedAction", it) }
                    record.recoveryBlockerUri?.let { put("recoveryBlockerUri", it) }
                    record.blockerSummary?.let { put("blockerSummary", it) }
                    record.restoreReadinessUri?.let { put("restoreReadinessUri", it) }
                    record.restoreReadinessSummary?.let { put("restoreReadinessSummary", it) }
                    record.trustFindingUri?.let { put("trustFindingUri", it) }
                    record.trustSummary?.let { put("trustSummary", it) }
                    record.priorityRank?.let { put("priorityRank", it) }
                    record.requestTitle?.let { put("requestTitle", it) }
                    record.currentStatus?.let { put("currentStatus", it) }
                    record.hoursInCurrentStage?.let { put("hoursInCurrentStage", it) }
                    record.neededByAt?.let { put("neededByAt", it) }
                    record.priorityLevel?.let { put("priorityLevel", it) }
                    record.businessImpact?.let { put("businessImpact", it) }
                    record.assetCriticalityScore?.let { put("assetCriticalityScore", it) }
                    record.downtimeScore?.let { put("downtimeScore", it) }
                    record.stageDelayScore?.let { put("stageDelayScore", it) }
                    record.infrastructureZoneImpactScore?.let { put("infrastructureZoneImpactScore", it) }
                    record.neededByUrgencyScore?.let { put("neededByUrgencyScore", it) }
                    record.repeatFailureScore?.let { put("repeatFailureScore", it) }
                    record.repeatFailureAssetCount?.let { put("repeatFailureAssetCount", it) }
                    record.engineerAssignmentDelayHours?.let { put("engineerAssignmentDelayHours", it) }
                    record.spareRiskScore?.let { put("spareRiskScore", it) }
                    record.capacityRiskScore?.let { put("capacityRiskScore", it) }
                    record.redundancyRiskScore?.let { put("redundancyRiskScore", it) }
                    record.thermalRiskScore?.let { put("thermalRiskScore", it) }
                    record.vendorEtaRiskScore?.let { put("vendorEtaRiskScore", it) }
                    record.mitigationCreditScore?.let { put("mitigationCreditScore", it) }
                    record.totalPriorityScore?.let { put("totalPriorityScore", it) }
                    record.redundancyState?.let { put("redundancyState", it) }
                    record.affectedRackCount?.let { put("affectedRackCount", it) }
                    record.estimatedGpuCapacityRiskPct?.let { put("estimatedGpuCapacityRiskPct", it) }
                    record.thermalBreachMinutes?.let { put("thermalBreachMinutes", it) }
                    record.powerRedundancyLost?.let { put("powerRedundancyLost", it) }
                    record.coolingRedundancyLost?.let { put("coolingRedundancyLost", it) }
                    record.mitigationStatus?.let { put("mitigationStatus", it) }
                    record.vendorEtaAt?.let { put("vendorEtaAt", it) }
                    record.vendorStatus?.let { put("vendorStatus", it) }
                }
            }
            is ImpactSummaryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("impactObservationCount", record.impactObservationCount)
                    put("incidentCount", record.incidentCount)
                    put("capacityRiskKw", record.capacityRiskKw)
                    put("affectedGpuCount", record.affectedGpuCount)
                    put("trustFindingCount", record.trustFindingCount)
                    record.affectedRackCount?.let { put("affectedRackCount", it) }
                    record.thermalBreachMinutes?.let { put("thermalBreachMinutes", it) }
                    record.redundancyLostIncidentCount?.let { put("redundancyLostIncidentCount", it) }
                    record.vendorEtaMissedCount?.let { put("vendorEtaMissedCount", it) }
                    record.mitigatedIncidentCount?.let { put("mitigatedIncidentCount", it) }
                }
            }
            is TopologyDependenciesEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("dependencyEdgeUri", record.dependencyEdgeUri)
                    put("dependencyId", record.dependencyId)
                    put("dependentAssetUri", record.dependentAssetUri)
                    put("dependentAssetId", record.dependentAssetId)
                    put("dependencyAssetUri", record.dependencyAssetUri)
                    put("dependencyAssetId", record.dependencyAssetId)
                    put("dependencyRole", record.dependencyRole)
                    record.impactScope?.let { put("impactScope", it) }
                    record.dependencyPathUri?.let { put("dependencyPathUri", it) }
                    record.pathId?.let { put("pathId", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is TrustFindingsEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("trustFindingUri", record.trustFindingUri)
                    record.trustFindingId?.let { put("trustFindingId", it) }
                    put("summary", record.summary)
                    put("sourceFactUri", record.sourceFactUri)
                    record.activityUri?.let { put("activityUri", it) }
                    record.severity?.let { put("severity", it) }
                    record.status?.let { put("status", it) }
                    record.createdAt?.let { put("createdAt", it) }
                }
            }
            is StageBottlenecksEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    put("incidentCount", record.incidentCount)
                    record.delayedCount?.let { put("delayedCount", it) }
                    record.avgDurationHours?.let { put("avgDurationHours", it) }
                    record.p90DurationHours?.let { put("p90DurationHours", it) }
                    record.totalDelayHours?.let { put("totalDelayHours", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is AssetDelaySummaryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("assetUri", record.assetUri)
                    put("assetId", record.assetId)
                    put("zoneUri", record.zoneUri)
                    put("zoneId", record.zoneId)
                    put("incidentCount", record.incidentCount)
                    put("impactObservationCount", record.impactObservationCount)
                    put("capacityRiskKw", record.capacityRiskKw)
                    put("affectedGpuCount", record.affectedGpuCount)
                    record.delayedIncidentCount?.let { put("delayedIncidentCount", it) }
                    record.repeatFailureCount?.let { put("repeatFailureCount", it) }
                    record.totalDurationHours?.let { put("totalDurationHours", it) }
                    record.avgDurationHours?.let { put("avgDurationHours", it) }
                    record.topFailureMode?.let { put("topFailureMode", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is ZoneDelaySummaryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("zoneUri", record.zoneUri)
                    put("zoneId", record.zoneId)
                    put("assetCount", record.assetCount)
                    put("incidentCount", record.incidentCount)
                    put("impactObservationCount", record.impactObservationCount)
                    put("capacityRiskKw", record.capacityRiskKw)
                    put("affectedGpuCount", record.affectedGpuCount)
                    record.delayedIncidentCount?.let { put("delayedIncidentCount", it) }
                    record.criticalIncidentCount?.let { put("criticalIncidentCount", it) }
                    record.totalDurationHours?.let { put("totalDurationHours", it) }
                    record.topBottleneckStage?.let { put("topBottleneckStage", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is SpareWaitSummaryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    put("incidentCount", record.incidentCount)
                    put("recoveryBlockerCount", record.recoveryBlockerCount)
                    record.totalWaitHours?.let { put("totalWaitHours", it) }
                    record.avgWaitHours?.let { put("avgWaitHours", it) }
                    record.stockStatus?.let { put("stockStatus", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is ValidationSummaryEnvelope -> envelope.records.map { record ->
                mapOf(
                    "graphUri" to record.graphUri,
                    "sourceRecordCount" to record.sourceRecordCount,
                    "incidentCount" to record.incidentCount,
                    "incidentWithProvenanceCount" to record.incidentWithProvenanceCount,
                    "assetCount" to record.assetCount,
                    "assetWithProvenanceCount" to record.assetWithProvenanceCount,
                )
            }
            is IncidentEvidenceEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                    record.impactUri?.let { put("impactUri", it) }
                    record.evidenceUri?.let { put("evidenceUri", it) }
                    record.evidenceClassUri?.let { put("evidenceClassUri", it) }
                    record.evidenceTimestamp?.let { put("evidenceTimestamp", it) }
                    record.confidenceState?.let { put("confidenceState", it) }
                    record.metricName?.let { put("metricName", it) }
                    record.metricValue?.let { put("metricValue", it) }
                    record.metricUnit?.let { put("metricUnit", it) }
                    record.telemetryStatus?.let { put("telemetryStatus", it) }
                    record.telemetryAlertId?.let { put("telemetryAlertId", it) }
                    record.alertType?.let { put("alertType", it) }
                    record.alertSeverity?.let { put("alertSeverity", it) }
                    record.alertTriggeredAt?.let { put("alertTriggeredAt", it) }
                    record.alertResolvedAt?.let { put("alertResolvedAt", it) }
                    record.validationId?.let { put("validationId", it) }
                    record.validationStatus?.let { put("validationStatus", it) }
                    record.validatorId?.let { put("validatorId", it) }
                    record.validationStartedAt?.let { put("validationStartedAt", it) }
                    record.validationCompletedAt?.let { put("validationCompletedAt", it) }
                    record.failureReason?.let { put("failureReason", it) }
                    record.workOrderId?.let { put("workOrderId", it) }
                    record.assignedTeam?.let { put("assignedTeam", it) }
                    record.assignedEngineerId?.let { put("assignedEngineerId", it) }
                    record.workOrderStatus?.let { put("workOrderStatus", it) }
                    record.plannedStartAt?.let { put("plannedStartAt", it) }
                    record.actualStartAt?.let { put("actualStartAt", it) }
                    record.actualCompletedAt?.let { put("actualCompletedAt", it) }
                    record.requiredSpareId?.let { put("requiredSpareId", it) }
                    record.requiredSpareName?.let { put("requiredSpareName", it) }
                    record.stockStatus?.let { put("stockStatus", it) }
                    record.trustFindingUri?.let { put("trustFindingUri", it) }
                    record.trustSummary?.let { put("trustSummary", it) }
                }
            }
            is IncidentTimelineEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("eventUri", record.eventUri)
                    record.eventId?.let { put("eventId", it) }
                    put("stageUri", record.stageUri)
                    record.stageLabel?.let { put("stageLabel", it) }
                    record.eventStatus?.let { put("eventStatus", it) }
                    record.enteredAt?.let { put("enteredAt", it) }
                    record.exitedAt?.let { put("exitedAt", it) }
                    record.durationHours?.let { put("durationHours", it) }
                    record.thresholdHours?.let { put("thresholdHours", it) }
                    record.delayHours?.let { put("delayHours", it) }
                    put("sourceRecordUri", record.sourceRecordUri)
                }
            }
            is DependencyImpactEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("assetUri", record.assetUri)
                    put("assetId", record.assetId)
                    record.dependencyEdgeUri?.let { put("dependencyEdgeUri", it) }
                    record.dependencyId?.let { put("dependencyId", it) }
                    record.dependencyAssetUri?.let { put("dependencyAssetUri", it) }
                    record.dependencyAssetId?.let { put("dependencyAssetId", it) }
                    record.dependencyRole?.let { put("dependencyRole", it) }
                    record.impactScope?.let { put("impactScope", it) }
                    record.findingUri?.let { put("findingUri", it) }
                    record.findingSummary?.let { put("findingSummary", it) }
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                }
            }
            is BlastRadiusEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("assetUri", record.assetUri)
                    put("assetId", record.assetId)
                    record.downstreamAssetUri?.let { put("downstreamAssetUri", it) }
                    record.downstreamAssetId?.let { put("downstreamAssetId", it) }
                    record.incidentUri?.let { put("incidentUri", it) }
                    record.incidentId?.let { put("incidentId", it) }
                    record.findingUri?.let { put("findingUri", it) }
                    record.findingSummary?.let { put("findingSummary", it) }
                }
            }
            is OntologyReviewQueueEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("queueId", record.queueId)
                    put("queueKind", record.queueKind)
                    put("reviewActionId", record.reviewActionId)
                    put("reviewActionLabel", record.reviewActionLabel)
                    put("reviewStatus", record.reviewStatus)
                    put("targetUri", record.targetUri)
                    put("targetType", record.targetType)
                    put("targetLabel", record.targetLabel)
                    put("releaseId", record.releaseId)
                    record.sourceGraphUri?.let { put("sourceGraphUri", it) }
                    record.canonicalGraphUri?.let { put("canonicalGraphUri", it) }
                    record.provenanceGraphUri?.let { put("provenanceGraphUri", it) }
                    record.reasoningAuditGraphUri?.let { put("reasoningAuditGraphUri", it) }
                    record.reasoningGraphUri?.let { put("reasoningGraphUri", it) }
                    put("evidenceSummary", record.evidenceSummary)
                    put("actionStatus", record.actionStatus)
                    put("disabledReason", record.disabledReason)
                    put("incidentCount", record.incidentCount)
                    put("assetCount", record.assetCount)
                    put("sourceRecordCount", record.sourceRecordCount)
                    put("activityCount", record.activityCount)
                    put("generatedFactCount", record.generatedFactCount)
                    put("prioritySortOrder", record.prioritySortOrder)
                }
            }
            is ActionAvailabilityEnvelope -> envelope.records.map { record ->
                mapOf(
                    "graphUri" to record.graphUri,
                    "incidentUri" to record.incidentUri,
                    "incidentId" to record.incidentId,
                    "assetUri" to record.assetUri,
                    "assetId" to record.assetId,
                    "sourceRecordUri" to record.sourceRecordUri,
                    "actionId" to record.actionId,
                    "actionLabel" to record.actionLabel,
                    "actionDescription" to record.actionDescription,
                    "actionStatus" to record.actionStatus,
                    "uiPlacement" to record.uiPlacement,
                    "detailKind" to record.detailKind,
                    "detailRole" to record.detailRole,
                    "detailLabel" to record.detailLabel,
                    "detailValue" to record.detailValue,
                    "detailSortOrder" to record.detailSortOrder,
                )
            }
            is ActionAuditHistoryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("executionUri", record.executionUri)
                    put("executionId", record.executionId)
                    put("requestUri", record.requestUri)
                    put("requestId", record.requestId)
                    put("validationReportUri", record.validationReportUri)
                    put("actionTypeUri", record.actionTypeUri)
                    put("actionTypeId", record.actionTypeId)
                    record.actionTypeLabel?.let { put("actionTypeLabel", it) }
                    put("idempotencyKey", record.idempotencyKey)
                    put("actorId", record.actorId)
                    put("actionReason", record.actionReason)
                    put("actionStatus", record.actionStatus)
                    put("requestedAt", record.requestedAt)
                    put("executedAt", record.executedAt)
                    record.targetObjectUri?.let { put("targetObjectUri", it) }
                    put("validationStatus", record.validationStatus)
                    record.validationSummary?.let { put("validationSummary", it) }
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                    record.assignedTeam?.let { put("assignedTeam", it) }
                    record.assigneeId?.let { put("assigneeId", it) }
                    record.reviewedStatus?.let { put("reviewedStatus", it) }
                    record.reviewSummary?.let { put("reviewSummary", it) }
                    record.supportingEvidenceUri?.let { put("supportingEvidenceUri", it) }
                }
            }
            is ActionNotificationQueueEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("notificationUri", record.notificationUri)
                    put("notificationId", record.notificationId)
                    put("notificationStatus", record.notificationStatus)
                    put("notificationSummary", record.notificationSummary)
                    put("executionUri", record.executionUri)
                    put("executionId", record.executionId)
                    put("requestUri", record.requestUri)
                    put("requestId", record.requestId)
                    put("actionTypeUri", record.actionTypeUri)
                    put("actionTypeId", record.actionTypeId)
                    put("actorId", record.actorId)
                    put("actionReason", record.actionReason)
                    put("requestedAt", record.requestedAt)
                    put("generatedAt", record.generatedAt)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    record.targetObjectUri?.let { put("targetObjectUri", it) }
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                    record.assignedTeam?.let { put("assignedTeam", it) }
                    record.assigneeId?.let { put("assigneeId", it) }
                    record.reviewedStatus?.let { put("reviewedStatus", it) }
                    record.reviewSummary?.let { put("reviewSummary", it) }
                }
            }
            is ActionReviewQueueEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("notificationUri", record.notificationUri)
                    put("notificationId", record.notificationId)
                    put("executionUri", record.executionUri)
                    put("executionId", record.executionId)
                    put("requestUri", record.requestUri)
                    put("requestId", record.requestId)
                    put("actionTypeUri", record.actionTypeUri)
                    put("actionTypeId", record.actionTypeId)
                    put("actorId", record.actorId)
                    put("actionReason", record.actionReason)
                    put("currentState", record.currentState)
                    put("stateGeneratedAt", record.stateGeneratedAt)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                }
            }
            is ActionTransitionHistoryEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("transitionUri", record.transitionUri)
                    put("transitionId", record.transitionId)
                    put("executionUri", record.executionUri)
                    put("executionId", record.executionId)
                    put("requestUri", record.requestUri)
                    put("requestId", record.requestId)
                    put("actionTypeUri", record.actionTypeUri)
                    put("actionTypeId", record.actionTypeId)
                    put("actorId", record.actorId)
                    put("transitionReason", record.transitionReason)
                    record.fromState?.let { put("fromState", it) }
                    put("toState", record.toState)
                    put("generatedAt", record.generatedAt)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                }
            }
            is ActionDispatchQueueEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("dispatchUri", record.dispatchUri)
                    put("dispatchId", record.dispatchId)
                    put("dispatchChannel", record.dispatchChannel)
                    put("dispatchStatus", record.dispatchStatus)
                    put("dispatchLifecycleState", record.dispatchLifecycleState)
                    put("dispatchSummary", record.dispatchSummary)
                    put("executionUri", record.executionUri)
                    put("executionId", record.executionId)
                    put("requestUri", record.requestUri)
                    put("requestId", record.requestId)
                    put("actionTypeUri", record.actionTypeUri)
                    put("actionTypeId", record.actionTypeId)
                    put("transitionUri", record.transitionUri)
                    put("transitionId", record.transitionId)
                    put("actorId", record.actorId)
                    put("generatedAt", record.generatedAt)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    record.sourceRecordUri?.let { put("sourceRecordUri", it) }
                }
            }
            is DynamicPlaybackEnvelope -> envelope.records.map { record ->
                buildMap {
                    put("graphUri", record.graphUri)
                    put("actionAuditReleaseId", record.actionAuditReleaseId)
                    put("eventUri", record.eventUri)
                    put("eventId", record.eventId)
                    put("scenarioId", record.scenarioId)
                    put("playbackBatchId", record.playbackBatchId)
                    put("playbackStep", record.playbackStep)
                    put("incidentUri", record.incidentUri)
                    put("incidentId", record.incidentId)
                    put("eventKind", record.eventKind)
                    put("sourceFamily", record.sourceFamily)
                    put("occurredAt", record.occurredAt)
                    put("summary", record.summary)
                    put("sourceRecordUri", record.sourceRecordUri)
                    put("beforeState", record.beforeState)
                    put("afterState", record.afterState)
                    put("beforeReasoningState", record.beforeReasoningState)
                    put("afterReasoningState", record.afterReasoningState)
                    put("beforeTrustState", record.beforeTrustState)
                    put("afterTrustState", record.afterTrustState)
                    put("beforeBlastRadiusCount", record.beforeBlastRadiusCount)
                    put("afterBlastRadiusCount", record.afterBlastRadiusCount)
                    put("actionLifecycleState", record.actionLifecycleState)
                    record.canonicalGraphUri?.let { put("canonicalGraphUri", it) }
                    record.provenanceGraphUri?.let { put("provenanceGraphUri", it) }
                    record.reasoningGraphUri?.let { put("reasoningGraphUri", it) }
                }
            }
            is AiProposalReviewQueueEnvelope -> envelope.records.map { aiProposalPayload(it) }
            is AiProposalDetailEnvelope -> envelope.records.map { aiProposalPayload(it) }
        }

        return buildMap {
            put("queryId", envelope.queryId)
            put("resultType", envelope.resultType.value)
            put("recordCount", envelope.recordCount)
            put("records", records)
            put("provenance", envelope.provenance.toPayload())
            page?.let {
                put(
                    "pageInfo",
                    mapOf(
                        "page" to it.page,
                        "pageSize" to it.pageSize,
                        "pageCount" to it.pageCount,
                        "totalRecords" to it.totalRecords,
                    ),
                )
            }
        }
    }

    fun error(
        code: SemanticErrorCode,
        message: String,
        detail: String? = null,
        queryId: String? = null,
    ): Map<String, Any> {
        val error = buildMap {
            put("code", code.value)
            put("message", message)
            detail?.let { put("detail", it) }
            queryId?.let { put("queryId", it) }
            put("contractVersion", SemanticErrorContract.VERSION)
        }

        return mapOf("error" to error)
    }

    private fun QueryResultEnvelopeProvenance.toPayload(): Map<String, String> {
        return mapOf(
            "queryId" to queryId,
            "graphScope" to graphScope,
            "contractVersion" to contractVersion,
        )
    }

    private fun aiProposalPayload(record: AiProposalRecord): Map<String, Any> {
        return mapOf(
            "graphUri" to record.graphUri,
            "aiAuditReleaseId" to record.aiAuditReleaseId,
            "proposalUri" to record.proposalUri,
            "proposalId" to record.proposalId,
            "proposalType" to record.proposalType,
            "proposalStatus" to record.proposalStatus,
            "reviewStatus" to record.reviewStatus,
            "disabledReason" to record.disabledReason,
            "summary" to record.summary,
            "rationale" to record.rationale,
            "confidenceScore" to record.confidenceScore,
            "riskLevel" to record.riskLevel,
            "modelId" to record.modelId,
            "promptId" to record.promptId,
            "promptHash" to record.promptHash,
            "actorId" to record.actorId,
            "generatedAt" to record.generatedAt,
            "batchUri" to record.batchUri,
            "batchId" to record.batchId,
            "validationReportUri" to record.validationReportUri,
            "validationStatus" to record.validationStatus,
            "validationSummary" to record.validationSummary,
            "incidentUri" to record.incidentUri,
            "incidentId" to record.incidentId,
            "targetObjectUri" to record.targetObjectUri,
            "sourceRecordUri" to record.sourceRecordUri,
            "supportingEvidenceUri" to record.supportingEvidenceUri,
        )
    }
}

enum class SemanticErrorCode(
    val value: String,
) {
    UNAPPROVED_QUERY_ID("unapproved-query-id"),
    UNSUPPORTED_RESULT_ENVELOPE("unsupported-result-envelope"),
    MISSING_REQUIRED_BINDING("missing-required-binding"),
    GRAPH_UNAVAILABLE("graph-unavailable"),
    CONTRACT_VALIDATION_FAILED("contract-validation-failed"),
    INTERNAL_SEMANTIC_SERVICE_ERROR("internal-semantic-service-error"),
}

object SemanticErrorContract {
    const val VERSION = "2026.06.phase18-error-envelope"
}
