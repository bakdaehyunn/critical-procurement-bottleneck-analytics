const SEMANTIC_API_BASE_URL = import.meta.env.VITE_SEMANTIC_API_BASE_URL ?? 'http://127.0.0.1:18080'
const SEMANTIC_SOURCE_RELEASE_ID = import.meta.env.VITE_SEMANTIC_SOURCE_RELEASE_ID ?? 'local-controlled-source-v1'
const SEMANTIC_REASONING_RUN_ID = import.meta.env.VITE_SEMANTIC_REASONING_RUN_ID ?? 'local-controlled-reasoning-v1'
const SEMANTIC_ACTION_AUDIT_RELEASE_ID = import.meta.env.VITE_SEMANTIC_ACTION_AUDIT_RELEASE_ID ?? 'local-action-audit-v1'
const SEMANTIC_AI_AUDIT_RELEASE_ID = import.meta.env.VITE_SEMANTIC_AI_AUDIT_RELEASE_ID ?? 'local-ai-governance-v1'

export type Overview = {
  total_requests: number
  open_requests: number
  delayed_requests: number
  critical_asset_delayed: number
  avg_downtime_hours: number
  top_bottleneck_stage: string | null
  spare_waiting_delay_hours: number
  repeat_failure_asset_count: number
  engineer_assignment_delay_hours: number
  capacity_risk_kw: number
  affected_gpu_count: number
  redundancy_lost_incidents: number
  vendor_eta_missed_count: number
  latest_pipeline_run_status: string | null
  data_quality_status: string
}

export type FollowUpItem = {
  priority_rank: number
  incident_id: string
  request_number: string
  request_title: string
  asset_id: string
  asset_name: string
  zone_id: string
  zone_name: string
  current_stage: string
  current_status: string
  hours_in_current_stage: number
  needed_by_at: string
  priority_level: string
  business_impact: string
  asset_criticality_score: number
  downtime_score: number
  stage_delay_score: number
  infrastructure_zone_impact_score: number
  needed_by_urgency_score: number
  repeat_failure_score: number
  spare_risk_score: number
  capacity_risk_score: number
  redundancy_risk_score: number
  thermal_risk_score: number
  vendor_eta_risk_score: number
  mitigation_credit_score: number
  total_priority_score: number
  recommended_action: string
  reason_summary: string
  redundancy_state: string | null
  affected_gpu_count: number
  estimated_capacity_risk_kw: number
  mitigation_status: string | null
  vendor_status: string | null
  impact_confidence_status: string
  impact_trust_issue_count: number
  restore_readiness_status: string
  restore_readiness_summary: string | null
  dependency_roles: string[]
  dependency_path_ids: string[]
}

export type StageBottleneck = {
  stage: string
  request_count: number
  delayed_count: number
  delay_rate: number
  avg_duration_hours: number
  p90_duration_hours: number
  total_delay_hours: number
}

export type InfrastructureAssetDelay = {
  asset_id: string
  asset_name: string
  zone_id: string
  zone_name: string
  request_count: number
  delayed_request_count: number
  repeat_failure_count: number
  total_downtime_hours: number
  avg_repair_duration_hours: number
  top_failure_mode: string
}

export type InfrastructureZoneDelay = {
  zone_id: string
  zone_name: string
  open_request_count: number
  delayed_request_count: number
  critical_asset_delayed_count: number
  total_downtime_hours: number
  top_bottleneck_stage: string
}

export type SpareWaiting = {
  spare_id: string
  spare_name: string
  spare_category: string
  waiting_request_count: number
  total_wait_hours: number
  avg_wait_hours: number
  critical_spare: boolean
  stock_status: string
}

export type DataQualityCheck = {
  check_result_id: string
  pipeline_run_id: string
  check_name: string
  graph_scope: string
  severity: string
  status: string
  failed_row_count: number
  sample_failed_keys: string[]
  message: string
  created_at: string
}

export type ImpactSummary = {
  incident_count: number
  capacity_risk_kw: number
  affected_rack_count: number
  affected_gpu_count: number
  redundancy_lost_incidents: number
  vendor_eta_missed_count: number
  mitigated_incidents: number
  thermal_breach_minutes: number
  trusted_impact_count: number
  warning_impact_count: number
  unverified_impact_count: number
}

export type InfrastructureDependency = {
  dependency_id: string
  dependent_asset_id: string
  dependent_asset_name: string
  dependent_asset_type: string
  dependent_status: string
  dependency_asset_id: string
  dependency_asset_name: string
  dependency_asset_type: string
  dependency_status: string
  dependency_type: string
  dependency_role: string
  impact_scope: string
  dependent_active_incident_count: number
  dependency_active_incident_count: number
}

export type SemanticDependencyEdge = {
  dependency_id: string
  dependent_asset_id: string
  dependency_asset_id: string
  dependency_type: string
  dependency_role: string
}

export type SemanticDependencyImpact = {
  asset_id: string
  direct_dependency_count: number
  direct_dependencies: SemanticDependencyEdge[]
  inferred_downstream_assets: string[]
}

export type SemanticIncidentEvidence = {
  incident_id: string
  found: boolean
  request_title: string | null
  asset_id: string | null
  workflow_stage: string | null
  current_status: string | null
  priority_level: string | null
  trust_issue_ids: string[]
}

export type SemanticBlastRadius = {
  asset_id: string
  inferred_downstream_assets: string[]
  affected_incident_count: number
  affected_incidents: {
    incident_id: string
    asset_id: string
    title: string
    stage: string
  }[]
}

export type SemanticValidation = {
  conforms: boolean
  issue_count: number
  issues: {
    focus_node: string
    result_path: string
    message: string
    severity: string
  }[]
}

export type RequestSemanticContext = {
  validation: SemanticValidation
  incidentEvidence: SemanticIncidentEvidence
  dependencyImpact: SemanticDependencyImpact
  blastRadius: SemanticBlastRadius
}

export type StageLeadTime = {
  stage: string
  entered_at: string
  exited_at: string | null
  duration_hours: number
  threshold_hours: number
  is_bottleneck: boolean
  delay_hours: number
}

export type TimelineEvent = {
  event_id: string
  stage: string
  event_type: string
  event_status: string
  occurred_at: string
  actor_type: string
  reason_code: string | null
  message: string | null
  source_record_uri: string | null
}

export type ProvenanceTraceItem = {
  step: string
  label: string
  resource_uri: string
  detail: string
}

export type OntologyActionPlacement = 'summary' | 'trust'

export type OntologyActionTarget = {
  role: string
  label: string
  resource_uri: string
}

export type OntologyActionAffordance = {
  action_id: string
  label: string
  description: string
  status: 'DISABLED'
  incident_uri: string
  incident_id: string
  source_record_uri: string
  ui_placement: OntologyActionPlacement[]
  target_objects: OntologyActionTarget[]
  required_parameters: string[]
  preconditions: string[]
  provenance_requirements: string[]
  disabled_reasons: string[]
}

export type OntologyActionAuditHistoryItem = {
  graph_uri: string
  action_audit_release_id: string
  execution_uri: string
  execution_id: string
  request_uri: string
  request_id: string
  validation_report_uri: string
  action_type_uri: string
  action_type_id: string
  action_type_label: string | null
  idempotency_key: string
  actor_id: string
  action_reason: string
  action_status: string
  requested_at: string
  executed_at: string
  target_object_uri: string | null
  validation_status: string
  validation_summary: string | null
  source_record_uri: string | null
  assigned_team: string | null
  assignee_id: string | null
  reviewed_status: string | null
  review_summary: string | null
  supporting_evidence_uri: string | null
}

export type OntologyActionNotificationItem = {
  graph_uri: string
  action_audit_release_id: string
  notification_uri: string
  notification_id: string
  notification_status: string
  notification_summary: string
  execution_uri: string
  execution_id: string
  request_uri: string
  request_id: string
  action_type_uri: string
  action_type_id: string
  actor_id: string
  action_reason: string
  requested_at: string
  generated_at: string
  incident_uri: string
  incident_id: string
  target_object_uri: string | null
  source_record_uri: string | null
  assigned_team: string | null
  assignee_id: string | null
  reviewed_status: string | null
  review_summary: string | null
}

export type OntologyActionReviewQueueItem = {
  graph_uri: string
  action_audit_release_id: string
  notification_uri: string
  notification_id: string
  execution_uri: string
  execution_id: string
  request_uri: string
  request_id: string
  action_type_uri: string
  action_type_id: string
  actor_id: string
  action_reason: string
  current_state: OntologyActionLifecycleState
  state_generated_at: string
  incident_uri: string
  incident_id: string
  source_record_uri: string | null
}

