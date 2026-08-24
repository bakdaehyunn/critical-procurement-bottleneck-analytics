import type { FollowUpItem, SemanticFollowUpDetailRecord, SemanticTopologyDependencyRecord } from '../recovery-queue'
import { canonicalStage, humanize, lastSegment, unique } from '../recovery-queue'
import {
  mapActionReviewQueue,
  mapActionTransitionHistory,
  mapAiProposal,
  type SemanticActionReviewQueueRecord,
  type SemanticActionTransitionHistoryRecord,
  type SemanticAiProposalRecord,
} from '../review-inbox'
import type {
  InfrastructureDependency,
  DynamicPlaybackItem,
  OntologyActionAuditHistoryItem,
  OntologyActionAffordance,
  OntologyActionDispatchQueueItem,
  OntologyActionNotificationItem,
  OntologyActionPlacement,
  OntologyActionTarget,
  OntologyEvidenceExplanation,
  OntologyEvidenceFact,
  ProvenanceTraceItem,
  RecoveryCaseActionResource,
  RecoveryCaseEvidenceResource,
  RecoveryCaseTimelineResource,
  RequestDetail,
  SemanticBlastRadius,
  SemanticDependencyImpact,
  SemanticIncidentEvidence,
  SemanticReasoningFinding,
  SemanticValidation,
} from './recoveryCaseModel'

export type SemanticValidationSummaryRecord = {
  sourceRecordCount: number
  incidentCount: number
  incidentWithProvenanceCount: number
  assetCount: number
  assetWithProvenanceCount: number
}

export type SemanticIncidentEvidenceRecord = {
  incidentId: string
  stageUri: string
  stageLabel?: string
  sourceRecordUri: string
  impactUri?: string
  evidenceUri?: string
  evidenceClassUri?: string
  evidenceTimestamp?: string
  confidenceState?: string
  metricName?: string
  metricValue?: number
  metricUnit?: string
  telemetryStatus?: string
  telemetryAlertId?: string
  alertType?: string
  alertSeverity?: string
  alertTriggeredAt?: string
  alertResolvedAt?: string
  validationId?: string
  validationStatus?: string
  validatorId?: string
  validationStartedAt?: string
  validationCompletedAt?: string
  failureReason?: string
  workOrderId?: string
  assignedTeam?: string
  assignedEngineerId?: string
  workOrderStatus?: string
  plannedStartAt?: string
  actualStartAt?: string
  actualCompletedAt?: string
  requiredSpareId?: string
  requiredSpareName?: string
  stockStatus?: string
  trustFindingUri?: string
  trustSummary?: string
}

export type SemanticIncidentTimelineRecord = {
  incidentId: string
  eventUri: string
  eventId?: string
  stageUri: string
  stageLabel?: string
  eventStatus?: string
  enteredAt?: string
  exitedAt?: string
  durationHours?: number
  thresholdHours?: number
  delayHours?: number
  sourceRecordUri: string
}

export type SemanticDependencyImpactRecord = {
  assetId: string
  dependencyId?: string
  dependencyAssetId?: string
  dependencyRole?: string
  impactScope?: string
  findingUri?: string
  findingSummary?: string
  sourceRecordUri?: string
}

export type SemanticBlastRadiusRecord = {
  assetId: string
  downstreamAssetId?: string
  incidentId?: string
  findingUri?: string
  findingSummary?: string
}

export type SemanticActionAvailabilityRecord = {
  graphUri: string
  incidentUri: string
  incidentId: string
  assetUri: string
  assetId: string
  sourceRecordUri: string
  actionId: string
  actionLabel: string
  actionDescription: string
  actionStatus: OntologyActionAffordance['status']
  uiPlacement: OntologyActionPlacement
  detailKind: 'targetObject' | 'requiredParameter' | 'precondition' | 'provenanceRequirement' | 'disabledReason'
  detailRole: string
  detailLabel: string
  detailValue: string
  detailSortOrder: number
}

export type SemanticActionAuditHistoryRecord = {
  graphUri: string
  actionAuditReleaseId: string
  executionUri: string
  executionId: string
  requestUri: string
  requestId: string
  validationReportUri: string
  actionTypeUri: string
  actionTypeId: string
  actionTypeLabel?: string
  idempotencyKey: string
  actorId: string
  actionReason: string
  actionStatus: string
  requestedAt: string
  executedAt: string
  targetObjectUri?: string
  validationStatus: string
  validationSummary?: string
  sourceRecordUri?: string
  assignedTeam?: string
  assigneeId?: string
  reviewedStatus?: string
  reviewSummary?: string
  supportingEvidenceUri?: string
}

export type SemanticActionNotificationQueueRecord = {
  graphUri: string
  actionAuditReleaseId: string
  notificationUri: string
  notificationId: string
  notificationStatus: string
  notificationSummary: string
  executionUri: string
  executionId: string
  requestUri: string
  requestId: string
  actionTypeUri: string
  actionTypeId: string
  actorId: string
  actionReason: string
  requestedAt: string
  generatedAt: string
  incidentUri: string
  incidentId: string
  targetObjectUri?: string
  sourceRecordUri?: string
  assignedTeam?: string
  assigneeId?: string
  reviewedStatus?: string
  reviewSummary?: string
}

