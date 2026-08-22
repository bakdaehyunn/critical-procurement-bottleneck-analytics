import { semanticQueryCatalog } from '../../semanticQueryCatalog'
import { postSemanticQuery } from '../../semanticQueryClient'
import type { FilterMetadata, FilterOption, FollowUpItem, Overview, RecoveryQueueSnapshot } from './recoveryQueueModel'

type SemanticDashboardOverviewRecord = {
  totalIncidents: number
  capacityRiskKw: number
  affectedGpuCount: number
  trustFindingCount: number
  avgDurationHours?: number
  repeatFailureAssetCount?: number
  engineerAssignmentDelayHours?: number
}

export type SemanticFollowUpQueueRecord = {
  graphUri: string
  incidentUri: string
  incidentId: string
  assetUri: string
  assetId: string
  zoneUri: string
  zoneId: string
  stageUri: string
  stageLabel?: string
  sourceRecordUri: string
  priorityRank?: number
  requestTitle?: string
  currentStatus?: string
  hoursInCurrentStage?: number
  neededByAt?: string
  priorityLevel?: string
  businessImpact?: string
  assetCriticalityScore?: number
  downtimeScore?: number
  stageDelayScore?: number
  infrastructureZoneImpactScore?: number
  neededByUrgencyScore?: number
  repeatFailureScore?: number
  spareRiskScore?: number
  capacityRiskScore?: number
  redundancyRiskScore?: number
  thermalRiskScore?: number
  vendorEtaRiskScore?: number
  mitigationCreditScore?: number
  totalPriorityScore?: number
}

export type SemanticFollowUpDetailRecord = SemanticFollowUpQueueRecord & {
  impactUri?: string
  capacityRiskKw?: number
  affectedGpuCount?: number
  recommendedAction?: string
  recoveryBlockerUri?: string
  blockerSummary?: string
  restoreReadinessUri?: string
  restoreReadinessSummary?: string
  trustFindingUri?: string
  trustSummary?: string
  redundancyState?: string
  affectedRackCount?: number
  estimatedGpuCapacityRiskPct?: number
  thermalBreachMinutes?: number
  powerRedundancyLost?: boolean
  coolingRedundancyLost?: boolean
  mitigationStatus?: string
  vendorEtaAt?: string
  vendorStatus?: string
}

export type SemanticTopologyDependencyRecord = {
  dependencyEdgeUri: string
  dependencyId: string
  dependentAssetId: string
  dependencyAssetId: string
  dependencyRole: string
  impactScope?: string
  pathId?: string
  sourceRecordUri: string
}

type SemanticFilterMetadataRecord = { filterType: string; id: string; label?: string }

export async function fetchRecoveryQueueSnapshot(): Promise<RecoveryQueueSnapshot> {
  const { overviewRecord, followUps } = await fetchFollowUpReadModel()
  return { overview: buildOverview(overviewRecord, followUps), followUps }
}

export async function fetchReviewAttentionSignals(): Promise<FollowUpItem[]> {
  return (await fetchFollowUpReadModel()).followUps
}

export async function fetchFilterMetadata(): Promise<FilterMetadata> {
  const records = await postSemanticQuery<SemanticFilterMetadataRecord>(semanticQueryCatalog.filterMetadata)
  const grouped = records.reduce<Record<string, FilterOption[]>>((summary, record) => {
    const values = summary[record.filterType] ?? []
    values.push({ id: record.id, name: record.label ?? humanize(record.id) })
    summary[record.filterType] = values
    return summary
  }, {})
  return {
    infrastructure_zones: grouped.zone ?? [],
    assets: grouped.asset ?? [],
    asset_types: unique(records.filter(({ filterType }) => filterType === 'assetType').map(({ id, label }) => label ?? id)),
    facilities_teams: [],
    spare_categories: [],
    priority_levels: ['CRITICAL', 'HIGH', 'MEDIUM'],
    request_types: [],
    failure_modes: [],
    stages: unique(records.filter(({ filterType }) => filterType === 'stage').map(({ id, label }) => label ?? id)),
  }
}

export async function fetchFollowUpRecords(options: { signal?: AbortSignal } = {}) {
  const [queueRecords, detailRecords] = await Promise.all([
    postSemanticQuery<SemanticFollowUpQueueRecord>(semanticQueryCatalog.followUpQueueList, {}, options),
    postSemanticQuery<SemanticFollowUpDetailRecord>(semanticQueryCatalog.followUpDetail, {}, options),
  ])
  return { queueRecords, detailRecords, followUps: buildFollowUps(queueRecords, detailRecords) }
}

async function fetchFollowUpReadModel(): Promise<{ overviewRecord?: SemanticDashboardOverviewRecord; followUps: FollowUpItem[] }> {
  const [overviewRecords, queueRecords, detailRecords, dependencyRecords] = await Promise.all([
    postSemanticQuery<SemanticDashboardOverviewRecord>(semanticQueryCatalog.dashboardOverview),
    postSemanticQuery<SemanticFollowUpQueueRecord>(semanticQueryCatalog.followUpQueueList),
    postSemanticQuery<SemanticFollowUpDetailRecord>(semanticQueryCatalog.followUpDetail),
    postSemanticQuery<SemanticTopologyDependencyRecord>(semanticQueryCatalog.topologyDependencies),
  ])
  return { overviewRecord: overviewRecords[0], followUps: buildFollowUps(queueRecords, detailRecords, dependencyRecords) }
}