export type OntologyActionTransitionHistoryItem = {
  graph_uri: string
  action_audit_release_id: string
  transition_uri: string
  transition_id: string
  execution_uri: string
  execution_id: string
  request_uri: string
  request_id: string
  action_type_uri: string
  action_type_id: string
  actor_id: string
  transition_reason: string
  from_state: OntologyActionLifecycleState | null
  to_state: OntologyActionLifecycleState
  generated_at: string
  incident_uri: string
  incident_id: string
}

export type OntologyActionDispatchQueueItem = {
  graph_uri: string
  action_audit_release_id: string
  dispatch_uri: string
  dispatch_id: string
  dispatch_channel: string
  dispatch_status: string
  dispatch_lifecycle_state: string
  dispatch_summary: string
  execution_uri: string
  execution_id: string
  request_uri: string
  request_id: string
  action_type_uri: string
  action_type_id: string
  transition_uri: string
  transition_id: string
  actor_id: string
  generated_at: string
  incident_uri: string
  incident_id: string
  source_record_uri: string | null
}

export type AiProposalItem = {
  graph_uri: string
  ai_audit_release_id: string
  proposal_uri: string
  proposal_id: string
  proposal_type: string
  proposal_status: string
  review_status: string
  disabled_reason: string
  summary: string
  rationale: string
  confidence_score: number
  risk_level: string
  model_id: string
  prompt_id: string
  prompt_hash: string
  actor_id: string
  generated_at: string
  batch_uri: string
  batch_id: string
  validation_report_uri: string
  validation_status: string
  validation_summary: string
  incident_uri: string
  incident_id: string
  target_object_uri: string
  source_record_uri: string
  supporting_evidence_uri: string
}

export type DynamicPlaybackItem = {
  graph_uri: string
  action_audit_release_id: string
  event_uri: string
  event_id: string
  scenario_id: string
  playback_batch_id: string
  playback_step: number
  incident_uri: string
  incident_id: string
  event_kind: string
  source_family: string
  occurred_at: string
  summary: string
  source_record_uri: string
  before_state: string
  after_state: string
  before_reasoning_state: string
  after_reasoning_state: string
  before_trust_state: string
  after_trust_state: string
  before_blast_radius_count: number
  after_blast_radius_count: number
  action_lifecycle_state: string
  canonical_graph_uri: string | null
  provenance_graph_uri: string | null
  reasoning_graph_uri: string | null
}

export type OntologyActionLifecycleState = 'REQUESTED' | 'VALIDATED' | 'QUEUED' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'CLOSED'

export type OntologyActionSubmission = {
  action_id: string
  actor_id: string
  action_reason: string
  incident_uri: string
  source_record_uri: string
  restore_readiness_finding_uri?: string
  recovery_blocker_uri?: string
  trust_finding_uri?: string
  validation_evidence_uri?: string
  assigned_team?: string
  assignee_id?: string
  reviewed_status?: string
  review_summary?: string
  supporting_evidence_uri?: string
}

export type OntologyActionSubmissionResult = {
  resultType: 'ontology-action-request'
  audited: boolean
  actionId: string
  requestId: string
  idempotencyKey: string
  actionAuditGraphUri: string
  writtenGraphUris: string[]
  idempotentReplay: boolean
  notificationStatus: string
  canonicalGraphMutation: boolean
  reasoningGraphMutation: boolean
  operationsGraphMutation: boolean
  externalSystemMutation: boolean
}

export type OntologyActionTransitionSubmission = {
  target_execution_uri: string
  to_state: OntologyActionLifecycleState
  actor_id: string
  transition_reason: string
}

export type OntologyActionTransitionResult = {
  resultType: 'ontology-action-transition'
  transitioned: boolean
  transitionId: string
  idempotencyKey: string
  targetExecutionUri: string
  currentState: OntologyActionLifecycleState
  actionAuditGraphUri: string
  writtenGraphUris: string[]
  idempotentReplay: boolean
  canonicalGraphMutation: boolean
  reasoningGraphMutation: boolean
  operationsGraphMutation: boolean
  externalSystemMutation: boolean
}

export type AiProposalReviewDecision = 'APPROVE' | 'REJECT'

export type AiProposalReviewSubmission = {
  proposal_uri: string
  proposal_id: string
  decision: AiProposalReviewDecision
  actor_id: string
  review_reason: string
  action_id?: string
}

export type AiProposalReviewResult = {
  resultType: 'ai-proposal-review'
  reviewed: boolean
  decision: AiProposalReviewDecision
  reviewStatus: string
  reviewId: string
  idempotencyKey: string
  proposalUri: string
  aiAuditGraphUri: string
  actionAuditGraphUri?: string
  writtenGraphUris: string[]
  idempotentReplay: boolean
  actionRequestCreated: boolean
  actionRequestId?: string
  actionId?: string
  canonicalGraphMutation: boolean
  reasoningGraphMutation: boolean
  provenanceGraphMutation: boolean
  sourceGraphMutation: boolean
  operationsGraphMutation: boolean
  externalSystemMutation: boolean
}

export type OntologyReviewQueueItem = {
  graph_uri: string
  queue_id: string
  queue_kind: string
  review_action_id: string
  review_action_label: string
  review_status: string
  target_uri: string
  target_type: string
  target_label: string
  release_id: string
  source_graph_uri: string | null
  canonical_graph_uri: string | null
  provenance_graph_uri: string | null
  reasoning_audit_graph_uri: string | null
  reasoning_graph_uri: string | null
  evidence_summary: string
  action_status: 'DISABLED'
  disabled_reason: string
  incident_count: number
  asset_count: number
  source_record_count: number
  activity_count: number
  generated_fact_count: number
  priority_sort_order: number
}

export type WorkOrder = {
  work_order_id: string
  assigned_team: string
  assigned_engineer_id: string | null
  work_order_status: string
  planned_start_at: string | null
  actual_start_at: string | null
  actual_completed_at: string | null
  required_spare_id: string | null
  required_spare_name: string | null
  stock_status: string | null
}

export type RequestDetail = {
  request: FollowUpItem
  stage_lead_times: StageLeadTime[]
  timeline: TimelineEvent[]
  work_orders: WorkOrder[]
  validation_results: {
    validation_id: string
    validation_status: string
    validator_id: string | null
    validation_started_at: string | null
    validation_completed_at: string | null
    failure_reason: string | null
  }[]
  telemetry_alerts: {
    telemetry_alert_id: string
    asset_id: string
    alert_type: string
    severity: string
    triggered_at: string
    resolved_at: string | null
  }[]
  impact_snapshot: {
    impact_snapshot_id: string
    incident_id: string
    asset_id: string
    zone_id: string
    snapshot_at: string
    redundancy_state: string
    affected_rack_count: number
    affected_gpu_count: number
    estimated_capacity_risk_kw: number
    estimated_gpu_capacity_risk_pct: number
    thermal_breach_minutes: number
    power_redundancy_lost: boolean
    cooling_redundancy_lost: boolean
    mitigation_status: string
    vendor_eta_at: string | null
    vendor_status: string
    source_system: string
    telemetry_readings: {
      metric: string
      value: number
      unit: string
      status: string
    }[]
  } | null
  quality_flags: string[]
  restore_readiness: {
    status: string
    summary: string | null
    finding_uri: string | null
  }
  impact_confidence_status: string
  impact_trust_flags: {
    issue_type: string
    severity: string
    message: string
    evidence: Record<string, unknown>
  }[]
  provenance_trace: ProvenanceTraceItem[]
  ontology_actions: OntologyActionAffordance[]
  action_audit_history: OntologyActionAuditHistoryItem[]
  action_notifications: OntologyActionNotificationItem[]
  action_review_queue: OntologyActionReviewQueueItem[]
  action_transition_history: OntologyActionTransitionHistoryItem[]
  action_dispatch_queue: OntologyActionDispatchQueueItem[]
  ai_proposals: AiProposalItem[]
  dynamic_event_timeline: DynamicPlaybackItem[]
  dynamic_state_changes: DynamicPlaybackItem[]
  dynamic_reasoning_changes: DynamicPlaybackItem[]
  dynamic_action_lifecycle: DynamicPlaybackItem[]
}

export type FilterOption = {
  id: string
  name: string
}

export type FilterMetadata = {
  infrastructure_zones: FilterOption[]
  assets: FilterOption[]
  asset_types: string[]
  facilities_teams: string[]
  spare_categories: string[]
  priority_levels: string[]
  request_types: string[]
  failure_modes: string[]
  stages: string[]
}

export type DashboardFilters = {
  zone_id?: string
  asset_id?: string
  priority_level?: string
  stage?: string
  delayed_only?: boolean
  critical_asset_delayed?: boolean
  capacity_risk?: boolean
  affected_gpu?: boolean
  evidence_review?: boolean
  redundancy_lost?: boolean
  vendor_parts_escalation?: boolean
  restore_blocked?: boolean
  trust_review?: boolean
  dependency_role?: string
}