export type SemanticActionDispatchQueueRecord = {
  graphUri: string
  actionAuditReleaseId: string
  dispatchUri: string
  dispatchId: string
  dispatchChannel: string
  dispatchStatus: string
  dispatchLifecycleState: string
  dispatchSummary: string
  executionUri: string
  executionId: string
  requestUri: string
  requestId: string
  actionTypeUri: string
  actionTypeId: string
  transitionUri: string
  transitionId: string
  actorId: string
  generatedAt: string
  incidentUri: string
  incidentId: string
  sourceRecordUri?: string
}

export type SemanticDynamicPlaybackRecord = {
  graphUri: string
  actionAuditReleaseId: string
  eventUri: string
  eventId: string
  scenarioId: string
  playbackBatchId: string
  playbackStep: number
  incidentUri: string
  incidentId: string
  eventKind: string
  sourceFamily: string
  occurredAt: string
  summary: string
  sourceRecordUri: string
  beforeState: string
  afterState: string
  beforeReasoningState: string
  afterReasoningState: string
  beforeTrustState: string
  afterTrustState: string
  beforeBlastRadiusCount: number
  afterBlastRadiusCount: number
  actionLifecycleState: string
  canonicalGraphUri?: string
  provenanceGraphUri?: string
  reasoningGraphUri?: string
}

export function pickTimelineResource(detail: RequestDetail): RecoveryCaseTimelineResource['detail'] {
  return {
    stage_lead_times: detail.stage_lead_times,
    timeline: detail.timeline,
    provenance_trace: detail.provenance_trace,
    ontology_evidence: detail.ontology_evidence,
  }
}

export function pickEvidenceResource(detail: RequestDetail): RecoveryCaseEvidenceResource['detail'] {
  return {
    work_orders: detail.work_orders,
    validation_results: detail.validation_results,
    telemetry_alerts: detail.telemetry_alerts,
    impact_snapshot: detail.impact_snapshot,
    quality_flags: detail.quality_flags,
    impact_trust_flags: detail.impact_trust_flags,
    provenance_trace: detail.provenance_trace,
    ontology_evidence: detail.ontology_evidence,
  }
}

export function pickActionResource(detail: RequestDetail): RecoveryCaseActionResource['detail'] {
  return {
    ontology_actions: detail.ontology_actions,
    action_audit_history: detail.action_audit_history,
    action_notifications: detail.action_notifications,
    action_review_queue: detail.action_review_queue,
    action_transition_history: detail.action_transition_history,
    action_dispatch_queue: detail.action_dispatch_queue,
    ontology_evidence: detail.ontology_evidence,
  }
}

export function buildTopologyDependencies(
  records: SemanticTopologyDependencyRecord[],
  followUps: FollowUpItem[],
): InfrastructureDependency[] {
  const activeByAsset = followUps.reduce<Map<string, number>>((summary, row) => {
    summary.set(row.asset_id, (summary.get(row.asset_id) ?? 0) + 1)
    return summary
  }, new Map())
  return records.map((record) => ({
    dependency_id: record.dependencyId,
    dependent_asset_id: record.dependentAssetId,
    dependent_asset_name: humanize(record.dependentAssetId),
    dependent_asset_type: 'Semantic asset',
    dependent_status: statusForAsset(record.dependentAssetId, activeByAsset),
    dependency_asset_id: record.dependencyAssetId,
    dependency_asset_name: humanize(record.dependencyAssetId),
    dependency_asset_type: 'Semantic asset',
    dependency_status: statusForAsset(record.dependencyAssetId, activeByAsset),
    dependency_type: record.pathId ?? 'UNKNOWN',
    dependency_role: record.dependencyRole,
    impact_scope: record.impactScope ?? 'unknown',
    dependent_active_incident_count: activeByAsset.get(record.dependentAssetId) ?? 0,
    dependency_active_incident_count: activeByAsset.get(record.dependencyAssetId) ?? 0,
  }))
}

