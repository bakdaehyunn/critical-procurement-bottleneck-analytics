import type { FollowUpItem } from '../features/recovery-queue/recoveryQueueModel'
import type { Tone } from '../components/ui'

export function titleCase(value?: string | null) {
  if (!value) return 'Unknown'
  return value
    .replace(/^urn:dcai:[^:]+:/, '')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase())
}

export function formatHours(value: number | null | undefined) {
  if (value == null) return 'Unknown'
  return `${value.toFixed(value >= 10 ? 0 : 1)}h`
}

export function formatDateTime(value?: string | null) {
  if (!value) return 'Not recorded'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value
  return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(parsed)
}

export function relativeTime(date: Date | null) {
  if (!date) return 'Waiting for first refresh'
  const seconds = Math.max(0, Math.round((Date.now() - date.getTime()) / 1000))
  if (seconds < 60) return 'Updated just now'
  return `Updated ${Math.round(seconds / 60)}m ago`
}

export function priorityTone(priority: string): Tone {
  if (priority === 'CRITICAL') return 'critical'
  if (priority === 'HIGH') return 'warning'
  return 'neutral'
}

export function evidenceLabel(status: string, issueCount = 0) {
  if (status === 'TRUSTED' && issueCount === 0) return 'Evidence trusted'
  if (status === 'UNVERIFIED') return 'Evidence unverified'
  return issueCount ? `${issueCount} evidence issue${issueCount === 1 ? '' : 's'}` : 'Evidence review'
}

export function evidenceTone(status: string, issueCount = 0): Tone {
  if (status === 'TRUSTED' && issueCount === 0) return 'success'
  return status === 'UNVERIFIED' ? 'neutral' : 'warning'
}

export function readinessLabel(status: string) {
  if (status === 'READY') return 'Ready to restore'
  if (status === 'NOT_READY') return 'Restore blocked'
  return titleCase(status)
}

export function readinessTone(status: string): Tone {
  if (status === 'READY') return 'success'
  if (status === 'NOT_READY') return 'critical'
  return 'warning'
}

export function dependencyOwner(row: FollowUpItem) {
  if (row.dependency_roles.length) return row.dependency_roles.map(titleCase).join(', ')
  if (row.vendor_status && row.vendor_status !== 'UNKNOWN') return `Vendor dependency · ${titleCase(row.vendor_status)}`
  return 'No owner or dependency recorded'
}