export type DashboardData = {
  overview: Overview
  followUps: FollowUpItem[]
  stageBottlenecks: StageBottleneck[]
  assetDelays: InfrastructureAssetDelay[]
  zoneDelays: InfrastructureZoneDelay[]
  spareWaiting: SpareWaiting[]
  qualityChecks: DataQualityCheck[]
  impactSummary: ImpactSummary
  topologyDependencies: InfrastructureDependency[]
  ontologyReviewQueue: OntologyReviewQueueItem[]
}

type SemanticEnvelope<T> = {
  queryId: string
  resultType: string
  recordCount: number
  records: T[]
  provenance: {
    queryId: string
    graphScope: string
    contractVersion: string
  }
}

type SemanticDashboardOverviewRecord = {
  totalIncidents: number
  assetCount: number
  zoneCount: number
  impactObservationCount: number
  capacityRiskKw: number
  affectedGpuCount: number
  dependencyEdgeCount: number
  trustFindingCount: number
  avgDurationHours?: number
  totalDurationHours?: number
  totalDelayHours?: number
  mitigatedIncidentCount?: number
  affectedRackCount?: number
  thermalBreachMinutes?: number
  redundancyLostIncidentCount?: number
  vendorEtaMissedCount?: number
  repeatFailureAssetCount?: number
  engineerAssignmentDelayHours?: number
}

type SemanticFollowUpQueueRecord = {
  graphUri: string
  incidentUri: string
  incidentId: string
  assetUri: string
  assetId: string
  zoneUri: string
  zoneId: string
  stageUri: string
  stageLabel?: string
  sourceRecordUri: string
  priorityRank?: number
  requestTitle?: string
  currentStatus?: string
  hoursInCurrentStage?: number
  neededByAt?: string
  priorityLevel?: string
  businessImpact?: string
  assetCriticalityScore?: number
  downtimeScore?: number
  stageDelayScore?: number
  infrastructureZoneImpactScore?: number
  neededByUrgencyScore?: number
  repeatFailureScore?: number
  repeatFailureAssetCount?: number
  engineerAssignmentDelayHours?: number
  spareRiskScore?: number
  capacityRiskScore?: number
  redundancyRiskScore?: number
  thermalRiskScore?: number
  vendorEtaRiskScore?: number
  mitigationCreditScore?: number
  totalPriorityScore?: number
}

type SemanticFollowUpDetailRecord = SemanticFollowUpQueueRecord & {
  impactUri?: string
  capacityRiskKw?: number
  affectedGpuCount?: number
  followUpDecisionUri?: string
  recommendedAction?: string
  recoveryBlockerUri?: string
  blockerSummary?: string
  restoreReadinessUri?: string
  restoreReadinessSummary?: string
  trustFindingUri?: string
  trustSummary?: string
  redundancyState?: string
  affectedRackCount?: number
  estimatedGpuCapacityRiskPct?: number
  thermalBreachMinutes?: number
  powerRedundancyLost?: boolean
  coolingRedundancyLost?: boolean
  mitigationStatus?: string
  vendorEtaAt?: string
  vendorStatus?: string
}

type SemanticFilterMetadataRecord = {
  filterType: string
  id: string
  label?: string
}

type SemanticImpactSummaryRecord = {
  impactObservationCount: number
  incidentCount: number
  capacityRiskKw: number
  affectedGpuCount: number
  trustFindingCount: number
  affectedRackCount?: number
  thermalBreachMinutes?: number
  redundancyLostIncidentCount?: number
  vendorEtaMissedCount?: number
  mitigatedIncidentCount?: number
}

type SemanticStageBottleneckRecord = {
  stageUri: string
  stageLabel?: string
  incidentCount: number
  delayedCount?: number
  avgDurationHours?: number
  p90DurationHours?: number
  totalDelayHours?: number
  sourceRecordUri: string
}

type SemanticAssetDelaySummaryRecord = {
  assetId: string
  zoneId: string
  incidentCount: number
  impactObservationCount: number
  capacityRiskKw: number
  affectedGpuCount: number
  delayedIncidentCount?: number
  repeatFailureCount?: number
  totalDurationHours?: number
  avgDurationHours?: number
  topFailureMode?: string
  sourceRecordUri: string
}

type SemanticZoneDelaySummaryRecord = {
  zoneId: string
  assetCount: number
  incidentCount: number
  impactObservationCount: number
  capacityRiskKw: number
  affectedGpuCount: number
  delayedIncidentCount?: number
  criticalIncidentCount?: number
  totalDurationHours?: number
  topBottleneckStage?: string
  sourceRecordUri: string
}

type SemanticSpareWaitSummaryRecord = {
  stageUri: string
  stageLabel?: string
  incidentCount: number
  recoveryBlockerCount: number
  totalWaitHours?: number
  avgWaitHours?: number
  stockStatus?: string
  sourceRecordUri: string
}

type SemanticTrustFindingRecord = {
  trustFindingUri: string
  trustFindingId?: string
  summary: string
  sourceFactUri: string
  activityUri?: string
  severity?: string
  status?: string
  createdAt?: string
}

type SemanticTopologyDependencyRecord = {
  dependencyEdgeUri: string
  dependencyId: string
  dependentAssetId: string
  dependencyAssetId: string
  dependencyRole: string
  impactScope?: string
  pathId?: string
  sourceRecordUri: string
}

type SemanticValidationSummaryRecord = {
  sourceRecordCount: number
  incidentCount: number
  incidentWithProvenanceCount: number
  assetCount: number
  assetWithProvenanceCount: number
}

