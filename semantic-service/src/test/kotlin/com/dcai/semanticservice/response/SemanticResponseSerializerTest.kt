package com.dcai.semanticservice.response

import com.dcai.semanticservice.query.AssetDelaySummaryEnvelope
import com.dcai.semanticservice.query.AssetDelaySummaryRecord
import com.dcai.semanticservice.query.ActionAuditHistoryEnvelope
import com.dcai.semanticservice.query.ActionAuditHistoryRecord
import com.dcai.semanticservice.query.ActionNotificationQueueEnvelope
import com.dcai.semanticservice.query.ActionNotificationQueueRecord
import com.dcai.semanticservice.query.ActionReviewQueueEnvelope
import com.dcai.semanticservice.query.ActionReviewQueueRecord
import com.dcai.semanticservice.query.ActionTransitionHistoryEnvelope
import com.dcai.semanticservice.query.ActionTransitionHistoryRecord
import com.dcai.semanticservice.query.ActionDispatchQueueEnvelope
import com.dcai.semanticservice.query.ActionDispatchQueueRecord
import com.dcai.semanticservice.query.ActionAvailabilityEnvelope
import com.dcai.semanticservice.query.ActionAvailabilityRecord
import com.dcai.semanticservice.query.AiProposalRecord
import com.dcai.semanticservice.query.AiProposalReviewQueueEnvelope
import com.dcai.semanticservice.query.DashboardOverviewEnvelope
import com.dcai.semanticservice.query.DashboardOverviewRecord
import com.dcai.semanticservice.query.DynamicPlaybackEnvelope
import com.dcai.semanticservice.query.DynamicPlaybackRecord
import com.dcai.semanticservice.query.FilterMetadataEnvelope
import com.dcai.semanticservice.query.FilterMetadataRecord
import com.dcai.semanticservice.query.FollowUpDetailEnvelope
import com.dcai.semanticservice.query.FollowUpDetailRecord
import com.dcai.semanticservice.query.IncidentSummaryEnvelope
import com.dcai.semanticservice.query.IncidentSummaryRecord
import com.dcai.semanticservice.query.FollowUpQueueEnvelope
import com.dcai.semanticservice.query.FollowUpQueueRecord
import com.dcai.semanticservice.query.ImpactSummaryEnvelope
import com.dcai.semanticservice.query.ImpactSummaryRecord
import com.dcai.semanticservice.query.IncidentEvidenceEnvelope
import com.dcai.semanticservice.query.IncidentEvidenceRecord
import com.dcai.semanticservice.query.IncidentTimelineEnvelope
import com.dcai.semanticservice.query.IncidentTimelineRecord
import com.dcai.semanticservice.query.NamedGraphInventoryEnvelope
import com.dcai.semanticservice.query.NamedGraphInventoryRecord
import com.dcai.semanticservice.query.OntologyReviewQueueEnvelope
import com.dcai.semanticservice.query.OntologyReviewQueueRecord
import com.dcai.semanticservice.query.ProvenanceSourceRecord
import com.dcai.semanticservice.query.ProvenanceSourceRecordsEnvelope
import com.dcai.semanticservice.query.QueryResultEnvelopeProvenance
import com.dcai.semanticservice.query.TopologyDependenciesEnvelope
import com.dcai.semanticservice.query.TopologyDependencyRecord
import com.dcai.semanticservice.query.TrustFindingRecord
import com.dcai.semanticservice.query.TrustFindingsEnvelope
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticResponseSerializerTest {
    private val serializer = SemanticResponseSerializer()

    @Test
    fun serializesNamedGraphInventoryEnvelope() {
        val payload = serializer.serialize(
            NamedGraphInventoryEnvelope(
                queryId = "fixtureNamedGraphInventory",
                records = listOf(
                    NamedGraphInventoryRecord(
                        graphUri = "urn:dcai:graph:fixture:canonical:minimal-incident",
                        subjectCount = 8,
                    ),
                ),
                provenance = provenance("fixtureNamedGraphInventory"),
            ),
        )

        assertEquals(setOf("queryId", "resultType", "recordCount", "records", "provenance"), payload.keys)
        assertEquals("fixtureNamedGraphInventory", payload["queryId"])
        assertEquals("named-graph-inventory", payload["resultType"])
        assertEquals(1, payload["recordCount"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                    "subjectCount" to 8,
                ),
            ),
            payload["records"],
        )
        assertEquals(provenancePayload("fixtureNamedGraphInventory"), payload["provenance"])
    }

    @Test
    fun serializesIncidentSummaryEnvelopeWithoutNullOptionalFields() {
        val payload = serializer.serialize(
            IncidentSummaryEnvelope(
                queryId = "fixtureIncidentSummary",
                records = listOf(
                    IncidentSummaryRecord(
                        graphUri = "urn:dcai:graph:fixture:canonical:minimal-incident",
                        incidentUri = "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                        incidentId = "INC-0001",
                        assetUri = "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                        stageUri = "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                        sourceRecordUri = null,
                    ),
                ),
                provenance = provenance("fixtureIncidentSummary"),
            ),
        )

        assertEquals("incident-summary", payload["resultType"])
        assertEquals(1, payload["recordCount"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                    "incidentUri" to "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                    "incidentId" to "INC-0001",
                    "assetUri" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                    "stageUri" to "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesProvenanceSourceRecordsEnvelope() {
        val payload = serializer.serialize(
            ProvenanceSourceRecordsEnvelope(
                queryId = "fixtureProvenanceSourceRecords",
                records = listOf(
                    ProvenanceSourceRecord(
                        graphUri = "urn:dcai:graph:fixture:source:minimal-incident",
                        sourceRecordUri = "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                        sourceRecordId = "SRC-INC-0001",
                        sourceSystemUri = "urn:dcai:fixture:valid:minimal-incident:facility-ops",
                        payloadHash = "sha256:phase3-minimal-incident",
                        activityUri = "urn:dcai:fixture:valid:minimal-incident:import-activity-0001",
                    ),
                ),
                provenance = provenance("fixtureProvenanceSourceRecords"),
            ),
        )

        assertEquals("provenance-source-records", payload["resultType"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:fixture:source:minimal-incident",
                    "sourceRecordUri" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                    "sourceRecordId" to "SRC-INC-0001",
                    "sourceSystemUri" to "urn:dcai:fixture:valid:minimal-incident:facility-ops",
                    "payloadHash" to "sha256:phase3-minimal-incident",
                    "activityUri" to "urn:dcai:fixture:valid:minimal-incident:import-activity-0001",
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesActionAuditHistoryEnvelopeWithoutNullOptionalFields() {
        val payload = serializer.serialize(
            ActionAuditHistoryEnvelope(
                queryId = "semanticActionAuditHistoryByIncident",
                records = listOf(
                    ActionAuditHistoryRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                        actionAuditReleaseId = "local-action-audit-v1",
                        executionUri = "urn:dcai:ontology-action-execution:ack-restore-001",
                        executionId = "ack-restore-001",
                        requestUri = "urn:dcai:ontology-action-request:ack-restore-001",
                        requestId = "REQ-ACTION-001",
                        validationReportUri = "urn:dcai:action-validation-report:ack-restore-001",
                        actionTypeUri = "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                        actionTypeId = "AcknowledgeRestoreBlocker",
                        idempotencyKey = "ack-restore-001",
                        actorId = "operator-001",
                        actionReason = "Reviewed restore blocker before shift handoff.",
                        actionStatus = "QUEUED",
                        requestedAt = "2026-06-14T10:15:30Z",
                        executedAt = "2026-06-14T10:15:30Z",
                        targetObjectUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        validationStatus = "CONFORMS",
                    ),
                ),
                provenance = provenance("semanticActionAuditHistoryByIncident"),
            ),
        )

        assertEquals("action-audit-history", payload["resultType"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:action-audit:local-action-audit-v1",
                    "actionAuditReleaseId" to "local-action-audit-v1",
                    "executionUri" to "urn:dcai:ontology-action-execution:ack-restore-001",
                    "executionId" to "ack-restore-001",
                    "requestUri" to "urn:dcai:ontology-action-request:ack-restore-001",
                    "requestId" to "REQ-ACTION-001",
                    "validationReportUri" to "urn:dcai:action-validation-report:ack-restore-001",
                    "actionTypeUri" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                    "actionTypeId" to "AcknowledgeRestoreBlocker",
                    "idempotencyKey" to "ack-restore-001",
                    "actorId" to "operator-001",
                    "actionReason" to "Reviewed restore blocker before shift handoff.",
                    "actionStatus" to "QUEUED",
                    "requestedAt" to "2026-06-14T10:15:30Z",
                    "executedAt" to "2026-06-14T10:15:30Z",
                    "targetObjectUri" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                    "validationStatus" to "CONFORMS",
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesActionAvailabilityEnvelope() {
        val payload = serializer.serialize(
            ActionAvailabilityEnvelope(
                queryId = "semanticAvailableActionsByFinding",
                records = listOf(
                    ActionAvailabilityRecord(
                        graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                        incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        incidentId = "INC-0001",
                        assetUri = "urn:dcai:fixture:valid:reasoning-output:asset-a",
                        assetId = "ASSET-A",
                        sourceRecordUri = "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                        actionId = "AcknowledgeRestoreBlocker",
                        actionLabel = "Acknowledge restore blocker",
                        actionDescription = "Record that an operator reviewed the restore-readiness blocker.",
                        actionStatus = "AVAILABLE_FOR_LOCAL_AUDIT",
                        uiPlacement = "summary",
                        detailKind = "targetObject",
                        detailRole = "RestoreReadinessFinding",
                        detailLabel = "Restore is not ready.",
                        detailValue = "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
                        detailSortOrder = 100,
                    ),
                ),
                provenance = provenance("semanticAvailableActionsByFinding"),
            ),
        )

        assertEquals("action-availability", payload["resultType"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                    "incidentUri" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                    "incidentId" to "INC-0001",
                    "assetUri" to "urn:dcai:fixture:valid:reasoning-output:asset-a",
                    "assetId" to "ASSET-A",
                    "sourceRecordUri" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                    "actionId" to "AcknowledgeRestoreBlocker",
                    "actionLabel" to "Acknowledge restore blocker",
                    "actionDescription" to "Record that an operator reviewed the restore-readiness blocker.",
                    "actionStatus" to "AVAILABLE_FOR_LOCAL_AUDIT",
                    "uiPlacement" to "summary",
                    "detailKind" to "targetObject",
                    "detailRole" to "RestoreReadinessFinding",
                    "detailLabel" to "Restore is not ready.",
                    "detailValue" to "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
                    "detailSortOrder" to 100,
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesActionNotificationQueueEnvelope() {
        val payload = serializer.serialize(
            ActionNotificationQueueEnvelope(
                queryId = "semanticActionNotificationQueueByIncident",
                records = listOf(
                    ActionNotificationQueueRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                        actionAuditReleaseId = "local-action-audit-v1",
                        notificationUri = "urn:dcai:ontology-action-notification:ack-restore-001",
                        notificationId = "REQ-ACTION-001:notification",
                        notificationStatus = "QUEUED",
                        notificationSummary = "Internal action request was queued.",
                        executionUri = "urn:dcai:ontology-action-execution:ack-restore-001",
                        executionId = "ack-restore-001",
                        requestUri = "urn:dcai:ontology-action-request:ack-restore-001",
                        requestId = "REQ-ACTION-001",
                        actionTypeUri = "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                        actionTypeId = "AcknowledgeRestoreBlocker",
                        actorId = "operator-001",
                        actionReason = "Reviewed restore blocker.",
                        requestedAt = "2026-06-14T10:15:30Z",
                        generatedAt = "2026-06-14T10:15:30Z",
                        incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        incidentId = "INC-0001",
                    ),
                ),
                provenance = provenance("semanticActionNotificationQueueByIncident"),
            ),
        )

        assertEquals("action-notification-queue", payload["resultType"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:action-audit:local-action-audit-v1",
                    "actionAuditReleaseId" to "local-action-audit-v1",
                    "notificationUri" to "urn:dcai:ontology-action-notification:ack-restore-001",
                    "notificationId" to "REQ-ACTION-001:notification",
                    "notificationStatus" to "QUEUED",
                    "notificationSummary" to "Internal action request was queued.",
                    "executionUri" to "urn:dcai:ontology-action-execution:ack-restore-001",
                    "executionId" to "ack-restore-001",
                    "requestUri" to "urn:dcai:ontology-action-request:ack-restore-001",
                    "requestId" to "REQ-ACTION-001",
                    "actionTypeUri" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                    "actionTypeId" to "AcknowledgeRestoreBlocker",
                    "actorId" to "operator-001",
                    "actionReason" to "Reviewed restore blocker.",
                    "requestedAt" to "2026-06-14T10:15:30Z",
                    "generatedAt" to "2026-06-14T10:15:30Z",
                    "incidentUri" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                    "incidentId" to "INC-0001",
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesActionReviewQueueEnvelope() {
        val payload = serializer.serialize(
            ActionReviewQueueEnvelope(
                queryId = "semanticActionReviewQueueByIncident",
                records = listOf(
                    ActionReviewQueueRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                        actionAuditReleaseId = "local-action-audit-v1",
                        notificationUri = "urn:dcai:ontology-action-notification:ack-restore-001",
                        notificationId = "REQ-ACTION-001:notification",
                        executionUri = "urn:dcai:ontology-action-execution:ack-restore-001",
                        executionId = "ack-restore-001",
                        requestUri = "urn:dcai:ontology-action-request:ack-restore-001",
                        requestId = "REQ-ACTION-001",
                        actionTypeUri = "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                        actionTypeId = "AcknowledgeRestoreBlocker",
                        actorId = "operator-001",
                        actionReason = "Reviewed restore blocker.",
                        currentState = "QUEUED",
                        stateGeneratedAt = "2026-06-14T10:15:33Z",
                        incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        incidentId = "INC-0001",
                    ),
                ),
                provenance = provenance("semanticActionReviewQueueByIncident"),
            ),
        )

        assertEquals("action-review-queue", payload["resultType"])
        assertEquals("QUEUED", firstRecord(payload)["currentState"])
        assertEquals("urn:dcai:ontology-action-execution:ack-restore-001", firstRecord(payload)["executionUri"])
    }

    @Test
    fun serializesActionTransitionHistoryEnvelope() {
        val payload = serializer.serialize(
            ActionTransitionHistoryEnvelope(
                queryId = "semanticActionTransitionHistoryByIncident",
                records = listOf(
                    ActionTransitionHistoryRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                        actionAuditReleaseId = "local-action-audit-v1",
                        transitionUri = "urn:dcai:ontology-action-transition:review-start-001",
                        transitionId = "ACT-TRN-REVIEW-001",
                        executionUri = "urn:dcai:ontology-action-execution:ack-restore-001",
                        executionId = "ack-restore-001",
                        requestUri = "urn:dcai:ontology-action-request:ack-restore-001",
                        requestId = "REQ-ACTION-001",
                        actionTypeUri = "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                        actionTypeId = "AcknowledgeRestoreBlocker",
                        actorId = "operator-001",
                        transitionReason = "Local reviewer started internal action review.",
                        fromState = "QUEUED",
                        toState = "IN_REVIEW",
                        generatedAt = "2026-06-14T10:20:30Z",
                        incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        incidentId = "INC-0001",
                    ),
                ),
                provenance = provenance("semanticActionTransitionHistoryByIncident"),
            ),
        )

        assertEquals("action-transition-history", payload["resultType"])
        assertEquals("QUEUED", firstRecord(payload)["fromState"])
        assertEquals("IN_REVIEW", firstRecord(payload)["toState"])
    }

    @Test
    fun serializesActionDispatchQueueEnvelope() {
        val payload = serializer.serialize(
            ActionDispatchQueueEnvelope(
                queryId = "semanticActionDispatchQueueByIncident",
                records = listOf(
                    ActionDispatchQueueRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                        actionAuditReleaseId = "local-action-audit-v1",
                        dispatchUri = "urn:dcai:ontology-action-dispatch:noc-queue-001",
                        dispatchId = "ACT-DSP-NOC-001",
                        dispatchChannel = "NOC_QUEUE",
                        dispatchStatus = "SIMULATED_QUEUED",
                        dispatchLifecycleState = "APPROVED",
                        dispatchSummary = "Simulated NOC queue dispatch for approved ontology action.",
                        executionUri = "urn:dcai:ontology-action-execution:ack-restore-001",
                        executionId = "ack-restore-001",
                        requestUri = "urn:dcai:ontology-action-request:ack-restore-001",
                        requestId = "REQ-ACTION-001",
                        actionTypeUri = "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
                        actionTypeId = "AcknowledgeRestoreBlocker",
                        transitionUri = "urn:dcai:ontology-action-transition:review-start-001",
                        transitionId = "ACT-TRN-REVIEW-001",
                        actorId = "operator-001",
                        generatedAt = "2026-06-14T10:20:30Z",
                        incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        incidentId = "INC-0001",
                    ),
                ),
                provenance = provenance("semanticActionDispatchQueueByIncident"),
            ),
        )

        assertEquals("action-dispatch-queue", payload["resultType"])
        assertEquals("NOC_QUEUE", firstRecord(payload)["dispatchChannel"])
        assertEquals("SIMULATED_QUEUED", firstRecord(payload)["dispatchStatus"])
        assertEquals("APPROVED", firstRecord(payload)["dispatchLifecycleState"])
        assertEquals("ACT-TRN-REVIEW-001", firstRecord(payload)["transitionId"])
    }

    @Test
    fun serializesAiProposalReviewQueueEnvelope() {
        val payload = serializer.serialize(
            AiProposalReviewQueueEnvelope(
                queryId = "semanticAiProposalReviewQueue",
                records = listOf(aiProposalRecord()),
                provenance = provenance("semanticAiProposalReviewQueue"),
            ),
        )

        assertEquals("ai-proposal-review-queue", payload["resultType"])
        assertEquals("AI-PROP-LOCAL-001", firstRecord(payload)["proposalId"])
        assertEquals("ACTION_RECOMMENDATION", firstRecord(payload)["proposalType"])
        assertEquals(0.82, firstRecord(payload)["confidenceScore"])
        assertEquals("PENDING_HUMAN_REVIEW", firstRecord(payload)["reviewStatus"])
    }

    @Test
    fun serializesOntologyReviewQueueEnvelopeWithoutNullOptionalFields() {
        val payload = serializer.serialize(
            OntologyReviewQueueEnvelope(
                queryId = "semanticReasoningReviewQueue",
                records = listOf(
                    OntologyReviewQueueRecord(
                        graphUri = "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
                        queueId = "reasoning-approval:local-controlled-reasoning-v1:RestoreReadinessFinding",
                        queueKind = "reasoning-approval",
                        reviewActionId = "ApproveReasoningFinding",
                        reviewActionLabel = "Review reasoning finding approval",
                        reviewStatus = "PENDING_APPROVAL_REVIEW",
                        targetUri = "urn:dcai:reasoning:restore-readiness:INC-0001",
                        targetType = "RestoreReadinessFinding",
                        targetLabel = "Restore is not ready.",
                        releaseId = "local-controlled-reasoning-v1",
                        canonicalGraphUri = "urn:dcai:graph:canonical:local-controlled-source-v1",
                        reasoningAuditGraphUri = "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
                        evidenceSummary = "Reasoning-audit graph contains candidate findings.",
                        actionStatus = "DISABLED",
                        disabledReason = "Reasoning finding approval remains internal-only.",
                        incidentCount = 1,
                        assetCount = 1,
                        sourceRecordCount = 2,
                        activityCount = 1,
                        generatedFactCount = 1,
                        prioritySortOrder = 110,
                    ),
                ),
                provenance = provenance("semanticReasoningReviewQueue"),
            ),
        )

        assertEquals("ontology-review-queue", payload["resultType"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
                    "queueId" to "reasoning-approval:local-controlled-reasoning-v1:RestoreReadinessFinding",
                    "queueKind" to "reasoning-approval",
                    "reviewActionId" to "ApproveReasoningFinding",
                    "reviewActionLabel" to "Review reasoning finding approval",
                    "reviewStatus" to "PENDING_APPROVAL_REVIEW",
                    "targetUri" to "urn:dcai:reasoning:restore-readiness:INC-0001",
                    "targetType" to "RestoreReadinessFinding",
                    "targetLabel" to "Restore is not ready.",
                    "releaseId" to "local-controlled-reasoning-v1",
                    "canonicalGraphUri" to "urn:dcai:graph:canonical:local-controlled-source-v1",
                    "reasoningAuditGraphUri" to "urn:dcai:graph:reasoning-audit:local-controlled-reasoning-v1",
                    "evidenceSummary" to "Reasoning-audit graph contains candidate findings.",
                    "actionStatus" to "DISABLED",
                    "disabledReason" to "Reasoning finding approval remains internal-only.",
                    "incidentCount" to 1,
                    "assetCount" to 1,
                    "sourceRecordCount" to 2,
                    "activityCount" to 1,
                    "generatedFactCount" to 1,
                    "prioritySortOrder" to 110,
                ),
            ),
            payload["records"],
        )
    }

    @Test
    fun serializesFollowUpQueueEnvelope() {
        val payload = serializer.serialize(
            FollowUpQueueEnvelope(
                queryId = "semanticFollowUpQueueList",
                records = listOf(
                    FollowUpQueueRecord(
                        graphUri = "urn:dcai:graph:fixture:canonical:minimal-incident",
                        incidentUri = "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                        incidentId = "INC-0001",
                        assetUri = "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                        assetId = "ASSET-GPU-RACK-ROW-A",
                        zoneUri = "urn:dcai:fixture:valid:minimal-incident:zone-a",
                        zoneId = "ZONE-A",
                        stageUri = "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                        stageLabel = "Validation",
                        sourceRecordUri = "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                    ),
                ),
                provenance = provenance("semanticFollowUpQueueList"),
            ),
        )

        assertEquals("semanticFollowUpQueueList", payload["queryId"])
        assertEquals("follow-up-queue", payload["resultType"])
        assertEquals(1, payload["recordCount"])
        assertEquals(
            listOf(
                mapOf(
                    "graphUri" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                    "incidentUri" to "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                    "incidentId" to "INC-0001",
                    "assetUri" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                    "assetId" to "ASSET-GPU-RACK-ROW-A",
                    "zoneUri" to "urn:dcai:fixture:valid:minimal-incident:zone-a",
                    "zoneId" to "ZONE-A",
                    "stageUri" to "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                    "stageLabel" to "Validation",
                    "sourceRecordUri" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                ),
            ),
            payload["records"],
        )
        assertEquals(provenancePayload("semanticFollowUpQueueList"), payload["provenance"])
    }

    @Test
    fun serializesAdditionalProductReadModelEnvelopes() {
        val payloads = listOf(
            serializer.serialize(
                DashboardOverviewEnvelope(
                    queryId = "semanticDashboardOverview",
                    records = listOf(
                        DashboardOverviewRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            totalIncidents = 2,
                            assetCount = 3,
                            zoneCount = 1,
                            impactObservationCount = 1,
                            capacityRiskKw = 900.0,
                            affectedGpuCount = 320,
                            dependencyEdgeCount = 1,
                            trustFindingCount = 1,
                            avgDurationHours = 17.5,
                            totalDelayHours = 45.0,
                            repeatFailureAssetCount = 1,
                            engineerAssignmentDelayHours = 4.0,
                        ),
                    ),
                    provenance = provenance("semanticDashboardOverview"),
                ),
            ),
            serializer.serialize(
                FilterMetadataEnvelope(
                    queryId = "semanticFilterMetadata",
                    records = listOf(
                        FilterMetadataRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:minimal-incident",
                            filterType = "asset",
                            resourceUri = "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                            id = "ASSET-GPU-RACK-ROW-A",
                            label = "GPU Rack Sensor Row A",
                        ),
                    ),
                    provenance = provenance("semanticFilterMetadata"),
                ),
            ),
            serializer.serialize(
                FollowUpDetailEnvelope(
                    queryId = "semanticFollowUpDetail",
                    records = listOf(
                        FollowUpDetailRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                            incidentId = "INC-0001",
                            assetUri = "urn:dcai:fixture:valid:reasoning-output:asset-a",
                            assetId = "ASSET-A",
                            zoneUri = "urn:dcai:fixture:valid:reasoning-output:zone-a",
                            zoneId = "ZONE-A",
                            stageUri = "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                            sourceRecordUri = "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                            capacityRiskKw = 900.0,
                            recommendedAction = "Escalate vendor ETA.",
                            restoreReadinessUri = "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
                            restoreReadinessSummary = "Restore is not ready.",
                            repeatFailureAssetCount = 1,
                            engineerAssignmentDelayHours = 4.0,
                        ),
                    ),
                    provenance = provenance("semanticFollowUpDetail"),
                ),
            ),
            serializer.serialize(
                ImpactSummaryEnvelope(
                    queryId = "semanticImpactSummary",
                    records = listOf(
                        ImpactSummaryRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            impactObservationCount = 1,
                            incidentCount = 1,
                            capacityRiskKw = 900.0,
                            affectedGpuCount = 320,
                            trustFindingCount = 1,
                            affectedRackCount = 40,
                            thermalBreachMinutes = 0,
                            redundancyLostIncidentCount = 1,
                            vendorEtaMissedCount = 1,
                            mitigatedIncidentCount = 1,
                        ),
                    ),
                    provenance = provenance("semanticImpactSummary"),
                ),
            ),
            serializer.serialize(
                TopologyDependenciesEnvelope(
                    queryId = "semanticTopologyDependencies",
                    records = listOf(
                        TopologyDependencyRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:dependency-path",
                            dependencyEdgeUri = "urn:dcai:fixture:valid:dependency-path:edge-rack-to-pdu",
                            dependencyId = "EDGE-RACK-PDU-A",
                            dependentAssetUri = "urn:dcai:fixture:valid:dependency-path:gpu-rack-row-a",
                            dependentAssetId = "ASSET-GPU-RACK-ROW-A",
                            dependencyAssetUri = "urn:dcai:fixture:valid:dependency-path:rack-pdu-a",
                            dependencyAssetId = "ASSET-RACK-PDU-A",
                            dependencyRole = "POWER_SUPPLY",
                            sourceRecordUri = "urn:dcai:fixture:valid:dependency-path:source-record-topology-0001",
                        ),
                    ),
                    provenance = provenance("semanticTopologyDependencies"),
                ),
            ),
            serializer.serialize(
                AssetDelaySummaryEnvelope(
                    queryId = "semanticAssetDelaySummary",
                    records = listOf(
                        AssetDelaySummaryRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            assetUri = "urn:dcai:fixture:valid:reasoning-output:asset-a",
                            assetId = "ASSET-A",
                            zoneUri = "urn:dcai:fixture:valid:reasoning-output:zone-a",
                            zoneId = "ZONE-A",
                            incidentCount = 1,
                            impactObservationCount = 1,
                            capacityRiskKw = 900.0,
                            affectedGpuCount = 320,
                            repeatFailureCount = 1,
                            sourceRecordUri = "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                        ),
                    ),
                    provenance = provenance("semanticAssetDelaySummary"),
                ),
            ),
            serializer.serialize(
                IncidentEvidenceEnvelope(
                    queryId = "semanticIncidentEvidence",
                    records = listOf(
                        IncidentEvidenceRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                            incidentId = "INC-0001",
                            stageUri = "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                            sourceRecordUri = "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                            evidenceUri = "urn:dcai:fixture:valid:reasoning-output:evidence-0001",
                            evidenceClassUri = "urn:dcai:ontology:TelemetryEvidence",
                            evidenceTimestamp = "2026-01-08T02:15:00Z",
                            metricName = "fuel_pressure_psi",
                            metricValue = 18.0,
                            metricUnit = "psi",
                            telemetryStatus = "CRITICAL",
                            telemetryAlertId = "TEL-ALERT-0001",
                            alertType = "FUEL_PRESSURE_LOW",
                            alertSeverity = "CRITICAL",
                            alertTriggeredAt = "2026-01-08T02:10:00Z",
                        ),
                    ),
                    provenance = provenance("semanticIncidentEvidence"),
                ),
            ),
            serializer.serialize(
                IncidentTimelineEnvelope(
                    queryId = "semanticIncidentTimeline",
                    records = listOf(
                        IncidentTimelineRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            incidentUri = "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                            incidentId = "INC-0001",
                            eventUri = "urn:dcai:fixture:valid:reasoning-output:workflow-event-0004",
                            eventId = "EVT-0004",
                            stageUri = "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                            stageLabel = "Spare/vendor waiting",
                            eventStatus = "ACTIVE",
                            enteredAt = "2026-01-06T08:00:00Z",
                            durationHours = 63.0,
                            thresholdHours = 18.0,
                            delayHours = 45.0,
                            sourceRecordUri = "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                        ),
                    ),
                    provenance = provenance("semanticIncidentTimeline"),
                ),
            ),
            serializer.serialize(
                TrustFindingsEnvelope(
                    queryId = "semanticTrustFindingList",
                    records = listOf(
                        TrustFindingRecord(
                            graphUri = "urn:dcai:graph:fixture:canonical:reasoning-output",
                            trustFindingUri = "urn:dcai:fixture:valid:reasoning-output:trust-finding-0001",
                            trustFindingId = "TRUST-0001",
                            summary = "Impact evidence is supported by telemetry.",
                            sourceFactUri = "urn:dcai:fixture:valid:reasoning-output:impact-0001",
                            severity = "WARNING",
                            status = "FAILED",
                            createdAt = "2026-01-08T02:20:00Z",
                        ),
                    ),
                    provenance = provenance("semanticTrustFindingList"),
                ),
            ),
        )

        assertEquals(
            setOf(
                "dashboard-overview",
                "filter-metadata",
                "follow-up-detail",
                "impact-summary",
                "asset-delay-summary",
                "incident-evidence",
                "incident-timeline",
                "topology-dependencies",
                "trust-findings",
            ),
            payloads.map { it["resultType"] }.toSet(),
        )
        assertEquals(900.0, firstRecord(payloads[0])["capacityRiskKw"])
        assertEquals(1, firstRecord(payloads[0])["repeatFailureAssetCount"])
        assertEquals(4.0, firstRecord(payloads[0])["engineerAssignmentDelayHours"])
        assertEquals("ASSET-GPU-RACK-ROW-A", firstRecord(payloads[1])["id"])
        assertEquals("Escalate vendor ETA.", firstRecord(payloads[2])["recommendedAction"])
        assertEquals("Restore is not ready.", firstRecord(payloads[2])["restoreReadinessSummary"])
        assertEquals(1, firstRecord(payloads[2])["repeatFailureAssetCount"])
        assertEquals(4.0, firstRecord(payloads[2])["engineerAssignmentDelayHours"])
        assertEquals(45.0, firstRecord(payloads[0])["totalDelayHours"])
        assertEquals("POWER_SUPPLY", firstRecord(payloads[4])["dependencyRole"])
        assertEquals(1, firstRecord(payloads[5])["repeatFailureCount"])
        assertEquals("fuel_pressure_psi", firstRecord(payloads[6])["metricName"])
        assertEquals("TEL-ALERT-0001", firstRecord(payloads[6])["telemetryAlertId"])
        assertEquals("FUEL_PRESSURE_LOW", firstRecord(payloads[6])["alertType"])
        assertEquals("EVT-0004", firstRecord(payloads[7])["eventId"])
        assertEquals("Impact evidence is supported by telemetry.", firstRecord(payloads[8])["summary"])
        assertEquals("TRUST-0001", firstRecord(payloads[8])["trustFindingId"])
        assertEquals("FAILED", firstRecord(payloads[8])["status"])
    }

    @Test
    fun serializesSemanticErrorEnvelope() {
        val payload = serializer.error(
            code = SemanticErrorCode.UNAPPROVED_QUERY_ID,
            message = "Query id is not approved.",
            detail = "queries/manifest.ttl has no matching entry.",
            queryId = "unknownQuery",
        )

        assertEquals(setOf("error"), payload.keys)
        assertEquals(
            mapOf(
                "code" to "unapproved-query-id",
                "message" to "Query id is not approved.",
                "detail" to "queries/manifest.ttl has no matching entry.",
                "queryId" to "unknownQuery",
                "contractVersion" to SemanticErrorContract.VERSION,
            ),
            payload["error"],
        )
    }

    @Test
    fun serializesDynamicPlaybackEnvelope() {
        val payload = serializer.serialize(
            DynamicPlaybackEnvelope(
                queryId = "semanticDynamicEventTimelineByIncident",
                records = listOf(
                    DynamicPlaybackRecord(
                        graphUri = "urn:dcai:graph:action-audit:local-dynamic-action-audit-v1",
                        actionAuditReleaseId = "local-dynamic-action-audit-v1",
                        eventUri = "urn:dcai:dynamic-playback-event:DYN-EVENT-1",
                        eventId = "DYN-EVENT-1",
                        scenarioId = "local-dynamic-playback-v1",
                        playbackBatchId = "local-dynamic-playback-batch-v1",
                        playbackStep = 1,
                        incidentUri = "urn:dcai:incident:INC-DYN-001",
                        incidentId = "INC-DYN-001",
                        eventKind = "TELEMETRY_IMPACT_CHANGE",
                        sourceFamily = "telemetry-impact",
                        occurredAt = "2026-06-10T00:05:00Z",
                        summary = "Telemetry export shows UPS degradation.",
                        sourceRecordUri = "urn:dcai:source-record:local-dynamic-source-systems:SRC-DYN-IMPACT-001",
                        beforeState = "SOURCE_EXPORT_RECEIVED",
                        afterState = "CANONICAL_PROMOTED",
                        beforeReasoningState = "NO_REASONING_OUTPUT",
                        afterReasoningState = "DEPENDENCY_EXPOSURE_INFERRED",
                        beforeTrustState = "UNKNOWN",
                        afterTrustState = "TRUSTED_TELEMETRY",
                        beforeBlastRadiusCount = 0,
                        afterBlastRadiusCount = 1,
                        actionLifecycleState = "NONE",
                        canonicalGraphUri = "urn:dcai:graph:canonical:local-dynamic-playback-v1-step-01",
                    ),
                ),
                provenance = provenance("semanticDynamicEventTimelineByIncident"),
            ),
        )

        assertEquals("dynamic-playback", payload["resultType"])
        assertEquals("DYN-EVENT-1", firstRecord(payload)["eventId"])
        assertEquals(1, firstRecord(payload)["playbackStep"])
        assertEquals("CANONICAL_PROMOTED", firstRecord(payload)["afterState"])
        assertEquals("DEPENDENCY_EXPOSURE_INFERRED", firstRecord(payload)["afterReasoningState"])
    }

    @Test
    fun semanticErrorCodesMatchPhaseEighteenContractNames() {
        assertEquals(
            setOf(
                "unapproved-query-id",
                "unsupported-result-envelope",
                "missing-required-binding",
                "graph-unavailable",
                "contract-validation-failed",
                "internal-semantic-service-error",
            ),
            SemanticErrorCode.entries.map { it.value }.toSet(),
        )
    }

    private fun provenance(queryId: String): QueryResultEnvelopeProvenance {
        return QueryResultEnvelopeProvenance(
            queryId = queryId,
            graphScope = "fixture canonical graph",
        )
    }

    private fun provenancePayload(queryId: String): Map<String, String> {
        return mapOf(
            "queryId" to queryId,
            "graphScope" to "fixture canonical graph",
            "contractVersion" to QueryResultEnvelopeProvenance.CONTRACT_VERSION,
        )
    }

    private fun aiProposalRecord(): AiProposalRecord {
        return AiProposalRecord(
            graphUri = "urn:dcai:graph:ai-audit:local-ai-governance-v1",
            aiAuditReleaseId = "local-ai-governance-v1",
            proposalUri = "urn:dcai:ai-proposal:local-ai-governance-v1",
            proposalId = "AI-PROP-LOCAL-001",
            proposalType = "ACTION_RECOMMENDATION",
            proposalStatus = "PENDING_REVIEW",
            reviewStatus = "PENDING_HUMAN_REVIEW",
            disabledReason = "AI proposal review is read-only.",
            summary = "AI proposal recommends a governed action.",
            rationale = "Evidence and reasoning support human review.",
            confidenceScore = 0.82,
            riskLevel = "HIGH",
            modelId = "local-governance-model-placeholder",
            promptId = "ai-governance-proposal-v1",
            promptHash = "sha256-local-placeholder-001",
            actorId = "local-ai-governance-simulator",
            generatedAt = "2026-06-09T02:45:00Z",
            batchUri = "urn:dcai:ai-proposal-batch:local-ai-governance-v1",
            batchId = "local-ai-governance-v1",
            validationReportUri = "urn:dcai:ai-proposal-validation-report:local-ai-governance-v1",
            validationStatus = "CONFORMS",
            validationSummary = "AI proposal passed policy.",
            incidentUri = "urn:dcai:incident:INC-001",
            incidentId = "INC-001",
            targetObjectUri = "urn:dcai:incident:INC-001",
            sourceRecordUri = "urn:dcai:source-record:local-controlled-facility-ops-file:SRC-INC-001",
            supportingEvidenceUri = "urn:dcai:reasoning:restore-readiness:urn%3Adcai%3Aincident%3AINC-001",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun firstRecord(payload: Map<String, Any>): Map<String, Any> {
        return (payload["records"] as List<Map<String, Any>>).single()
    }
}