export function buildRequestDetail(
  request: FollowUpItem,
  detail: SemanticFollowUpDetailRecord | undefined,
  evidence: SemanticIncidentEvidenceRecord[] = [],
  workflowTimeline: SemanticIncidentTimelineRecord[] = [],
  actionAvailabilityRecords: SemanticActionAvailabilityRecord[] = [],
  actionAuditRecords: SemanticActionAuditHistoryRecord[] = [],
  actionNotificationRecords: SemanticActionNotificationQueueRecord[] = [],
  actionReviewRecords: SemanticActionReviewQueueRecord[] = [],
  actionTransitionRecords: SemanticActionTransitionHistoryRecord[] = [],
  actionDispatchRecords: SemanticActionDispatchQueueRecord[] = [],
  aiProposalRecords: SemanticAiProposalRecord[] = [],
  dynamicTimelineRecords: SemanticDynamicPlaybackRecord[] = [],
  dynamicStateChangeRecords: SemanticDynamicPlaybackRecord[] = [],
  dynamicReasoningChangeRecords: SemanticDynamicPlaybackRecord[] = [],
  dynamicActionLifecycleRecords: SemanticDynamicPlaybackRecord[] = [],
): RequestDetail {
  const evidenceIssues = uniqueTrustFindingEvidence(evidence.filter((record) => record.trustFindingUri))
  const telemetryEvidence = evidence.filter(isTelemetryEvidence)
  const validationEvidence = evidence.filter(isValidationEvidence)
  const workOrderEvidence = evidence.filter(isWorkOrderEvidence)
  const provenanceTrace = buildProvenanceTrace(request, detail, evidence, workflowTimeline)
  const evidenceTimeline = evidence
    .filter((record) => record.evidenceUri || record.trustFindingUri)
    .map((record) => ({
      event_id: record.evidenceUri ?? record.trustFindingUri ?? record.sourceRecordUri,
      stage: canonicalStage(record.stageLabel ?? record.stageUri),
      event_type: evidenceTypeLabel(record),
      event_status: record.telemetryStatus ?? record.validationStatus ?? record.workOrderStatus ?? record.confidenceState ?? 'UNKNOWN',
      occurred_at: record.evidenceTimestamp ?? '',
      actor_type: record.validatorId ?? record.assignedTeam ?? 'UNKNOWN',
      reason_code: record.trustFindingUri ? 'TRUST_FINDING' : null,
      message: record.trustSummary ?? record.failureReason ?? null,
      source_record_uri: record.sourceRecordUri ?? null,
    }))
    .sort((left, right) => left.occurred_at.localeCompare(right.occurred_at))
  const workflowEvents = workflowTimeline.map((record) => ({
    event_id: record.eventId ?? record.eventUri,
    stage: canonicalStage(record.stageLabel ?? record.stageUri),
    event_type: 'WORKFLOW_STAGE',
    event_status: record.eventStatus ?? 'UNKNOWN',
    occurred_at: record.enteredAt ?? '',
    actor_type: 'semantic-service',
    reason_code: null,
    message: record.delayHours && record.delayHours > 0 ? `${record.delayHours}h over threshold` : null,
    source_record_uri: record.sourceRecordUri ?? null,
  }))
  const timeline = [...workflowEvents, ...evidenceTimeline].sort((left, right) => left.occurred_at.localeCompare(right.occurred_at))
  const workOrders = workOrderEvidence
    .filter((record) => record.workOrderId || record.evidenceUri)
    .map((record) => ({
        work_order_id: record.workOrderId ?? record.evidenceUri!,
        assigned_team: record.assignedTeam ?? 'UNKNOWN',
        assigned_engineer_id: record.assignedEngineerId ?? null,
        work_order_status: record.workOrderStatus ?? record.confidenceState ?? 'UNKNOWN',
        planned_start_at: record.plannedStartAt ?? null,
        actual_start_at: record.actualStartAt ?? null,
        actual_completed_at: record.actualCompletedAt ?? null,
        required_spare_id: record.requiredSpareId ?? null,
        required_spare_name: record.requiredSpareName ?? null,
        stock_status: record.stockStatus ?? null,
      }))
  return {
    request,
    stage_lead_times: workflowTimeline.map((record) => ({
          stage: canonicalStage(record.stageLabel ?? record.stageUri),
          entered_at: record.enteredAt ?? '',
          exited_at: record.exitedAt ?? null,
          duration_hours: record.durationHours ?? null,
          threshold_hours: record.thresholdHours ?? null,
          is_bottleneck: record.delayHours == null ? null : record.delayHours > 0,
          delay_hours: record.delayHours ?? null,
        })),
    timeline,
    work_orders: workOrders,
    validation_results: validationEvidence.filter((record) => record.validationId || record.evidenceUri).map((record) => ({
      validation_id: record.validationId ?? record.evidenceUri!,
      validation_status: record.validationStatus ?? record.confidenceState ?? 'UNKNOWN',
      validator_id: record.validatorId ?? null,
      validation_started_at: record.validationStartedAt ?? record.evidenceTimestamp ?? null,
      validation_completed_at: record.validationCompletedAt ?? null,
      failure_reason: record.failureReason ?? null,
    })),
    telemetry_alerts: telemetryEvidence
      .filter((record) => record.telemetryAlertId || record.evidenceUri)
      .map((record) => ({
        telemetry_alert_id: record.telemetryAlertId ?? record.evidenceUri!,
        asset_id: request.asset_id,
        alert_type: record.alertType ?? record.metricName ?? 'UNKNOWN',
        severity: record.alertSeverity ?? record.telemetryStatus ?? record.confidenceState ?? 'UNKNOWN',
        triggered_at: record.alertTriggeredAt ?? record.evidenceTimestamp ?? '',
        resolved_at: record.alertResolvedAt ?? null,
      })),
    impact_snapshot: detail?.impactUri ? {
      impact_snapshot_id: detail.impactUri,
      incident_id: request.incident_id,
      asset_id: request.asset_id,
      zone_id: request.zone_id,
      snapshot_at: '',
      redundancy_state: detail?.redundancyState ?? request.redundancy_state ?? 'Unknown',
      affected_rack_count: detail?.affectedRackCount ?? null,
      affected_gpu_count: detail?.affectedGpuCount ?? null,
      estimated_capacity_risk_kw: detail?.capacityRiskKw ?? null,
      estimated_gpu_capacity_risk_pct: detail?.estimatedGpuCapacityRiskPct ?? null,
      thermal_breach_minutes: detail?.thermalBreachMinutes ?? null,
      power_redundancy_lost: detail?.powerRedundancyLost ?? null,
      cooling_redundancy_lost: detail?.coolingRedundancyLost ?? null,
      mitigation_status: detail?.mitigationStatus ?? request.mitigation_status ?? 'UNKNOWN',
      vendor_eta_at: detail?.vendorEtaAt ?? null,
      vendor_status: detail?.vendorStatus ?? request.vendor_status ?? 'UNKNOWN',
      source_system: 'ontology-native semantic graph',
      telemetry_readings: telemetryEvidence.filter((record) => record.metricValue != null).map((record) => ({
        metric: record.metricName ?? lastSegment(record.evidenceUri ?? 'semantic_metric'),
        value: record.metricValue!,
        unit: record.metricUnit ?? '',
        status: record.telemetryStatus ?? record.confidenceState ?? 'UNKNOWN',
      })),
    } : null,
    quality_flags: evidenceIssues.map((record) => record.trustSummary ?? 'Semantic trust finding requires review'),
    restore_readiness: {
      status: request.restore_readiness_status,
      summary: request.restore_readiness_summary,
      finding_uri: detail?.restoreReadinessUri ?? null,
    },
    impact_confidence_status: request.impact_confidence_status,
    impact_trust_flags: evidenceIssues.map((record) => ({
      issue_type: 'semantic_trust_finding',
      severity: 'WARNING',
      message: record.trustSummary ?? 'Semantic trust finding requires review',
      evidence: {
        sourceRecordUri: record.sourceRecordUri,
        trustFindingUri: record.trustFindingUri,
      },
    })),
    provenance_trace: provenanceTrace,
    ontology_evidence: buildOntologyEvidenceExplanation(
      request,
      detail,
      evidence,
      workflowTimeline,
      provenanceTrace,
      actionAvailabilityRecords,
    ),
    ontology_actions: mapOntologyActionAffordances(actionAvailabilityRecords),
    action_audit_history: actionAuditRecords.map(mapActionAuditHistory),
    action_notifications: actionNotificationRecords.map(mapActionNotification),
    action_review_queue: actionReviewRecords.map(mapActionReviewQueue),
    action_transition_history: actionTransitionRecords.map(mapActionTransitionHistory),
    action_dispatch_queue: actionDispatchRecords.map(mapActionDispatchQueue),
    ai_proposals: aiProposalRecords.map(mapAiProposal),
    dynamic_event_timeline: dynamicTimelineRecords.map(mapDynamicPlayback),
    dynamic_state_changes: dynamicStateChangeRecords.map(mapDynamicPlayback),
    dynamic_reasoning_changes: dynamicReasoningChangeRecords.map(mapDynamicPlayback),
    dynamic_action_lifecycle: dynamicActionLifecycleRecords.map(mapDynamicPlayback),
  }
}

