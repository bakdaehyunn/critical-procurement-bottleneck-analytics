import type { OntologyActionLifecycleState } from '../../api'

const transitions: Partial<Record<OntologyActionLifecycleState, OntologyActionLifecycleState[]>> = {
  REQUESTED: ['VALIDATED'],
  VALIDATED: ['QUEUED'],
  QUEUED: ['IN_REVIEW', 'REJECTED'],
  IN_REVIEW: ['APPROVED', 'REJECTED'],
  APPROVED: ['CLOSED'],
  REJECTED: ['CLOSED'],
}

export function availableActionTransitions(state: OntologyActionLifecycleState): OntologyActionLifecycleState[] {
  return transitions[state] ?? []
}

export function isAiProposalActionable(reviewStatus: string): boolean {
  return ['PENDING', 'PENDING_HUMAN_REVIEW', 'NEEDS_REVIEW'].includes(reviewStatus.toUpperCase())
}
