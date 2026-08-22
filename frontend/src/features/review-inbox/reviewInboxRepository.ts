import type { OntologyActionLifecycleState } from '../../ontologyActionApi'
import { semanticQueryCatalog } from '../../semanticQueryCatalog'
import { postSemanticQueryPage } from '../../semanticQueryClient'
import type { PagedResult } from '../../shared/pagination'
import type { AiProposalItem, OntologyActionReviewQueueItem, OntologyActionTransitionHistoryItem, OntologyReviewQueueItem } from './reviewInboxModel'

export type SemanticActionReviewQueueRecord = {
  graphUri: string; actionAuditReleaseId: string; notificationUri: string; notificationId: string
  executionUri: string; executionId: string; requestUri: string; requestId: string
  actionTypeUri: string; actionTypeId: string; actorId: string; actionReason: string
  currentState: OntologyActionLifecycleState; stateGeneratedAt: string; incidentUri: string; incidentId: string; sourceRecordUri?: string
}

export type SemanticActionTransitionHistoryRecord = {
  graphUri: string; actionAuditReleaseId: string; transitionUri: string; transitionId: string
  executionUri: string; executionId: string; requestUri: string; requestId: string
  actionTypeUri: string; actionTypeId: string; actorId: string; transitionReason: string
  fromState?: OntologyActionLifecycleState; toState: OntologyActionLifecycleState
  generatedAt: string; incidentUri: string; incidentId: string
}

export type SemanticAiProposalRecord = {
  graphUri: string; aiAuditReleaseId: string; proposalUri: string; proposalId: string
  proposalType: string; proposalStatus: string; reviewStatus: string; disabledReason: string
  summary: string; rationale: string; confidenceScore: number; riskLevel: string
  modelId: string; promptId: string; promptHash: string; actorId: string; generatedAt: string
  batchUri: string; batchId: string; validationReportUri: string; validationStatus: string; validationSummary: string
  incidentUri: string; incidentId: string; targetObjectUri: string; sourceRecordUri: string; supportingEvidenceUri: string
}

type SemanticOntologyReviewQueueRecord = {
  graphUri: string; queueId: string; queueKind: string; reviewActionId: string; reviewActionLabel: string
  reviewStatus: string; targetUri: string; targetType: string; targetLabel: string; releaseId: string
  sourceGraphUri?: string; canonicalGraphUri?: string; provenanceGraphUri?: string
  reasoningAuditGraphUri?: string; reasoningGraphUri?: string; evidenceSummary: string
  actionStatus: string; disabledReason: string; incidentCount: number; assetCount: number
  sourceRecordCount: number; activityCount: number; generatedFactCount: number; prioritySortOrder: number
}

export async function fetchActionReviewQueuePage(page: number, pageSize: number): Promise<PagedResult<OntologyActionReviewQueueItem>> {
  const result = await postSemanticQueryPage<SemanticActionReviewQueueRecord>(semanticQueryCatalog.actionReviewQueueByIncident, page, pageSize)
  return { records: result.records.map(mapActionReviewQueue), page_info: result.pageInfo }
}

export async function fetchAiProposalReviewQueuePage(page: number, pageSize: number): Promise<PagedResult<AiProposalItem>> {
  const result = await postSemanticQueryPage<SemanticAiProposalRecord>(semanticQueryCatalog.aiProposalReviewQueue, page, pageSize)
  return { records: result.records.map(mapAiProposal), page_info: result.pageInfo }
}

export async function fetchOntologyReviewQueuePage(kind: 'promotion' | 'reasoning', page: number, pageSize: number): Promise<PagedResult<OntologyReviewQueueItem>> {
  const queryId = kind === 'promotion' ? semanticQueryCatalog.promotionReviewQueue : semanticQueryCatalog.reasoningReviewQueue
  const result = await postSemanticQueryPage<SemanticOntologyReviewQueueRecord>(queryId, page, pageSize)
  return { records: mapOntologyReviewQueue(result.records), page_info: result.pageInfo }
}