function mapActionDispatchQueue(record: SemanticActionDispatchQueueRecord): OntologyActionDispatchQueueItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    dispatch_uri: record.dispatchUri,
    dispatch_id: record.dispatchId,
    dispatch_channel: record.dispatchChannel,
    dispatch_status: record.dispatchStatus,
    dispatch_lifecycle_state: record.dispatchLifecycleState,
    dispatch_summary: record.dispatchSummary,
    execution_uri: record.executionUri,
    execution_id: record.executionId,
    request_uri: record.requestUri,
    request_id: record.requestId,
    action_type_uri: record.actionTypeUri,
    action_type_id: record.actionTypeId,
    transition_uri: record.transitionUri,
    transition_id: record.transitionId,
    actor_id: record.actorId,
    generated_at: record.generatedAt,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
    source_record_uri: record.sourceRecordUri ?? null,
  }
}

export function mapDynamicPlayback(record: SemanticDynamicPlaybackRecord): DynamicPlaybackItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    event_uri: record.eventUri,
    event_id: record.eventId,
    scenario_id: record.scenarioId,
    playback_batch_id: record.playbackBatchId,
    playback_step: record.playbackStep,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
    event_kind: record.eventKind,
    source_family: record.sourceFamily,
    occurred_at: record.occurredAt,
    summary: record.summary,
    source_record_uri: record.sourceRecordUri,
    before_state: record.beforeState,
    after_state: record.afterState,
    before_reasoning_state: record.beforeReasoningState,
    after_reasoning_state: record.afterReasoningState,
    before_trust_state: record.beforeTrustState,
    after_trust_state: record.afterTrustState,
    before_blast_radius_count: record.beforeBlastRadiusCount,
    after_blast_radius_count: record.afterBlastRadiusCount,
    action_lifecycle_state: record.actionLifecycleState,
    canonical_graph_uri: record.canonicalGraphUri ?? null,
    provenance_graph_uri: record.provenanceGraphUri ?? null,
    reasoning_graph_uri: record.reasoningGraphUri ?? null,
  }
}

