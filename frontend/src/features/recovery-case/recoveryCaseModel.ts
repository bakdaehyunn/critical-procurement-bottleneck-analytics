import type { FollowUpItem } from '../recovery-queue'
import type {
  AiProposalItem,
  OntologyActionReviewQueueItem,
  OntologyActionTransitionHistoryItem,
} from '../review-inbox'

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
  finding_uri: string | null
  finding_summary: string | null
  source_record_uri: string | null
}

export type SemanticReasoningFinding = {
  finding_uri: string
  summary: string
  source_record_uri: string | null
}

export type SemanticDependencyImpact = {
  asset_id: string
  direct_dependency_count: number
  direct_dependencies: SemanticDependencyEdge[]
  inferred_downstream_assets: string[]
  reasoning_findings: SemanticReasoningFinding[]
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
  reasoning_findings: SemanticReasoningFinding[]
  affected_incidents: {
    incident_id: string
    asset_id: string
    title: string
    stage: string
  }[]
}

export type SemanticValidation = {
  status: 'CONFORMS' | 'NON_CONFORMING' | 'UNKNOWN'
  conforms: boolean | null
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
  duration_hours: number | null
  threshold_hours: number | null
  is_bottleneck: boolean | null
  delay_hours: number | null
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

export type OntologyEvidenceFact = {
  kind: 'direct-fact' | 'inferred-fact' | 'provenance' | 'action-eligibility'
  label: string
  value: string
  detail: string
  resource_uri: string | null
  confidence: 'trusted' | 'review' | 'blocked'
}

export type OntologyEvidenceExplanation = {
  question: string
  answer: string
  direct_facts: OntologyEvidenceFact[]
  inferred_facts: OntologyEvidenceFact[]
  provenance_links: OntologyEvidenceFact[]
  action_eligibility: OntologyEvidenceFact[]
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
  status: 'AVAILABLE_FOR_LOCAL_AUDIT' | 'DISABLED'
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
    affected_rack_count: number | null
    affected_gpu_count: number | null
    estimated_capacity_risk_kw: number | null
    estimated_gpu_capacity_risk_pct: number | null
    thermal_breach_minutes: number | null
    power_redundancy_lost: boolean | null
    cooling_redundancy_lost: boolean | null
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
  ontology_evidence: OntologyEvidenceExplanation
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

export type RecoveryCaseTimelineResource = {
  kind: 'timeline'
  detail: Pick<RequestDetail, 'stage_lead_times' | 'timeline' | 'provenance_trace' | 'ontology_evidence'>
}

export type RecoveryCaseEvidenceResource = {
  kind: 'evidence'
  detail: Pick<RequestDetail, 'work_orders' | 'validation_results' | 'telemetry_alerts' | 'impact_snapshot' | 'quality_flags' | 'impact_trust_flags' | 'provenance_trace' | 'ontology_evidence'>
  incidentEvidence: RequestSemanticContext['incidentEvidence']
}

export type RecoveryCaseImpactResource = {
  kind: 'impact'
  semantic: Pick<RequestSemanticContext, 'validation' | 'dependencyImpact' | 'blastRadius'>
}

export type RecoveryCaseActionResource = {
  kind: 'actions'
  detail: Pick<RequestDetail, 'ontology_actions' | 'action_audit_history' | 'action_notifications' | 'action_review_queue' | 'action_transition_history' | 'action_dispatch_queue' | 'ontology_evidence'>
}

export type RecoveryCaseAiResource = {
  kind: 'ai'
  detail: Pick<RequestDetail, 'ai_proposals'>
}

export type RecoveryCasePlaybackResource = {
  kind: 'playback'
  detail: Pick<RequestDetail, 'dynamic_event_timeline' | 'dynamic_state_changes' | 'dynamic_reasoning_changes' | 'dynamic_action_lifecycle'>
}

export type RecoveryCaseTopologyResource = {
  kind: 'topology'
  dependencies: InfrastructureDependency[]
}

export type RecoveryCaseResource =
  | RecoveryCaseTimelineResource
  | RecoveryCaseEvidenceResource
  | RecoveryCaseImpactResource
  | RecoveryCaseActionResource
  | RecoveryCaseAiResource
  | RecoveryCasePlaybackResource
  | RecoveryCaseTopologyResource

export type RecoveryCaseResourceOutcome =
  | { status: 'fulfilled'; resource: RecoveryCaseResource }
  | { status: 'rejected'; kind: RecoveryCaseResource['kind']; reason: unknown }

export type RecoveryCaseSession = {
  core: RequestDetail
  loadResources: () => Promise<RecoveryCaseResourceOutcome[]>
}