export function mapActionReviewQueue(record: SemanticActionReviewQueueRecord): OntologyActionReviewQueueItem {
  return {
    graph_uri: record.graphUri, action_audit_release_id: record.actionAuditReleaseId,
    notification_uri: record.notificationUri, notification_id: record.notificationId,
    execution_uri: record.executionUri, execution_id: record.executionId,
    request_uri: record.requestUri, request_id: record.requestId,
    action_type_uri: record.actionTypeUri, action_type_id: record.actionTypeId,
    actor_id: record.actorId, action_reason: record.actionReason, current_state: record.currentState,
    state_generated_at: record.stateGeneratedAt, incident_uri: record.incidentUri,
    incident_id: record.incidentId, source_record_uri: record.sourceRecordUri ?? null,
  }
}

export function mapActionTransitionHistory(record: SemanticActionTransitionHistoryRecord): OntologyActionTransitionHistoryItem {
  return {
    graph_uri: record.graphUri, action_audit_release_id: record.actionAuditReleaseId,
    transition_uri: record.transitionUri, transition_id: record.transitionId,
    execution_uri: record.executionUri, execution_id: record.executionId,
    request_uri: record.requestUri, request_id: record.requestId,
    action_type_uri: record.actionTypeUri, action_type_id: record.actionTypeId,
    actor_id: record.actorId, transition_reason: record.transitionReason, from_state: record.fromState ?? null,
    to_state: record.toState, generated_at: record.generatedAt, incident_uri: record.incidentUri, incident_id: record.incidentId,
  }
}

export function mapAiProposal(record: SemanticAiProposalRecord): AiProposalItem {
  return {
    graph_uri: record.graphUri, ai_audit_release_id: record.aiAuditReleaseId,
    proposal_uri: record.proposalUri, proposal_id: record.proposalId, proposal_type: record.proposalType,
    proposal_status: record.proposalStatus, review_status: record.reviewStatus, disabled_reason: record.disabledReason,
    summary: record.summary, rationale: record.rationale, confidence_score: record.confidenceScore,
    risk_level: record.riskLevel, model_id: record.modelId, prompt_id: record.promptId, prompt_hash: record.promptHash,
    actor_id: record.actorId, generated_at: record.generatedAt, batch_uri: record.batchUri, batch_id: record.batchId,
    validation_report_uri: record.validationReportUri, validation_status: record.validationStatus,
    validation_summary: record.validationSummary, incident_uri: record.incidentUri, incident_id: record.incidentId,
    target_object_uri: record.targetObjectUri, source_record_uri: record.sourceRecordUri,
    supporting_evidence_uri: record.supportingEvidenceUri,
  }
}

function mapOntologyReviewQueue(records: SemanticOntologyReviewQueueRecord[]): OntologyReviewQueueItem[] {
  return records.map((record) => ({
    graph_uri: record.graphUri, queue_id: record.queueId, queue_kind: record.queueKind,
    review_action_id: record.reviewActionId, review_action_label: record.reviewActionLabel,
    review_status: record.reviewStatus, target_uri: record.targetUri, target_type: record.targetType,
    target_label: record.targetLabel, release_id: record.releaseId, source_graph_uri: record.sourceGraphUri ?? null,
    canonical_graph_uri: record.canonicalGraphUri ?? null, provenance_graph_uri: record.provenanceGraphUri ?? null,
    reasoning_audit_graph_uri: record.reasoningAuditGraphUri ?? null, reasoning_graph_uri: record.reasoningGraphUri ?? null,
    evidence_summary: record.evidenceSummary, action_status: record.actionStatus, disabled_reason: record.disabledReason,
    incident_count: record.incidentCount, asset_count: record.assetCount, source_record_count: record.sourceRecordCount,
    activity_count: record.activityCount, generated_fact_count: record.generatedFactCount, priority_sort_order: record.prioritySortOrder,
  })).sort((left, right) => left.priority_sort_order - right.priority_sort_order || left.queue_id.localeCompare(right.queue_id))
}