function mapActionNotification(record: SemanticActionNotificationQueueRecord): OntologyActionNotificationItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    notification_uri: record.notificationUri,
    notification_id: record.notificationId,
    notification_status: record.notificationStatus,
    notification_summary: record.notificationSummary,
    execution_uri: record.executionUri,
    execution_id: record.executionId,
    request_uri: record.requestUri,
    request_id: record.requestId,
    action_type_uri: record.actionTypeUri,
    action_type_id: record.actionTypeId,
    actor_id: record.actorId,
    action_reason: record.actionReason,
    requested_at: record.requestedAt,
    generated_at: record.generatedAt,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
    target_object_uri: record.targetObjectUri ?? null,
    source_record_uri: record.sourceRecordUri ?? null,
    assigned_team: record.assignedTeam ?? null,
    assignee_id: record.assigneeId ?? null,
    reviewed_status: record.reviewedStatus ?? null,
    review_summary: record.reviewSummary ?? null,
  }
}

function mapActionAuditHistory(record: SemanticActionAuditHistoryRecord): OntologyActionAuditHistoryItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    execution_uri: record.executionUri,
    execution_id: record.executionId,
    request_uri: record.requestUri,
    request_id: record.requestId,
    validation_report_uri: record.validationReportUri,
    action_type_uri: record.actionTypeUri,
    action_type_id: record.actionTypeId,
    action_type_label: record.actionTypeLabel ?? null,
    idempotency_key: record.idempotencyKey,
    actor_id: record.actorId,
    action_reason: record.actionReason,
    action_status: record.actionStatus,
    requested_at: record.requestedAt,
    executed_at: record.executedAt,
    target_object_uri: record.targetObjectUri ?? null,
    validation_status: record.validationStatus,
    validation_summary: record.validationSummary ?? null,
    source_record_uri: record.sourceRecordUri ?? null,
    assigned_team: record.assignedTeam ?? null,
    assignee_id: record.assigneeId ?? null,
    reviewed_status: record.reviewedStatus ?? null,
    review_summary: record.reviewSummary ?? null,
    supporting_evidence_uri: record.supportingEvidenceUri ?? null,
  }
}

function mapOntologyActionAffordances(records: SemanticActionAvailabilityRecord[]): OntologyActionAffordance[] {
  const grouped = new Map<string, {
    action: OntologyActionAffordance
    sortOrders: Map<string, number>
  }>()
  records.forEach((record) => {
    const existing = grouped.get(record.actionId)
    const action = existing?.action ?? {
      action_id: record.actionId,
      label: record.actionLabel,
      description: record.actionDescription,
      status: record.actionStatus,
      incident_uri: record.incidentUri,
      incident_id: record.incidentId,
      source_record_uri: record.sourceRecordUri,
      ui_placement: [],
      target_objects: [],
      required_parameters: [],
      preconditions: [],
      provenance_requirements: [],
      disabled_reasons: [],
    }
    const sortOrders = existing?.sortOrders ?? new Map<string, number>()
    appendUnique(action.ui_placement, record.uiPlacement)
    sortOrders.set(`${record.detailKind}:${record.detailRole}:${record.detailValue}`, record.detailSortOrder)
    if (record.detailKind === 'targetObject') {
      appendUniqueTarget(action.target_objects, {
        role: record.detailRole,
        label: record.detailLabel,
        resource_uri: record.detailValue,
      })
    } else if (record.detailKind === 'requiredParameter') {
      appendUnique(action.required_parameters, record.detailValue)
    } else if (record.detailKind === 'precondition') {
      appendUnique(action.preconditions, record.detailValue)
    } else if (record.detailKind === 'provenanceRequirement') {
      appendUnique(action.provenance_requirements, record.detailValue)
    } else if (record.detailKind === 'disabledReason') {
      appendUnique(action.disabled_reasons, record.detailValue)
    }
    grouped.set(record.actionId, { action, sortOrders })
  })
  return Array.from(grouped.values())
    .map(({ action, sortOrders }) => ({
      ...action,
      ui_placement: [...action.ui_placement].sort(),
      target_objects: sortTargets(action.target_objects, sortOrders),
      required_parameters: sortStrings(action.required_parameters, 'requiredParameter', sortOrders),
      preconditions: sortStrings(action.preconditions, 'precondition', sortOrders),
      provenance_requirements: sortStrings(action.provenance_requirements, 'provenanceRequirement', sortOrders),
      disabled_reasons: sortStrings(action.disabled_reasons, 'disabledReason', sortOrders),
    }))
    .sort((left, right) => left.action_id.localeCompare(right.action_id))
}

function appendUnique(values: string[], value: string) {
  if (!values.includes(value)) {
    values.push(value)
  }
}

function appendUniqueTarget(values: OntologyActionTarget[], value: OntologyActionTarget) {
  if (!values.some((item) => item.role === value.role && item.resource_uri === value.resource_uri)) {
    values.push(value)
  }
}

function sortTargets(values: OntologyActionTarget[], sortOrders: Map<string, number>): OntologyActionTarget[] {
  return [...values].sort((left, right) => {
    const leftOrder = sortOrders.get(`targetObject:${left.role}:${left.resource_uri}`) ?? 999
    const rightOrder = sortOrders.get(`targetObject:${right.role}:${right.resource_uri}`) ?? 999
    return leftOrder - rightOrder || left.role.localeCompare(right.role) || left.resource_uri.localeCompare(right.resource_uri)
  })
}