export function buildFollowUps(
  queueRecords: SemanticFollowUpQueueRecord[],
  detailRecords: SemanticFollowUpDetailRecord[],
  dependencyRecords: SemanticTopologyDependencyRecord[] = [],
): FollowUpItem[] {
  const detailsByIncident = new Map(detailRecords.map((record) => [record.incidentId, record]))
  const dependenciesByAsset = dependencyRecords.reduce<Map<string, SemanticTopologyDependencyRecord[]>>((summary, record) => {
    const edges = summary.get(record.dependentAssetId) ?? []
    edges.push(record)
    summary.set(record.dependentAssetId, edges)
    return summary
  }, new Map())
  return queueRecords
    .map((record) => mapFollowUp(record, detailsByIncident.get(record.incidentId), dependenciesByAsset.get(record.assetId) ?? []))
    .sort((left, right) => left.priority_rank - right.priority_rank || compareNullableDesc(left.total_priority_score, right.total_priority_score) || left.incident_id.localeCompare(right.incident_id))
    .filter(uniqueBy((row) => row.incident_id))
    .map((row, index) => ({ ...row, priority_rank: row.priority_rank || index + 1 }))
}

function mapFollowUp(record: SemanticFollowUpQueueRecord, detail?: SemanticFollowUpDetailRecord, dependencies: SemanticTopologyDependencyRecord[] = []): FollowUpItem {
  const semantic = detail ?? record
  const stage = canonicalStage(semantic.stageLabel ?? record.stageLabel ?? record.stageUri)
  const capacityRiskKw = detail?.capacityRiskKw ?? null
  const affectedGpuCount = detail?.affectedGpuCount ?? null
  const trustIssueCount = detail?.trustFindingUri ? 1 : 0
  const redundancyState = normalizeRedundancyState(detail?.redundancyState)
  const vendorStatus = normalizeVendorStatus(detail?.vendorStatus)
  return {
    priority_rank: semantic.priorityRank ?? 0,
    incident_id: record.incidentId,
    request_number: record.incidentId,
    request_title: semantic.requestTitle ?? detail?.blockerSummary ?? detail?.recommendedAction ?? 'Unknown incident title',
    asset_id: record.assetId,
    asset_name: humanize(record.assetId),
    zone_id: record.zoneId,
    zone_name: humanize(record.zoneId),
    current_stage: stage,
    current_status: semantic.currentStatus ?? 'UNKNOWN',
    hours_in_current_stage: semantic.hoursInCurrentStage ?? null,
    needed_by_at: semantic.neededByAt ?? '',
    priority_level: semantic.priorityLevel ?? 'UNKNOWN',
    business_impact: semantic.businessImpact ?? 'Unknown',
    asset_criticality_score: semantic.assetCriticalityScore ?? null,
    downtime_score: semantic.downtimeScore ?? null,
    stage_delay_score: semantic.stageDelayScore ?? null,
    infrastructure_zone_impact_score: semantic.infrastructureZoneImpactScore ?? null,
    needed_by_urgency_score: semantic.neededByUrgencyScore ?? null,
    repeat_failure_score: semantic.repeatFailureScore ?? null,
    spare_risk_score: semantic.spareRiskScore ?? null,
    capacity_risk_score: semantic.capacityRiskScore ?? null,
    redundancy_risk_score: semantic.redundancyRiskScore ?? null,
    thermal_risk_score: semantic.thermalRiskScore ?? null,
    vendor_eta_risk_score: semantic.vendorEtaRiskScore ?? null,
    mitigation_credit_score: semantic.mitigationCreditScore ?? null,
    total_priority_score: semantic.totalPriorityScore ?? null,
    recommended_action: detail?.recommendedAction ?? 'No governed recommendation is available',
    reason_summary: detail?.blockerSummary ?? 'No authoritative reasoning summary is available',
    redundancy_state: redundancyState,
    affected_gpu_count: affectedGpuCount,
    estimated_capacity_risk_kw: capacityRiskKw,
    mitigation_status: detail?.mitigationStatus ?? null,
    vendor_status: vendorStatus,
    impact_confidence_status: detail?.trustFindingUri ? 'WARNING' : detail?.impactUri ? 'TRUSTED' : 'UNKNOWN',
    impact_trust_issue_count: trustIssueCount,
    restore_readiness_status: restoreReadinessStatusFor(detail?.restoreReadinessSummary),
    restore_readiness_summary: detail?.restoreReadinessSummary ?? null,
    dependency_roles: unique(dependencies.map(({ dependencyRole }) => dependencyRole)),
    dependency_path_ids: unique(dependencies.map(({ pathId }) => pathId).filter(Boolean) as string[]),
  }
}

