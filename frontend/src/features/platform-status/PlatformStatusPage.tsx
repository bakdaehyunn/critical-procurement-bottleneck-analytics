import { useMemo } from 'react'
import { Activity, AlertTriangle, CheckCircle2, Database, GitBranch, RefreshCcw, Server, ShieldCheck, Workflow } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../app/AppShell'
import { Disclosure, EmptyState, ErrorState, LoadingState, Metric, PageHeader, Pagination, StatusBadge } from '../../components/ui'
import { useDashboard } from '../../hooks/useDashboard'
import { formatDateTime, relativeTime, titleCase } from '../../utils/format'
import { uniqueOntologyReviewRows, uniqueQualityChecks } from '../../utils/semanticRows'

const QUALITY_PAGE_SIZE = 15
const LIFECYCLE_PREVIEW_SIZE = 12

export function PlatformStatusPage() {
  const { data, loading, error, refreshedAt, refresh } = useDashboard()
  const [searchParams, setSearchParams] = useSearchParams()
  const qualityChecks = useMemo(() => uniqueQualityChecks(data?.qualityChecks ?? []), [data])
  const lifecycleRows = useMemo(() => uniqueOntologyReviewRows(data?.ontologyReviewQueue ?? []), [data])
  const qualityIssues = qualityChecks.filter((item) => item.status !== 'PASS' && item.status !== 'PASSED')
  const healthy = Boolean(data) && !error && qualityIssues.filter((item) => item.severity.toUpperCase() === 'CRITICAL').length === 0
  const overview = data?.overview
  const requestedQualityPage = Math.max(1, Number.parseInt(searchParams.get('qualityPage') ?? '1', 10) || 1)
  const qualityPageCount = Math.max(1, Math.ceil(qualityChecks.length / QUALITY_PAGE_SIZE))
  const qualityPage = Math.min(requestedQualityPage, qualityPageCount)
  const qualityPageRows = qualityChecks.slice((qualityPage - 1) * QUALITY_PAGE_SIZE, qualityPage * QUALITY_PAGE_SIZE)
  const collapsedQualityCount = Math.max(0, (data?.qualityChecks.length ?? 0) - qualityChecks.length)
  const collapsedLifecycleCount = Math.max(0, (data?.ontologyReviewQueue.length ?? 0) - lifecycleRows.length)

  const setQualityPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page === 1) next.delete('qualityPage')
    else next.set('qualityPage', String(page))
    setSearchParams(next, { replace: true })
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Technical operations"
        title="Platform Status"
        description="Data freshness, validation, and semantic runtime health—separate from incident severity."
        actions={<div className="freshness-actions"><StatusBadge label={healthy ? 'Operational' : 'Attention required'} tone={healthy ? 'success' : 'warning'} /><button className="icon-button" type="button" onClick={() => void refresh()} aria-label="Refresh platform status"><RefreshCcw size={17} className={loading ? 'spin' : ''} /></button></div>}
      />
      {error && !data ? <ErrorState message={error} retry={() => void refresh()} /> : null}
      {loading && !data ? <LoadingState label="Checking platform health" /> : null}
      {data && overview ? (
        <div className="workspace-stack">
          <section className={`platform-verdict ${healthy ? 'success' : 'warning'}`}>
            <div className="verdict-icon">{healthy ? <CheckCircle2 size={24} /> : <AlertTriangle size={24} />}</div>
            <div><span className="eyebrow">Current platform state</span><h2>{healthy ? 'Operational data is available for recovery decisions' : 'Review platform findings before relying on all recommendations'}</h2><p>{relativeTime(refreshedAt)} · Pipeline {titleCase(overview.latest_pipeline_run_status ?? 'unknown')}</p></div>
          </section>
          <section className="case-metrics four">
            <Metric label="Semantic service" value={error ? 'Unavailable' : 'Connected'} detail="Approved query boundary" tone={error ? 'critical' : 'success'} icon={<Server size={17} />} />
            <Metric label="Pipeline" value={titleCase(overview.latest_pipeline_run_status ?? 'unknown')} detail="Latest analysis release" tone={overview.latest_pipeline_run_status === 'SUCCESS' ? 'success' : 'warning'} icon={<Workflow size={17} />} />
            <Metric label="Data quality" value={titleCase(overview.data_quality_status)} detail={`${qualityIssues.length} finding${qualityIssues.length === 1 ? '' : 's'} requiring review`} tone={qualityIssues.length ? 'warning' : 'success'} icon={<ShieldCheck size={17} />} />
            <Metric label="Topology model" value={`${data.topologyDependencies.length} edges`} detail="Power, cooling, and telemetry dependencies" tone="info" icon={<GitBranch size={17} />} />
          </section>
          <section className="platform-grid">
            <div className="workspace-section compact">
              <div className="section-heading"><div><h2>Source and analysis coverage</h2><p>Current read-model inventory</p></div></div>
              <div className="coverage-list">
                <CoverageRow icon={<Activity size={16} />} label="Active incidents" value={String(data.followUps.length)} detail="Ranked recovery cases" />
                <CoverageRow icon={<Database size={16} />} label="Infrastructure assets" value={String(data.assetDelays.length)} detail="Assets represented in delay summaries" />
                <CoverageRow icon={<GitBranch size={16} />} label="Dependency edges" value={String(data.topologyDependencies.length)} detail="Configured topology relationships" />
                <CoverageRow icon={<ShieldCheck size={16} />} label="Trust findings" value={String(qualityChecks.length)} detail="Distinct source and evidence checks" />
              </div>
            </div>
            <div className="workspace-section compact">
              <div className="section-heading"><div><h2>Lifecycle state</h2><p>Controlled semantic review boundaries</p></div></div>
              <div className="coverage-list">
                <CoverageRow icon={<Workflow size={16} />} label="Review decisions" value={String(lifecycleRows.length)} detail="Distinct promotion and reasoning targets" />
                <CoverageRow icon={<CheckCircle2 size={16} />} label="Available results" value={String(overview.total_requests)} detail="Incidents in latest analysis" />
                <CoverageRow icon={<Activity size={16} />} label="Average downtime" value={`${overview.avg_downtime_hours.toFixed(1)}h`} detail="Across current semantic snapshot" />
              </div>
            </div>
          </section>
          <section className="workspace-section">
            <div className="section-heading"><div><h2>Data-quality findings</h2><p>{qualityChecks.length} distinct findings from {data.qualityChecks.length} observations{collapsedQualityCount ? ` · ${collapsedQualityCount} repeated observations collapsed` : ''}.</p></div><StatusBadge label={`${qualityIssues.length} need review`} tone={qualityIssues.length ? 'warning' : 'success'} /></div>
            {qualityChecks.length ? <><div className="quality-table-wrap"><table className="quality-table"><thead><tr><th>Operational area</th><th>Status</th><th>Severity</th><th>Finding</th><th>Observed</th></tr></thead><tbody>{qualityPageRows.map((item) => { const passed = item.status === 'PASS' || item.status === 'PASSED'; const critical = item.severity.toUpperCase() === 'CRITICAL'; return <tr key={item.check_result_id}><td>{titleCase(item.check_name)}</td><td><StatusBadge label={titleCase(item.status)} tone={passed ? 'success' : critical ? 'critical' : 'warning'} /></td><td>{titleCase(item.severity)}</td><td>{item.message}</td><td>{formatDateTime(item.created_at)}</td></tr> })}</tbody></table></div><Pagination page={qualityPage} pageCount={qualityPageCount} total={qualityChecks.length} pageSize={QUALITY_PAGE_SIZE} onChange={setQualityPage} label="Data-quality finding pages" /></> : <EmptyState title="No quality findings returned" description="The latest semantic response contains no source-quality check records." />}
          </section>
          <Disclosure title="Graph lifecycle details" summary={`${lifecycleRows.length} distinct decisions · specialist-only release context`}>
            {lifecycleRows.length ? <><p className="disclosure-context">Showing the first {Math.min(LIFECYCLE_PREVIEW_SIZE, lifecycleRows.length)} decisions. {collapsedLifecycleCount ? `${collapsedLifecycleCount} repeated graph observations are collapsed.` : ''}</p><div className="technical-list">{lifecycleRows.slice(0, LIFECYCLE_PREVIEW_SIZE).map((row) => <div key={`${row.queue_id}-${row.target_uri}-${row.release_id}`}><strong>{row.target_label}</strong><span>{row.evidence_summary}</span><code>{row.graph_uri}</code></div>)}</div><div className="disclosure-footer"><Link className="button secondary" to="/reviews?type=action">Open governed reviews</Link></div></> : <p className="technical-empty">No graph lifecycle review items are active.</p>}
          </Disclosure>
        </div>
      ) : null}
    </AppShell>
  )
}

function CoverageRow({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return <div><div className="coverage-icon">{icon}</div><div><span>{label}</span><small>{detail}</small></div><strong>{value}</strong></div>
}
