import { describe, expect, it } from 'vitest'
import type { OntologyActionAffordance } from './recoveryCaseModel'
import { actionAvailability, buildActionSubmission } from './actionUtils'

function action(actionId: string, role: string): OntologyActionAffordance {
  return {
    action_id: actionId,
    label: actionId,
    description: 'Test action',
    status: 'DISABLED',
    incident_uri: 'urn:dcai:incident:1',
    incident_id: 'INC-1',
    source_record_uri: 'urn:dcai:source:1',
    ui_placement: ['summary'],
    target_objects: [{ role, label: role, resource_uri: `urn:dcai:${role}:1` }],
    required_parameters: [],
    preconditions: [],
    provenance_requirements: [],
    disabled_reasons: [],
  }
}

describe('governed recovery action inputs', () => {
  it('requires accountable actor and reason values', () => {
    const affordance = action('AcknowledgeRestoreBlocker', 'RestoreReadinessFinding')
    expect(buildActionSubmission(affordance, { actorId: '', actionReason: '' })).toBeNull()
  })

  it('uses editable assignment values in the request payload', () => {
    const affordance = action('AssignEvidenceReview', 'TrustFinding')
    const submission = buildActionSubmission(affordance, {
      actorId: 'operator-42',
      actionReason: 'Independent evidence review is required.',
      assignedTeam: 'FACILITIES-QA',
      assigneeId: 'reviewer-7',
    })
    expect(actionAvailability(affordance).available).toBe(true)
    expect(submission).toMatchObject({ actor_id: 'operator-42', assigned_team: 'FACILITIES-QA', assignee_id: 'reviewer-7' })
  })

  it('requires a source-backed validation review summary', () => {
    const affordance = action('RecordValidationReview', 'ValidationEvidence')
    expect(buildActionSubmission(affordance, {
      actorId: 'operator-42',
      actionReason: 'Record the outcome.',
      reviewedStatus: 'PASSED',
      reviewSummary: '',
    })).toBeNull()
  })
})
