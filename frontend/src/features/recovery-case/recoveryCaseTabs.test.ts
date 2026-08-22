import { describe, expect, it } from 'vitest'
import { caseTabSearchParams, keyboardCaseTab, resolveCaseTab } from './recoveryCaseTabs'

describe('Recovery Case tab behavior', () => {
  it('resolves URL tabs and keeps overview as the canonical URL', () => {
    expect(resolveCaseTab('evidence')).toBe('evidence')
    expect(resolveCaseTab('unsupported')).toBe('overview')

    const selected = caseTabSearchParams(new URLSearchParams('source=handoff'), 'evidence')
    expect(selected.toString()).toBe('source=handoff&tab=evidence')
    expect(caseTabSearchParams(selected, 'overview').toString()).toBe('source=handoff')
  })

  it('supports wrapping arrows plus Home and End', () => {
    expect(keyboardCaseTab(0, 'ArrowLeft')).toBe('dependencies')
    expect(keyboardCaseTab(4, 'ArrowRight')).toBe('overview')
    expect(keyboardCaseTab(2, 'Home')).toBe('overview')
    expect(keyboardCaseTab(2, 'End')).toBe('dependencies')
    expect(keyboardCaseTab(2, 'Enter')).toBeNull()
  })
})
