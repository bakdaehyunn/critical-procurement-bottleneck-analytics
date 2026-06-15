import { type ReactNode, useEffect, useMemo, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Boxes,
  CheckCircle2,
  Clock3,
  Cpu,
  Database,
  Filter,
  GitBranch,
  Network,
  RefreshCcw,
  ServerCog,
  ShieldAlert,
  Target,
  Wrench,
  Zap,
} from 'lucide-react'
import { Link, Route, Routes, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  type DashboardData,
  type DashboardFilters,
  type AiProposalItem,
  type FilterMetadata,
  type FollowUpItem,
  type InfrastructureDependency,
  type OntologyActionAffordance,
  type OntologyActionAuditHistoryItem,
  type OntologyActionDispatchQueueItem,
  type OntologyActionLifecycleState,
  type OntologyActionNotificationItem,
  type OntologyActionPlacement,
  type OntologyActionReviewQueueItem,
  type OntologyActionSubmission,
  type OntologyActionTransitionHistoryItem,
  type OntologyReviewQueueItem,
  type RequestDetail,
  type RequestSemanticContext,
  fetchDashboardData,
  fetchFilterMetadata,
  fetchRequestDetail,
  fetchRequestSemanticContext,
  fetchTopologyDependencies,
  filterDashboardData,
  isRedundancyLost,
  isVendorPartsEscalation,
  submitAiProposalReview,
  submitOntologyActionRequest,
  submitOntologyActionTransition,
} from './api'
import './App.css'

type DetailTab = 'summary' | 'impact' | 'trust' | 'dependencies'

const detailTabs: { id: DetailTab; label: string }[] = [
  { id: 'summary', label: 'Summary' },
  { id: 'impact', label: 'Impact' },
  { id: 'trust', label: 'Trust' },
  { id: 'dependencies', label: 'Dependencies' },
]

const queueScopes = {
  criticalDelayed: { critical_asset_delayed: true },
  redundancyLost: { redundancy_lost: true },
  vendorPartsEscalation: { vendor_parts_escalation: true },
  recoveryStage: { stage: 'RECOVERY' },
  validationStage: { stage: 'VALIDATION' },
  trustReview: { trust_review: true },
  restoreBlocked: { restore_blocked: true },
} satisfies Record<string, DashboardFilters>

const queueScopeControls: { id: string; label: string; filters: DashboardFilters }[] = [
  { id: 'all', label: 'All findings', filters: {} },
  { id: 'restore-blocked', label: 'Restore blocked', filters: queueScopes.restoreBlocked },
  { id: 'trust-review', label: 'Trust review', filters: queueScopes.trustReview },
  { id: 'redundancy-lost', label: 'Redundancy lost', filters: queueScopes.redundancyLost },
  { id: 'vendor-parts-escalation', label: 'Vendor/parts escalation', filters: queueScopes.vendorPartsEscalation },
  { id: 'recovery-stage', label: 'Recovery stage', filters: queueScopes.recoveryStage },
  { id: 'validation-stage', label: 'Validation stage', filters: queueScopes.validationStage },
  { id: 'critical-asset-delay', label: 'Critical asset delay', filters: queueScopes.criticalDelayed },
]

function App() {
  return (
    <Routes>
      <Route path="/" element={<FollowUpQueuePage />} />
      <Route path="/findings/:incidentId" element={<FollowUpDetailPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

function FollowUpQueuePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filterQuery = searchParams.toString()
  const filters = useMemo(() => filtersFromQuery(filterQuery), [filterQuery])
  const [dashboardSnapshot, setDashboardSnapshot] = useState<DashboardData | null>(null)
  const [metadata, setMetadata] = useState<FilterMetadata | null>(null)
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function loadMetadata() {
      try {
        const filterMetadata = await fetchFilterMetadata()
        if (!cancelled) setMetadata(filterMetadata)
      } catch {
        if (!cancelled) setMetadata(null)
      }
    }
    loadMetadata()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const dashboardData = await fetchDashboardData()
        if (!cancelled) {
          setDashboardSnapshot(dashboardData)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  const dashboard = useMemo(
    () => dashboardSnapshot ? filterDashboardData(dashboardSnapshot, filters) : null,
    [dashboardSnapshot, filters],
  )

  const setFilter = (key: keyof DashboardFilters, value: string) => {
    applyFilters(setSearchParams, { ...filters, [key]: value || undefined })
  }

  const setQueueScope = (scope: DashboardFilters) => {
    applyFilters(setSearchParams, scope)
  }
  const followUps = dashboard?.followUps ?? []
  const queueSummary = summarizeQueue(followUps)
  const selectedFollowUp = followUps.find((row) => row.incident_id === selectedIncidentId) ?? null

  return (
    <main className="app-shell">
      <header className="topbar dashboard-hero">
        <div>
          <p className="eyebrow">AI infrastructure operations</p>
          <h1>AI Data Center Infrastructure Semantic Operations Platform</h1>
          <p className="page-summary">Semantic findings workbench for restore readiness, evidence trust, dependency exposure, and blast-radius decisions.</p>
        </div>
        <button className="icon-button" onClick={() => applyFilters(setSearchParams, {})} title="Reset filters">
          <RefreshCcw size={18} />
        </button>
      </header>

      <OperationalCommandStrip
        dashboard={dashboard}
        summary={queueSummary}
        loading={loading}
        onRefresh={() => window.location.reload()}
        onTrustReview={() => setQueueScope(queueScopes.trustReview)}
        onFocusBoundary={() => document.getElementById('semantic-findings')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
        onShowFindings={() => {
          setQueueScope({})
          document.getElementById('semantic-findings')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }}
      />

      {error ? <div className="error-banner">{error}</div> : null}

      <SectionLabel label="Filters" />
      <section className="filters" aria-label="Dashboard filters">
        <Filter size={18} />
        <Select label="Zone" value={filters.zone_id ?? ''} options={metadata?.infrastructure_zones ?? []} onChange={(value) => setFilter('zone_id', value)} />
        <Select label="Asset" value={filters.asset_id ?? ''} options={metadata?.assets ?? []} onChange={(value) => setFilter('asset_id', value)} />
        <Select label="Priority" value={filters.priority_level ?? ''} values={metadata?.priority_levels ?? []} onChange={(value) => setFilter('priority_level', value)} />
        <Select label="Stage" value={filters.stage ?? ''} values={metadata?.stages ?? []} onChange={(value) => setFilter('stage', value)} />
      </section>

      <SectionLabel label="Graph Finding Summary" />
      <section className="kpi-grid">
        <Kpi icon={<Wrench size={18} />} label="Finding items" value={queueSummary.queueItems} />
        <Kpi icon={<Clock3 size={18} />} label="Restore blocked" value={queueSummary.restoreBlockedItems} tone="danger" />
        <Kpi icon={<AlertTriangle size={18} />} label="Critical priority" value={queueSummary.criticalPriorityItems} tone="danger" />
        <Kpi icon={<Zap size={18} />} label="Capacity at risk" value={`${queueSummary.capacityRiskKw.toFixed(0)} kW`} tone="danger" />
        <Kpi icon={<Cpu size={18} />} label="Affected GPUs" value={queueSummary.affectedGpuCount} tone="warning" />
      </section>

      <section className="exposure-strip" aria-label="Operational exposure">
        <ExposureMetric icon={<ShieldAlert size={16} />} label="Redundancy lost" value={queueSummary.n1ExposureItems} tone="danger" />
        <ExposureMetric icon={<Clock3 size={16} />} label="Vendor/parts escalation" value={queueSummary.vendorEtaMissedItems} tone="warning" />
        <ExposureMetric icon={<Boxes size={16} />} label="Dependency roles" value={queueSummary.dependencyRoleCount} />
        <ExposureMetric icon={<Database size={16} />} label="Trust review" value={queueSummary.evidenceReviewItems} tone={queueSummary.evidenceReviewItems ? 'danger' : 'ok'} />
      </section>

      <SectionLabel label="Graph Insight" />
      <QueueIntelligence rows={followUps} selectedRow={selectedFollowUp} summary={queueSummary} />

      <SectionLabel label="Internal Review Queues" />
      <OntologyReviewQueuePanel rows={dashboard?.ontologyReviewQueue ?? []} loading={loading} />

      <SectionLabel label="Semantic Findings" />
      <section id="semantic-findings" className="queue-scope-bar" aria-label="Semantic finding scopes">
        <div>
          {queueScopeControls.map((scope) => (
            <button
              key={scope.id}
              type="button"
              data-scope-id={scope.id}
              className={isExactScopeActive(filters, scope.filters) ? 'active' : ''}
              aria-pressed={isExactScopeActive(filters, scope.filters)}
              onClick={() => setQueueScope(scope.filters)}
            >
              {scope.label}
            </button>
          ))}
        </div>
      </section>

      <section className="queue-workspace">
        <section className="panel queue-panel">
          {loading ? (
            <div className="empty-state">Loading semantic findings</div>
          ) : (
            <FollowUpTable rows={followUps} selectedIncidentId={selectedFollowUp?.incident_id ?? null} onSelect={setSelectedIncidentId} />
          )}
        </section>
      </section>
    </main>
  )
}

function OperationalCommandStrip({ dashboard, summary, loading, onRefresh, onTrustReview, onFocusBoundary, onShowFindings }: {
  dashboard: DashboardData | null
  summary: ReturnType<typeof summarizeQueue>
  loading: boolean
  onRefresh: () => void
  onTrustReview: () => void
  onFocusBoundary: () => void
  onShowFindings: () => void
}) {
  const pipelineStatus = dashboard?.overview.latest_pipeline_run_status ?? (loading ? 'Loading' : 'Unavailable')
  const dataQualityStatus = dashboard?.overview.data_quality_status ?? (loading ? 'Loading' : 'Unavailable')
  return (
    <section className="command-strip" aria-label="Semantic operations status">
      <SystemSignal icon={<Activity size={16} />} label="Pipeline run" value={formatStage(pipelineStatus)} tone={statusSignalTone(pipelineStatus)} onClick={onRefresh} actionLabel="Refresh semantic dashboard data" />
      <SystemSignal icon={<CheckCircle2 size={16} />} label="Data quality" value={formatStage(dataQualityStatus)} tone={qualitySignalTone(dataQualityStatus)} onClick={onTrustReview} actionLabel="Show findings that need trust review" />
      <SystemSignal icon={<ServerCog size={16} />} label="Semantic boundary" value="Approved query catalog" tone="ok" onClick={onFocusBoundary} actionLabel="Focus approved semantic finding controls" />
      <SystemSignal icon={<Target size={16} />} label="Visible findings" value={`${summary.queueItems} active`} tone={summary.queueItems ? 'warning' : 'ok'} onClick={onShowFindings} actionLabel="Show all visible semantic findings" />
    </section>
  )
}

function SystemSignal({ icon, label, value, tone, onClick, actionLabel }: {
  icon: ReactNode
  label: string
  value: string
  tone?: 'ok' | 'warning' | 'danger'
  onClick?: () => void
  actionLabel?: string
}) {
  const content = (
    <>
      <div className="system-signal-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </>
  )
  if (onClick) {
    return (
      <button type="button" className={`system-signal interactive ${tone ?? ''}`} onClick={onClick} aria-label={actionLabel ?? label}>
        {content}
      </button>
    )
  }
  return (
    <div className={`system-signal ${tone ?? ''}`}>
      {content}
    </div>
  )
}

function filtersFromQuery(filterQuery: string): DashboardFilters {
  const searchParams = new URLSearchParams(filterQuery)
  return {
    zone_id: searchParams.get('zone_id') || undefined,
    asset_id: searchParams.get('asset_id') || undefined,
    priority_level: searchParams.get('priority_level') || undefined,
    stage: searchParams.get('stage') || undefined,
    delayed_only: searchParams.get('delayed_only') === 'true' || undefined,
    critical_asset_delayed: searchParams.get('critical_asset_delayed') === 'true' || undefined,
    capacity_risk: searchParams.get('capacity_risk') === 'true' || undefined,
    affected_gpu: searchParams.get('affected_gpu') === 'true' || undefined,
    evidence_review: searchParams.get('evidence_review') === 'true' || undefined,
    redundancy_lost: searchParams.get('redundancy_lost') === 'true' || undefined,
    vendor_parts_escalation: searchParams.get('vendor_parts_escalation') === 'true' || undefined,
    restore_blocked: searchParams.get('restore_blocked') === 'true' || undefined,
    trust_review: searchParams.get('trust_review') === 'true' || undefined,
    dependency_role: searchParams.get('dependency_role') || undefined,
  }
}

function applyFilters(setSearchParams: (nextInit: URLSearchParams) => void, filters: DashboardFilters) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, String(value))
    }
  })
  setSearchParams(params)
}

