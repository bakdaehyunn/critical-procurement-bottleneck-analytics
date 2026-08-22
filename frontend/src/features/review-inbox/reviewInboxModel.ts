import type { OntologyActionLifecycleState } from '../../ontologyActionApi'

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
  action_status: string
  disabled_reason: string
  incident_count: number
  asset_count: number
  source_record_count: number
  activity_count: number
  generated_fact_count: number
  priority_sort_order: number
}