function sortStrings(values: string[], kind: string, sortOrders: Map<string, number>): string[] {
  return [...values].sort((left, right) => {
    const leftOrder = minSortOrder(kind, left, sortOrders)
    const rightOrder = minSortOrder(kind, right, sortOrders)
    return leftOrder - rightOrder || left.localeCompare(right)
  })
}

function minSortOrder(kind: string, value: string, sortOrders: Map<string, number>): number {
  const matches = Array.from(sortOrders.entries())
    .filter(([key]) => key.startsWith(`${kind}:`) && key.endsWith(`:${value}`))
    .map(([, order]) => order)
  return matches.length ? Math.min(...matches) : 999
}

function buildProvenanceTrace(
  request: FollowUpItem,
  detail: SemanticFollowUpDetailRecord | undefined,
  evidence: SemanticIncidentEvidenceRecord[],
  workflowTimeline: SemanticIncidentTimelineRecord[],
): ProvenanceTraceItem[] {
  const trace: ProvenanceTraceItem[] = []
  pushTrace(trace, {
    step: 'Source extract',
    label: 'Recorded source record',
    resource_uri: detail?.sourceRecordUri ?? evidence[0]?.sourceRecordUri ?? workflowTimeline[0]?.sourceRecordUri ?? '',
    detail: 'Connector-style export row that was mapped into canonical RDF',
  })
  pushTrace(trace, {
    step: 'Canonical graph',
    label: 'Incident assertion',
    resource_uri: detail?.incidentUri ?? '',
    detail: `${request.asset_name} in ${request.zone_name}`,
  })
  pushTrace(trace, {
    step: 'Impact observation',
    label: 'Operational impact',
    resource_uri: detail?.impactUri ?? '',
    detail: `${knownNumber(request.affected_gpu_count)} GPUs, ${knownNumber(request.estimated_capacity_risk_kw, 0)} kW at risk`,
  })
  pushTrace(trace, {
    step: 'Recovery blocker',
    label: 'Blocker finding',
    resource_uri: detail?.recoveryBlockerUri ?? '',
    detail: detail?.blockerSummary ?? request.reason_summary,
  })
  pushTrace(trace, {
    step: 'Reasoning graph',
    label: 'Restore readiness',
    resource_uri: detail?.restoreReadinessUri ?? '',
    detail: request.restore_readiness_summary ?? 'Restore-readiness finding from reasoning output',
  })

  uniqueTrustFindingEvidence(evidence.filter((record) => record.trustFindingUri)).forEach((record) => {
    pushTrace(trace, {
      step: 'Trust finding',
      label: 'Evidence confidence',
      resource_uri: record.trustFindingUri ?? '',
      detail: record.trustSummary ?? 'Semantic trust finding requires review',
    })
  })

  return trace
}