function isExactScopeActive(filters: DashboardFilters, scope: DashboardFilters) {
  const filterEntries = Object.entries(filters).filter(([, value]) => Boolean(value))
  const scopeEntries = Object.entries(scope).filter(([, value]) => Boolean(value))
  return filterEntries.length === scopeEntries.length
    && scopeEntries.every(([key, value]) => filters[key as keyof DashboardFilters] === value)
}

function summarizeQueue(rows: FollowUpItem[]) {
  return {
    queueItems: rows.length,
    delayedItems: rows.filter((row) => row.hours_in_current_stage > 0).length,
    restoreBlockedItems: rows.filter((row) => row.restore_readiness_status === 'NOT_READY').length,
    criticalPriorityItems: rows.filter((row) => row.priority_level === 'CRITICAL').length,
    capacityRiskKw: rows.reduce((total, row) => total + row.estimated_capacity_risk_kw, 0),
    affectedGpuCount: rows.reduce((total, row) => total + row.affected_gpu_count, 0),
    n1ExposureItems: rows.filter((row) => isRedundancyLost(row.redundancy_state)).length,
    vendorEtaMissedItems: rows.filter((row) => isVendorPartsEscalation(row.vendor_status)).length,
    spareVendorWaitItems: rows.filter((row) => row.current_stage === 'RECOVERY').length,
    spareVendorWaitHours: rows
      .filter((row) => row.current_stage === 'RECOVERY')
      .reduce((total, row) => total + row.hours_in_current_stage, 0),
    dependencyRoleCount: new Set(rows.flatMap((row) => row.dependency_roles)).size,
    evidenceReviewItems: rows.filter((row) => row.impact_confidence_status !== 'TRUSTED').length,
  }
}

type QueueIntelligenceItem = {
  label: string
  value: string
  tone?: 'ok' | 'warning' | 'danger'
}