function buildOverview(record: SemanticDashboardOverviewRecord | undefined, followUps: FollowUpItem[]): Overview {
  const delayed = followUps.map(isKnownDelayed)
  const criticalDelayed = followUps.filter((row) => row.priority_level === 'CRITICAL').map(isKnownDelayed)
  const recoveryHours = followUps.filter((row) => row.current_stage === 'RECOVERY').map((row) => row.hours_in_current_stage)
  return {
    total_requests: record?.totalIncidents ?? followUps.length,
    open_requests: followUps.length,
    delayed_requests: delayed.some((value) => value == null) ? null : delayed.filter(Boolean).length,
    critical_asset_delayed: criticalDelayed.some((value) => value == null) ? null : criticalDelayed.filter(Boolean).length,
    avg_downtime_hours: record?.avgDurationHours ?? null,
    top_bottleneck_stage: topStage(followUps),
    spare_waiting_delay_hours: sumKnown(recoveryHours),
    repeat_failure_asset_count: record?.repeatFailureAssetCount ?? null,
    engineer_assignment_delay_hours: record?.engineerAssignmentDelayHours ?? null,
    capacity_risk_kw: record?.capacityRiskKw ?? sumKnown(followUps.map((row) => row.estimated_capacity_risk_kw)),
    affected_gpu_count: record?.affectedGpuCount ?? sumKnown(followUps.map((row) => row.affected_gpu_count)),
    redundancy_lost_incidents: followUps.filter((row) => isRedundancyLost(row.redundancy_state)).length,
    vendor_eta_missed_count: followUps.filter((row) => isVendorPartsEscalation(row.vendor_status)).length,
    latest_pipeline_run_status: record ? 'SEMANTIC_GRAPH' : null,
    data_quality_status: record == null ? 'Unknown' : record.trustFindingCount > 0 ? 'Needs review' : 'Trusted',
  }
}

function isKnownDelayed(row: FollowUpItem): boolean | null {
  if (row.estimated_capacity_risk_kw == null || row.hours_in_current_stage == null) return null
  return row.estimated_capacity_risk_kw > 0 || row.hours_in_current_stage > 0
}

function sumKnown(values: (number | null)[]): number | null {
  if (values.some((value) => value == null)) return null
  return values.reduce<number>((total, value) => total + requireNumber(value), 0)
}

function requireNumber(value: number | null): number {
  if (value == null) throw new Error('Expected a known number')
  return value
}

function compareNullableDesc(left: number | null, right: number | null): number {
  if (left == null) return right == null ? 0 : 1
  if (right == null) return -1
  return right - left
}

function uniqueBy<T>(keyFor: (item: T) => string) {
  const seen = new Set<string>()
  return (item: T) => !seen.has(keyFor(item)) && Boolean(seen.add(keyFor(item)))
}

function topStage(rows: FollowUpItem[]): string | null {
  const [stage] = [...rows.reduce<Map<string, number>>((summary, row) => summary.set(row.current_stage, (summary.get(row.current_stage) ?? 0) + 1), new Map()).entries()]
    .sort(([, left], [, right]) => right - left)[0] ?? []
  return stage ?? null
}

export function normalizeRedundancyState(value?: string | null): string | null {
  if (!value) return null
  const normalized = canonicalStage(value)
  if (normalized === 'N_PLUS_0' || normalized.includes('PLUS_0')) return 'REDUNDANCY_LOST'
  if (normalized === 'N_PLUS_1' || normalized.includes('PLUS_1')) return 'REDUNDANCY_AVAILABLE'
  return normalized
}

export function isRedundancyLost(value?: string | null) { return normalizeRedundancyState(value) === 'REDUNDANCY_LOST' }
export function normalizeVendorStatus(value?: string | null) { return value ? canonicalStage(value) : null }
export function isVendorPartsEscalation(value?: string | null) { return ['VENDOR_ENGAGED', 'PARTS_REVIEW'].includes(normalizeVendorStatus(value) ?? '') }
export function canonicalStage(value: string) {
  const normalized = lastSegment(value).trim()
  if (!normalized) return 'UNKNOWN'
  return normalized.replace(/([a-z0-9])([A-Z])/g, '$1_$2').replace(/[\s-]+/g, '_').replace(/[^A-Za-z0-9_]/g, '_').replace(/_+/g, '_').toUpperCase()
}
export function humanize(value: string) { return lastSegment(value).replace(/[_-]+/g, ' ').replace(/\b\w/g, (character) => character.toUpperCase()) }
export function lastSegment(value: string) { return value.split(/[/#:]/).filter(Boolean).at(-1) ?? value }
export function unique(values: string[]) { return [...new Set(values)] }

function restoreReadinessStatusFor(summary?: string): string {
  if (!summary) return 'UNKNOWN'
  const normalized = summary.toLowerCase()
  if (normalized.includes('not ready') || normalized.includes('blocked')) return 'NOT_READY'
  if (normalized.includes('ready for review') || normalized.includes('ready')) return 'READY'
  return 'REVIEW'
}
