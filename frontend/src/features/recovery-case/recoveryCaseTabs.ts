export type CaseTab = 'overview' | 'recovery' | 'impact' | 'evidence' | 'dependencies'

export const recoveryCaseTabs: { id: CaseTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'recovery', label: 'Recovery & Actions' },
  { id: 'impact', label: 'Impact' },
  { id: 'evidence', label: 'Evidence' },
  { id: 'dependencies', label: 'Dependencies' },
]

export function resolveCaseTab(value: string | null): CaseTab {
  return recoveryCaseTabs.some((tab) => tab.id === value) ? value as CaseTab : 'overview'
}

export function caseTabSearchParams(current: URLSearchParams, tab: CaseTab): URLSearchParams {
  const next = new URLSearchParams(current)
  if (tab === 'overview') next.delete('tab')
  else next.set('tab', tab)
  return next
}

export function keyboardCaseTab(index: number, key: string): CaseTab | null {
  if (key === 'Home') return recoveryCaseTabs[0].id
  if (key === 'End') return recoveryCaseTabs[recoveryCaseTabs.length - 1].id
  if (key !== 'ArrowLeft' && key !== 'ArrowRight') return null
  const offset = key === 'ArrowRight' ? 1 : -1
  return recoveryCaseTabs[(index + offset + recoveryCaseTabs.length) % recoveryCaseTabs.length].id
}