type SemanticIncidentEvidenceRecord = {
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

type SemanticIncidentTimelineRecord = {
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

type SemanticDependencyImpactRecord = {
  assetId: string
  dependencyId?: string
  dependencyAssetId?: string
  dependencyRole?: string
  impactScope?: string
  findingUri?: string
  findingSummary?: string
  sourceRecordUri?: string
}

type SemanticBlastRadiusRecord = {
  assetId: string
  downstreamAssetId?: string
  incidentId?: string
  findingUri?: string
  findingSummary?: string
}

type SemanticActionAvailabilityRecord = {
  graphUri: string
  incidentUri: string
  incidentId: string
  assetUri: string
  assetId: string
  sourceRecordUri: string
  actionId: string
  actionLabel: string
  actionDescription: string
  actionStatus: string
  uiPlacement: OntologyActionPlacement
  detailKind: 'targetObject' | 'requiredParameter' | 'precondition' | 'provenanceRequirement' | 'disabledReason'
  detailRole: string
  detailLabel: string
  detailValue: string
  detailSortOrder: number
}

type SemanticActionAuditHistoryRecord = {
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

type SemanticActionNotificationQueueRecord = {
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

type SemanticActionReviewQueueRecord = {
  graphUri: string
  actionAuditReleaseId: string
  notificationUri: string
  notificationId: string
  executionUri: string
  executionId: string
  requestUri: string
  requestId: string
  actionTypeUri: string
  actionTypeId: string
  actorId: string
  actionReason: string
  currentState: OntologyActionLifecycleState
  stateGeneratedAt: string
  incidentUri: string
  incidentId: string
  sourceRecordUri?: string
}

type SemanticActionTransitionHistoryRecord = {
  graphUri: string
  actionAuditReleaseId: string
  transitionUri: string
  transitionId: string
  executionUri: string
  executionId: string
  requestUri: string
  requestId: string
  actionTypeUri: string
  actionTypeId: string
  actorId: string
  transitionReason: string
  fromState?: OntologyActionLifecycleState
  toState: OntologyActionLifecycleState
  generatedAt: string
  incidentUri: string
  incidentId: string
}

type SemanticActionDispatchQueueRecord = {
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

type SemanticAiProposalRecord = {
  graphUri: string
  aiAuditReleaseId: string
  proposalUri: string
  proposalId: string
  proposalType: string
  proposalStatus: string
  reviewStatus: string
  disabledReason: string
  summary: string
  rationale: string
  confidenceScore: number
  riskLevel: string
  modelId: string
  promptId: string
  promptHash: string
  actorId: string
  generatedAt: string
  batchUri: string
  batchId: string
  validationReportUri: string
  validationStatus: string
  validationSummary: string
  incidentUri: string
  incidentId: string
  targetObjectUri: string
  sourceRecordUri: string
  supportingEvidenceUri: string
}

type SemanticDynamicPlaybackRecord = {
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

type SemanticOntologyReviewQueueRecord = {
  graphUri: string
  queueId: string
  queueKind: string
  reviewActionId: string
  reviewActionLabel: string
  reviewStatus: string
  targetUri: string
  targetType: string
  targetLabel: string
  releaseId: string
  sourceGraphUri?: string
  canonicalGraphUri?: string
  provenanceGraphUri?: string
  reasoningAuditGraphUri?: string
  reasoningGraphUri?: string
  evidenceSummary: string
  actionStatus: string
  disabledReason: string
  incidentCount: number
  assetCount: number
  sourceRecordCount: number
  activityCount: number
  generatedFactCount: number
  prioritySortOrder: number
}

export async function fetchDashboardData(filters: DashboardFilters = {}): Promise<DashboardData> {
  const [
    overviewRecords,
    queueRecords,
    detailRecords,
    stageBottlenecks,
    assetDelays,
    zoneDelays,
    spareWaiting,
    trustFindings,
    impactRecords,
    dependencyRecords,
    promotionReviewQueue,
    reasoningReviewQueue,
  ] = await Promise.all([
    postSemanticQuery<SemanticDashboardOverviewRecord>('semanticDashboardOverview'),
    postSemanticQuery<SemanticFollowUpQueueRecord>('semanticFollowUpQueueList'),
    postSemanticQuery<SemanticFollowUpDetailRecord>('semanticFollowUpDetail'),
    postSemanticQuery<SemanticStageBottleneckRecord>('semanticStageBottlenecks'),
    postSemanticQuery<SemanticAssetDelaySummaryRecord>('semanticAssetDelaySummary'),
    postSemanticQuery<SemanticZoneDelaySummaryRecord>('semanticZoneDelaySummary'),
    postSemanticQuery<SemanticSpareWaitSummaryRecord>('semanticSpareWaitSummary'),
    postSemanticQuery<SemanticTrustFindingRecord>('semanticTrustFindingList'),
    postSemanticQuery<SemanticImpactSummaryRecord>('semanticImpactSummary'),
    postSemanticQuery<SemanticTopologyDependencyRecord>('semanticTopologyDependencies'),
    postSemanticQuery<SemanticOntologyReviewQueueRecord>('semanticPromotionReviewQueue'),
    postSemanticQuery<SemanticOntologyReviewQueueRecord>('semanticReasoningReviewQueue'),
  ])

  const followUps = applyDashboardFilters(buildFollowUps(queueRecords, detailRecords, dependencyRecords), filters)

  return {
    overview: buildOverview(overviewRecords[0], followUps),
    followUps,
    stageBottlenecks: stageBottlenecks.map(mapStageBottleneck),
    assetDelays: assetDelays.map(mapAssetDelaySummary),
    zoneDelays: zoneDelays.map(mapZoneDelaySummary),
    spareWaiting: spareWaiting.map(mapSpareWaitSummary),
    qualityChecks: trustFindings.map(mapTrustFinding),
    impactSummary: buildImpactSummary(impactRecords[0], followUps),
    topologyDependencies: buildTopologyDependencies(dependencyRecords, followUps),
    ontologyReviewQueue: buildOntologyReviewQueue([...promotionReviewQueue, ...reasoningReviewQueue]),
  }
}

export function filterDashboardData(data: DashboardData, filters: DashboardFilters): DashboardData {
  const followUps = applyDashboardFilters(data.followUps, filters)
  return {
    ...data,
    overview: {
      ...buildOverview(undefined, followUps),
      latest_pipeline_run_status: data.overview.latest_pipeline_run_status,
      data_quality_status: data.overview.data_quality_status,
    },
    followUps,
    impactSummary: buildImpactSummary(undefined, followUps),
    ontologyReviewQueue: data.ontologyReviewQueue,
  }
}

export async function fetchFilterMetadata(): Promise<FilterMetadata> {
  const records = await postSemanticQuery<SemanticFilterMetadataRecord>('semanticFilterMetadata')
  const grouped = records.reduce<Record<string, FilterOption[]>>((summary, record) => {
    const key = record.filterType
    summary[key] = summary[key] ?? []
    summary[key].push({
      id: record.id,
      name: record.label ?? humanize(record.id),
    })
    return summary
  }, {})
  return {
    infrastructure_zones: grouped.zone ?? [],
    assets: grouped.asset ?? [],
    asset_types: unique(records.filter((record) => record.filterType === 'assetType').map((record) => record.label ?? record.id)),
    facilities_teams: [],
    spare_categories: [],
    priority_levels: ['CRITICAL', 'HIGH', 'MEDIUM'],
    request_types: [],
    failure_modes: [],
    stages: unique(records.filter((record) => record.filterType === 'stage').map((record) => record.label ?? record.id)),
  }
}

export async function fetchRequestDetail(infrastructureRequestId: string): Promise<RequestDetail> {
  const [
    queueRecords,
    detailRecords,
    evidenceRecords,
    timelineRecords,
    actionAvailabilityRecords,
    actionAuditRecords,
    actionNotificationRecords,
    actionReviewRecords,
    actionTransitionRecords,
    actionDispatchRecords,
    aiProposalRecords,
    dynamicTimelineRecords,
    dynamicStateChangeRecords,
    dynamicReasoningChangeRecords,
    dynamicActionLifecycleRecords,
  ] = await Promise.all([
    postSemanticQuery<SemanticFollowUpQueueRecord>('semanticFollowUpQueueList'),
    postSemanticQuery<SemanticFollowUpDetailRecord>('semanticFollowUpDetail', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticIncidentEvidenceRecord>('semanticIncidentEvidence', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticIncidentTimelineRecord>('semanticIncidentTimeline', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionAvailabilityRecord>('semanticAvailableActionsByFinding', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionAuditHistoryRecord>('semanticActionAuditHistoryByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionNotificationQueueRecord>('semanticActionNotificationQueueByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionReviewQueueRecord>('semanticActionReviewQueueByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionTransitionHistoryRecord>('semanticActionTransitionHistoryByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticActionDispatchQueueRecord>('semanticActionDispatchQueueByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticAiProposalRecord>('semanticAiProposalDetailByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticDynamicPlaybackRecord>('semanticDynamicEventTimelineByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticDynamicPlaybackRecord>('semanticDynamicStateChangesByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticDynamicPlaybackRecord>('semanticDynamicReasoningChangesByIncident', { incidentIdParam: infrastructureRequestId }),
    postSemanticQuery<SemanticDynamicPlaybackRecord>('semanticDynamicActionLifecycleByIncident', { incidentIdParam: infrastructureRequestId }),
  ])
  const request = buildFollowUps(queueRecords, detailRecords).find((row) => row.incident_id === infrastructureRequestId)
  if (!request) {
    throw new Error(`Semantic finding not found: ${infrastructureRequestId}`)
  }
  const detailRecord = detailRecords.find((record) => record.incidentId === infrastructureRequestId)
  const evidence = evidenceRecords
  const timeline = timelineRecords
  return buildRequestDetail(
    request,
    detailRecord,
    evidence,
    timeline,
    actionAvailabilityRecords,
    actionAuditRecords,
    actionNotificationRecords,
    actionReviewRecords,
    actionTransitionRecords,
    actionDispatchRecords,
    aiProposalRecords,
    dynamicTimelineRecords,
    dynamicStateChangeRecords,
    dynamicReasoningChangeRecords,
    dynamicActionLifecycleRecords,
  )
}

export async function submitOntologyActionRequest(
  submission: OntologyActionSubmission,
): Promise<OntologyActionSubmissionResult> {
  const requestId = `ACT-REQ-${submission.action_id}-${submission.actor_id}-${new Date().toISOString().replace(/[-:.TZ]/g, '')}`
  const idempotencyKey = `${SEMANTIC_ACTION_AUDIT_RELEASE_ID}:${submission.action_id}:${submission.incident_uri}:${requestId}`
  const response = await fetch(`${SEMANTIC_API_BASE_URL}/semantic/internal/action-request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      requestId,
      actionId: submission.action_id,
      idempotencyKey,
      actorId: submission.actor_id,
      requestedAt: new Date().toISOString(),
      incidentUri: submission.incident_uri,
      actionReason: submission.action_reason,
      sourceRecordUri: submission.source_record_uri,
      restoreReadinessFindingUri: submission.restore_readiness_finding_uri,
      recoveryBlockerUri: submission.recovery_blocker_uri,
      trustFindingUri: submission.trust_finding_uri,
      validationEvidenceUri: submission.validation_evidence_uri,
      assignedTeam: submission.assigned_team,
      assigneeId: submission.assignee_id,
      reviewedStatus: submission.reviewed_status,
      reviewSummary: submission.review_summary,
      supportingEvidenceUri: submission.supporting_evidence_uri,
      sourceReleaseId: SEMANTIC_SOURCE_RELEASE_ID,
      reasoningRunId: SEMANTIC_REASONING_RUN_ID,
      actionAuditReleaseId: SEMANTIC_ACTION_AUDIT_RELEASE_ID,
    }),
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Ontology action request failed: ${response.status} ${response.statusText} ${payload}`)
  }
  return await response.json() as OntologyActionSubmissionResult
}

export async function submitOntologyActionTransition(
  submission: OntologyActionTransitionSubmission,
): Promise<OntologyActionTransitionResult> {
  const transitionId = `ACT-TRN-${submission.to_state}-${submission.actor_id}-${new Date().toISOString().replace(/[-:.TZ]/g, '')}`
  const idempotencyKey = `${SEMANTIC_ACTION_AUDIT_RELEASE_ID}:transition:${submission.to_state}:${submission.target_execution_uri}:${transitionId}`
  const response = await fetch(`${SEMANTIC_API_BASE_URL}/semantic/internal/action-transition`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      transitionId,
      idempotencyKey,
      actorId: submission.actor_id,
      requestedAt: new Date().toISOString(),
      targetExecutionUri: submission.target_execution_uri,
      toState: submission.to_state,
      transitionReason: submission.transition_reason,
      sourceReleaseId: SEMANTIC_SOURCE_RELEASE_ID,
      reasoningRunId: SEMANTIC_REASONING_RUN_ID,
      actionAuditReleaseId: SEMANTIC_ACTION_AUDIT_RELEASE_ID,
    }),
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Ontology action transition failed: ${response.status} ${response.statusText} ${payload}`)
  }
  return await response.json() as OntologyActionTransitionResult
}

export async function submitAiProposalReview(
  submission: AiProposalReviewSubmission,
): Promise<AiProposalReviewResult> {
  const reviewedAt = new Date().toISOString()
  const timestamp = reviewedAt.replace(/[-:.TZ]/g, '')
  const reviewId = `AI-REV-${submission.decision}-${submission.proposal_id}-${timestamp}`
  const idempotencyKey = `${SEMANTIC_AI_AUDIT_RELEASE_ID}:review:${submission.decision}:${submission.proposal_id}:${timestamp}`
  const response = await fetch(`${SEMANTIC_API_BASE_URL}/semantic/internal/ai-proposal-review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      reviewId,
      idempotencyKey,
      actorId: submission.actor_id,
      reviewedAt,
      proposalUri: submission.proposal_uri,
      decision: submission.decision,
      reviewReason: submission.review_reason,
      actionId: submission.action_id,
      sourceReleaseId: SEMANTIC_SOURCE_RELEASE_ID,
      reasoningRunId: SEMANTIC_REASONING_RUN_ID,
      aiAuditReleaseId: SEMANTIC_AI_AUDIT_RELEASE_ID,
      actionAuditReleaseId: SEMANTIC_ACTION_AUDIT_RELEASE_ID,
    }),
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`AI proposal review failed: ${response.status} ${response.statusText} ${payload}`)
  }
  return await response.json() as AiProposalReviewResult
}

export async function fetchDataQualityCheck(checkResultId: string): Promise<DataQualityCheck> {
  const records = await postSemanticQuery<SemanticTrustFindingRecord>('semanticTrustFindingList', {
    trustFindingIdParam: checkResultId,
  })
  const selected = records[0]
  if (!selected) {
    throw new Error(`Semantic trust finding not found: ${checkResultId}`)
  }
  return mapTrustFinding(selected)
}

export async function fetchTopologyDependencies(): Promise<InfrastructureDependency[]> {
  const [dependencyRecords, queueRecords, detailRecords] = await Promise.all([
    postSemanticQuery<SemanticTopologyDependencyRecord>('semanticTopologyDependencies'),
    postSemanticQuery<SemanticFollowUpQueueRecord>('semanticFollowUpQueueList'),
    postSemanticQuery<SemanticFollowUpDetailRecord>('semanticFollowUpDetail'),
  ])
  return buildTopologyDependencies(dependencyRecords, buildFollowUps(queueRecords, detailRecords))
}

export async function fetchRequestSemanticContext(
  incidentId: string,
  assetId: string,
): Promise<RequestSemanticContext> {
  const [validationRecords, evidenceRecords, dependencyRecords, blastRadiusRecords] = await Promise.all([
    postSemanticQuery<SemanticValidationSummaryRecord>('semanticValidationSummary'),
    postSemanticQuery<SemanticIncidentEvidenceRecord>('semanticIncidentEvidence', { incidentIdParam: incidentId }),
    postSemanticQuery<SemanticDependencyImpactRecord>('semanticDependencyImpactByAsset', { assetIdParam: assetId }),
    postSemanticQuery<SemanticBlastRadiusRecord>('semanticBlastRadiusByAsset', { assetIdParam: assetId }),
  ])
  const incidentEvidenceRecords = evidenceRecords
  const dependencyImpactRecords = dependencyRecords
  const selectedBlastRadiusRecords = blastRadiusRecords
  return {
    validation: mapSemanticValidation(validationRecords),
    incidentEvidence: mapSemanticIncidentEvidence(incidentId, incidentEvidenceRecords),
    dependencyImpact: mapSemanticDependencyImpact(assetId, dependencyImpactRecords),
    blastRadius: mapSemanticBlastRadius(assetId, selectedBlastRadiusRecords),
  }
}

async function postSemanticQuery<T>(
  queryId: string,
  parameters: Record<string, string> = {},
): Promise<T[]> {
  const response = await fetch(`${SEMANTIC_API_BASE_URL}/semantic/query/${queryId}`, {
    method: 'POST',
    headers: Object.keys(parameters).length ? { 'Content-Type': 'application/json' } : undefined,
    body: Object.keys(parameters).length ? JSON.stringify({ parameters }) : undefined,
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Semantic query failed: ${queryId} ${response.status} ${response.statusText} ${payload}`)
  }
  const payload = await response.json() as SemanticEnvelope<T>
  return payload.records
}

function buildFollowUps(
  queueRecords: SemanticFollowUpQueueRecord[],
  detailRecords: SemanticFollowUpDetailRecord[],
  dependencyRecords: SemanticTopologyDependencyRecord[] = [],
): FollowUpItem[] {
  const detailsByIncident = new Map(detailRecords.map((record) => [record.incidentId, record]))
  const dependenciesByAsset = dependencyRecords.reduce<Map<string, SemanticTopologyDependencyRecord[]>>((summary, record) => {
    const edges = summary.get(record.dependentAssetId) ?? []
    edges.push(record)
    summary.set(record.dependentAssetId, edges)
    return summary
  }, new Map())
  return queueRecords
    .map((record) => mapFollowUp(record, detailsByIncident.get(record.incidentId), dependenciesByAsset.get(record.assetId) ?? []))
    .sort((left, right) => left.priority_rank - right.priority_rank || right.total_priority_score - left.total_priority_score || left.incident_id.localeCompare(right.incident_id))
    .map((row, index) => ({ ...row, priority_rank: row.priority_rank || index + 1 }))
}

function mapFollowUp(
  record: SemanticFollowUpQueueRecord,
  detail?: SemanticFollowUpDetailRecord,
  dependencies: SemanticTopologyDependencyRecord[] = [],
): FollowUpItem {
  const semantic = detail ?? record
  const stage = canonicalStage(semantic.stageLabel ?? record.stageLabel ?? record.stageUri)
  const capacityRiskKw = detail?.capacityRiskKw ?? 0
  const affectedGpuCount = detail?.affectedGpuCount ?? 0
  const trustIssueCount = detail?.trustFindingUri ? 1 : 0
  const restoreReadinessStatus = restoreReadinessStatusFor(detail?.restoreReadinessSummary)
  const priorityLevel = semantic.priorityLevel ?? priorityFor(capacityRiskKw, affectedGpuCount, trustIssueCount)
  const redundancyState = normalizeRedundancyState(detail?.redundancyState)
  const vendorStatus = normalizeVendorStatus(detail?.vendorStatus)
  const totalPriorityScore = semantic.totalPriorityScore ?? capacityRiskKw / 10 + affectedGpuCount / 4 + trustIssueCount * 20

  return {
    priority_rank: semantic.priorityRank ?? 0,
    incident_id: record.incidentId,
    request_number: record.incidentId,
    request_title: semantic.requestTitle ?? detail?.blockerSummary ?? detail?.recommendedAction ?? `${humanize(record.assetId)} semantic finding`,
    asset_id: record.assetId,
    asset_name: humanize(record.assetId),
    zone_id: record.zoneId,
    zone_name: humanize(record.zoneId),
    current_stage: stage,
    current_status: semantic.currentStatus ?? (capacityRiskKw > 0 ? 'BLOCKED' : 'GRAPH_ACTIVE'),
    hours_in_current_stage: semantic.hoursInCurrentStage ?? 0,
    needed_by_at: semantic.neededByAt ?? '',
    priority_level: priorityLevel,
    business_impact: semantic.businessImpact ?? (affectedGpuCount ? `${affectedGpuCount} GPUs affected` : 'Semantic graph finding'),
    asset_criticality_score: semantic.assetCriticalityScore ?? (affectedGpuCount ? 20 : 0),
    downtime_score: semantic.downtimeScore ?? 0,
    stage_delay_score: semantic.stageDelayScore ?? 0,
    infrastructure_zone_impact_score: semantic.infrastructureZoneImpactScore ?? (capacityRiskKw ? 20 : 0),
    needed_by_urgency_score: semantic.neededByUrgencyScore ?? 0,
    repeat_failure_score: semantic.repeatFailureScore ?? 0,
    spare_risk_score: semantic.spareRiskScore ?? (isVendorPartsEscalation(vendorStatus) ? 20 : 0),
    capacity_risk_score: semantic.capacityRiskScore ?? Math.min(30, capacityRiskKw / 30),
    redundancy_risk_score: semantic.redundancyRiskScore ?? (isRedundancyLost(redundancyState) ? 24 : 0),
    thermal_risk_score: semantic.thermalRiskScore ?? 0,
    vendor_eta_risk_score: semantic.vendorEtaRiskScore ?? (isVendorPartsEscalation(vendorStatus) ? 22 : 0),
    mitigation_credit_score: semantic.mitigationCreditScore ?? 0,
    total_priority_score: totalPriorityScore,
    recommended_action: detail?.recommendedAction ?? `Review semantic graph evidence for ${record.incidentId}`,
    reason_summary: `${record.incidentId} is linked to ${humanize(record.assetId)} in ${humanize(record.zoneId)} through canonical RDF source ${lastSegment(record.sourceRecordUri)}.`,
    redundancy_state: redundancyState,
    affected_gpu_count: affectedGpuCount,
    estimated_capacity_risk_kw: capacityRiskKw,
    mitigation_status: detail?.mitigationStatus ?? (capacityRiskKw > 0 ? 'RUNNING_DEGRADED' : null),
    vendor_status: vendorStatus,
    impact_confidence_status: trustIssueCount ? 'WARNING' : 'TRUSTED',
    impact_trust_issue_count: trustIssueCount,
    restore_readiness_status: restoreReadinessStatus,
    restore_readiness_summary: detail?.restoreReadinessSummary ?? null,
    dependency_roles: unique(dependencies.map((dependency) => dependency.dependencyRole)),
    dependency_path_ids: unique(dependencies.map((dependency) => dependency.pathId).filter(Boolean) as string[]),
  }
}

function applyDashboardFilters(rows: FollowUpItem[], filters: DashboardFilters): FollowUpItem[] {
  return rows.filter((row) => {
    if (filters.zone_id && row.zone_id !== filters.zone_id) return false
    if (filters.asset_id && row.asset_id !== filters.asset_id) return false
    if (filters.priority_level && row.priority_level !== filters.priority_level) return false
    if (filters.stage && row.current_stage !== canonicalStage(filters.stage)) return false
    if (filters.critical_asset_delayed && row.priority_level !== 'CRITICAL') return false
    if (filters.capacity_risk && row.estimated_capacity_risk_kw <= 0) return false
    if (filters.affected_gpu && row.affected_gpu_count <= 0) return false
    if (filters.evidence_review && row.impact_confidence_status === 'TRUSTED') return false
    if (filters.redundancy_lost && !isRedundancyLost(row.redundancy_state)) return false
    if (filters.vendor_parts_escalation && !isVendorPartsEscalation(row.vendor_status)) return false
    if (filters.restore_blocked && row.restore_readiness_status !== 'NOT_READY') return false
    if (filters.trust_review && row.impact_confidence_status === 'TRUSTED') return false
    if (filters.dependency_role && !row.dependency_roles.includes(filters.dependency_role)) return false
    if (filters.delayed_only && row.hours_in_current_stage <= 0 && row.estimated_capacity_risk_kw <= 0) return false
    return true
  })
}

function buildOntologyReviewQueue(records: SemanticOntologyReviewQueueRecord[]): OntologyReviewQueueItem[] {
  return records
    .map((record) => ({
      graph_uri: record.graphUri,
      queue_id: record.queueId,
      queue_kind: record.queueKind,
      review_action_id: record.reviewActionId,
      review_action_label: record.reviewActionLabel,
      review_status: record.reviewStatus,
      target_uri: record.targetUri,
      target_type: record.targetType,
      target_label: record.targetLabel,
      release_id: record.releaseId,
      source_graph_uri: record.sourceGraphUri ?? null,
      canonical_graph_uri: record.canonicalGraphUri ?? null,
      provenance_graph_uri: record.provenanceGraphUri ?? null,
      reasoning_audit_graph_uri: record.reasoningAuditGraphUri ?? null,
      reasoning_graph_uri: record.reasoningGraphUri ?? null,
      evidence_summary: record.evidenceSummary,
      action_status: 'DISABLED' as const,
      disabled_reason: record.disabledReason,
      incident_count: record.incidentCount,
      asset_count: record.assetCount,
      source_record_count: record.sourceRecordCount,
      activity_count: record.activityCount,
      generated_fact_count: record.generatedFactCount,
      priority_sort_order: record.prioritySortOrder,
    }))
    .sort((left, right) => left.priority_sort_order - right.priority_sort_order || left.queue_id.localeCompare(right.queue_id))
}

function buildOverview(record: SemanticDashboardOverviewRecord | undefined, followUps: FollowUpItem[]): Overview {
  const capacityRiskKw = record?.capacityRiskKw ?? followUps.reduce((total, row) => total + row.estimated_capacity_risk_kw, 0)
  const affectedGpuCount = record?.affectedGpuCount ?? followUps.reduce((total, row) => total + row.affected_gpu_count, 0)
  return {
    total_requests: record?.totalIncidents ?? followUps.length,
    open_requests: followUps.length,
    delayed_requests: followUps.filter((row) => row.estimated_capacity_risk_kw > 0 || row.hours_in_current_stage > 0).length,
    critical_asset_delayed: followUps.filter((row) => row.priority_level === 'CRITICAL').length,
    avg_downtime_hours: record?.avgDurationHours ?? 0,
    top_bottleneck_stage: topStage(followUps),
    spare_waiting_delay_hours: followUps.filter((row) => row.current_stage === 'RECOVERY').reduce((total, row) => total + row.hours_in_current_stage, 0),
    repeat_failure_asset_count: record?.repeatFailureAssetCount ?? 0,
    engineer_assignment_delay_hours: record?.engineerAssignmentDelayHours ?? 0,
    capacity_risk_kw: capacityRiskKw,
    affected_gpu_count: affectedGpuCount,
    redundancy_lost_incidents: followUps.filter((row) => isRedundancyLost(row.redundancy_state)).length,
    vendor_eta_missed_count: followUps.filter((row) => isVendorPartsEscalation(row.vendor_status)).length,
    latest_pipeline_run_status: 'SEMANTIC_GRAPH',
    data_quality_status: (record?.trustFindingCount ?? 0) > 0 ? 'Needs review' : 'Trusted',
  }
}

function buildImpactSummary(record: SemanticImpactSummaryRecord | undefined, followUps: FollowUpItem[]): ImpactSummary {
  const warningCount = followUps.filter((row) => row.impact_confidence_status !== 'TRUSTED').length
  return {
    incident_count: record?.incidentCount ?? followUps.length,
    capacity_risk_kw: record?.capacityRiskKw ?? followUps.reduce((total, row) => total + row.estimated_capacity_risk_kw, 0),
    affected_rack_count: record?.affectedRackCount ?? 0,
    affected_gpu_count: record?.affectedGpuCount ?? followUps.reduce((total, row) => total + row.affected_gpu_count, 0),
    redundancy_lost_incidents: followUps.filter((row) => isRedundancyLost(row.redundancy_state)).length,
    vendor_eta_missed_count: followUps.filter((row) => isVendorPartsEscalation(row.vendor_status)).length,
    mitigated_incidents: record?.mitigatedIncidentCount ?? 0,
    thermal_breach_minutes: record?.thermalBreachMinutes ?? 0,
    trusted_impact_count: followUps.length - warningCount,
    warning_impact_count: warningCount,
    unverified_impact_count: 0,
  }
}

function mapStageBottleneck(record: SemanticStageBottleneckRecord): StageBottleneck {
  return {
    stage: canonicalStage(record.stageLabel ?? record.stageUri),
    request_count: record.incidentCount,
    delayed_count: record.delayedCount ?? 0,
    delay_rate: record.incidentCount ? (record.delayedCount ?? 0) / record.incidentCount : 0,
    avg_duration_hours: record.avgDurationHours ?? 0,
    p90_duration_hours: record.p90DurationHours ?? 0,
    total_delay_hours: record.totalDelayHours ?? 0,
  }
}

function mapAssetDelaySummary(record: SemanticAssetDelaySummaryRecord): InfrastructureAssetDelay {
  return {
    asset_id: record.assetId,
    asset_name: humanize(record.assetId),
    zone_id: record.zoneId,
    zone_name: humanize(record.zoneId),
    request_count: record.incidentCount,
    delayed_request_count: record.delayedIncidentCount ?? (record.capacityRiskKw > 0 ? record.incidentCount : 0),
    repeat_failure_count: record.repeatFailureCount ?? 0,
    total_downtime_hours: record.totalDurationHours ?? 0,
    avg_repair_duration_hours: record.avgDurationHours ?? 0,
    top_failure_mode: record.topFailureMode ?? (record.impactObservationCount ? 'Semantic impact observation' : 'None'),
  }
}

function mapZoneDelaySummary(record: SemanticZoneDelaySummaryRecord): InfrastructureZoneDelay {
  return {
    zone_id: record.zoneId,
    zone_name: humanize(record.zoneId),
    open_request_count: record.incidentCount,
    delayed_request_count: record.delayedIncidentCount ?? (record.capacityRiskKw > 0 ? record.incidentCount : 0),
    critical_asset_delayed_count: record.criticalIncidentCount ?? (record.affectedGpuCount > 0 ? record.incidentCount : 0),
    total_downtime_hours: record.totalDurationHours ?? 0,
    top_bottleneck_stage: canonicalStage(record.topBottleneckStage ?? 'SEMANTIC_GRAPH'),
  }
}

function mapSpareWaitSummary(record: SemanticSpareWaitSummaryRecord): SpareWaiting {
  return {
    spare_id: lastSegment(record.stageUri),
    spare_name: humanize(record.stageLabel ?? record.stageUri),
    spare_category: 'Semantic recovery blocker',
    waiting_request_count: record.incidentCount,
    total_wait_hours: record.totalWaitHours ?? 0,
    avg_wait_hours: record.avgWaitHours ?? 0,
    critical_spare: record.recoveryBlockerCount > 0,
    stock_status: record.stockStatus ?? (record.recoveryBlockerCount > 0 ? 'REVIEW' : 'OK'),
  }
}

function mapTrustFinding(record: SemanticTrustFindingRecord): DataQualityCheck {
  return {
    check_result_id: record.trustFindingId ?? record.trustFindingUri,
    pipeline_run_id: record.activityUri ?? 'semantic-service',
    check_name: 'Semantic evidence issue',
    graph_scope: 'reasoning graph',
    severity: record.severity ?? 'WARNING',
    status: record.status ?? 'FAILED',
    failed_row_count: 1,
    sample_failed_keys: [record.sourceFactUri],
    message: record.summary,
    created_at: record.createdAt ?? '',
  }
}

function buildTopologyDependencies(
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
    dependency_type: record.pathId ?? 'SEMANTIC_DEPENDENCY',
    dependency_role: record.dependencyRole,
    impact_scope: record.impactScope ?? 'unknown',
    dependent_active_incident_count: activeByAsset.get(record.dependentAssetId) ?? 0,
    dependency_active_incident_count: activeByAsset.get(record.dependencyAssetId) ?? 0,
  }))
}

function buildRequestDetail(
  request: FollowUpItem,
  detail: SemanticFollowUpDetailRecord | undefined,
  evidence: SemanticIncidentEvidenceRecord[],
  workflowTimeline: SemanticIncidentTimelineRecord[],
  actionAvailabilityRecords: SemanticActionAvailabilityRecord[],
  actionAuditRecords: SemanticActionAuditHistoryRecord[],
  actionNotificationRecords: SemanticActionNotificationQueueRecord[],
  actionReviewRecords: SemanticActionReviewQueueRecord[],
  actionTransitionRecords: SemanticActionTransitionHistoryRecord[],
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
      event_status: record.telemetryStatus ?? record.validationStatus ?? record.workOrderStatus ?? record.confidenceState ?? 'ASSERTED',
      occurred_at: record.evidenceTimestamp ?? '',
      actor_type: record.validatorId ?? record.assignedTeam ?? 'semantic-service',
      reason_code: record.trustFindingUri ? 'TRUST_FINDING' : null,
      message: record.trustSummary ?? record.failureReason ?? null,
      source_record_uri: record.sourceRecordUri ?? null,
    }))
    .sort((left, right) => left.occurred_at.localeCompare(right.occurred_at))
  const workflowEvents = workflowTimeline.map((record) => ({
    event_id: record.eventId ?? record.eventUri,
    stage: canonicalStage(record.stageLabel ?? record.stageUri),
    event_type: 'WORKFLOW_STAGE',
    event_status: record.eventStatus ?? 'ASSERTED',
    occurred_at: record.enteredAt ?? '',
    actor_type: 'semantic-service',
    reason_code: null,
    message: record.delayHours && record.delayHours > 0 ? `${record.delayHours}h over threshold` : null,
    source_record_uri: record.sourceRecordUri ?? null,
  }))
  const timeline = [...workflowEvents, ...evidenceTimeline].sort((left, right) => left.occurred_at.localeCompare(right.occurred_at))
  const workOrders = workOrderEvidence.length
    ? workOrderEvidence.map((record) => ({
        work_order_id: record.workOrderId ?? record.evidenceUri ?? `${request.incident_id}:semantic-work-order`,
        assigned_team: record.assignedTeam ?? 'Semantic Operations',
        assigned_engineer_id: record.assignedEngineerId ?? null,
        work_order_status: record.workOrderStatus ?? record.confidenceState ?? 'REVIEW',
        planned_start_at: record.plannedStartAt ?? null,
        actual_start_at: record.actualStartAt ?? null,
        actual_completed_at: record.actualCompletedAt ?? null,
        required_spare_id: record.requiredSpareId ?? null,
        required_spare_name: record.requiredSpareName ?? null,
        stock_status: record.stockStatus ?? null,
      }))
    : [
        {
          work_order_id: detail?.followUpDecisionUri ?? `${request.incident_id}:semantic-finding`,
          assigned_team: 'Semantic Operations',
          assigned_engineer_id: null,
          work_order_status: detail?.recommendedAction ? 'RECOMMENDED' : 'REVIEW',
          planned_start_at: null,
          actual_start_at: null,
          actual_completed_at: null,
          required_spare_id: null,
          required_spare_name: null,
          stock_status: null,
        },
      ]
  return {
    request,
    stage_lead_times: workflowTimeline.length
      ? workflowTimeline.map((record) => ({
          stage: canonicalStage(record.stageLabel ?? record.stageUri),
          entered_at: record.enteredAt ?? '',
          exited_at: record.exitedAt ?? null,
          duration_hours: record.durationHours ?? 0,
          threshold_hours: record.thresholdHours ?? 0,
          is_bottleneck: (record.delayHours ?? 0) > 0,
          delay_hours: record.delayHours ?? 0,
        }))
      : [
          {
            stage: request.current_stage,
            entered_at: '',
            exited_at: null,
            duration_hours: request.hours_in_current_stage,
            threshold_hours: 0,
            is_bottleneck: request.estimated_capacity_risk_kw > 0 || request.impact_trust_issue_count > 0,
            delay_hours: request.hours_in_current_stage,
          },
        ],
    timeline,
    work_orders: workOrders,
    validation_results: validationEvidence.map((record) => ({
      validation_id: record.validationId ?? record.evidenceUri ?? `${request.incident_id}:semantic-validation`,
      validation_status: record.validationStatus ?? record.confidenceState ?? 'REVIEW',
      validator_id: record.validatorId ?? null,
      validation_started_at: record.validationStartedAt ?? record.evidenceTimestamp ?? null,
      validation_completed_at: record.validationCompletedAt ?? null,
      failure_reason: record.failureReason ?? null,
    })),
    telemetry_alerts: telemetryEvidence
      .filter((record) => record.telemetryAlertId || record.alertType || record.alertSeverity || record.alertTriggeredAt)
      .map((record) => ({
        telemetry_alert_id: record.telemetryAlertId ?? record.evidenceUri ?? `${request.incident_id}:semantic-telemetry-alert`,
        asset_id: request.asset_id,
        alert_type: record.alertType ?? record.metricName ?? 'SEMANTIC_TELEMETRY',
        severity: record.alertSeverity ?? record.telemetryStatus ?? record.confidenceState ?? 'INFO',
        triggered_at: record.alertTriggeredAt ?? record.evidenceTimestamp ?? '',
        resolved_at: record.alertResolvedAt ?? null,
      })),
    impact_snapshot: {
      impact_snapshot_id: detail?.impactUri ?? `${request.incident_id}:semantic-impact`,
      incident_id: request.incident_id,
      asset_id: request.asset_id,
      zone_id: request.zone_id,
      snapshot_at: '',
      redundancy_state: detail?.redundancyState ?? request.redundancy_state ?? 'Unknown',
      affected_rack_count: detail?.affectedRackCount ?? 0,
      affected_gpu_count: request.affected_gpu_count,
      estimated_capacity_risk_kw: request.estimated_capacity_risk_kw,
      estimated_gpu_capacity_risk_pct: detail?.estimatedGpuCapacityRiskPct ?? (request.affected_gpu_count > 0 ? 100 : 0),
      thermal_breach_minutes: detail?.thermalBreachMinutes ?? 0,
      power_redundancy_lost: detail?.powerRedundancyLost ?? isRedundancyLost(request.redundancy_state),
      cooling_redundancy_lost: detail?.coolingRedundancyLost ?? false,
      mitigation_status: detail?.mitigationStatus ?? request.mitigation_status ?? 'UNKNOWN',
      vendor_eta_at: detail?.vendorEtaAt ?? null,
      vendor_status: detail?.vendorStatus ?? request.vendor_status ?? 'UNKNOWN',
      source_system: 'ontology-native semantic graph',
      telemetry_readings: telemetryEvidence.map((record) => ({
        metric: record.metricName ?? lastSegment(record.evidenceUri ?? 'semantic_metric'),
        value: record.metricValue ?? 0,
        unit: record.metricUnit ?? '',
        status: record.telemetryStatus ?? record.confidenceState ?? 'ASSERTED',
      })),
    },
    quality_flags: [],
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

function mapAiProposal(record: SemanticAiProposalRecord): AiProposalItem {
  return {
    graph_uri: record.graphUri,
    ai_audit_release_id: record.aiAuditReleaseId,
    proposal_uri: record.proposalUri,
    proposal_id: record.proposalId,
    proposal_type: record.proposalType,
    proposal_status: record.proposalStatus,
    review_status: record.reviewStatus,
    disabled_reason: record.disabledReason,
    summary: record.summary,
    rationale: record.rationale,
    confidence_score: record.confidenceScore,
    risk_level: record.riskLevel,
    model_id: record.modelId,
    prompt_id: record.promptId,
    prompt_hash: record.promptHash,
    actor_id: record.actorId,
    generated_at: record.generatedAt,
    batch_uri: record.batchUri,
    batch_id: record.batchId,
    validation_report_uri: record.validationReportUri,
    validation_status: record.validationStatus,
    validation_summary: record.validationSummary,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
    target_object_uri: record.targetObjectUri,
    source_record_uri: record.sourceRecordUri,
    supporting_evidence_uri: record.supportingEvidenceUri,
  }
}

function mapDynamicPlayback(record: SemanticDynamicPlaybackRecord): DynamicPlaybackItem {
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

function mapActionReviewQueue(record: SemanticActionReviewQueueRecord): OntologyActionReviewQueueItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    notification_uri: record.notificationUri,
    notification_id: record.notificationId,
    execution_uri: record.executionUri,
    execution_id: record.executionId,
    request_uri: record.requestUri,
    request_id: record.requestId,
    action_type_uri: record.actionTypeUri,
    action_type_id: record.actionTypeId,
    actor_id: record.actorId,
    action_reason: record.actionReason,
    current_state: record.currentState,
    state_generated_at: record.stateGeneratedAt,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
    source_record_uri: record.sourceRecordUri ?? null,
  }
}

function mapActionTransitionHistory(record: SemanticActionTransitionHistoryRecord): OntologyActionTransitionHistoryItem {
  return {
    graph_uri: record.graphUri,
    action_audit_release_id: record.actionAuditReleaseId,
    transition_uri: record.transitionUri,
    transition_id: record.transitionId,
    execution_uri: record.executionUri,
    execution_id: record.executionId,
    request_uri: record.requestUri,
    request_id: record.requestId,
    action_type_uri: record.actionTypeUri,
    action_type_id: record.actionTypeId,
    actor_id: record.actorId,
    transition_reason: record.transitionReason,
    from_state: record.fromState ?? null,
    to_state: record.toState,
    generated_at: record.generatedAt,
    incident_uri: record.incidentUri,
    incident_id: record.incidentId,
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
      status: 'DISABLED',
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
    resource_uri: detail?.incidentUri ?? `urn:dcai:incident:${request.incident_id}`,
    detail: `${request.asset_name} in ${request.zone_name}`,
  })
  pushTrace(trace, {
    step: 'Impact observation',
    label: 'Operational impact',
    resource_uri: detail?.impactUri ?? '',
    detail: `${request.affected_gpu_count} GPUs, ${request.estimated_capacity_risk_kw.toFixed(0)} kW at risk`,
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

function pushTrace(trace: ProvenanceTraceItem[], item: ProvenanceTraceItem) {
  if (!item.resource_uri || trace.some((existing) => existing.resource_uri === item.resource_uri)) {
    return
  }
  trace.push(item)
}

function restoreReadinessStatusFor(summary?: string): string {
  if (!summary) return 'UNKNOWN'
  const normalized = summary.toLowerCase()
  if (normalized.includes('not ready') || normalized.includes('blocked')) return 'NOT_READY'
  if (normalized.includes('ready for review') || normalized.includes('ready')) return 'READY'
  return 'REVIEW'
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

function mapSemanticValidation(records: SemanticValidationSummaryRecord[]): SemanticValidation {
  const issueCount = records.reduce((total, record) => {
    return total +
      Math.max(0, record.incidentCount - record.incidentWithProvenanceCount) +
      Math.max(0, record.assetCount - record.assetWithProvenanceCount)
  }, 0)
  return {
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

function mapSemanticIncidentEvidence(
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

function mapSemanticDependencyImpact(
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
        dependency_role: record.dependencyRole ?? 'dependency',
      })),
    inferred_downstream_assets: unique(records.map((record) => record.dependencyAssetId).filter(Boolean) as string[]),
  }
}

function mapSemanticBlastRadius(
  assetId: string,
  records: SemanticBlastRadiusRecord[],
): SemanticBlastRadius {
  return {
    asset_id: assetId,
    inferred_downstream_assets: unique(records.map((record) => record.downstreamAssetId).filter(Boolean) as string[]),
    affected_incident_count: unique(records.map((record) => record.incidentId).filter(Boolean) as string[]).length,
    affected_incidents: records
      .filter((record) => record.incidentId)
      .map((record) => ({
        incident_id: record.incidentId as string,
        asset_id: record.downstreamAssetId ?? assetId,
        title: record.findingSummary ?? 'Semantic blast-radius finding',
        stage: 'SEMANTIC_GRAPH',
      })),
  }
}

function topStage(rows: FollowUpItem[]): string | null {
  const [stage] = [...rows.reduce<Map<string, number>>((summary, row) => {
    summary.set(row.current_stage, (summary.get(row.current_stage) ?? 0) + 1)
    return summary
  }, new Map()).entries()].sort(([, left], [, right]) => right - left)[0] ?? []
  return stage ?? null
}

function priorityFor(capacityRiskKw: number, affectedGpuCount: number, trustIssueCount: number): string {
  if (capacityRiskKw >= 500 || affectedGpuCount >= 256 || trustIssueCount > 0) return 'CRITICAL'
  if (capacityRiskKw > 0 || affectedGpuCount > 0) return 'HIGH'
  return 'MEDIUM'
}

export function normalizeRedundancyState(value?: string | null): string | null {
  if (!value) return null
  const normalized = canonicalStage(value)
  if (normalized === 'N_PLUS_0' || normalized.includes('PLUS_0')) return 'REDUNDANCY_LOST'
  if (normalized === 'N_PLUS_1' || normalized.includes('PLUS_1')) return 'REDUNDANCY_AVAILABLE'
  return normalized
}

export function isRedundancyLost(value?: string | null): boolean {
  return normalizeRedundancyState(value) === 'REDUNDANCY_LOST'
}

export function normalizeVendorStatus(value?: string | null): string | null {
  if (!value) return null
  return canonicalStage(value)
}

export function isVendorPartsEscalation(value?: string | null): boolean {
  const normalized = normalizeVendorStatus(value)
  return normalized === 'VENDOR_ENGAGED' || normalized === 'PARTS_REVIEW'
}

function canonicalStage(value: string): string {
  const normalized = lastSegment(value).trim()
  if (!normalized) return 'SEMANTIC_GRAPH'
  return normalized
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/[\s-]+/g, '_')
    .replace(/[^A-Za-z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .toUpperCase()
}

function statusForAsset(assetId: string, activeByAsset: Map<string, number>): string {
  return (activeByAsset.get(assetId) ?? 0) > 0 ? 'Degraded' : 'Running'
}

function humanize(value: string): string {
  return lastSegment(value)
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (character) => character.toUpperCase())
}

function lastSegment(value: string): string {
  return value.split(/[/#:]/).filter(Boolean).at(-1) ?? value
}

function unique(values: string[]): string[] {
  return [...new Set(values)]
}