function QueueIntelligence({ rows, selectedRow, summary }: {
  rows: FollowUpItem[]
  selectedRow: FollowUpItem | null
  summary: ReturnType<typeof summarizeQueue>
}) {
  const items = selectedRow ? buildSelectedFollowUpIntelligence(selectedRow) : buildQueueIntelligence(rows, summary)
  return (
    <section className={`queue-intelligence ${selectedRow ? 'selected' : ''}`} aria-label="Graph finding insight">
      <div className="queue-intelligence-header">
        <span>{selectedRow ? 'Selected finding preview' : 'Visible graph readout'}</span>
        {selectedRow ? (
          <Link to={`/findings/${selectedRow.incident_id}`}>
            Open detail
            <ArrowRight size={14} />
          </Link>
        ) : (
          <strong>{rows.length} active finding{rows.length === 1 ? '' : 's'}</strong>
        )}
      </div>
      <div className="queue-intelligence-grid">
        {items.map((item) => (
          <div className={`queue-intelligence-item ${item.tone ?? ''}`} key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>
    </section>
  )
}

function OntologyReviewQueuePanel({ rows, loading }: {
  rows: OntologyReviewQueueItem[]
  loading: boolean
}) {
  const visibleRows = rows.slice(0, 6)
  if (loading) {
    return <section className="ontology-review-queue loading-state">Loading lifecycle review state</section>
  }
  if (!visibleRows.length) {
    return <section className="ontology-review-queue empty-state">No lifecycle or reasoning review state is available from approved semantic queries</section>
  }
  return (
    <section className="ontology-review-queue" aria-label="Internal ontology lifecycle and reasoning review queues">
      {visibleRows.map((row) => (
        <article className={`ontology-review-card ${reviewQueueTone(row)}`} key={row.queue_id}>
          <div className="ontology-review-card-header">
            <div>
              <span>{formatReviewQueueKind(row.queue_kind)}</span>
              <strong>{row.review_action_label}</strong>
            </div>
            <button type="button" disabled title={row.disabled_reason}>
              {formatStage(row.action_status)}
            </button>
          </div>
          <p>{row.evidence_summary}</p>
          <div className="ontology-review-facts">
            <SummaryMetric label="Status" value={formatStage(row.review_status)} tone={reviewStatusTone(row.review_status)} />
            <SummaryMetric label="Release" value={row.release_id} detail={row.target_type} />
            <SummaryMetric label="Incidents" value={String(row.incident_count)} />
            <SummaryMetric label="Activities" value={String(row.activity_count)} detail={`${row.generated_fact_count} generated facts`} />
          </div>
          <div className="ontology-review-graphs">
            <ActionHistoryLink label="Target" uri={row.target_uri} />
            <ActionHistoryLink label="Canonical" uri={row.canonical_graph_uri} />
            <ActionHistoryLink label="Provenance" uri={row.provenance_graph_uri} />
            <ActionHistoryLink label="Reasoning audit" uri={row.reasoning_audit_graph_uri} />
            <ActionHistoryLink label="Reasoning" uri={row.reasoning_graph_uri} />
          </div>
          <div className="ontology-review-disabled">
            <span>Disabled reason</span>
            <strong>{row.disabled_reason}</strong>
          </div>
        </article>
      ))}
    </section>
  )
}

function formatReviewQueueKind(queueKind: string) {
  if (queueKind === 'promotion-batch') return 'Promotion batch review'
  if (queueKind === 'reasoning-refresh') return 'Reasoning refresh review'
  if (queueKind === 'reasoning-approval') return 'Reasoning approval review'
  return formatStage(queueKind)
}

function reviewQueueTone(row: OntologyReviewQueueItem) {
  if (row.review_status.startsWith('PENDING')) return 'warning'
  if (row.review_status.includes('APPROVED') || row.review_status === 'REFRESHED') return 'ok'
  return ''
}

function reviewStatusTone(status: string): 'ok' | 'warning' | 'danger' | undefined {
  if (status.startsWith('PENDING')) return 'warning'
  if (status.includes('APPROVED') || status === 'REFRESHED') return 'ok'
  return undefined
}

function buildSelectedFollowUpIntelligence(row: FollowUpItem): QueueIntelligenceItem[] {
  return [
    {
      label: 'Incident',
      value: row.request_number,
      tone: row.priority_level === 'CRITICAL' ? 'danger' : row.priority_level === 'HIGH' ? 'warning' : undefined,
    },
    {
      label: 'Summary',
      value: row.request_title,
    },
    {
      label: 'Next action',
      value: row.recommended_action,
      tone: row.priority_level === 'CRITICAL' ? 'danger' : row.priority_level === 'HIGH' ? 'warning' : undefined,
    },
    {
      label: 'Blocker',
      value: formatStage(row.current_stage),
      tone: row.hours_in_current_stage > 0 ? 'warning' : undefined,
    },
    {
      label: 'Time',
      value: formatHours(row.hours_in_current_stage),
      tone: row.hours_in_current_stage > 0 ? 'warning' : undefined,
    },
    {
      label: 'GPUs',
      value: row.affected_gpu_count ? String(row.affected_gpu_count) : '0',
      tone: row.affected_gpu_count > 0 ? 'warning' : undefined,
    },
    {
      label: 'Capacity risk',
      value: `${row.estimated_capacity_risk_kw.toFixed(0)} kW`,
      tone: isRedundancyLost(row.redundancy_state) || row.estimated_capacity_risk_kw > 0 ? 'danger' : undefined,
    },
    {
      label: 'Trust',
      value: trustStatusLabel(row.impact_confidence_status),
      tone: row.impact_confidence_status === 'TRUSTED' ? 'ok' : 'warning',
    },
    {
      label: 'Restore',
      value: restoreReadinessLabel(row.restore_readiness_status),
      tone: restoreReadinessTone(row.restore_readiness_status),
    },
  ]
}

function buildQueueIntelligence(rows: FollowUpItem[], summary: ReturnType<typeof summarizeQueue>): QueueIntelligenceItem[] {
  const topBlocker = topBlockerSignal(rows)
  const exposureTone = summary.capacityRiskKw > 0 ? 'danger' : undefined
  return [
    {
      label: 'Top blocker',
      value: topBlocker.value,
      tone: topBlocker.tone,
    },
    {
      label: 'Capacity risk',
      value: `${summary.capacityRiskKw.toFixed(0)} kW`,
      tone: exposureTone,
    },
    {
      label: 'Affected GPUs',
      value: String(summary.affectedGpuCount),
      tone: summary.affectedGpuCount > 0 ? 'warning' : undefined,
    },
    {
      label: 'Trust review',
      value: summary.evidenceReviewItems ? `${summary.evidenceReviewItems} need review` : 'Trusted graph',
      tone: summary.evidenceReviewItems ? 'warning' : 'ok',
    },
    primaryRiskSignal(summary),
  ]
}

function topBlockerSignal(rows: FollowUpItem[]): QueueIntelligenceItem {
  if (!rows.length) {
    return {
      label: 'Top blocker',
      value: 'No blocker',
    }
  }
  const stages = rows.reduce<Map<string, { count: number; hours: number }>>((stats, row) => {
    const current = stats.get(row.current_stage) ?? { count: 0, hours: 0 }
    current.count += 1
    current.hours += row.hours_in_current_stage
    stats.set(row.current_stage, current)
    return stats
  }, new Map())
  const [stage, stats] = [...stages.entries()].sort(([, left], [, right]) => right.hours - left.hours || right.count - left.count)[0]
  return {
    label: 'Top blocker',
    value: formatStage(stage),
    tone: stats.hours > 0 ? 'warning' : undefined,
  }
}

function primaryRiskSignal(summary: ReturnType<typeof summarizeQueue>): QueueIntelligenceItem {
  if (!summary.queueItems) {
    return {
      label: 'Primary risk',
      value: 'No active risk',
    }
  }
  if (summary.n1ExposureItems && summary.vendorEtaMissedItems) {
    return {
      label: 'Primary risk',
      value: 'Redundancy + vendor',
      tone: 'danger',
    }
  }
  if (summary.n1ExposureItems) {
    return {
      label: 'Primary risk',
      value: 'Redundancy lost',
      tone: 'danger',
    }
  }
  if (summary.vendorEtaMissedItems) {
    return {
      label: 'Primary risk',
      value: 'Vendor/parts escalation',
      tone: 'warning',
    }
  }
  if (summary.spareVendorWaitItems) {
    return {
      label: 'Primary risk',
      value: 'Recovery blockers',
      tone: 'warning',
    }
  }
  return {
    label: 'Primary risk',
    value: 'Graph finding review',
    tone: summary.delayedItems ? 'warning' : undefined,
  }
}

function Kpi({ icon, label, value, tone }: {
  icon: ReactNode
  label: string
  value: ReactNode
  tone?: 'ok' | 'warning' | 'danger'
}) {
  return (
    <div className={`kpi ${tone ?? ''}`}>
      <div className="kpi-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function ExposureMetric({ icon, label, value, tone }: {
  icon: ReactNode
  label: string
  value: ReactNode
  tone?: 'ok' | 'warning' | 'danger'
}) {
  return (
    <div className={`exposure-metric ${tone ?? ''}`}>
      <div className="exposure-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Select({
  label,
  value,
  options,
  values,
  onChange,
}: {
  label: string
  value: string
  options?: { id: string; name: string }[]
  values?: string[]
  onChange: (value: string) => void
}) {
  const selectOptions = uniqueSelectOptions(options ?? values?.map((item) => ({ id: item, name: formatStage(item) })) ?? [])
  return (
    <label className="select-field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">All</option>
        {selectOptions.map((option) => (
          <option key={option.id} value={option.id}>
            {option.name}
          </option>
        ))}
      </select>
    </label>
  )
}

function uniqueSelectOptions(options: { id: string; name: string }[]) {
  return [...options.reduce<Map<string, { id: string; name: string }>>((summary, option) => {
    if (!summary.has(option.id)) {
      summary.set(option.id, option)
    }
    return summary
  }, new Map()).values()]
}

function SectionLabel({ label }: { label: string }) {
  return <div className="section-label">{label}</div>
}

function FollowUpTable({ rows, selectedIncidentId, onSelect }: {
  rows: FollowUpItem[]
  selectedIncidentId: string | null
  onSelect: (incidentId: string) => void
}) {
  if (!rows.length) {
    return <div className="empty-state">No semantic findings match the current graph filters</div>
  }
  return (
    <div className="followup-table-wrap">
      <table className="followup-table">
        <thead>
          <tr>
            <th scope="col">Rank</th>
            <th scope="col">Priority</th>
            <th scope="col">Incident</th>
            <th scope="col">Asset</th>
            <th scope="col">Zone</th>
            <th scope="col">Blocker</th>
            <th scope="col">Time</th>
            <th scope="col">Action</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              className={row.incident_id === selectedIncidentId ? 'selected' : ''}
              key={row.incident_id}
              tabIndex={0}
              aria-selected={row.incident_id === selectedIncidentId}
              onClick={() => onSelect(row.incident_id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  onSelect(row.incident_id)
                }
              }}
            >
              <td data-label="Rank" className="rank-cell">
                <strong>#{row.priority_rank}</strong>
              </td>
              <td data-label="Priority">
                <span className={`priority-pill ${row.priority_level.toLowerCase()}`}>{formatStage(row.priority_level)}</span>
              </td>
              <td data-label="Incident" className="primary-cell">
                <strong>{row.request_number}</strong>
              </td>
              <td data-label="Asset" className="primary-cell">
                <strong>{row.asset_name}</strong>
              </td>
              <td data-label="Zone" className="primary-cell">
                <span>{row.zone_name}</span>
              </td>
              <td data-label="Blocker" className="primary-cell">
                <strong>{formatStage(row.current_stage)}</strong>
              </td>
              <td data-label="Time" className="primary-cell">
                <span>{formatHours(row.hours_in_current_stage)}</span>
              </td>
              <td data-label="Action" className="queue-detail-action">
                <Link to={`/findings/${row.incident_id}`} onClick={(event) => event.stopPropagation()}>
                  View details
                  <ArrowRight size={15} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function FollowUpDetailPage() {
  const { incidentId } = useParams()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<DetailTab>('summary')
  const [detail, setDetail] = useState<RequestDetail | null>(null)
  const [semanticContext, setSemanticContext] = useState<RequestSemanticContext | null>(null)
  const [topologyDependencies, setTopologyDependencies] = useState<InfrastructureDependency[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    async function loadDetail() {
      if (!incidentId) {
        setError('Semantic finding not found')
        setLoading(false)
        return
      }
      setLoading(true)
      setError(null)
      try {
        const requestDetail = await fetchRequestDetail(incidentId)
        const [dependencies, semantic] = await Promise.all([
          fetchTopologyDependencies(),
          fetchRequestSemanticContext(requestDetail.request.incident_id, requestDetail.request.asset_id),
        ])
        if (!cancelled) {
          setDetail(requestDetail)
          setSemanticContext(semantic)
          setTopologyDependencies(dependencies)
        }
      } catch {
        if (!cancelled) {
          setDetail(null)
          setSemanticContext(null)
          setTopologyDependencies([])
          setError('Semantic finding not found or unavailable')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadDetail()
    return () => {
      cancelled = true
    }
  }, [incidentId, reloadVersion])

  return (
    <main className="app-shell detail-page-shell">
      <header className="topbar detail-page-header">
        <div>
          <p className="eyebrow">Selected semantic finding</p>
          <h1>{detail?.request.request_number ?? incidentId ?? 'Incident details'}</h1>
          <span>{detail?.request.request_title ?? (loading ? 'Loading selected incident' : 'Incident detail unavailable')}</span>
        </div>
        <button className="icon-button" onClick={() => navigate(-1)} title="Back">
          <ArrowLeft size={18} />
        </button>
      </header>

      <Link className="back-link" to="/">
        <ArrowLeft size={16} />
        Back to Semantic Findings
      </Link>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="panel detail-route-panel">
        <nav className="detail-tabs" aria-label="Selected incident detail views" role="tablist">
          {detailTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              data-tab-id={tab.id}
              role="tab"
              className={activeTab === tab.id ? 'active' : ''}
              aria-selected={activeTab === tab.id}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <div className="detail-page-body">
          {loading ? <div className="empty-state">Loading selected incident details</div> : null}
          {!loading && activeTab === 'summary' ? <RequestDetailView detail={detail} semanticContext={semanticContext} topologyDependencies={topologyDependencies} onActionSubmitted={() => setReloadVersion((version) => version + 1)} /> : null}
          {!loading && activeTab === 'impact' ? <ImpactView detail={detail} semanticContext={semanticContext} /> : null}
          {!loading && activeTab === 'trust' ? <RequestTrustView detail={detail} semanticContext={semanticContext} onActionSubmitted={() => setReloadVersion((version) => version + 1)} /> : null}
          {!loading && activeTab === 'dependencies' ? <DependencyDetailView detail={detail} semanticContext={semanticContext} topologyDependencies={topologyDependencies} /> : null}
        </div>
      </section>
    </main>
  )
}

function NotFoundPage() {
  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">AI infrastructure operations</p>
          <h1>Page not found</h1>
        </div>
      </header>
      <Link className="back-link" to="/">
        <ArrowLeft size={16} />
        Back to Semantic Findings
      </Link>
    </main>
  )
}

function RequestDetailView({ detail, semanticContext, topologyDependencies, onActionSubmitted }: {
  detail: RequestDetail | null
  semanticContext: RequestSemanticContext | null
  topologyDependencies: InfrastructureDependency[]
  onActionSubmitted: () => void
}) {
  if (!detail) {
    return <div className="empty-state">Select a semantic finding to inspect the recommended action</div>
  }
  const affectedGpuCount = detail.impact_snapshot?.affected_gpu_count ?? detail.request.affected_gpu_count
  const capacityRiskKw = detail.impact_snapshot?.estimated_capacity_risk_kw ?? detail.request.estimated_capacity_risk_kw
  const evidenceIssueCount = detail.impact_trust_flags.length
  const selectedDependencyRows = selectedDependenciesFor(detail, topologyDependencies)
  return (
    <div className="detail-stack summary-brief">
      <div className="detail-hero">
        <div>
          <strong>{detail.request.request_title}</strong>
          <span>{formatStage(detail.request.priority_level)} priority · blocked at {formatStage(detail.request.current_stage)}</span>
        </div>
        <div className="status-badge-stack">
          <ReadinessBadge status={detail.restore_readiness.status} />
          <TrustBadge status={detail.impact_confidence_status} count={detail.impact_trust_flags.length} />
        </div>
      </div>

      <div className="detail-action brief-action">
        <span>Next operational action</span>
        <strong>{detail.request.recommended_action}</strong>
      </div>

      <OntologyActionPanel detail={detail} placement="summary" onActionSubmitted={onActionSubmitted} />

      <SemanticTracePanel semanticContext={semanticContext} />

      <SemanticExplanationCanvas
        detail={detail}
        semanticContext={semanticContext}
        topologyDependencies={selectedDependencyRows}
      />

      <div className="summary-glance-grid" aria-label="Selected incident at a glance">
        <SummaryMetric label="Asset" value={detail.request.asset_name} />
        <SummaryMetric label="Zone" value={detail.request.zone_name} />
        <SummaryMetric label="Blocker" value={formatStage(detail.request.current_stage)} tone="danger" />
        <SummaryMetric label="Time in stage" value={formatHours(detail.request.hours_in_current_stage)} tone="danger" />
        <SummaryMetric label="Affected GPUs" value={String(affectedGpuCount)} tone={affectedGpuCount > 0 ? 'warning' : undefined} />
        <SummaryMetric label="Capacity risk" value={`${capacityRiskKw.toFixed(0)} kW`} tone={capacityRiskKw > 0 ? 'danger' : undefined} />
        <SummaryMetric
          label="Restore readiness"
          value={restoreReadinessLabel(detail.restore_readiness.status)}
          detail={detail.restore_readiness.summary ?? 'No restore-readiness finding'}
          tone={restoreReadinessTone(detail.restore_readiness.status)}
        />
        <SummaryMetric label="Trust" value={trustStatusLabel(detail.impact_confidence_status)} tone={detail.impact_confidence_status === 'TRUSTED' ? 'ok' : 'warning'} />
        <SummaryMetric label="Evidence issues" value={String(evidenceIssueCount)} tone={evidenceIssueCount ? 'warning' : 'ok'} />
        <SummaryMetric
          label="Ontology validation"
          value={semanticContext?.validation.conforms ? 'Conforms' : 'Review'}
          tone={semanticContext?.validation.conforms ? 'ok' : 'warning'}
        />
        <SummaryMetric
          label="Semantic evidence"
          value={semanticContext?.incidentEvidence.found ? 'Linked' : 'Missing'}
          tone={semanticContext?.incidentEvidence.found ? 'ok' : 'warning'}
        />
      </div>

      <div className="detail-summary">
        <span>Why it matters</span>
        <p>{detail.restore_readiness.summary ?? detail.request.reason_summary}</p>
      </div>

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Recovery blocker evidence</strong>
        <div className="blocker-stage-grid">
          {detail.stage_lead_times.map((stage) => (
            <div className={stage.is_bottleneck ? 'blocker-stage-card bottleneck' : 'blocker-stage-card'} key={`${stage.stage}-${stage.entered_at}`}>
              <span>{formatStage(stage.stage)}</span>
              <strong>{formatHours(stage.duration_hours)}</strong>
              <small>{stage.delay_hours > 0 ? `${formatHours(stage.delay_hours)} over threshold` : `Threshold ${formatHours(stage.threshold_hours)}`}</small>
            </div>
          ))}
        </div>
        <div className="work-order brief-work-orders">
          {detail.work_orders.map((order) => (
            <div key={order.work_order_id}>
              <strong>{order.assigned_team}</strong>
              <span>{formatStage(order.work_order_status)}</span>
              {order.required_spare_name ? <span>{order.required_spare_name} · {formatStage(order.stock_status ?? 'unknown')}</span> : null}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function OntologyActionPanel({ detail, placement, onActionSubmitted }: {
  detail: RequestDetail
  placement: OntologyActionPlacement
  onActionSubmitted: () => void
}) {
  const [selectedAction, setSelectedAction] = useState<OntologyActionAffordance | null>(null)
  const actions = detail.ontology_actions.filter((action) => action.ui_placement.includes(placement))
  const aiProposals = placement === 'summary' ? detail.ai_proposals : []
  if (!actions.length && !aiProposals.length) {
    return null
  }
  return (
    <section className="ontology-action-panel" aria-label={`${placement} ontology action affordances`}>
      <div className="ontology-action-header">
        <Wrench size={17} />
        <div>
          <span>Governed ontology actions</span>
          <strong>{actions.length} request contract{actions.length === 1 ? '' : 's'} and {aiProposals.length} AI proposal{aiProposals.length === 1 ? '' : 's'}</strong>
        </div>
      </div>
      {actions.length ? (
        <div className="ontology-action-grid">
          {actions.map((action) => (
            <OntologyActionCard action={action} key={`${placement}-${action.action_id}`} onRequest={() => setSelectedAction(action)} />
          ))}
        </div>
      ) : null}
      <AiProposalPanel proposals={aiProposals} onReviewed={onActionSubmitted} />
      <OntologyActionLifecycleReviewPanel queue={detail.action_review_queue} onTransitioned={onActionSubmitted} />
      <OntologyActionNotificationPanel notifications={detail.action_notifications} />
      <OntologyActionDispatchQueuePanel dispatches={detail.action_dispatch_queue} />
      <OntologyActionTransitionHistoryPanel history={detail.action_transition_history} />
      <OntologyActionAuditHistoryPanel history={detail.action_audit_history} />
      {selectedAction ? (
        <OntologyActionRequestModal
          action={selectedAction}
          onClose={() => setSelectedAction(null)}
          onSubmitted={() => {
            setSelectedAction(null)
            onActionSubmitted()
          }}
        />
      ) : null}
    </section>
  )
}

function AiProposalPanel({ proposals, onReviewed }: {
  proposals: AiProposalItem[]
  onReviewed: () => void
}) {
  const uniqueProposals = uniqueAiProposals(proposals)
  const [pendingProposal, setPendingProposal] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  if (!uniqueProposals.length) {
    return null
  }
  async function reviewProposal(proposal: AiProposalItem, decision: 'APPROVE' | 'REJECT') {
    setPendingProposal(`${proposal.proposal_uri}:${decision}`)
    setError(null)
    try {
      await submitAiProposalReview({
        proposal_uri: proposal.proposal_uri,
        proposal_id: proposal.proposal_id,
        decision,
        actor_id: 'operator-local-reviewer',
        review_reason: decision === 'APPROVE'
          ? `Human reviewer approved AI proposal ${proposal.proposal_id} for governed local action audit creation.`
          : `Human reviewer rejected AI proposal ${proposal.proposal_id} after local governance review.`,
        action_id: decision === 'APPROVE' && proposal.proposal_type === 'ACTION_RECOMMENDATION'
          ? 'AcknowledgeRestoreBlocker'
          : undefined,
      })
      onReviewed()
    } catch (reviewError) {
      setError(reviewError instanceof Error ? reviewError.message : 'AI proposal review failed.')
    } finally {
      setPendingProposal(null)
    }
  }
  return (
    <div className="ai-proposal-panel" aria-label="AI governance proposals">
      <div className="ontology-action-history-header">
        <ShieldAlert size={16} />
        <div>
          <span>AI governance proposals</span>
          <strong>{uniqueProposals.length} managed ai-audit proposal{uniqueProposals.length === 1 ? '' : 's'} pending review</strong>
        </div>
      </div>
      {error ? <div className="ontology-action-error">{error}</div> : null}
      <div className="ai-proposal-list">
        {uniqueProposals.map((proposal) => {
          const reviewable = proposal.review_status === 'PENDING_HUMAN_REVIEW'
          const approvePending = pendingProposal === `${proposal.proposal_uri}:APPROVE`
          const rejectPending = pendingProposal === `${proposal.proposal_uri}:REJECT`
          return (
            <article key={proposal.proposal_uri} className="ai-proposal-card">
              <div className="ai-proposal-card-header">
                <div>
                  <span>{formatStage(proposal.proposal_type)} · {formatStage(proposal.risk_level)} · {formatStage(proposal.review_status)}</span>
                  <strong>{proposal.summary}</strong>
                </div>
                <span className="history-status">{Math.round(proposal.confidence_score * 100)}%</span>
              </div>
              <p>{proposal.rationale}</p>
              <div className="ontology-action-transition-controls">
                <button
                  type="button"
                  disabled={!reviewable || pendingProposal !== null}
                  title={reviewable ? 'Approve AI proposal and create a governed local action request when applicable.' : proposal.disabled_reason}
                  onClick={() => void reviewProposal(proposal, 'APPROVE')}
                >
                  {approvePending ? 'Approving' : 'Approve'}
                </button>
                <button
                  type="button"
                  disabled={!reviewable || pendingProposal !== null}
                  title={reviewable ? 'Reject AI proposal in the managed ai-audit graph.' : proposal.disabled_reason}
                  onClick={() => void reviewProposal(proposal, 'REJECT')}
                >
                  {rejectPending ? 'Rejecting' : 'Reject'}
                </button>
                <span>{reviewable ? 'Human review writes ai-audit only; approved action recommendations queue action-audit requests.' : proposal.disabled_reason}</span>
              </div>
              <div className="ontology-action-history-links">
                <ActionHistoryLink label="Proposal" uri={proposal.proposal_uri} />
                <ActionHistoryLink label="Evidence" uri={proposal.supporting_evidence_uri} />
                <ActionHistoryLink label="Source" uri={proposal.source_record_uri} />
                <ActionHistoryLink label="Validation" uri={proposal.validation_report_uri} />
              </div>
            </article>
          )
        })}
      </div>
    </div>
  )
}

function OntologyActionCard({ action, onRequest }: {
  action: OntologyActionAffordance
  onRequest: () => void
}) {
  const requestability = ontologyActionRequestability(action)
  return (
    <article className="ontology-action-card" aria-label={`${action.label} ontology action`}>
      <div className="ontology-action-card-header">
        <div>
          <span>{action.action_id}</span>
          <strong>{action.label}</strong>
        </div>
        <button
          type="button"
          disabled={!requestability.requestable}
          title={requestability.reason}
          onClick={onRequest}
        >
          Queue request
        </button>
      </div>
      <p>{action.description}</p>
      <ActionDetailGroup title="Target objects">
        {action.target_objects.length ? (
          action.target_objects.map((targetObject) => (
            <li key={`${action.action_id}-${targetObject.role}-${targetObject.resource_uri}`}>
              <span>{targetObject.role}</span>
              <strong>{targetObject.label}</strong>
              <code>{targetObject.resource_uri}</code>
            </li>
          ))
        ) : (
          <li>
            <span>Missing target</span>
            <strong>No target object URI is available</strong>
          </li>
        )}
      </ActionDetailGroup>
      <ActionDetailGroup title="Required parameters">
        {action.required_parameters.map((parameter) => (
          <li key={`${action.action_id}-parameter-${parameter}`}>
            <code>{parameter}</code>
          </li>
        ))}
      </ActionDetailGroup>
      <ActionDetailGroup title="Preconditions">
        {action.preconditions.map((precondition) => (
          <li key={`${action.action_id}-precondition-${precondition}`}>{precondition}</li>
        ))}
      </ActionDetailGroup>
      <ActionDetailGroup title="Provenance requirements">
        {action.provenance_requirements.map((requirement) => (
          <li key={`${action.action_id}-provenance-${requirement}`}>{requirement}</li>
        ))}
      </ActionDetailGroup>
      <ActionDetailGroup title="Disabled reasons" tone="warning">
        {action.disabled_reasons.map((reason) => (
          <li key={`${action.action_id}-disabled-${reason}`}>{reason}</li>
        ))}
      </ActionDetailGroup>
      <div className="ontology-action-scope">
        <span>Internal governed record</span>
        <strong>{requestability.reason}; no external writeback is attempted.</strong>
      </div>
    </article>
  )
}

function OntologyActionRequestModal({ action, onClose, onSubmitted }: {
  action: OntologyActionAffordance
  onClose: () => void
  onSubmitted: () => void
}) {
  const [actorId, setActorId] = useState('operator-local-reviewer')
  const [reason, setReason] = useState(defaultActionReason(action))
  const [assignedTeam, setAssignedTeam] = useState('OPS_VALIDATION')
  const [assigneeId, setAssigneeId] = useState('')
  const [reviewedStatus, setReviewedStatus] = useState('NEEDS_REVIEW')
  const [reviewSummary, setReviewSummary] = useState(defaultReviewSummary(action))
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const requestability = ontologyActionRequestability(action)

  async function submit() {
    const submission = buildActionSubmission(action, {
      actorId,
      reason,
      assignedTeam,
      assigneeId,
      reviewedStatus,
      reviewSummary,
    })
    if (!submission) {
      setError('Required semantic target object is missing.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await submitOntologyActionRequest(submission)
      onSubmitted()
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Ontology action request failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="ontology-action-modal" role="dialog" aria-modal="true" aria-label={`${action.label} request`}>
        <div className="ontology-action-modal-header">
          <div>
            <span>Controlled ontology action</span>
            <strong>{action.label}</strong>
          </div>
          <button type="button" className="icon-button" onClick={onClose} title="Close">
            ×
          </button>
        </div>
        <div className="ontology-action-modal-grid">
          <label>
            <span>Actor</span>
            <input value={actorId} onChange={(event) => setActorId(event.target.value)} />
          </label>
          <label>
            <span>Reason</span>
            <textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={3} />
          </label>
          {action.action_id === 'AssignEvidenceReview' ? (
            <>
              <label>
                <span>Assigned team</span>
                <input value={assignedTeam} onChange={(event) => setAssignedTeam(event.target.value)} />
              </label>
              <label>
                <span>Assignee</span>
                <input value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)} />
              </label>
            </>
          ) : null}
          {action.action_id === 'RecordValidationReview' ? (
            <>
              <label>
                <span>Reviewed status</span>
                <select value={reviewedStatus} onChange={(event) => setReviewedStatus(event.target.value)}>
                  <option value="NEEDS_REVIEW">Needs review</option>
                  <option value="PASSED">Passed</option>
                  <option value="FAILED">Failed</option>
                  <option value="BLOCKED">Blocked</option>
                  <option value="CONFLICTING_VALIDATION">Conflicting validation</option>
                </select>
              </label>
              <label>
                <span>Review summary</span>
                <textarea value={reviewSummary} onChange={(event) => setReviewSummary(event.target.value)} rows={3} />
              </label>
            </>
          ) : null}
        </div>
        <div className="ontology-action-modal-targets">
          <ActionHistoryLink label="Incident" uri={action.incident_uri} />
          <ActionHistoryLink label="Source record" uri={action.source_record_uri} />
          <ActionHistoryLink label="Primary target" uri={primaryActionTarget(action)?.resource_uri ?? null} />
        </div>
        {error ? <div className="error-banner compact-error">{error}</div> : null}
        <div className="ontology-action-modal-footer">
          <span>{requestability.reason}</span>
          <div>
            <button type="button" onClick={onClose}>Cancel</button>
            <button type="button" disabled={!requestability.requestable || submitting || !actorId.trim() || !reason.trim()} onClick={submit}>
              {submitting ? 'Submitting' : 'Submit request'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function OntologyActionLifecycleReviewPanel({ queue, onTransitioned }: {
  queue: OntologyActionReviewQueueItem[]
  onTransitioned: () => void
}) {
  const reviewItems = uniqueActionReviewQueue(queue)
  const [actorId, setActorId] = useState('operator-local-reviewer')
  const [submittingKey, setSubmittingKey] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function submitTransition(item: OntologyActionReviewQueueItem, transition: ActionLifecycleTransitionOption) {
    setSubmittingKey(`${item.execution_uri}-${transition.toState}`)
    setError(null)
    try {
      await submitOntologyActionTransition({
        target_execution_uri: item.execution_uri,
        to_state: transition.toState,
        actor_id: actorId.trim(),
        transition_reason: transition.reason(item),
      })
      onTransitioned()
    } catch (transitionError) {
      setError(transitionError instanceof Error ? transitionError.message : 'Ontology action transition failed.')
    } finally {
      setSubmittingKey(null)
    }
  }

  return (
    <div className="ontology-action-lifecycle" aria-label="Internal ontology action lifecycle review">
      <div className="ontology-action-history-header">
        <CheckCircle2 size={16} />
        <div>
          <span>Internal lifecycle review</span>
          <strong>{reviewItems.length ? `${reviewItems.length} action${reviewItems.length === 1 ? '' : 's'} awaiting local lifecycle decision` : 'No queued internal action records'}</strong>
        </div>
      </div>
      {reviewItems.length ? (
        <>
          <label className="lifecycle-actor">
            <span>Reviewer</span>
            <input value={actorId} onChange={(event) => setActorId(event.target.value)} />
          </label>
          <div className="ontology-action-lifecycle-list">
            {reviewItems.map((item) => {
              const transitions = actionTransitionOptions(item.current_state)
              return (
                <article className="ontology-action-lifecycle-card" key={item.execution_uri}>
                  <div className="ontology-action-lifecycle-card-header">
                    <div>
                      <span>{item.request_id}</span>
                      <strong>{item.action_type_id}</strong>
                    </div>
                    <span className={`history-status ${actionStateTone(item.current_state)}`}>
                      {formatStage(item.current_state)}
                    </span>
                  </div>
                  <p>{item.action_reason}</p>
                  <div className="ontology-action-history-links">
                    <ActionHistoryLink label="Execution" uri={item.execution_uri} />
                    <ActionHistoryLink label="Notification" uri={item.notification_uri} />
                    <ActionHistoryLink label="Source" uri={item.source_record_uri} />
                  </div>
                  <div className="ontology-action-transition-controls">
                    {transitions.length ? transitions.map((transition) => {
                      const key = `${item.execution_uri}-${transition.toState}`
                      return (
                        <button
                          type="button"
                          key={transition.toState}
                          disabled={Boolean(submittingKey) || !actorId.trim()}
                          onClick={() => submitTransition(item, transition)}
                        >
                          {submittingKey === key ? 'Writing' : transition.label}
                        </button>
                      )
                    }) : (
                      <span>Lifecycle is closed for local review.</span>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        </>
      ) : (
        <div className="empty-state compact-empty">No internal ontology action has been queued for this incident yet.</div>
      )}
      {error ? <div className="error-banner compact-error">{error}</div> : null}
      <div className="ontology-action-boundary-note">
        These controls write only governed action-audit lifecycle state. They do not update canonical, reasoning, operations, production, or external source-system records.
      </div>
    </div>
  )
}

function OntologyActionNotificationPanel({ notifications }: { notifications: OntologyActionNotificationItem[] }) {
  const groupedNotifications = uniqueActionNotifications(notifications)
  return (
    <div className="ontology-action-notifications" aria-label="Ontology action notifications">
      <div className="ontology-action-history-header">
        <AlertTriangle size={16} />
        <div>
          <span>Local action notifications</span>
          <strong>{groupedNotifications.length ? `${groupedNotifications.length} local lifecycle notification${groupedNotifications.length === 1 ? '' : 's'}` : 'No local lifecycle notifications'}</strong>
        </div>
      </div>
      {groupedNotifications.length ? (
        <div className="ontology-action-notification-list">
          {groupedNotifications.map((item) => (
            <article key={item.notification_uri} className="ontology-action-notification-card">
              <div>
                <span>{formatStage(item.notification_status)}</span>
                <strong>{item.action_type_id}</strong>
              </div>
              <p>{item.notification_summary}</p>
              <div className="ontology-action-history-links">
                <ActionHistoryLink label="Notification" uri={item.notification_uri} />
                <ActionHistoryLink label="Execution" uri={item.execution_uri} />
                <ActionHistoryLink label="Target" uri={item.target_object_uri} />
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function OntologyActionDispatchQueuePanel({ dispatches }: { dispatches: OntologyActionDispatchQueueItem[] }) {
  const queuedDispatches = uniqueActionDispatches(dispatches)
  return (
    <div className="ontology-action-dispatches" aria-label="Ontology action dispatch simulation">
      <div className="ontology-action-history-header">
        <ServerCog size={16} />
        <div>
          <span>Simulated operations dispatch</span>
          <strong>{queuedDispatches.length ? `${queuedDispatches.length} internal dispatch record${queuedDispatches.length === 1 ? '' : 's'}` : 'No simulated dispatch records'}</strong>
        </div>
      </div>
      {queuedDispatches.length ? (
        <div className="ontology-action-dispatch-list">
          {queuedDispatches.map((item) => (
            <article key={item.dispatch_uri} className="ontology-action-dispatch-card">
              <div>
                <span>{formatStage(item.dispatch_channel)} · {formatStage(item.dispatch_status)}</span>
                <strong>{item.action_type_id}</strong>
              </div>
              <p>{item.dispatch_summary}</p>
              <div className="ontology-action-history-links">
                <ActionHistoryLink label="Dispatch" uri={item.dispatch_uri} />
                <ActionHistoryLink label="Transition" uri={item.transition_uri} />
                <ActionHistoryLink label="Execution" uri={item.execution_uri} />
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="empty-state compact-empty">Approved local actions have not produced simulated dispatch records for this incident yet.</div>
      )}
    </div>
  )
}

function ActionDetailGroup({ title, tone, children }: {
  title: string
  tone?: 'warning'
  children: ReactNode
}) {
  return (
    <div className={`ontology-action-detail ${tone ?? ''}`}>
      <span>{title}</span>
      <ul>{children}</ul>
    </div>
  )
}

function OntologyActionTransitionHistoryPanel({ history }: { history: OntologyActionTransitionHistoryItem[] }) {
  const transitions = uniqueActionTransitionHistory(history).slice(0, 12)
  return (
    <div className="ontology-action-transition-history" aria-label="Ontology action transition history">
      <div className="ontology-action-history-header">
        <GitBranch size={16} />
        <div>
          <span>Lifecycle transition history</span>
          <strong>{transitions.length ? `${transitions.length} graph-backed transition${transitions.length === 1 ? '' : 's'}` : 'No transition facts for this finding'}</strong>
        </div>
      </div>
      {transitions.length ? (
        <div className="ontology-action-transition-list">
          {transitions.map((item) => (
            <article className="ontology-action-transition-card" key={item.transition_uri}>
              <div>
                <span>{item.generated_at}</span>
                <strong>{formatTransitionEdge(item.from_state, item.to_state)}</strong>
              </div>
              <p>{item.transition_reason}</p>
              <div className="ontology-action-history-links">
                <ActionHistoryLink label="Transition" uri={item.transition_uri} />
                <ActionHistoryLink label="Execution" uri={item.execution_uri} />
                <ActionHistoryLink label="Request" uri={item.request_uri} />
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="empty-state compact-empty">No lifecycle transitions have been written for this selected incident yet.</div>
      )}
    </div>
  )
}

function OntologyActionAuditHistoryPanel({ history }: { history: OntologyActionAuditHistoryItem[] }) {
  const groupedHistory = uniqueActionAuditHistory(history)
  return (
    <div className="ontology-action-audit-history" aria-label="Ontology action audit history">
      <div className="ontology-action-history-header">
        <GitBranch size={16} />
        <div>
          <span>Action audit history</span>
          <strong>{groupedHistory.length ? `${groupedHistory.length} audited execution${groupedHistory.length === 1 ? '' : 's'}` : 'No audited executions for this finding'}</strong>
        </div>
      </div>
      {groupedHistory.length ? (
        <div className="ontology-action-history-list">
          {groupedHistory.map((item) => (
            <article className="ontology-action-history-card" key={item.execution_uri}>
              <div className="ontology-action-history-card-header">
                <div>
                  <span>{item.action_audit_release_id}</span>
                  <strong>{item.action_type_label ?? item.action_type_id}</strong>
                </div>
                <span className={item.validation_status === 'CONFORMS' ? 'history-status ok' : 'history-status warning'}>
                  {formatStage(item.action_status)} · {formatStage(item.validation_status)}
                </span>
              </div>
              <p>{item.action_reason}</p>
              <div className="ontology-action-history-facts">
                <SummaryMetric label="Actor" value={item.actor_id} detail={item.executed_at} />
                <SummaryMetric label="Idempotency" value={item.idempotency_key} detail={item.request_id} />
                <SummaryMetric label="Validation" value={formatStage(item.validation_status)} detail={item.validation_summary ?? 'No validation summary'} tone={item.validation_status === 'CONFORMS' ? 'ok' : 'warning'} />
                <SummaryMetric label="Graph" value={uriTail(item.graph_uri)} detail={item.action_audit_release_id} />
              </div>
              <div className="ontology-action-history-links">
                <ActionHistoryLink label="Execution" uri={item.execution_uri} />
                <ActionHistoryLink label="Request" uri={item.request_uri} />
                <ActionHistoryLink label="Target" uri={item.target_object_uri} />
                <ActionHistoryLink label="Source" uri={item.source_record_uri} />
                <ActionHistoryLink label="Validation report" uri={item.validation_report_uri} />
                <ActionHistoryLink label="Supporting evidence" uri={item.supporting_evidence_uri} />
              </div>
              {item.assigned_team || item.assignee_id || item.reviewed_status || item.review_summary ? (
                <div className="ontology-action-review-note">
                  <span>{[item.assigned_team, item.assignee_id, item.reviewed_status].filter(Boolean).join(' · ')}</span>
                  {item.review_summary ? <strong>{item.review_summary}</strong> : null}
                </div>
              ) : null}
            </article>
          ))}
        </div>
      ) : (
        <div className="empty-state compact-empty">No internal action audit records are linked to this selected incident yet.</div>
      )}
    </div>
  )
}

function ActionHistoryLink({ label, uri }: { label: string; uri: string | null }) {
  if (!uri) {
    return null
  }
  return (
    <div>
      <span>{label}</span>
      <code>{uri}</code>
    </div>
  )
}

function uniqueActionAuditHistory(history: OntologyActionAuditHistoryItem[]): OntologyActionAuditHistoryItem[] {
  const byExecution = new Map<string, OntologyActionAuditHistoryItem>()
  history.forEach((item) => {
    const existing = byExecution.get(item.execution_uri)
    if (!existing || (!existing.target_object_uri && item.target_object_uri)) {
      byExecution.set(item.execution_uri, item)
    }
  })
  return Array.from(byExecution.values()).sort((left, right) => right.executed_at.localeCompare(left.executed_at))
}

function uniqueActionNotifications(notifications: OntologyActionNotificationItem[]): OntologyActionNotificationItem[] {
  const byNotification = new Map<string, OntologyActionNotificationItem>()
  notifications.forEach((item) => {
    const existing = byNotification.get(item.notification_uri)
    if (!existing || (!existing.target_object_uri && item.target_object_uri)) {
      byNotification.set(item.notification_uri, item)
    }
  })
  return Array.from(byNotification.values()).sort((left, right) => right.generated_at.localeCompare(left.generated_at))
}

function uniqueActionDispatches(dispatches: OntologyActionDispatchQueueItem[]): OntologyActionDispatchQueueItem[] {
  const byDispatch = new Map<string, OntologyActionDispatchQueueItem>()
  dispatches.forEach((dispatch) => {
    byDispatch.set(dispatch.dispatch_uri, dispatch)
  })
  return [...byDispatch.values()].sort((left, right) => {
    const timeSort = right.generated_at.localeCompare(left.generated_at)
    if (timeSort !== 0) return timeSort
    return left.dispatch_channel.localeCompare(right.dispatch_channel)
  })
}

function uniqueAiProposals(proposals: AiProposalItem[]): AiProposalItem[] {
  const byProposal = new Map<string, AiProposalItem>()
  proposals.forEach((proposal) => {
    byProposal.set(proposal.proposal_uri, proposal)
  })
  return [...byProposal.values()].sort((left, right) => {
    const timeSort = right.generated_at.localeCompare(left.generated_at)
    if (timeSort !== 0) return timeSort
    return right.confidence_score - left.confidence_score
  })
}

type ActionLifecycleTransitionOption = {
  toState: OntologyActionLifecycleState
  label: string
  reason: (item: OntologyActionReviewQueueItem) => string
}

function uniqueActionReviewQueue(queue: OntologyActionReviewQueueItem[]): OntologyActionReviewQueueItem[] {
  const byExecution = new Map<string, OntologyActionReviewQueueItem>()
  queue.forEach((item) => {
    const existing = byExecution.get(item.execution_uri)
    if (!existing || item.state_generated_at.localeCompare(existing.state_generated_at) > 0) {
      byExecution.set(item.execution_uri, item)
    }
  })
  return Array.from(byExecution.values()).sort((left, right) => right.state_generated_at.localeCompare(left.state_generated_at))
}

function uniqueActionTransitionHistory(history: OntologyActionTransitionHistoryItem[]): OntologyActionTransitionHistoryItem[] {
  const byTransition = new Map<string, OntologyActionTransitionHistoryItem>()
  history.forEach((item) => {
    byTransition.set(item.transition_uri, item)
  })
  return Array.from(byTransition.values()).sort((left, right) => right.generated_at.localeCompare(left.generated_at))
}

function actionTransitionOptions(state: OntologyActionLifecycleState): ActionLifecycleTransitionOption[] {
  if (state === 'QUEUED') {
    return [
      {
        toState: 'IN_REVIEW',
        label: 'Start review',
        reason: (item) => `Local reviewer started internal review for ${item.request_id}.`,
      },
      {
        toState: 'REJECTED',
        label: 'Reject',
        reason: (item) => `Local reviewer rejected queued internal action ${item.request_id}.`,
      },
    ]
  }
  if (state === 'IN_REVIEW') {
    return [
      {
        toState: 'APPROVED',
        label: 'Approve',
        reason: (item) => `Local reviewer approved internal action ${item.request_id}.`,
      },
      {
        toState: 'REJECTED',
        label: 'Reject',
        reason: (item) => `Local reviewer rejected internal action ${item.request_id}.`,
      },
    ]
  }
  if (state === 'APPROVED' || state === 'REJECTED') {
    return [
      {
        toState: 'CLOSED',
        label: 'Close',
        reason: (item) => `Local reviewer closed internal action ${item.request_id} after ${item.current_state}.`,
      },
    ]
  }
  return []
}

function actionStateTone(state: OntologyActionLifecycleState): 'ok' | 'warning' | 'danger' | '' {
  if (state === 'APPROVED' || state === 'CLOSED') return 'ok'
  if (state === 'REJECTED') return 'danger'
  if (state === 'QUEUED' || state === 'IN_REVIEW') return 'warning'
  return ''
}

function formatTransitionEdge(fromState: OntologyActionLifecycleState | null, toState: OntologyActionLifecycleState) {
  return `${fromState ? formatStage(fromState) : 'Created'} -> ${formatStage(toState)}`
}

function ontologyActionRequestability(action: OntologyActionAffordance): { requestable: boolean; reason: string } {
  if (!['AcknowledgeRestoreBlocker', 'AssignEvidenceReview', 'RecordValidationReview'].includes(action.action_id)) {
    return {
      requestable: false,
      reason: 'This action is not accepted by the internal action request endpoint.',
    }
  }
  const submission = buildActionSubmission(action, {
    actorId: 'operator-local-reviewer',
    reason: defaultActionReason(action),
    assignedTeam: 'OPS_VALIDATION',
    assigneeId: '',
    reviewedStatus: 'NEEDS_REVIEW',
    reviewSummary: defaultReviewSummary(action),
  })
  if (!submission) {
    return {
      requestable: false,
      reason: 'Required semantic target or provenance URI is missing.',
    }
  }
  return {
    requestable: true,
    reason: 'Creates an audited local request and notification only.',
  }
}

function buildActionSubmission(
  action: OntologyActionAffordance,
  values: {
    actorId: string
    reason: string
    assignedTeam: string
    assigneeId: string
    reviewedStatus: string
    reviewSummary: string
  },
): OntologyActionSubmission | null {
  const base = {
    action_id: action.action_id,
    actor_id: values.actorId.trim(),
    action_reason: values.reason.trim(),
    incident_uri: action.incident_uri,
    source_record_uri: action.source_record_uri,
  }
  if (!base.actor_id || !base.action_reason || !base.incident_uri || !base.source_record_uri) {
    return null
  }
  if (action.action_id === 'AcknowledgeRestoreBlocker') {
    const restoreReadiness = actionTarget(action, 'RestoreReadinessFinding')
    if (!restoreReadiness) return null
    return {
      ...base,
      restore_readiness_finding_uri: restoreReadiness.resource_uri,
      recovery_blocker_uri: actionTarget(action, 'RecoveryBlocker')?.resource_uri,
    }
  }
  if (action.action_id === 'AssignEvidenceReview') {
    const trustFinding = actionTarget(action, 'TrustFinding')
    if (!trustFinding || !values.assignedTeam.trim()) return null
    return {
      ...base,
      trust_finding_uri: trustFinding.resource_uri,
      assigned_team: values.assignedTeam.trim(),
      assignee_id: values.assigneeId.trim() || undefined,
    }
  }
  if (action.action_id === 'RecordValidationReview') {
    const validationEvidence = actionTarget(action, 'ValidationEvidence')
    if (!validationEvidence || !values.reviewedStatus.trim() || !values.reviewSummary.trim()) return null
    return {
      ...base,
      validation_evidence_uri: validationEvidence.resource_uri,
      reviewed_status: values.reviewedStatus.trim(),
      review_summary: values.reviewSummary.trim(),
      supporting_evidence_uri: validationEvidence.resource_uri,
    }
  }
  return null
}

function defaultActionReason(action: OntologyActionAffordance): string {
  if (action.action_id === 'AcknowledgeRestoreBlocker') {
    return 'Operator reviewed the restore-readiness blocker for local follow-up.'
  }
  if (action.action_id === 'AssignEvidenceReview') {
    return 'Assign evidence trust finding to local validation review.'
  }
  if (action.action_id === 'RecordValidationReview') {
    return 'Record local review of validation evidence without changing canonical facts.'
  }
  return 'Request local ontology action audit.'
}

function defaultReviewSummary(action: OntologyActionAffordance): string {
  if (action.action_id === 'RecordValidationReview') {
    return 'Validation evidence requires follow-up review before restore decision.'
  }
  return ''
}

function actionTarget(action: OntologyActionAffordance, role: string) {
  return action.target_objects.find((target) => target.role === role && target.resource_uri.startsWith('urn:dcai:')) ?? null
}

function primaryActionTarget(action: OntologyActionAffordance) {
  return actionTarget(action, 'RestoreReadinessFinding') ??
    actionTarget(action, 'TrustFinding') ??
    actionTarget(action, 'ValidationEvidence') ??
    actionTarget(action, 'InfrastructureIncident')
}

function SemanticExplanationCanvas({ detail, semanticContext, topologyDependencies }: {
  detail: RequestDetail
  semanticContext: RequestSemanticContext | null
  topologyDependencies: InfrastructureDependency[]
}) {
  const paths = buildTopologyPaths(topologyDependencies)
  const activeIncidentCount = paths.reduce((total, path) => total + path.activeIncidentCount, 0)
  const latestTimeline = [...detail.timeline]
    .filter((event) => event.occurred_at || event.message || event.source_record_uri)
    .sort((left, right) => right.occurred_at.localeCompare(left.occurred_at))
    .slice(0, 6)
  return (
    <section className="semantic-explanation-canvas" aria-label="Semantic explanation canvas">
      <div className="explanation-panel reasoning-panel">
        <div className="explanation-panel-header">
          <ShieldAlert size={17} />
          <div>
            <span>Restore-readiness reasoning</span>
            <strong>{restoreReadinessLabel(detail.restore_readiness.status)}</strong>
          </div>
        </div>
        <p className="reasoning-copy">{detail.restore_readiness.summary ?? detail.request.reason_summary}</p>
        <EvidenceRows
          items={[
            {
              label: 'Active blocker',
              value: formatStage(detail.request.current_stage),
              detail: `${formatHours(detail.request.hours_in_current_stage)} in stage`,
              tone: detail.restore_readiness.status === 'NOT_READY' ? 'danger' : 'warning',
            },
            {
              label: 'Work-order state',
              value: detail.work_orders[0] ? formatStage(detail.work_orders[0].work_order_status) : 'No work order',
              detail: detail.work_orders[0]?.assigned_team ?? 'No assigned team evidence',
              tone: detail.restore_readiness.status === 'NOT_READY' ? 'warning' : undefined,
            },
          ]}
        />
      </div>

      <div className="explanation-panel dependency-panel">
        <div className="explanation-panel-header">
          <Network size={17} />
          <div>
            <span>Dependency and blast-radius path</span>
            <strong>{paths.length ? `${paths.length} semantic path${paths.length === 1 ? '' : 's'}` : 'No path evidence'}</strong>
          </div>
        </div>
        <FactStrip
          ariaLabel="Selected incident semantic dependency explanation"
          items={[
            {
              label: 'Direct graph edges',
              value: String(semanticContext?.dependencyImpact.direct_dependency_count ?? 0),
              detail: 'Asset dependency assertions',
              tone: (semanticContext?.dependencyImpact.direct_dependency_count ?? 0) ? 'warning' : undefined,
            },
            {
              label: 'Path incidents',
              value: String(activeIncidentCount),
              detail: 'Incidents on displayed dependency paths',
              tone: activeIncidentCount ? 'warning' : 'ok',
            },
            {
              label: 'Blast-radius incidents',
              value: String(semanticContext?.blastRadius.affected_incident_count ?? 0),
              detail: `${semanticContext?.blastRadius.inferred_downstream_assets.length ?? 0} inferred downstream assets`,
              tone: (semanticContext?.blastRadius.affected_incident_count ?? 0) ? 'warning' : 'ok',
            },
          ]}
        />
        <TopologyRows rows={topologyDependencies} />
      </div>

      <div className="explanation-panel">
        <div className="explanation-panel-header">
          <Clock3 size={17} />
          <div>
            <span>Evidence timeline</span>
            <strong>{latestTimeline.length ? `${latestTimeline.length} latest graph facts` : 'No dated evidence'}</strong>
          </div>
        </div>
        {latestTimeline.length ? (
          <div className="semantic-timeline-list">
            {latestTimeline.map((event, index) => (
              <div className="semantic-timeline-event" key={`${event.event_id}-${event.occurred_at}-${index}`}>
                <span>{event.occurred_at || 'No timestamp'}</span>
                <strong>{formatStage(event.event_type)} · {formatStage(event.event_status)}</strong>
                <small>{event.message ?? formatStage(event.stage)}</small>
                {event.source_record_uri ? <code>{event.source_record_uri}</code> : null}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">No timeline evidence is attached to this semantic finding</div>
        )}
      </div>

      <DynamicPlaybackPanel detail={detail} />

      <div className="explanation-panel">
        <div className="explanation-panel-header">
          <GitBranch size={17} />
          <div>
            <span>Provenance chain</span>
            <strong>{detail.provenance_trace.length ? `${detail.provenance_trace.length} linked resources` : 'No provenance links'}</strong>
          </div>
        </div>
        {detail.provenance_trace.length ? (
          <div className="provenance-chain">
            {detail.provenance_trace.map((item, index) => (
              <div className="provenance-step" data-step={index + 1} key={item.resource_uri}>
                <span>{item.step}</span>
                <strong>{item.label}</strong>
                <small>{item.detail}</small>
                <code>{item.resource_uri}</code>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">No provenance chain is available for this finding</div>
        )}
      </div>
    </section>
  )
}

function DynamicPlaybackPanel({ detail }: { detail: RequestDetail }) {
  const timeline = [...detail.dynamic_event_timeline]
    .sort((left, right) => left.playback_step - right.playback_step || left.occurred_at.localeCompare(right.occurred_at))
  const latestReasoning = detail.dynamic_reasoning_changes.at(-1)
  const latestAction = detail.dynamic_action_lifecycle.at(-1)
  return (
    <div className="explanation-panel dynamic-playback-panel">
      <div className="explanation-panel-header">
        <Activity size={17} />
        <div>
          <span>Dynamic ontology playback</span>
          <strong>{timeline.length ? `${timeline.length} replayed graph event${timeline.length === 1 ? '' : 's'}` : 'No playback events'}</strong>
        </div>
      </div>
      {timeline.length ? (
        <>
          <div className="dynamic-state-grid" aria-label="Dynamic playback state summary">
            <div>
              <span>Reasoning</span>
              <strong>{latestReasoning ? formatStage(latestReasoning.after_reasoning_state) : 'No reasoning delta'}</strong>
              <small>{latestReasoning ? formatStage(latestReasoning.after_trust_state) : 'No trust delta'}</small>
            </div>
            <div>
              <span>Blast radius</span>
              <strong>{latestReasoning ? `${latestReasoning.before_blast_radius_count} to ${latestReasoning.after_blast_radius_count}` : '0 to 0'}</strong>
              <small>Inferred downstream exposure</small>
            </div>
            <div>
              <span>Action lifecycle</span>
              <strong>{latestAction ? formatStage(latestAction.action_lifecycle_state) : 'No action delta'}</strong>
              <small>{latestAction?.action_audit_release_id ?? 'No playback action audit'}</small>
            </div>
          </div>
          <div className="dynamic-playback-list">
            {timeline.map((event) => (
              <div className="dynamic-playback-event" key={event.event_uri}>
                <div className="dynamic-playback-step">
                  <span>Step {event.playback_step}</span>
                  <strong>{formatStage(event.event_kind)}</strong>
                  <small>{event.occurred_at}</small>
                </div>
                <div className="dynamic-playback-change">
                  <span>{formatStage(event.source_family)}</span>
                  <strong>{formatStage(event.before_state)} to {formatStage(event.after_state)}</strong>
                  <small>{event.summary}</small>
                </div>
                <div className="dynamic-playback-change">
                  <span>Reasoning and trust</span>
                  <strong>{formatStage(event.before_reasoning_state)} to {formatStage(event.after_reasoning_state)}</strong>
                  <small>{formatStage(event.before_trust_state)} to {formatStage(event.after_trust_state)} · blast radius {event.before_blast_radius_count} to {event.after_blast_radius_count}</small>
                </div>
                <code>{event.source_record_uri}</code>
              </div>
            ))}
          </div>
        </>
      ) : (
        <div className="empty-state compact-empty">No dynamic playback graph facts are attached to this incident</div>
      )}
    </div>
  )
}

function SemanticTracePanel({ semanticContext }: { semanticContext: RequestSemanticContext | null }) {
  const validationConforms = Boolean(semanticContext?.validation.conforms)
  const evidenceLinked = Boolean(semanticContext?.incidentEvidence.found)
  const dependencyCount = semanticContext?.dependencyImpact.direct_dependency_count ?? 0
  const blastRadiusCount = semanticContext?.blastRadius.affected_incident_count ?? 0
  const inferredDownstreamCount = semanticContext?.blastRadius.inferred_downstream_assets.length ?? 0
  return (
    <section className="semantic-trace-panel" aria-label="Semantic evidence trace">
      <div className="semantic-trace-heading">
        <GitBranch size={17} />
        <div>
          <span>Semantic evidence trace</span>
          <strong>{evidenceLinked ? 'RDF incident evidence is linked to this finding' : 'RDF incident evidence needs review'}</strong>
        </div>
      </div>
      <div className="semantic-trace-grid">
        <SummaryMetric
          label="SHACL contract"
          value={validationConforms ? 'Conforms' : 'Review'}
          detail={`${semanticContext?.validation.issue_count ?? 0} validation issues`}
          tone={validationConforms ? 'ok' : 'warning'}
        />
        <SummaryMetric
          label="Incident evidence"
          value={evidenceLinked ? 'Linked' : 'Missing'}
          detail={semanticContext?.incidentEvidence.workflow_stage ? formatStage(semanticContext.incidentEvidence.workflow_stage) : 'No workflow assertion'}
          tone={evidenceLinked ? 'ok' : 'warning'}
        />
        <SummaryMetric
          label="Dependency assertions"
          value={String(dependencyCount)}
          detail="Direct asset dependency edges"
          tone={dependencyCount ? 'warning' : undefined}
        />
        <SummaryMetric
          label="Blast-radius evidence"
          value={String(blastRadiusCount)}
          detail={`${inferredDownstreamCount} inferred downstream assets`}
          tone={blastRadiusCount ? 'warning' : 'ok'}
        />
      </div>
    </section>
  )
}

function SummaryMetric({ label, value, detail, tone }: {
  label: string
  value: string
  detail?: string
  tone?: 'ok' | 'warning' | 'danger'
}) {
  return (
    <div className={`summary-metric ${tone ?? ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      {detail ? <small>{detail}</small> : null}
    </div>
  )
}

function ImpactView({ detail, semanticContext }: {
  detail: RequestDetail | null
  semanticContext: RequestSemanticContext | null
}) {
  if (!detail) {
    return <div className="empty-state">Select a semantic finding to inspect impact context</div>
  }
  const impact = detail.impact_snapshot
  return (
    <div className="detail-stack summary-brief">
      <div className="detail-hero">
        <div>
          <strong>Operational impact</strong>
          <span>{detail.request.request_number} · {detail.request.asset_name}</span>
        </div>
        <TrustBadge status={detail.impact_confidence_status} count={detail.impact_trust_flags.length} />
      </div>

      <div className="detail-action brief-action">
        <span>Impact question</span>
        <strong>{impact ? `${impact.affected_gpu_count} GPUs and ${impact.estimated_capacity_risk_kw.toFixed(0)} kW at risk` : 'No impact snapshot is available for this incident'}</strong>
      </div>

      {impact ? (
        <>
          <FactStrip
            ariaLabel="Selected incident impact at a glance"
            items={[
              { label: 'Redundancy', value: redundancyLabel(impact.redundancy_state), detail: `${impact.affected_rack_count} affected rack${impact.affected_rack_count === 1 ? '' : 's'}`, tone: redundancyTone(impact.redundancy_state) },
              { label: 'Capacity risk', value: `${impact.estimated_capacity_risk_kw.toFixed(0)} kW`, detail: `${impact.estimated_gpu_capacity_risk_pct.toFixed(1)}% GPU capacity risk`, tone: capacityRiskTone(impact.estimated_capacity_risk_kw, impact.estimated_gpu_capacity_risk_pct) },
              { label: 'Affected GPUs', value: String(impact.affected_gpu_count), detail: impact.source_system },
              { label: 'Thermal breach', value: `${impact.thermal_breach_minutes}m`, detail: 'Thermal exposure window', tone: impact.thermal_breach_minutes > 0 ? 'warning' : undefined },
              { label: 'Semantic asset link', value: semanticContext?.incidentEvidence.asset_id ?? 'Missing', detail: 'RDF incident-to-asset assertion', tone: semanticContext?.incidentEvidence.found ? undefined : 'warning' },
            ]}
          />

          <div className="detail-section evidence-section">
            <strong className="detail-section-title">Operational state evidence</strong>
            <EvidenceRows
              items={[
                { label: 'Vendor state', value: formatStage(impact.vendor_status), detail: impact.vendor_eta_at ? `ETA ${impact.vendor_eta_at}` : 'No vendor ETA recorded', tone: vendorStatusTone(impact.vendor_status) },
                { label: 'Mitigation', value: formatStage(impact.mitigation_status), detail: impact.mitigation_status === 'RUNNING_DEGRADED' ? 'Service remains degraded' : 'Mitigation status from impact snapshot', tone: mitigationTone(impact.mitigation_status) },
                { label: 'Power redundancy', value: impact.power_redundancy_lost ? 'Lost' : 'Available', detail: 'Power path redundancy', tone: impact.power_redundancy_lost ? 'danger' : undefined },
                { label: 'Cooling redundancy', value: impact.cooling_redundancy_lost ? 'Lost' : 'Available', detail: 'Cooling path redundancy', tone: impact.cooling_redundancy_lost ? 'danger' : undefined },
              ]}
            />
          </div>

          <div className="detail-section evidence-section">
            <strong className="detail-section-title">Telemetry evidence</strong>
            {impact.telemetry_readings.length ? (
              <EvidenceRows
                items={impact.telemetry_readings.map((reading) => ({
                  label: formatStage(reading.metric),
                  value: `${reading.value} ${reading.unit}`,
                  detail: formatStage(reading.status),
                  tone: telemetryTone(reading.status),
                }))}
              />
            ) : (
              <div className="empty-state compact-empty">No telemetry readings are attached to this impact snapshot</div>
            )}
          </div>
        </>
      ) : (
        <div className="empty-state compact-empty">No impact snapshot for the selected incident</div>
      )}

      <div className="detail-section evidence-section secondary-evidence">
        <strong className="detail-section-title">Priority score inputs</strong>
        <div className="score-list">
          <Score label="Downtime" value={detail.request.downtime_score} />
          <Score label="Stage delay" value={detail.request.stage_delay_score} />
          <Score label="Recovery blocker risk" value={detail.request.spare_risk_score} />
          <Score label="Capacity risk" value={detail.request.capacity_risk_score} />
          <Score label="Redundancy risk" value={detail.request.redundancy_risk_score} />
          <Score label="Vendor/parts risk" value={detail.request.vendor_eta_risk_score} />
          <Score label="Mitigation credit" value={detail.request.mitigation_credit_score} />
        </div>
      </div>
    </div>
  )
}

function RequestTrustView({ detail, semanticContext, onActionSubmitted }: {
  detail: RequestDetail | null
  semanticContext: RequestSemanticContext | null
  onActionSubmitted: () => void
}) {
  if (!detail) {
    return <div className="empty-state">Select a semantic finding to review evidence trust</div>
  }
  const validationSummaryText = validationStatusSummary(detail.validation_results)
  const trustNeedsReview = detail.impact_confidence_status !== 'TRUSTED' || detail.impact_trust_flags.length > 0 || detail.quality_flags.length > 0
  return (
    <div className="detail-stack summary-brief">
      <div className="detail-hero">
        <div>
          <strong>Recommendation trust</strong>
          <span>{detail.request.request_number} · evidence confidence for selected finding</span>
        </div>
        <div className="status-badge-stack">
          <ReadinessBadge status={detail.restore_readiness.status} />
          <TrustBadge status={detail.impact_confidence_status} count={detail.impact_trust_flags.length} />
        </div>
      </div>

      <div className="detail-action brief-action">
        <span>Trust question</span>
        <strong>{detail.restore_readiness.status === 'NOT_READY' ? 'Do not restore until readiness blockers are cleared' : trustNeedsReview ? 'Review evidence before relying on this recommendation' : 'Recommendation evidence is trusted for the latest analysis run'}</strong>
      </div>

      <OntologyActionPanel detail={detail} placement="trust" onActionSubmitted={onActionSubmitted} />

      <div className="summary-glance-grid" aria-label="Selected incident trust at a glance">
        <SummaryMetric
          label="Restore readiness"
          value={restoreReadinessLabel(detail.restore_readiness.status)}
          detail={detail.restore_readiness.summary ?? 'No restore-readiness finding'}
          tone={restoreReadinessTone(detail.restore_readiness.status)}
        />
        <SummaryMetric label="Impact confidence" value={trustStatusLabel(detail.impact_confidence_status)} detail="Latest impact evidence check" tone={detail.impact_confidence_status === 'TRUSTED' ? 'ok' : 'warning'} />
        <SummaryMetric label="Evidence issues" value={String(detail.impact_trust_flags.length)} detail="Impact evidence flags" tone={detail.impact_trust_flags.length ? 'warning' : 'ok'} />
        <SummaryMetric label="Source quality" value={String(detail.quality_flags.length)} detail="Incident source flags" tone={detail.quality_flags.length ? 'danger' : 'ok'} />
        <SummaryMetric label="Validation records" value={String(detail.validation_results.length)} detail={validationSummaryText} tone={validationTone(validationSummaryText)} />
        <SummaryMetric
          label="Ontology validation"
          value={semanticContext?.validation.conforms ? 'Conforms' : 'Review'}
          detail={`${semanticContext?.validation.issue_count ?? 0} SHACL issues`}
          tone={semanticContext?.validation.conforms ? 'ok' : 'warning'}
        />
        <SummaryMetric
          label="Semantic trust links"
          value={String(semanticContext?.incidentEvidence.trust_issue_ids.length ?? 0)}
          detail="Trust issues linked in RDF graph"
          tone={(semanticContext?.incidentEvidence.trust_issue_ids.length ?? 0) ? 'warning' : 'ok'}
        />
      </div>

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Ontology validation evidence</strong>
        {semanticContext?.validation.issues.length ? (
          <div className="brief-card-grid">
            {semanticContext.validation.issues.map((issue, index) => (
              <div className="brief-evidence-card warning" key={`${issue.focus_node}-${issue.result_path}-${issue.message}-${index}`}>
                <strong>{issue.focus_node}</strong>
                <span>{issue.message}</span>
                <small>{issue.result_path} · {issue.severity}</small>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">RDF graph conforms to the current SHACL ontology contract</div>
        )}
      </div>

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Impact evidence review</strong>
        {detail.impact_trust_flags.length ? (
          <div className="brief-card-grid">
            {detail.impact_trust_flags.map((flag, index) => (
              <div className="brief-evidence-card warning" key={`${flag.issue_type}-${flag.message}-${index}`}>
                <strong>{trustIssueLabel(flag.issue_type)}</strong>
                <span>{flag.message}</span>
                {Object.keys(flag.evidence).length ? <small>{formatEvidence(flag.evidence)}</small> : null}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">Impact evidence matches the latest analysis run</div>
        )}
      </div>

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Source quality evidence</strong>
        {detail.quality_flags.length ? (
          <div className="brief-card-grid" aria-label="Request quality flags">
            {detail.quality_flags.map((flag) => (
              <div className="brief-evidence-card danger" key={flag}>
                <strong>{trustIssueLabel(flag)}</strong>
                <span>Source quality flag is attached to this selected incident</span>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">No source quality flags were found for this selected incident</div>
        )}
      </div>

      <div className="detail-section evidence-section secondary-evidence">
        <strong className="detail-section-title">Validation evidence</strong>
        {detail.validation_results.length ? (
          <div className="brief-card-grid">
            {detail.validation_results.map((validation, index) => (
              <div className={`brief-evidence-card ${validation.validation_status === 'PASSED' ? 'ok' : 'warning'}`} key={`${validation.validation_id}-${index}`}>
                <strong>{formatStage(validation.validation_status)}</strong>
                <span>{validation.validator_id ?? 'No validator assigned'}</span>
                {validation.failure_reason ? <small>{validation.failure_reason}</small> : null}
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">No validation records are attached to this incident</div>
        )}
      </div>
    </div>
  )
}

function DependencyDetailView({ detail, semanticContext, topologyDependencies }: {
  detail: RequestDetail | null
  semanticContext: RequestSemanticContext | null
  topologyDependencies: InfrastructureDependency[]
}) {
  if (!detail) {
    return <div className="empty-state">Select a semantic finding to compare dependency context against the active blocker</div>
  }
  const selectedDependencyRows = selectedDependenciesFor(detail, topologyDependencies)
  const paths = buildTopologyPaths(selectedDependencyRows)
  const activeIncidentCount = paths.reduce((total, path) => total + path.activeIncidentCount, 0)
  return (
    <div className="detail-stack summary-brief">
      <div className="detail-hero">
        <div>
          <strong>Dependency impact</strong>
          <span>{formatStage(detail.request.current_stage)} · {detail.request.asset_name}</span>
        </div>
        <TrustBadge status={detail.impact_confidence_status} count={detail.impact_trust_flags.length} />
      </div>

      <div className="detail-action brief-action">
        <span>Dependency question</span>
        <strong>Does this blocker expose power, cooling, redundancy, or GPU capacity risk?</strong>
      </div>

      <FactStrip
        ariaLabel="Selected incident dependency impact at a glance"
        items={[
          { label: 'Dependency paths', value: String(paths.length), detail: 'Power, cooling, and telemetry paths' },
          { label: 'Path incidents', value: String(activeIncidentCount), detail: 'Active incidents on paths', tone: activeIncidentCount > 0 ? 'warning' : undefined },
          { label: 'Capacity risk', value: `${detail.request.estimated_capacity_risk_kw.toFixed(0)} kW`, detail: `${detail.request.affected_gpu_count} affected GPUs`, tone: capacityRiskTone(detail.request.estimated_capacity_risk_kw) },
          { label: 'Redundancy', value: redundancyLabel(detail.request.redundancy_state), detail: 'Selected incident redundancy state', tone: redundancyTone(detail.request.redundancy_state) },
          { label: 'Inferred downstream', value: String(semanticContext?.blastRadius.inferred_downstream_assets.length ?? 0), detail: 'SPARQL blast-radius traversal', tone: (semanticContext?.blastRadius.inferred_downstream_assets.length ?? 0) ? 'warning' : undefined },
        ]}
      />

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Semantic dependency evidence</strong>
        <EvidenceRows
          items={[
            {
              label: 'Direct graph edges',
              value: String(semanticContext?.dependencyImpact.direct_dependency_count ?? 0),
              detail: 'Incident asset dependency assertions',
              tone: (semanticContext?.dependencyImpact.direct_dependency_count ?? 0) ? 'warning' : undefined,
            },
            {
              label: 'Blast-radius incidents',
              value: String(semanticContext?.blastRadius.affected_incident_count ?? 0),
              detail: 'Incidents on selected asset or inferred downstream assets',
              tone: (semanticContext?.blastRadius.affected_incident_count ?? 0) ? 'warning' : undefined,
            },
          ]}
        />
      </div>

      <div className="detail-section evidence-section">
        <strong className="detail-section-title">Graph dependency paths</strong>
        <TopologyRows rows={selectedDependencyRows} />
      </div>
    </div>
  )
}

function selectedDependenciesFor(detail: RequestDetail, topologyDependencies: InfrastructureDependency[]) {
  return topologyDependencies.filter((dependency) =>
    dependency.dependent_asset_id === detail.request.asset_id ||
    dependency.dependency_asset_id === detail.request.asset_id,
  )
}

function Score({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value.toFixed(1)}</strong>
    </div>
  )
}

type BriefFact = {
  label: string
  value: string
  detail?: string
  tone?: 'ok' | 'warning' | 'danger'
}

function FactStrip({ ariaLabel, items }: { ariaLabel: string; items: BriefFact[] }) {
  return (
    <div className="metric-card-grid" aria-label={ariaLabel}>
      {items.map((item) => (
        <div className={item.tone ?? undefined} key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          {item.detail ? <small>{item.detail}</small> : null}
        </div>
      ))}
    </div>
  )
}

function EvidenceRows({ items }: { items: BriefFact[] }) {
  return (
    <div className="evidence-compact-grid">
      {items.map((item) => (
        <div className={item.tone ?? undefined} key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          {item.detail ? <small>{item.detail}</small> : null}
        </div>
      ))}
    </div>
  )
}

function TrustBadge({ status, count }: { status: string; count: number }) {
  return (
    <span className={`trust-badge ${trustTone(status)}`}>
      {trustStatusLabel(status)}
      {count ? ` ${count}` : ''}
    </span>
  )
}

function ReadinessBadge({ status }: { status: string }) {
  return (
    <span className={`trust-badge ${readinessBadgeTone(status)}`}>
      {restoreReadinessLabel(status)}
    </span>
  )
}

function statusSignalTone(status: string): 'ok' | 'warning' | 'danger' {
  const normalized = status.toUpperCase()
  if (normalized.includes('FAILED') || normalized.includes('ERROR')) return 'danger'
  if (normalized.includes('WARNING') || normalized.includes('UNAVAILABLE') || normalized.includes('LOADING')) return 'warning'
  return 'ok'
}

function qualitySignalTone(status: string): 'ok' | 'warning' | 'danger' {
  const normalized = status.toUpperCase()
  if (normalized.includes('FAILED') || normalized.includes('ERROR')) return 'danger'
  if (normalized.includes('WARNING') || normalized.includes('REVIEW') || normalized.includes('UNAVAILABLE') || normalized.includes('LOADING')) return 'warning'
  return 'ok'
}

function TopologyRows({ rows }: { rows: InfrastructureDependency[] }) {
  if (!rows.length) {
    return <div className="empty-state">No dependency evidence</div>
  }
  const paths = buildTopologyPaths(rows)
  if (!paths.length) {
    return <div className="empty-state">No configured power or cooling dependency paths match the selected evidence</div>
  }
  return (
    <div className="topology-path-list">
      {paths.map((path) => (
        <div className="topology-path" key={path.id}>
          <div className="topology-path-header">
            <Network size={16} />
            <strong>{path.label}</strong>
            <span>{path.activeIncidentCount} active incident{path.activeIncidentCount === 1 ? '' : 's'}</span>
          </div>
          <div className="topology-node-row">
            {path.nodes.map((node, index) => (
              <div className="topology-node-group" data-step={index + 1} key={`${path.id}-${node.assetId}-${index}`}>
                <div className={`topology-node ${statusTone(node.status)}`}>
                  <small>Step {index + 1}</small>
                  <strong>{node.assetName}</strong>
                  <span>{formatStage(node.status)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

type TopologyPath = {
  id: string
  label: string
  nodes: TopologyNode[]
  activeIncidentCount: number
}

type TopologyNode = {
  assetId: string
  assetName: string
  status: string
  activeIncidentCount: number
}

function buildTopologyPaths(rows: InfrastructureDependency[]): TopologyPath[] {
  const pathGroups = rows.reduce<Map<string, InfrastructureDependency[]>>((summary, row) => {
    const key = row.dependency_role || row.dependency_type || row.dependency_id
    const edges = summary.get(key) ?? []
    edges.push(row)
    summary.set(key, edges)
    return summary
  }, new Map())
  return [...pathGroups.entries()]
    .map(([id, edges]) => {
      const nodes: TopologyNode[] = []
      edges.forEach((edge, index) => {
        if (index === 0) {
          nodes.push({
            assetId: edge.dependent_asset_id,
            assetName: edge.dependent_asset_name,
            status: edge.dependent_status,
            activeIncidentCount: edge.dependent_active_incident_count,
          })
        }
        nodes.push({
          assetId: edge.dependency_asset_id,
          assetName: edge.dependency_asset_name,
          status: edge.dependency_status,
          activeIncidentCount: edge.dependency_active_incident_count,
        })
      })
      const activeIncidentCount = nodes.reduce((total, node) => total + node.activeIncidentCount, 0)
      return {
        id,
        label: dependencyPathLabel(edges[0]),
        nodes,
        activeIncidentCount,
      }
    })
    .sort((left, right) => right.activeIncidentCount - left.activeIncidentCount || left.label.localeCompare(right.label))
}

function dependencyPathLabel(edge: InfrastructureDependency): string {
  const role = edge.dependency_role ? formatStage(edge.dependency_role) : 'Dependency'
  const scope = edge.impact_scope && edge.impact_scope !== 'unknown' ? ` · ${formatStage(edge.impact_scope)}` : ''
  return `${role}${scope}`
}

function formatHours(value: number) {
  return `${value.toFixed(value >= 100 ? 0 : 1)}h`
}

function formatStage(value: string) {
  return value
    .toLowerCase()
    .split(/[_-]+/)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function uriTail(value: string) {
  return value.split(/[#:]/).filter(Boolean).pop() ?? value
}

function trustStatusLabel(status: string) {
  if (status === 'TRUSTED') return 'Trusted'
  if (status === 'WARNING') return 'Review evidence'
  return 'Unverified'
}

function restoreReadinessLabel(status: string) {
  if (status === 'READY') return 'Restore ready'
  if (status === 'NOT_READY') return 'Restore blocked'
  if (status === 'REVIEW') return 'Restore review'
  return 'Readiness unknown'
}

function restoreReadinessTone(status: string): 'ok' | 'warning' | 'danger' {
  if (status === 'READY') return 'ok'
  if (status === 'NOT_READY') return 'danger'
  return 'warning'
}

function trustIssueLabel(issueName: string) {
  const labels: Record<string, string> = {
    duplicate_source_record: 'Duplicate source records detected',
    missing_required_fields: 'Required source fields are missing',
    infrastructure_incident_without_stage_event: 'Incident is missing stage history',
    stage_event_timestamp_out_of_order: 'Stage events arrived out of order',
    validation_without_completed_work: 'Validation exists without completed work',
    spare_waiting_without_required_spare: 'Recovery blocker has no required parts record',
    stale_impact_snapshot: 'Impact snapshot is stale',
    missing_impact_snapshot: 'Impact snapshot is missing',
    contradictory_impact_evidence: 'Impact evidence is contradictory',
  }
  return labels[issueName] ?? formatStage(issueName)
}

function trustTone(status: string) {
  if (status === 'TRUSTED') return 'trusted'
  if (status === 'WARNING') return 'warning'
  return 'unverified'
}

function readinessBadgeTone(status: string) {
  if (status === 'READY') return 'trusted'
  if (status === 'NOT_READY') return 'warning'
  return 'unverified'
}

function validationStatusSummary(results: RequestDetail['validation_results']) {
  if (!results.length) return 'No validation records'
  const counts = results.reduce<Record<string, number>>((summary, result) => {
    summary[result.validation_status] = (summary[result.validation_status] ?? 0) + 1
    return summary
  }, {})
  return Object.entries(counts)
    .map(([status, count]) => `${count} ${formatStage(status)}`)
    .join(' / ')
}

function validationTone(summary: string): 'ok' | 'warning' | 'danger' {
  if (summary.includes('Failed') || summary.includes('Rejected')) return 'danger'
  if (summary === 'No validation records') return 'warning'
  return 'ok'
}

function redundancyTone(state?: string | null): 'warning' | 'danger' | undefined {
  if (isRedundancyLost(state)) return 'danger'
  if (state) return 'warning'
  return undefined
}

function redundancyLabel(state?: string | null): string {
  if (!state) return 'Unknown'
  if (isRedundancyLost(state)) return 'Redundancy lost'
  if (state === 'REDUNDANCY_AVAILABLE') return 'Redundancy available'
  return formatStage(state)
}

function capacityRiskTone(riskKw: number, riskPct = 0): 'warning' | 'danger' | undefined {
  if (riskKw <= 0) return undefined
  if (riskKw >= 500 || riskPct >= 25) return 'danger'
  return 'warning'
}

function vendorStatusTone(status: string): 'warning' | 'danger' | undefined {
  if (isVendorPartsEscalation(status)) return 'danger'
  if (status) return 'warning'
  return undefined
}

function mitigationTone(status: string): 'warning' | undefined {
  if (status === 'RUNNING_DEGRADED' || status === 'LOAD_SHIFTED') return 'warning'
  return undefined
}

function telemetryTone(status: string): 'warning' | 'danger' | undefined {
  const normalized = status.toUpperCase()
  if (normalized.includes('CRITICAL') || normalized.includes('ALARM') || normalized.includes('BREACH') || normalized.includes('FAILED')) return 'danger'
  if (normalized.includes('WARNING') || normalized.includes('DEGRADED') || normalized.includes('AT_RISK')) return 'warning'
  return undefined
}

function statusTone(status: string) {
  if (status === 'RUNNING') return 'running'
  if (status === 'DEGRADED' || status === 'AT_RISK') return 'warning'
  if (status === 'STOPPED' || status === 'LOCKED_OUT') return 'danger'
  return 'unknown'
}

function formatEvidence(evidence: Record<string, unknown>) {
  return Object.entries(evidence)
    .slice(0, 4)
    .map(([key, value]) => `${formatStage(key)}: ${String(value)}`)
    .join(' · ')
}

export default App
