import type { OntologyActionAffordance, OntologyActionSubmission } from '../../api'

const supportedActions = new Set(['AcknowledgeRestoreBlocker', 'AssignEvidenceReview', 'RecordValidationReview'])

export type ActionInputValues = {
  actorId: string
  actionReason: string
  assignedTeam?: string
  assigneeId?: string
  reviewedStatus?: string
  reviewSummary?: string
}

function target(action: OntologyActionAffordance, role: string) {
  return action.target_objects.find((item) => item.role === role && item.resource_uri.startsWith('urn:dcai:'))
}

export function actionAvailability(action: OntologyActionAffordance) {
  if (!supportedActions.has(action.action_id)) return { available: false, reason: 'This action is informational in the current local workflow.' }
  if (!action.incident_uri || !action.source_record_uri) return { available: false, reason: 'Required incident provenance is missing.' }
  if (action.action_id === 'AcknowledgeRestoreBlocker' && !target(action, 'RestoreReadinessFinding')) return { available: false, reason: 'Restore-readiness evidence is missing.' }
  if (action.action_id === 'AssignEvidenceReview' && !target(action, 'TrustFinding')) return { available: false, reason: 'Trust-finding evidence is missing.' }
  if (action.action_id === 'RecordValidationReview' && !target(action, 'ValidationEvidence')) return { available: false, reason: 'Validation evidence is missing.' }
  return { available: true, reason: 'Creates an audited local request only; no operational system is mutated.' }
}

export function buildActionSubmission(action: OntologyActionAffordance, values: ActionInputValues): OntologyActionSubmission | null {
  if (!values.actorId.trim() || !values.actionReason.trim()) return null
  const base = {
    action_id: action.action_id,
    actor_id: values.actorId.trim(),
    action_reason: values.actionReason.trim(),
    incident_uri: action.incident_uri,
    source_record_uri: action.source_record_uri,
  }
  if (action.action_id === 'AcknowledgeRestoreBlocker') {
    const readiness = target(action, 'RestoreReadinessFinding')
    if (!readiness) return null
    return { ...base, restore_readiness_finding_uri: readiness.resource_uri, recovery_blocker_uri: target(action, 'RecoveryBlocker')?.resource_uri }
  }
  if (action.action_id === 'AssignEvidenceReview') {
    const trust = target(action, 'TrustFinding')
    if (!trust) return null
    if (!values.assignedTeam?.trim()) return null
    return { ...base, trust_finding_uri: trust.resource_uri, assigned_team: values.assignedTeam.trim(), assignee_id: values.assigneeId?.trim() || undefined }
  }
  if (action.action_id === 'RecordValidationReview') {
    const validation = target(action, 'ValidationEvidence')
    if (!validation) return null
    if (!values.reviewedStatus?.trim() || !values.reviewSummary?.trim()) return null
    return {
      ...base,
      validation_evidence_uri: validation.resource_uri,
      reviewed_status: values.reviewedStatus.trim(),
      review_summary: values.reviewSummary.trim(),
      supporting_evidence_uri: validation.resource_uri,
    }
  }
  return null
}
