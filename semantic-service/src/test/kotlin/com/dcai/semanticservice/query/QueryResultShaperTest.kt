package com.dcai.semanticservice.query

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class QueryResultShaperTest {
    private val manifest = ApprovedQueryManifest(
        entries = mapOf(
            "fixtureNamedGraphInventory" to definition("fixtureNamedGraphInventory", "fixture source graph, fixture canonical graph"),
            "fixtureIncidentSummary" to definition("fixtureIncidentSummary", "fixture canonical graph"),
            "fixtureProvenanceSourceRecords" to definition("fixtureProvenanceSourceRecords", "fixture source graph, fixture canonical graph"),
            "semanticFollowUpQueueList" to definition("semanticFollowUpQueueList", "fixture canonical graph"),
            "semanticDashboardOverview" to definition("semanticDashboardOverview", "fixture canonical graph"),
            "semanticFilterMetadata" to definition("semanticFilterMetadata", "fixture canonical graph"),
            "semanticFollowUpDetail" to definition("semanticFollowUpDetail", "fixture canonical graph"),
            "semanticImpactSummary" to definition("semanticImpactSummary", "fixture canonical graph"),
            "semanticTopologyDependencies" to definition("semanticTopologyDependencies", "fixture canonical graph"),
            "semanticTrustFindingList" to definition("semanticTrustFindingList", "fixture canonical graph"),
            "semanticStageBottlenecks" to definition("semanticStageBottlenecks", "fixture canonical graph"),
            "semanticAssetDelaySummary" to definition("semanticAssetDelaySummary", "fixture canonical graph"),
            "semanticZoneDelaySummary" to definition("semanticZoneDelaySummary", "fixture canonical graph"),
            "semanticSpareWaitSummary" to definition("semanticSpareWaitSummary", "fixture canonical graph"),
            "semanticValidationSummary" to definition("semanticValidationSummary", "fixture canonical graph"),
            "semanticIncidentEvidence" to definition("semanticIncidentEvidence", "fixture canonical graph"),
            "semanticIncidentTimeline" to definition("semanticIncidentTimeline", "fixture canonical graph"),
            "semanticDependencyImpactByAsset" to definition("semanticDependencyImpactByAsset", "fixture canonical graph"),
            "semanticBlastRadiusByAsset" to definition("semanticBlastRadiusByAsset", "fixture canonical graph"),
            "semanticPromotionReviewQueue" to definition("semanticPromotionReviewQueue", "fixture or promoted graph lifecycle state"),
            "semanticReasoningReviewQueue" to definition("semanticReasoningReviewQueue", "reasoning review state"),
            "semanticAvailableActionsByFinding" to definition("semanticAvailableActionsByFinding", "fixture canonical graph"),
            "semanticActionAuditHistoryByRelease" to definition("semanticActionAuditHistoryByRelease", "managed action-audit graph"),
            "semanticActionAuditHistoryByIncident" to definition("semanticActionAuditHistoryByIncident", "managed action-audit graph"),
            "semanticActionAuditHistoryByTarget" to definition("semanticActionAuditHistoryByTarget", "managed action-audit graph"),
            "semanticActionNotificationQueueByIncident" to definition("semanticActionNotificationQueueByIncident", "managed action-audit notification state"),
            "semanticActionReviewQueueByIncident" to definition("semanticActionReviewQueueByIncident", "managed action-audit lifecycle state"),
            "semanticActionTransitionHistoryByIncident" to definition("semanticActionTransitionHistoryByIncident", "managed action-audit lifecycle state"),
            "semanticActionDispatchQueueByIncident" to definition("semanticActionDispatchQueueByIncident", "managed action-audit dispatch simulation state"),
            "semanticDynamicEventTimelineByIncident" to definition("semanticDynamicEventTimelineByIncident", "managed action-audit dynamic playback state"),
            "semanticDynamicStateChangesByIncident" to definition("semanticDynamicStateChangesByIncident", "managed action-audit dynamic playback state"),
            "semanticDynamicReasoningChangesByIncident" to definition("semanticDynamicReasoningChangesByIncident", "managed action-audit dynamic playback state"),
            "semanticDynamicActionLifecycleByIncident" to definition("semanticDynamicActionLifecycleByIncident", "managed action-audit dynamic playback state"),
            "semanticAiProposalReviewQueue" to definition("semanticAiProposalReviewQueue", "managed ai-audit graph"),
            "semanticAiProposalDetailByIncident" to definition("semanticAiProposalDetailByIncident", "managed ai-audit graph"),
        ),
    )
    private val shaper = QueryResultShaper(manifest)

    @Test
    fun shapesNamedGraphInventoryRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
                rowCount = 1,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "subjectCount" to "8",
                    ),
                ),
            ),
        )

        val typed = assertIs<NamedGraphInventoryEnvelope>(envelope)
        assertEquals(QueryResultType.NAMED_GRAPH_INVENTORY, typed.resultType)
        assertEquals(1, typed.recordCount)
        assertEquals(8, typed.records.single().subjectCount)
        assertEquals(QueryResultEnvelopeProvenance.CONTRACT_VERSION, typed.provenance.contractVersion)
    }

    @Test
    fun shapesIncidentSummaryRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "fixtureIncidentSummary",
                mode = QueryMode.SELECT,
                rowCount = 1,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "incident" to "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                        "incidentId" to "INC-0001",
                        "asset" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                        "stage" to "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                        "sourceRecord" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                    ),
                ),
            ),
        )

        val typed = assertIs<IncidentSummaryEnvelope>(envelope)
        assertEquals(QueryResultType.INCIDENT_SUMMARY, typed.resultType)
        assertEquals("INC-0001", typed.records.single().incidentId)
        assertEquals("fixture canonical graph", typed.provenance.graphScope)
    }

    @Test
    fun shapesProvenanceSourceRecordRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "fixtureProvenanceSourceRecords",
                mode = QueryMode.SELECT,
                rowCount = 1,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:source:minimal-incident",
                        "sourceRecord" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                        "sourceRecordId" to "SRC-INC-0001",
                        "sourceSystem" to "urn:dcai:fixture:valid:minimal-incident:facility-ops",
                        "payloadHash" to "sha256:phase3-minimal-incident",
                        "activity" to "urn:dcai:fixture:valid:minimal-incident:import-activity-0001",
                    ),
                ),
            ),
        )

        val typed = assertIs<ProvenanceSourceRecordsEnvelope>(envelope)
        assertEquals(QueryResultType.PROVENANCE_SOURCE_RECORDS, typed.resultType)
        assertEquals("SRC-INC-0001", typed.records.single().sourceRecordId)
    }

    @Test
    fun shapesFollowUpQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticFollowUpQueueList",
                mode = QueryMode.SELECT,
                rowCount = 1,
                rows = listOf(followUpQueueRow()),
            ),
        )

        val typed = assertIs<FollowUpQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.FOLLOW_UP_QUEUE, typed.resultType)
        assertEquals("INC-0001", record.incidentId)
        assertEquals("ASSET-GPU-RACK-ROW-A", record.assetId)
        assertEquals("ZONE-A", record.zoneId)
        assertEquals("Validation", record.stageLabel)
        assertEquals("urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001", record.sourceRecordUri)
        assertEquals(1, record.priorityRank)
        assertEquals(63.0, record.hoursInCurrentStage)
        assertEquals("CRITICAL", record.priorityLevel)
        assertEquals(169.0, record.totalPriorityScore)
        assertEquals("fixture canonical graph", typed.provenance.graphScope)
    }

    @Test
    fun shapesDashboardOverviewRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticDashboardOverview",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "totalIncidents" to "2",
                        "assetCount" to "3",
                        "zoneCount" to "1",
                        "impactObservationCount" to "1",
                        "capacityRiskKw" to "900.0",
                        "affectedGpuCount" to "320",
                        "dependencyEdgeCount" to "1",
                        "trustFindingCount" to "1",
                        "repeatFailureAssetCount" to "1",
                        "engineerAssignmentDelayHours" to "4.0",
                    ),
                ),
            ),
        )

        val typed = assertIs<DashboardOverviewEnvelope>(envelope)
        assertEquals(QueryResultType.DASHBOARD_OVERVIEW, typed.resultType)
        assertEquals(900.0, typed.records.single().capacityRiskKw)
        assertEquals(320, typed.records.single().affectedGpuCount)
        assertEquals(1, typed.records.single().repeatFailureAssetCount)
        assertEquals(4.0, typed.records.single().engineerAssignmentDelayHours)
    }

    @Test
    fun shapesFilterMetadataRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticFilterMetadata",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "filterType" to "asset",
                        "resource" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                        "id" to "ASSET-GPU-RACK-ROW-A",
                        "label" to "GPU Rack Sensor Row A",
                        "sourceRecord" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                    ),
                ),
            ),
        )

        val typed = assertIs<FilterMetadataEnvelope>(envelope)
        assertEquals(QueryResultType.FILTER_METADATA, typed.resultType)
        assertEquals("asset", typed.records.single().filterType)
    }

    @Test
    fun shapesFollowUpDetailRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticFollowUpDetail",
                mode = QueryMode.SELECT,
                rows = listOf(followUpDetailRow()),
            ),
        )

        val typed = assertIs<FollowUpDetailEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.FOLLOW_UP_DETAIL, typed.resultType)
        assertEquals("INC-0001", record.incidentId)
        assertEquals(900.0, record.capacityRiskKw)
        assertEquals("Escalate vendor ETA.", record.recommendedAction)
        assertEquals("Restore is not ready.", record.restoreReadinessSummary)
        assertEquals("N-1", record.redundancyState)
        assertEquals(40, record.affectedRackCount)
        assertEquals(1, record.repeatFailureAssetCount)
        assertEquals(4.0, record.engineerAssignmentDelayHours)
        assertEquals(true, record.powerRedundancyLost)
        assertEquals("ETA_MISSED", record.vendorStatus)
    }

    @Test
    fun shapesImpactSummaryRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticImpactSummary",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "impactObservationCount" to "1",
                        "incidentCount" to "1",
                        "capacityRiskKw" to "900.0",
                        "affectedGpuCount" to "320",
                        "trustFindingCount" to "1",
                    ),
                ),
            ),
        )

        val typed = assertIs<ImpactSummaryEnvelope>(envelope)
        assertEquals(QueryResultType.IMPACT_SUMMARY, typed.resultType)
        assertEquals(1, typed.records.single().trustFindingCount)
    }

    @Test
    fun shapesTopologyDependencyRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticTopologyDependencies",
                mode = QueryMode.SELECT,
                rows = listOf(topologyDependencyRow()),
            ),
        )

        val typed = assertIs<TopologyDependenciesEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.TOPOLOGY_DEPENDENCIES, typed.resultType)
        assertEquals("POWER_SUPPLY", record.dependencyRole)
        assertEquals("SRC-TOPO", record.sourceRecordUri.substringAfterLast(":"))
    }

    @Test
    fun shapesTrustFindingRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticTrustFindingList",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "trustFinding" to "urn:dcai:fixture:valid:reasoning-output:trust-finding-0001",
                        "trustFindingId" to "TRUST-0001",
                        "summary" to "Impact evidence is supported by telemetry.",
                        "sourceFact" to "urn:dcai:fixture:valid:reasoning-output:impact-0001",
                        "activity" to "urn:dcai:fixture:valid:reasoning-output:reasoning-activity-0001",
                        "severity" to "WARNING",
                        "status" to "FAILED",
                        "createdAt" to "2026-01-08T02:20:00Z",
                    ),
                ),
            ),
        )

        val typed = assertIs<TrustFindingsEnvelope>(envelope)
        assertEquals(QueryResultType.TRUST_FINDINGS, typed.resultType)
        assertEquals("Impact evidence is supported by telemetry.", typed.records.single().summary)
        assertEquals("TRUST-0001", typed.records.single().trustFindingId)
        assertEquals("WARNING", typed.records.single().severity)
        assertEquals("FAILED", typed.records.single().status)
        assertEquals("2026-01-08T02:20:00Z", typed.records.single().createdAt)
    }

    @Test
    fun shapesIncidentEvidenceDetailRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticIncidentEvidence",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        "incidentId" to "INC-REASONING-0001",
                        "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                        "stageLabel" to "Spare/vendor waiting",
                        "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                        "impact" to "urn:dcai:fixture:valid:reasoning-output:impact-0001",
                        "evidence" to "urn:dcai:fixture:valid:reasoning-output:evidence-0001",
                        "evidenceClass" to "urn:dcai:ontology:TelemetryEvidence",
                        "evidenceTimestamp" to "2026-01-08T02:15:00Z",
                        "confidenceState" to "TRUSTED",
                        "metricName" to "fuel_pressure_psi",
                        "metricValue" to "18.0",
                        "metricUnit" to "psi",
                        "telemetryStatus" to "CRITICAL",
                        "telemetryAlertId" to "TEL-ALERT-0001",
                        "alertType" to "FUEL_PRESSURE_LOW",
                        "alertSeverity" to "CRITICAL",
                        "alertTriggeredAt" to "2026-01-08T02:10:00Z",
                        "trustFinding" to "urn:dcai:fixture:valid:reasoning-output:trust-finding-0001",
                        "trustSummary" to "Impact evidence is supported by telemetry.",
                    ),
                ),
            ),
        )

        val typed = assertIs<IncidentEvidenceEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.INCIDENT_EVIDENCE, typed.resultType)
        assertEquals("urn:dcai:ontology:TelemetryEvidence", record.evidenceClassUri)
        assertEquals("2026-01-08T02:15:00Z", record.evidenceTimestamp)
        assertEquals("fuel_pressure_psi", record.metricName)
        assertEquals(18.0, record.metricValue)
        assertEquals("CRITICAL", record.telemetryStatus)
        assertEquals("TEL-ALERT-0001", record.telemetryAlertId)
        assertEquals("FUEL_PRESSURE_LOW", record.alertType)
        assertEquals("CRITICAL", record.alertSeverity)
        assertEquals("2026-01-08T02:10:00Z", record.alertTriggeredAt)
    }

    @Test
    fun shapesIncidentTimelineRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticIncidentTimeline",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        "incidentId" to "INC-REASONING-0001",
                        "event" to "urn:dcai:fixture:valid:reasoning-output:workflow-event-0004",
                        "eventId" to "EVT-0004",
                        "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                        "stageLabel" to "Spare/vendor waiting",
                        "eventStatus" to "ACTIVE",
                        "enteredAt" to "2026-01-06T08:00:00Z",
                        "durationHours" to "63.0",
                        "thresholdHours" to "18.0",
                        "delayHours" to "45.0",
                        "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                    ),
                ),
            ),
        )

        val typed = assertIs<IncidentTimelineEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.INCIDENT_TIMELINE, typed.resultType)
        assertEquals("EVT-0004", record.eventId)
        assertEquals(63.0, record.durationHours)
        assertEquals(18.0, record.thresholdHours)
        assertEquals(45.0, record.delayHours)
    }

    @Test
    fun shapesRemainingDashboardReadModelRows() {
        val cases = listOf(
            "semanticStageBottlenecks" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                "stageLabel" to "Spare/vendor waiting",
                "incidentCount" to "2",
                "delayedCount" to "1",
                "avgDurationHours" to "63.0",
                "p90DurationHours" to "63.0",
                "totalDelayHours" to "45.0",
                "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            ),
            "semanticAssetDelaySummary" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "asset" to "urn:dcai:fixture:valid:reasoning-output:asset-a",
                "assetId" to "ASSET-A",
                "zone" to "urn:dcai:fixture:valid:reasoning-output:zone-a",
                "zoneId" to "ZONE-A",
                "incidentCount" to "1",
                "impactObservationCount" to "1",
                "capacityRiskKw" to "900.0",
                "affectedGpuCount" to "320",
                "delayedIncidentCount" to "1",
                "repeatFailureCount" to "1",
                "totalDurationHours" to "63.0",
                "avgDurationHours" to "63.0",
                "topFailureMode" to "Spare/vendor waiting",
                "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            ),
            "semanticZoneDelaySummary" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "zone" to "urn:dcai:fixture:valid:reasoning-output:zone-a",
                "zoneId" to "ZONE-A",
                "assetCount" to "1",
                "incidentCount" to "1",
                "impactObservationCount" to "1",
                "capacityRiskKw" to "900.0",
                "affectedGpuCount" to "320",
                "delayedIncidentCount" to "1",
                "criticalIncidentCount" to "1",
                "totalDurationHours" to "63.0",
                "topBottleneckStage" to "Spare/vendor waiting",
                "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            ),
            "semanticSpareWaitSummary" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                "stageLabel" to "Spare/vendor waiting",
                "incidentCount" to "1",
                "recoveryBlockerCount" to "1",
                "totalWaitHours" to "63.0",
                "avgWaitHours" to "63.0",
                "stockStatus" to "OUT_OF_STOCK",
                "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            ),
            "semanticValidationSummary" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "sourceRecordCount" to "1",
                "incidentCount" to "1",
                "incidentWithProvenanceCount" to "1",
                "assetCount" to "1",
                "assetWithProvenanceCount" to "1",
            ),
            "semanticIncidentEvidence" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                "incidentId" to "INC-REASONING-0001",
                "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                "stageLabel" to "Spare/vendor waiting",
                "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                "impact" to "urn:dcai:fixture:valid:reasoning-output:impact-0001",
                "evidence" to "urn:dcai:fixture:valid:reasoning-output:evidence-0001",
                "evidenceClass" to "urn:dcai:ontology:TelemetryEvidence",
                "evidenceTimestamp" to "2026-01-08T02:15:00Z",
                "confidenceState" to "TRUSTED",
                "metricName" to "fuel_pressure_psi",
                "metricValue" to "18.0",
                "metricUnit" to "psi",
                "telemetryStatus" to "CRITICAL",
                "telemetryAlertId" to "TEL-ALERT-0001",
                "alertType" to "FUEL_PRESSURE_LOW",
                "alertSeverity" to "CRITICAL",
                "alertTriggeredAt" to "2026-01-08T02:10:00Z",
                "trustFinding" to "urn:dcai:fixture:valid:reasoning-output:trust-finding-0001",
                "trustSummary" to "Impact evidence is supported by telemetry.",
            ),
            "semanticDependencyImpactByAsset" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:dependency-path",
                "asset" to "urn:dcai:fixture:valid:dependency-path:gpu-rack-row-a",
                "assetId" to "ASSET-GPU-RACK-ROW-A",
                "dependencyEdge" to "urn:dcai:fixture:valid:dependency-path:edge-rack-to-pdu",
                "dependencyId" to "EDGE-RACK-PDU-A",
                "dependencyAsset" to "urn:dcai:fixture:valid:dependency-path:rack-pdu-a",
                "dependencyAssetId" to "ASSET-RACK-PDU-A",
                "dependencyRole" to "POWER_SUPPLY",
                "impactScope" to "RACK_ROW",
                "sourceRecord" to "urn:dcai:fixture:valid:dependency-path:source-record-topology-0001",
            ),
            "semanticBlastRadiusByAsset" to mapOf(
                "graph" to "urn:dcai:graph:fixture:canonical:dependency-path",
                "asset" to "urn:dcai:fixture:valid:dependency-path:rack-pdu-a",
                "assetId" to "ASSET-RACK-PDU-A",
                "downstreamAsset" to "urn:dcai:fixture:valid:dependency-path:gpu-rack-row-a",
                "downstreamAssetId" to "ASSET-GPU-RACK-ROW-A",
            ),
        )

        val resultTypes = cases.map { (queryId, row) ->
            shaper.shape(
                QueryExecutionReport(
                    queryId = queryId,
                    mode = QueryMode.SELECT,
                    rows = listOf(row),
                ),
            ).resultType
        }

        assertEquals(
            listOf(
                QueryResultType.STAGE_BOTTLENECKS,
                QueryResultType.ASSET_DELAY_SUMMARY,
                QueryResultType.ZONE_DELAY_SUMMARY,
                QueryResultType.SPARE_WAIT_SUMMARY,
                QueryResultType.VALIDATION_SUMMARY,
                QueryResultType.INCIDENT_EVIDENCE,
                QueryResultType.DEPENDENCY_IMPACT,
                QueryResultType.BLAST_RADIUS,
            ),
            resultTypes,
        )
    }

    @Test
    fun rejectsUnsupportedEnvelopeQueryId() {
        val unsupportedManifest = ApprovedQueryManifest(
            entries = mapOf(
                "unsupported" to definition("unsupported", "fixture graph"),
            ),
        )

        assertFailsWith<IllegalStateException> {
            QueryResultShaper(unsupportedManifest).shape(
                QueryExecutionReport(
                    queryId = "unsupported",
                    mode = QueryMode.SELECT,
                ),
            )
        }
    }

    @Test
    fun rejectsModeMismatchBetweenReportAndManifest() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "fixtureNamedGraphInventory",
                    mode = QueryMode.ASK,
                ),
            )
        }
    }

    @Test
    fun rejectsMissingRequiredBindings() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "fixtureNamedGraphInventory",
                    mode = QueryMode.SELECT,
                    rows = listOf(mapOf("graph" to "urn:dcai:graph:fixture:source:minimal-incident")),
                ),
            )
        }
    }

    @Test
    fun rejectsFollowUpQueueRowsMissingProvenanceCarryingIdentifiers() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "semanticFollowUpQueueList",
                    mode = QueryMode.SELECT,
                    rows = listOf(
                        followUpQueueRow() - "incidentId",
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsFollowUpQueueRowsMissingSourceRecordProvenance() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "semanticFollowUpQueueList",
                    mode = QueryMode.SELECT,
                    rows = listOf(
                        followUpQueueRow() - "sourceRecord",
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsFollowUpDetailRowsMissingSourceRecordProvenance() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "semanticFollowUpDetail",
                    mode = QueryMode.SELECT,
                    rows = listOf(
                        followUpDetailRow() - "sourceRecord",
                    ),
                ),
            )
        }
    }

    @Test
    fun rejectsTopologyRowsMissingSourceRecordProvenance() {
        assertFailsWith<IllegalArgumentException> {
            shaper.shape(
                QueryExecutionReport(
                    queryId = "semanticTopologyDependencies",
                    mode = QueryMode.SELECT,
                    rows = listOf(
                        topologyDependencyRow() - "sourceRecord",
                    ),
                ),
            )
        }
    }

    @Test
    fun shapesActionAuditHistoryRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticActionAuditHistoryByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionAuditHistoryRow()),
            ),
        )

        val typed = assertIs<ActionAuditHistoryEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_AUDIT_HISTORY, typed.resultType)
        assertEquals("local-action-audit-v1", record.actionAuditReleaseId)
        assertEquals("AcknowledgeRestoreBlocker", record.actionTypeId)
        assertEquals("operator-001", record.actorId)
        assertEquals("CONFORMS", record.validationStatus)
        assertEquals("urn:dcai:fixture:valid:reasoning-output:incident-0001", record.targetObjectUri)
        assertEquals("managed action-audit graph", typed.provenance.graphScope)
    }

    @Test
    fun shapesActionAvailabilityRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticAvailableActionsByFinding",
                mode = QueryMode.SELECT,
                rows = listOf(actionAvailabilityRow()),
            ),
        )

        val typed = assertIs<ActionAvailabilityEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_AVAILABILITY, typed.resultType)
        assertEquals("INC-0001", record.incidentId)
        assertEquals("AcknowledgeRestoreBlocker", record.actionId)
        assertEquals("RestoreReadinessFinding", record.detailRole)
        assertEquals("targetObject", record.detailKind)
        assertEquals(100, record.detailSortOrder)
    }

    @Test
    fun shapesActionNotificationQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticActionNotificationQueueByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionNotificationQueueRow()),
            ),
        )

        val typed = assertIs<ActionNotificationQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_NOTIFICATION_QUEUE, typed.resultType)
        assertEquals("local-action-audit-v1", record.actionAuditReleaseId)
        assertEquals("QUEUED", record.notificationStatus)
        assertEquals("AcknowledgeRestoreBlocker", record.actionTypeId)
        assertEquals("INC-0001", record.incidentId)
        assertEquals("managed action-audit notification state", typed.provenance.graphScope)
    }

    @Test
    fun shapesActionReviewQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticActionReviewQueueByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionReviewQueueRow()),
            ),
        )

        val typed = assertIs<ActionReviewQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_REVIEW_QUEUE, typed.resultType)
        assertEquals("local-action-audit-v1", record.actionAuditReleaseId)
        assertEquals("AcknowledgeRestoreBlocker", record.actionTypeId)
        assertEquals("QUEUED", record.currentState)
        assertEquals("INC-0001", record.incidentId)
        assertEquals("managed action-audit lifecycle state", typed.provenance.graphScope)
    }

    @Test
    fun shapesActionTransitionHistoryRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticActionTransitionHistoryByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionTransitionHistoryRow()),
            ),
        )

        val typed = assertIs<ActionTransitionHistoryEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_TRANSITION_HISTORY, typed.resultType)
        assertEquals("ACT-TRN-REVIEW-001", record.transitionId)
        assertEquals("QUEUED", record.fromState)
        assertEquals("IN_REVIEW", record.toState)
        assertEquals("managed action-audit lifecycle state", typed.provenance.graphScope)
    }

    @Test
    fun shapesActionDispatchQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticActionDispatchQueueByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionDispatchQueueRow()),
            ),
        )

        val typed = assertIs<ActionDispatchQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ACTION_DISPATCH_QUEUE, typed.resultType)
        assertEquals("ACT-DSP-NOC-001", record.dispatchId)
        assertEquals("NOC_QUEUE", record.dispatchChannel)
        assertEquals("SIMULATED_QUEUED", record.dispatchStatus)
        assertEquals("APPROVED", record.dispatchLifecycleState)
        assertEquals("ACT-TRN-REVIEW-001", record.transitionId)
        assertEquals("managed action-audit dispatch simulation state", typed.provenance.graphScope)
    }

    @Test
    fun shapesDynamicPlaybackRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticDynamicEventTimelineByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(dynamicPlaybackRow()),
            ),
        )

        val typed = assertIs<DynamicPlaybackEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.DYNAMIC_PLAYBACK, typed.resultType)
        assertEquals("DYN-EVENT-1", record.eventId)
        assertEquals(1, record.playbackStep)
        assertEquals("SOURCE_EXPORT_RECEIVED", record.beforeState)
        assertEquals("CANONICAL_PROMOTED", record.afterState)
        assertEquals(0, record.beforeBlastRadiusCount)
        assertEquals(1, record.afterBlastRadiusCount)
        assertEquals("managed action-audit dynamic playback state", typed.provenance.graphScope)
    }

    @Test
    fun shapesAiProposalReviewQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticAiProposalReviewQueue",
                mode = QueryMode.SELECT,
                rows = listOf(aiProposalRow()),
            ),
        )

        val typed = assertIs<AiProposalReviewQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.AI_PROPOSAL_REVIEW_QUEUE, typed.resultType)
        assertEquals("AI-PROP-LOCAL-001", record.proposalId)
        assertEquals("ACTION_RECOMMENDATION", record.proposalType)
        assertEquals(0.82, record.confidenceScore)
        assertEquals("PENDING_HUMAN_REVIEW", record.reviewStatus)
        assertEquals("managed ai-audit graph", typed.provenance.graphScope)
    }

    @Test
    fun shapesAiProposalDetailRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticAiProposalDetailByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(aiProposalRow()),
            ),
        )

        val typed = assertIs<AiProposalDetailEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.AI_PROPOSAL_DETAIL, typed.resultType)
        assertEquals("INC-001", record.incidentId)
        assertEquals("HIGH", record.riskLevel)
    }

    @Test
    fun shapesOntologyReviewQueueRows() {
        val envelope = shaper.shape(
            QueryExecutionReport(
                queryId = "semanticReasoningReviewQueue",
                mode = QueryMode.SELECT,
                rows = listOf(ontologyReviewQueueRow()),
            ),
        )

        val typed = assertIs<OntologyReviewQueueEnvelope>(envelope)
        val record = typed.records.single()
        assertEquals(QueryResultType.ONTOLOGY_REVIEW_QUEUE, typed.resultType)
        assertEquals("reasoning-approval", record.queueKind)
        assertEquals("ApproveReasoningFinding", record.reviewActionId)
        assertEquals("PENDING_APPROVAL_REVIEW", record.reviewStatus)
        assertEquals("ReasoningActivity", record.targetType)
        assertEquals(3, record.generatedFactCount)
        assertEquals("reasoning review state", typed.provenance.graphScope)
    }

    private fun followUpQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
            "incident" to "urn:dcai:fixture:valid:minimal-incident:inc-0001",
            "incidentId" to "INC-0001",
            "asset" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
            "assetId" to "ASSET-GPU-RACK-ROW-A",
            "zone" to "urn:dcai:fixture:valid:minimal-incident:zone-a",
            "zoneId" to "ZONE-A",
            "stage" to "urn:dcai:fixture:valid:minimal-incident:stage-validation",
            "stageLabel" to "Validation",
            "sourceRecord" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
            "priorityRank" to "1",
            "requestTitle" to "Backup generator fuel system vendor wait",
            "currentStatus" to "BLOCKED",
            "hoursInCurrentStage" to "63.0",
            "neededByAt" to "2026-01-08T18:00:00Z",
            "priorityLevel" to "CRITICAL",
            "businessImpact" to "320 GPUs and 900 kW at risk",
            "assetCriticalityScore" to "20.0",
            "downtimeScore" to "30.0",
            "stageDelayScore" to "15.0",
            "infrastructureZoneImpactScore" to "20.0",
            "neededByUrgencyScore" to "10.0",
            "repeatFailureScore" to "0.0",
            "repeatFailureAssetCount" to "1",
            "engineerAssignmentDelayHours" to "4.0",
            "spareRiskScore" to "22.0",
            "capacityRiskScore" to "30.0",
            "redundancyRiskScore" to "24.0",
            "thermalRiskScore" to "0.0",
            "vendorEtaRiskScore" to "22.0",
            "mitigationCreditScore" to "4.0",
            "totalPriorityScore" to "169.0",
        )
    }

    private fun followUpDetailRow(): Map<String, String> {
        return followUpQueueRow() + mapOf(
            "impact" to "urn:dcai:fixture:valid:reasoning-output:impact-0001",
            "capacityRiskKw" to "900.0",
            "affectedGpuCount" to "320",
            "followUpDecision" to "urn:dcai:fixture:valid:reasoning-output:follow-up-decision-0001",
            "recommendedAction" to "Escalate vendor ETA.",
            "recoveryBlocker" to "urn:dcai:fixture:valid:reasoning-output:recovery-blocker-0001",
            "blockerSummary" to "Current blocker is spare/vendor waiting.",
            "restoreReadiness" to "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
            "restoreReadinessSummary" to "Restore is not ready.",
            "trustFinding" to "urn:dcai:fixture:valid:reasoning-output:trust-finding-0001",
            "trustSummary" to "Impact evidence is supported by telemetry.",
            "redundancyState" to "N-1",
            "affectedRackCount" to "40",
            "estimatedGpuCapacityRiskPct" to "40.0",
            "thermalBreachMinutes" to "0",
            "powerRedundancyLost" to "true",
            "coolingRedundancyLost" to "false",
            "mitigationStatus" to "RUNNING_DEGRADED",
            "vendorEtaAt" to "2026-01-08T01:00:00Z",
            "vendorStatus" to "ETA_MISSED",
        )
    }

    private fun topologyDependencyRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:fixture:canonical:dependency-path",
            "dependencyEdge" to "urn:dcai:fixture:valid:dependency-path:edge-rack-to-pdu",
            "dependencyId" to "EDGE-RACK-PDU-A",
            "dependentAsset" to "urn:dcai:fixture:valid:dependency-path:gpu-rack-row-a",
            "dependentAssetId" to "ASSET-GPU-RACK-ROW-A",
            "dependencyAsset" to "urn:dcai:fixture:valid:dependency-path:rack-pdu-a",
            "dependencyAssetId" to "ASSET-RACK-PDU-A",
            "dependencyRole" to "POWER_SUPPLY",
            "impactScope" to "RACK_ROW",
            "dependencyPath" to "urn:dcai:fixture:valid:dependency-path:power-path-a",
            "pathId" to "PATH-POWER-A",
            "sourceRecord" to "urn:dcai:fixture:valid:dependency-path:SRC-TOPO",
        )
    }

    private fun actionAuditHistoryRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "validationReport" to "urn:dcai:action-validation-report:ack-restore-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "actionTypeLabel" to "AcknowledgeRestoreBlocker",
            "idempotencyKey" to "ack-restore-001",
            "actorId" to "operator-001",
            "actionReason" to "Reviewed restore blocker before shift handoff.",
            "actionStatus" to "QUEUED",
            "requestedAt" to "2026-06-14T10:15:30Z",
            "executedAt" to "2026-06-14T10:15:30Z",
            "targetObject" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "validationStatus" to "CONFORMS",
            "validationSummary" to "Ontology action request passed local precondition and provenance validation.",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            "assignedTeam" to "DC_FACILITY_OPS",
            "assigneeId" to "engineer-017",
            "reviewedStatus" to "ACKNOWLEDGED",
            "reviewSummary" to "Blocker reviewed.",
            "supportingEvidence" to "urn:dcai:fixture:valid:reasoning-output:evidence-0001",
        )
    }

    private fun actionNotificationQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "notification" to "urn:dcai:ontology-action-notification:ack-restore-001",
            "notificationId" to "REQ-ACTION-001:notification",
            "notificationStatus" to "QUEUED",
            "notificationSummary" to "Internal AcknowledgeRestoreBlocker request was audited and queued for local review.",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "actorId" to "operator-001",
            "actionReason" to "Reviewed restore blocker before shift handoff.",
            "requestedAt" to "2026-06-14T10:15:30Z",
            "generatedAt" to "2026-06-14T10:15:30Z",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-0001",
            "targetObject" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            "assignedTeam" to "DC_FACILITY_OPS",
            "assigneeId" to "engineer-017",
            "reviewedStatus" to "ACKNOWLEDGED",
            "reviewSummary" to "Blocker reviewed.",
        )
    }

    private fun actionReviewQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "notification" to "urn:dcai:ontology-action-notification:ack-restore-001",
            "notificationId" to "REQ-ACTION-001:notification",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "actorId" to "operator-001",
            "actionReason" to "Reviewed restore blocker before shift handoff.",
            "currentState" to "QUEUED",
            "stateGeneratedAt" to "2026-06-14T10:15:33Z",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-0001",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
        )
    }

    private fun actionTransitionHistoryRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "transition" to "urn:dcai:ontology-action-transition:review-start-001",
            "transitionId" to "ACT-TRN-REVIEW-001",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "actorId" to "operator-001",
            "transitionReason" to "Local reviewer started internal action review.",
            "fromState" to "QUEUED",
            "toState" to "IN_REVIEW",
            "generatedAt" to "2026-06-14T10:20:30Z",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-0001",
        )
    }

    private fun actionDispatchQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "dispatch" to "urn:dcai:ontology-action-dispatch:noc-queue-001",
            "dispatchId" to "ACT-DSP-NOC-001",
            "dispatchChannel" to "NOC_QUEUE",
            "dispatchStatus" to "SIMULATED_QUEUED",
            "dispatchLifecycleState" to "APPROVED",
            "dispatchSummary" to "Simulated NOC queue dispatch for approved ontology action.",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "transition" to "urn:dcai:ontology-action-transition:review-start-001",
            "transitionId" to "ACT-TRN-REVIEW-001",
            "actorId" to "operator-001",
            "generatedAt" to "2026-06-14T10:20:30Z",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-0001",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
        )
    }

    private fun dynamicPlaybackRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-dynamic-action-audit-v1",
            "actionAuditReleaseId" to "local-dynamic-action-audit-v1",
            "event" to "urn:dcai:dynamic-playback-event:DYN-EVENT-1",
            "eventId" to "DYN-EVENT-1",
            "scenarioId" to "local-dynamic-playback-v1",
            "playbackBatchId" to "local-dynamic-playback-batch-v1",
            "playbackStep" to "1",
            "incident" to "urn:dcai:incident:INC-DYN-001",
            "incidentId" to "INC-DYN-001",
            "eventKind" to "TELEMETRY_IMPACT_CHANGE",
            "sourceFamily" to "telemetry-impact",
            "occurredAt" to "2026-06-10T00:05:00Z",
            "summary" to "Telemetry export shows UPS degradation.",
            "sourceRecord" to "urn:dcai:source-record:local-dynamic-source-systems:SRC-DYN-IMPACT-001",
            "beforeState" to "SOURCE_EXPORT_RECEIVED",
            "afterState" to "CANONICAL_PROMOTED",
            "beforeReasoningState" to "NO_REASONING_OUTPUT",
            "afterReasoningState" to "DEPENDENCY_EXPOSURE_INFERRED",
            "beforeTrustState" to "UNKNOWN",
            "afterTrustState" to "TRUSTED_TELEMETRY",
            "beforeBlastRadiusCount" to "0",
            "afterBlastRadiusCount" to "1",
            "actionLifecycleState" to "NONE",
            "canonicalGraph" to "urn:dcai:graph:canonical:local-dynamic-playback-v1-step-01",
            "provenanceGraph" to "urn:dcai:graph:provenance:local-dynamic-playback-v1-step-01",
            "reasoningGraph" to "urn:dcai:graph:reasoning:local-dynamic-playback-v1-reasoning-01",
        )
    }

    private fun aiProposalRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:ai-audit:local-ai-governance-v1",
            "aiAuditReleaseId" to "local-ai-governance-v1",
            "proposal" to "urn:dcai:ai-proposal:local-ai-governance-v1",
            "proposalId" to "AI-PROP-LOCAL-001",
            "proposalType" to "ACTION_RECOMMENDATION",
            "proposalStatus" to "PENDING_REVIEW",
            "reviewStatus" to "PENDING_HUMAN_REVIEW",
            "disabledReason" to "AI proposal review is read-only.",
            "summary" to "AI proposal recommends a governed action.",
            "rationale" to "Evidence and reasoning support human review.",
            "confidenceScore" to "0.82",
            "riskLevel" to "HIGH",
            "modelId" to "local-governance-model-placeholder",
            "promptId" to "ai-governance-proposal-v1",
            "promptHash" to "sha256-local-placeholder-001",
            "actorId" to "local-ai-governance-simulator",
            "generatedAt" to "2026-06-09T02:45:00Z",
            "batch" to "urn:dcai:ai-proposal-batch:local-ai-governance-v1",
            "batchId" to "local-ai-governance-v1",
            "validationReport" to "urn:dcai:ai-proposal-validation-report:local-ai-governance-v1",
            "validationStatus" to "CONFORMS",
            "validationSummary" to "AI proposal passed policy.",
            "incident" to "urn:dcai:incident:INC-001",
            "incidentId" to "INC-001",
            "targetObject" to "urn:dcai:incident:INC-001",
            "sourceRecord" to "urn:dcai:source-record:local-controlled-facility-ops-file:SRC-INC-001",
            "supportingEvidence" to "urn:dcai:reasoning:restore-readiness:urn%3Adcai%3Aincident%3AINC-001",
        )
    }

    private fun actionAvailabilityRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-0001",
            "asset" to "urn:dcai:fixture:valid:reasoning-output:asset-a",
            "assetId" to "ASSET-A",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            "actionId" to "AcknowledgeRestoreBlocker",
            "actionLabel" to "Acknowledge restore blocker",
            "actionDescription" to "Record that an operator reviewed the restore-readiness blocker without changing canonical or reasoning graph state.",
            "actionStatus" to "DISABLED",
            "uiPlacement" to "summary",
            "detailKind" to "targetObject",
            "detailRole" to "RestoreReadinessFinding",
            "detailLabel" to "Restore is not ready.",
            "detailValue" to "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
            "detailSortOrder" to "100",
        )
    }

    private fun ontologyReviewQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
            "queueId" to "reasoning-approval:local-controlled-reasoning-v1:ReasoningActivity",
            "queueKind" to "reasoning-approval",
            "reviewActionId" to "ApproveReasoningFinding",
            "reviewActionLabel" to "Review reasoning finding approval",
            "reviewStatus" to "PENDING_APPROVAL_REVIEW",
            "targetUri" to "urn:dcai:reasoning-activity:local-controlled-reasoning-v1",
            "targetType" to "ReasoningActivity",
            "targetLabel" to "local-controlled-reasoning-v1",
            "releaseId" to "local-controlled-reasoning-v1",
            "canonicalGraph" to "urn:dcai:graph:canonical:local-controlled-source-v1",
            "reasoningAuditGraph" to "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
            "evidenceSummary" to "Reasoning-audit graph contains candidate findings with ReasoningActivity provenance.",
            "actionStatus" to "DISABLED",
            "disabledReason" to "Reasoning finding approval remains internal-only.",
            "incidentCount" to "2",
            "assetCount" to "2",
            "sourceRecordCount" to "4",
            "activityCount" to "1",
            "generatedFactCount" to "3",
            "prioritySortOrder" to "110",
        )
    }

    private fun definition(
        id: String,
        graphScope: String,
    ): ApprovedQueryDefinition {
        return ApprovedQueryDefinition(
            id = id,
            path = Path.of("queries/inspection/$id.select.rq"),
            mode = QueryMode.SELECT,
            graphScope = graphScope,
            sparql = "SELECT * WHERE { ?s ?p ?o }",
        )
    }
}
