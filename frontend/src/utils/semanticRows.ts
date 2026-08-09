import type { DataQualityCheck, OntologyReviewQueueItem } from '../api'

function lifecycleCompleteness(row: OntologyReviewQueueItem) {
  return row.incident_count + row.asset_count + row.source_record_count + row.activity_count + row.generated_fact_count
}

export function uniqueOntologyReviewRows(rows: OntologyReviewQueueItem[]) {
  const unique = new Map<string, OntologyReviewQueueItem>()
  rows.forEach((row) => {
    const key = `${row.queue_kind}|${row.review_action_id}|${row.target_uri}|${row.release_id}`
    const existing = unique.get(key)
    if (!existing || lifecycleCompleteness(row) > lifecycleCompleteness(existing)) unique.set(key, row)
  })
  return [...unique.values()].sort((left, right) => left.priority_sort_order - right.priority_sort_order || left.target_label.localeCompare(right.target_label))
}

const severityRank: Record<string, number> = { CRITICAL: 4, ERROR: 3, WARNING: 2, INFO: 1 }

export function uniqueQualityChecks(rows: DataQualityCheck[]) {
  const unique = new Map<string, DataQualityCheck>()
  rows.forEach((row) => {
    const existing = unique.get(row.check_result_id)
    const rowTime = Date.parse(row.created_at) || 0
    const existingTime = existing ? Date.parse(existing.created_at) || 0 : -1
    const rowSeverity = severityRank[row.severity.toUpperCase()] ?? 0
    const existingSeverity = existing ? severityRank[existing.severity.toUpperCase()] ?? 0 : -1
    if (!existing || rowSeverity > existingSeverity || (rowSeverity === existingSeverity && rowTime > existingTime)) unique.set(row.check_result_id, row)
  })
  return [...unique.values()].sort((left, right) => {
    const severityDifference = (severityRank[right.severity.toUpperCase()] ?? 0) - (severityRank[left.severity.toUpperCase()] ?? 0)
    return severityDifference || (Date.parse(right.created_at) || 0) - (Date.parse(left.created_at) || 0) || left.check_result_id.localeCompare(right.check_result_id)
  })
}
