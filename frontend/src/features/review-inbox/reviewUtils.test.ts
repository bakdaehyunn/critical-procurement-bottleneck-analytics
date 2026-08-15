import { describe, expect, it } from 'vitest'
import { availableActionTransitions, isAiProposalActionable } from './reviewUtils'

describe('availableActionTransitions', () => {
  it('exposes only valid review branches from a queued action', () => {
    expect(availableActionTransitions('QUEUED')).toEqual(['IN_REVIEW', 'REJECTED'])
  })

  it('closes approved or rejected actions and leaves closed actions inert', () => {
    expect(availableActionTransitions('APPROVED')).toEqual(['CLOSED'])
    expect(availableActionTransitions('REJECTED')).toEqual(['CLOSED'])
    expect(availableActionTransitions('CLOSED')).toEqual([])
  })

  it('recognizes the service pending-human-review vocabulary', () => {
    expect(isAiProposalActionable('PENDING_HUMAN_REVIEW')).toBe(true)
    expect(isAiProposalActionable('APPROVED')).toBe(false)
  })
})