function buildOntologyEvidenceExplanation(
  request: FollowUpItem,
  detail: SemanticFollowUpDetailRecord | undefined,
  evidence: SemanticIncidentEvidenceRecord[],
  workflowTimeline: SemanticIncidentTimelineRecord[],
  provenanceTrace: ProvenanceTraceItem[],
  actionAvailabilityRecords: SemanticActionAvailabilityRecord[],
): OntologyEvidenceExplanation {
  const latestWorkflow = workflowTimeline.at(-1)
  const evidenceIssueCount = uniqueTrustFindingEvidence(evidence.filter((record) => record.trustFindingUri)).length
  const actionDetails = actionAvailabilityRecords.reduce<Map<string, SemanticActionAvailabilityRecord[]>>((summary, record) => {
    const existing = summary.get(record.actionId) ?? []
    existing.push(record)
    summary.set(record.actionId, existing)
    return summary
  }, new Map())
  const availableActionCount = [...actionDetails.values()].filter((records) =>
    records.some((record) => record.actionStatus !== 'DISABLED'),
  ).length
  const blockedActionCount = [...actionDetails.values()].length - availableActionCount
  const directFacts: OntologyEvidenceFact[] = [
    {
      kind: 'direct-fact',
      label: 'Incident to asset assertion',
      value: `${request.incident_id} -> ${request.asset_id}`,
      detail: `${request.asset_name} in ${request.zone_name}`,
      resource_uri: detail?.incidentUri ?? null,
      confidence: 'trusted',
    },
    {
      kind: 'direct-fact',
      label: 'Workflow state',
      value: formatOntologyFactValue(latestWorkflow?.stageLabel ?? latestWorkflow?.stageUri ?? request.current_stage),
      detail: latestWorkflow?.delayHours && latestWorkflow.delayHours > 0
        ? `${latestWorkflow.delayHours}h over threshold`
        : request.hours_in_current_stage == null
          ? 'Time in selected stage is unknown'
          : `${request.hours_in_current_stage.toFixed(1)}h in selected stage`,
      resource_uri: latestWorkflow?.eventUri ?? null,
      confidence: request.hours_in_current_stage == null ? 'review' : request.hours_in_current_stage > 0 ? 'review' : 'trusted',
    },
    {
      kind: 'direct-fact',
      label: 'Impact observation',
      value: `${knownNumber(request.affected_gpu_count)} GPUs / ${knownNumber(request.estimated_capacity_risk_kw, 0)} kW`,
      detail: `Redundancy ${formatOntologyFactValue(request.redundancy_state ?? 'UNKNOWN')}`,
      resource_uri: detail?.impactUri ?? null,
      confidence: request.impact_confidence_status === 'TRUSTED' ? 'trusted' : 'review',
    },
  ]

  const inferredFacts: OntologyEvidenceFact[] = [
    {
      kind: 'inferred-fact',
      label: 'Restore readiness',
      value: request.restore_readiness_status,
      detail: request.restore_readiness_summary ?? 'No restore-readiness reasoning summary is attached',
      resource_uri: detail?.restoreReadinessUri ?? null,
      confidence: request.restore_readiness_status === 'NOT_READY' ? 'blocked' : request.restore_readiness_status === 'READY' ? 'trusted' : 'review',
    },
    {
      kind: 'inferred-fact',
      label: 'Recovery blocker',
      value: detail?.recoveryBlockerUri ? 'Derived blocker' : 'No blocker finding',
      detail: detail?.blockerSummary ?? request.reason_summary,
      resource_uri: detail?.recoveryBlockerUri ?? null,
      confidence: detail?.recoveryBlockerUri ? 'blocked' : 'review',
    },
    {
      kind: 'inferred-fact',
      label: 'Evidence trust',
      value: evidenceIssueCount ? `${evidenceIssueCount} trust issue${evidenceIssueCount === 1 ? '' : 's'}` : 'Unknown',
      detail: evidenceIssueCount ? 'Trust findings are linked to source evidence' : 'No authoritative trust evidence is attached to this selected incident',
      resource_uri: evidence.find((record) => record.trustFindingUri)?.trustFindingUri ?? null,
      confidence: 'review',
    },
  ]

  const provenanceLinks = provenanceTrace.map<OntologyEvidenceFact>((item) => ({
    kind: 'provenance',
    label: item.step,
    value: item.label,
    detail: item.detail,
    resource_uri: item.resource_uri,
    confidence: 'trusted',
  }))

  const actionEligibility = [...actionDetails.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map<OntologyEvidenceFact>(([actionId, records]) => {
      const disabledReason = records.find((record) => record.detailKind === 'disabledReason')?.detailValue
      const requiredParameters = records.filter((record) => record.detailKind === 'requiredParameter').length
      const provenanceRequirements = records.filter((record) => record.detailKind === 'provenanceRequirement').length
      const status = records[0]?.actionStatus ?? 'UNKNOWN'
      return {
        kind: 'action-eligibility',
        label: actionId,
        value: status,
        detail: disabledReason ?? `${requiredParameters} required parameter${requiredParameters === 1 ? '' : 's'} and ${provenanceRequirements} provenance gate${provenanceRequirements === 1 ? '' : 's'}`,
        resource_uri: records.find((record) => record.detailKind === 'targetObject')?.detailValue ?? null,
        confidence: status === 'DISABLED' ? 'review' : 'trusted',
      }
    })

  return {
    question: 'Why is this selected finding actionable?',
    answer: [
      `${request.incident_id} is linked to ${request.asset_name} through canonical graph facts.`,
      request.restore_readiness_status === 'NOT_READY'
        ? 'Reasoning marks restore readiness as blocked.'
        : 'Reasoning keeps restore readiness available for review.',
      evidenceIssueCount ? `${evidenceIssueCount} trust issue${evidenceIssueCount === 1 ? '' : 's'} need review.` : 'Evidence trust is unknown because no authoritative trust evidence is attached.',
      blockedActionCount ? `${blockedActionCount} governed action path${blockedActionCount === 1 ? '' : 's'} remain gated.` : 'Governed action paths are available for local audit.',
    ].join(' '),
    direct_facts: directFacts,
    inferred_facts: inferredFacts,
    provenance_links: provenanceLinks,
    action_eligibility: actionEligibility,
  }
}

function formatOntologyFactValue(value: string): string {
  return humanize(canonicalStage(value))
}

function knownNumber(value: number | null, fractionDigits = 0): string {
  return value == null ? 'Unknown' : value.toFixed(fractionDigits)
}

function pushTrace(trace: ProvenanceTraceItem[], item: ProvenanceTraceItem) {
  if (!item.resource_uri || trace.some((existing) => existing.resource_uri === item.resource_uri)) {
    return
  }
  trace.push(item)
}

function uniqueTrustFindingEvidence(records: SemanticIncidentEvidenceRecord[]): SemanticIncidentEvidenceRecord[] {
  return [...records.reduce<Map<string, SemanticIncidentEvidenceRecord>>((summary, record) => {
    const key = record.trustFindingUri ?? `${record.sourceRecordUri}:${record.trustSummary ?? ''}`
    if (!summary.has(key)) {
      summary.set(key, record)
    }
    return summary
  }, new Map()).values()]
}

export function mapSemanticValidation(records: SemanticValidationSummaryRecord[]): SemanticValidation {
  if (!records.length) {
    return {
      status: 'UNKNOWN',
      conforms: null,
      issue_count: 0,
      issues: [],
    }
  }
  const issueCount = records.reduce((total, record) => {
    return total +
      Math.max(0, record.incidentCount - record.incidentWithProvenanceCount) +
      Math.max(0, record.assetCount - record.assetWithProvenanceCount)
  }, 0)
  return {
    status: issueCount === 0 ? 'CONFORMS' : 'NON_CONFORMING',
    conforms: issueCount === 0,
    issue_count: issueCount,
    issues: issueCount
      ? [{
          focus_node: 'named graph provenance',
          result_path: 'prov:wasDerivedFrom',
          message: 'Some semantic graph resources are missing provenance links.',
          severity: 'WARNING',
        }]
      : [],
  }
}

export function mapSemanticIncidentEvidence(
  incidentId: string,
  records: SemanticIncidentEvidenceRecord[],
): SemanticIncidentEvidence {
  const first = records[0]
  return {
    incident_id: incidentId,
    found: records.length > 0,
    request_title: first?.trustSummary ?? null,
    asset_id: null,
    workflow_stage: first ? canonicalStage(first.stageLabel ?? first.stageUri) : null,
    current_status: first?.confidenceState ?? null,
    priority_level: null,
    trust_issue_ids: unique(records.map((record) => record.trustFindingUri).filter(Boolean) as string[]),
  }
}

function isTelemetryEvidence(record: SemanticIncidentEvidenceRecord): boolean {
  return record.evidenceClassUri?.endsWith('TelemetryEvidence') === true || Boolean(record.metricName)
}

function isValidationEvidence(record: SemanticIncidentEvidenceRecord): boolean {
  return record.evidenceClassUri?.endsWith('ValidationEvidence') === true || Boolean(record.validationId)
}

function isWorkOrderEvidence(record: SemanticIncidentEvidenceRecord): boolean {
  return record.evidenceClassUri?.endsWith('WorkOrderEvidence') === true || Boolean(record.workOrderId)
}

function evidenceTypeLabel(record: SemanticIncidentEvidenceRecord): string {
  if (isTelemetryEvidence(record)) return 'TELEMETRY_EVIDENCE'
  if (isValidationEvidence(record)) return 'VALIDATION_EVIDENCE'
  if (isWorkOrderEvidence(record)) return 'WORK_ORDER_EVIDENCE'
  return 'SEMANTIC_EVIDENCE'
}

export function mapSemanticDependencyImpact(
  assetId: string,
  records: SemanticDependencyImpactRecord[],
): SemanticDependencyImpact {
  return {
    asset_id: assetId,
    direct_dependency_count: records.filter((record) => record.dependencyId).length,
    direct_dependencies: records
      .filter((record) => record.dependencyId && record.dependencyAssetId)
      .map((record) => ({
        dependency_id: record.dependencyId as string,
        dependent_asset_id: assetId,
        dependency_asset_id: record.dependencyAssetId as string,
        dependency_type: record.impactScope ?? 'SEMANTIC_DEPENDENCY',
        dependency_role: record.dependencyRole ?? 'UNKNOWN',
        finding_uri: record.findingUri ?? null,
        finding_summary: record.findingSummary ?? null,
        source_record_uri: record.sourceRecordUri ?? null,
      })),
    inferred_downstream_assets: unique(records.map((record) => record.dependencyAssetId).filter(Boolean) as string[]),
    reasoning_findings: uniqueReasoningFindings(
      records
        .filter((record) => record.findingUri)
        .map((record) => ({
          finding_uri: record.findingUri as string,
          summary: record.findingSummary ?? 'Dependency exposure finding',
          source_record_uri: record.sourceRecordUri ?? null,
        })),
    ),
  }
}

export function mapSemanticBlastRadius(
  assetId: string,
  records: SemanticBlastRadiusRecord[],
): SemanticBlastRadius {
  return {
    asset_id: assetId,
    inferred_downstream_assets: unique(records.map((record) => record.downstreamAssetId).filter(Boolean) as string[]),
    affected_incident_count: unique(records.map((record) => record.incidentId).filter(Boolean) as string[]).length,
    reasoning_findings: uniqueReasoningFindings(
      records
        .filter((record) => record.findingUri)
        .map((record) => ({
          finding_uri: record.findingUri as string,
          summary: record.findingSummary ?? 'Blast-radius finding',
          source_record_uri: null,
        })),
    ),
    affected_incidents: records
      .filter((record) => record.incidentId)
      .map((record) => ({
        incident_id: record.incidentId as string,
        asset_id: record.downstreamAssetId ?? 'UNKNOWN',
        title: record.findingSummary ?? 'Semantic blast-radius finding',
        stage: 'SEMANTIC_GRAPH',
      })),
  }
}

function uniqueReasoningFindings(findings: SemanticReasoningFinding[]): SemanticReasoningFinding[] {
  return [...findings.reduce<Map<string, SemanticReasoningFinding>>((summary, finding) => {
    if (!summary.has(finding.finding_uri)) {
      summary.set(finding.finding_uri, finding)
    }
    return summary
  }, new Map()).values()]
}

function statusForAsset(assetId: string, activeByAsset: Map<string, number>): string {
  return (activeByAsset.get(assetId) ?? 0) > 0 ? 'Degraded' : 'Unknown'
}
