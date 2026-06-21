import {
  SEMANTIC_ACTION_AUDIT_RELEASE_ID,
  SEMANTIC_API_BASE_URL,
  SEMANTIC_REASONING_RUN_ID,
  SEMANTIC_SOURCE_RELEASE_ID,
} from './semanticRuntimeConfig'

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
