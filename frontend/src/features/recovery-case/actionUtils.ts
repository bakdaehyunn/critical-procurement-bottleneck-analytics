import type { OntologyActionAffordance, OntologyActionSubmission } from '../../api'

const supportedActions = new Set(['AcknowledgeRestoreBlocker', 'AssignEvidenceReview', 'RecordValidationReview'])

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

export function buildActionSubmission(action: OntologyActionAffordance): OntologyActionSubmission | null {
  const base = {
    action_id: action.action_id,
    actor_id: 'operator-local-reviewer',
    action_reason: action.action_id === 'AcknowledgeRestoreBlocker'
      ? 'Operator reviewed the restore-readiness blocker for local follow-up.'
      : action.action_id === 'AssignEvidenceReview'
        ? 'Assign evidence trust finding to local validation review.'
        : 'Record local review of validation evidence without changing canonical facts.',
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
    return { ...base, trust_finding_uri: trust.resource_uri, assigned_team: 'OPS_VALIDATION' }
  }
  if (action.action_id === 'RecordValidationReview') {
    const validation = target(action, 'ValidationEvidence')
    if (!validation) return null
    return {
      ...base,
      validation_evidence_uri: validation.resource_uri,
      reviewed_status: 'NEEDS_REVIEW',
      review_summary: 'Validation evidence requires follow-up before the restore decision.',
      supporting_evidence_uri: validation.resource_uri,
    }
  }
  return null
}
