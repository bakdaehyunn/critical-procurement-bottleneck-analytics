import { useCallback, type ReactNode } from 'react'
import { Activity, AlertTriangle, CheckCircle2, Database, GitBranch, RefreshCcw, Server, ShieldCheck, Workflow } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchOntologyReviewQueuePage, fetchPlatformStatus, fetchQualityCheckPage, type OntologyReviewQueueItem } from '../../api'
import { AppShell } from '../../app/AppShell'
import { Disclosure, EmptyState, ErrorState, LoadingState, Metric, PageHeader, Pagination, StatusBadge, type Tone } from '../../components/ui'
import { useAsyncResource } from '../../hooks/useAsyncResource'
import { formatDateTime, relativeTime, titleCase } from '../../utils/format'

const QUALITY_PAGE_SIZE = 15

async function fetchLifecyclePreview() {
  const results = await Promise.allSettled([fetchOntologyReviewQueuePage('promotion', 1, 6), fetchOntologyReviewQueuePage('reasoning', 1, 6)])
  const records: OntologyReviewQueueItem[] = []
  let total = 0, unavailable = 0
  results.forEach((result) => {
    if (result.status === 'fulfilled') { records.push(...result.value.records); total += result.value.page_info.totalRecords }
    else unavailable += 1
  })
  return { records, total, unavailable }
}

function statusTone(value: string): Tone {
  const normalized = value.toUpperCase()
  if (['SUCCESS', 'CURRENT', 'COMPLETE', 'COMPLETED', 'OPERATIONAL'].includes(normalized)) return 'success'
  if (['FAILED', 'ERROR', 'DEGRADED'].includes(normalized)) return 'critical'
  if (normalized === 'UNKNOWN') return 'neutral'
  return 'warning'
}

export function PlatformStatusPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedPage = Math.max(1, Number.parseInt(searchParams.get('qualityPage') ?? '1', 10) || 1)
  const platform = useAsyncResource(useCallback(() => fetchPlatformStatus(), []))
  const quality = useAsyncResource(useCallback(() => fetchQualityCheckPage(requestedPage, QUALITY_PAGE_SIZE), [requestedPage]))
  const lifecycle = useAsyncResource(useCallback(() => fetchLifecyclePreview(), []))
  const status = platform.data
  const currentQuality = quality.data?.page_info.page === requestedPage ? quality.data : null
  const qualityPage = currentQuality?.page_info
  const qualityRows = currentQuality?.records ?? []
  const qualityIssues = qualityRows.filter((item) => item.status !== 'PASS' && item.status !== 'PASSED')
  const verdict = status?.platform_verdict ?? 'UNKNOWN'
  const verdictTone = statusTone(verdict)
  const refresh = () => { platform.refresh(); quality.refresh(); lifecycle.refresh() }
  const setQualityPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page === 1) next.delete('qualityPage'); else next.set('qualityPage', String(page))
    setSearchParams(next)
  }

  return <AppShell>
    <PageHeader eyebrow="Technical operations" title="Platform Status" description="Authoritative source freshness, release, reasoning, and reconciliation state—unknown when no persisted evidence exists." actions={<div className="freshness-actions"><StatusBadge label={titleCase(verdict)} tone={verdictTone} /><button className="icon-button" type="button" onClick={refresh} aria-label="Refresh platform status"><RefreshCcw size={17} className={platform.loading ? 'spin' : ''} /></button></div>} />
    {platform.error && !status ? <ErrorState message={platform.error} retry={refresh} /> : null}
    {platform.error && status ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />Platform refresh failed. The last successful status remains visible.</div> : null}
    {platform.loading && !status ? <LoadingState label="Checking source-backed platform status" /> : null}
    {status ? <div className="workspace-stack">
      <section className={`platform-verdict ${verdictTone}`}><div className="verdict-icon">{verdict === 'OPERATIONAL' ? <CheckCircle2 size={24} /> : <AlertTriangle size={24} />}</div><div><span className="eyebrow">Current platform state</span><h2>{verdict === 'OPERATIONAL' ? 'Authoritative platform evidence is available' : verdict === 'DEGRADED' ? 'Authoritative platform evidence reports degraded coverage' : 'Platform health cannot be fully determined from persisted evidence'}</h2><p>{titleCase(status.reason_code)} · {relativeTime(platform.refreshedAt)} · {status.service_boundary}</p></div></section>
      <section className="case-metrics four"><Metric label="Semantic service" value="Connected" detail={status.service_boundary} tone="success" icon={<Server size={17} />} /><Metric label="Source freshness" value={titleCase(status.source_freshness_status)} detail={status.latest_source_import_at ? formatDateTime(status.latest_source_import_at) : 'No import timestamp persisted'} tone={statusTone(status.source_freshness_status)} icon={<Database size={17} />} /><Metric label="Pipeline" value={titleCase(status.pipeline_status)} detail={status.latest_reasoning_run_id ?? 'No reasoning run persisted'} tone={statusTone(status.pipeline_status)} icon={<Workflow size={17} />} /><Metric label="Graph validation" value={titleCase(status.graph_validation_status)} detail="No health claim without an authoritative report" tone={statusTone(status.graph_validation_status)} icon={<ShieldCheck size={17} />} /></section>
      <section className="platform-grid"><div className="workspace-section compact"><div className="section-heading"><div><h2>Release and analysis</h2><p>Latest persisted lifecycle evidence</p></div></div><div className="coverage-list"><CoverageRow icon={<GitBranch size={16} />} label="Canonical release" value={status.latest_canonical_release_id ?? 'Unknown'} detail={`${titleCase(status.promotion_status)}${status.latest_promotion_at ? ` · ${formatDateTime(status.latest_promotion_at)}` : ''}`} /><CoverageRow icon={<Activity size={16} />} label="Reasoning run" value={status.latest_reasoning_run_id ?? 'Unknown'} detail={`${titleCase(status.analysis_status)}${status.latest_analysis_at ? ` · ${formatDateTime(status.latest_analysis_at)}` : ''}`} /><CoverageRow icon={<ShieldCheck size={16} />} label="Reconciliation" value={titleCase(status.reconciliation_status)} detail={`${status.incident_with_provenance_count}/${status.incident_count} incidents · ${status.asset_with_provenance_count}/${status.asset_count} assets with provenance`} /></div></div><div className="workspace-section compact"><div className="section-heading"><div><h2>Source coverage</h2><p>Counts from the platform-status read model</p></div></div><div className="coverage-list"><CoverageRow icon={<Database size={16} />} label="Source systems" value={String(status.source_system_count)} detail={`${status.source_record_count} source records`} /><CoverageRow icon={<Activity size={16} />} label="Incidents" value={String(status.incident_count)} detail={`${status.incident_with_provenance_count} with provenance`} /><CoverageRow icon={<Server size={16} />} label="Assets" value={String(status.asset_count)} detail={`${status.asset_with_provenance_count} with provenance`} /></div></div></section>
      <section className="workspace-section"><div className="section-heading"><div><h2>Data-quality findings</h2><p>Distinct latest findings, paged by the semantic service.</p></div>{currentQuality ? <StatusBadge label={`${currentQuality.page_info.totalRecords} total`} tone={qualityIssues.length ? 'warning' : 'success'} /> : null}</div>{quality.error && !currentQuality ? <ErrorState message={quality.error} retry={quality.refresh} /> : null}{quality.error && currentQuality ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />Quality refresh failed. The last successful page remains visible.</div> : null}{quality.loading && !currentQuality ? <LoadingState label="Loading data-quality findings" /> : null}{currentQuality ? qualityRows.length ? <><div className="quality-table-wrap"><table className="quality-table"><thead><tr><th>Operational area</th><th>Status</th><th>Severity</th><th>Finding</th><th>Observed</th></tr></thead><tbody>{qualityRows.map((item) => { const passed = item.status === 'PASS' || item.status === 'PASSED'; const critical = item.severity.toUpperCase() === 'CRITICAL'; return <tr key={item.check_result_id}><td>{titleCase(item.check_name)}</td><td><StatusBadge label={titleCase(item.status)} tone={passed ? 'success' : critical ? 'critical' : 'warning'} /></td><td>{titleCase(item.severity)}</td><td>{item.message}</td><td>{formatDateTime(item.created_at)}</td></tr> })}</tbody></table></div>{qualityPage ? <Pagination page={qualityPage.page} pageCount={qualityPage.pageCount} total={qualityPage.totalRecords} pageSize={qualityPage.pageSize} onChange={setQualityPage} label="Data-quality finding pages" /> : null}</> : <EmptyState title="No quality findings returned" description="The latest semantic response contains no source-quality check records." /> : null}</section>
      <Disclosure title="Graph lifecycle details" summary={`${lifecycle.data?.total ?? 'Unknown'} authoritative decisions · read only`}>{lifecycle.error && !lifecycle.data ? <ErrorState message={lifecycle.error} retry={lifecycle.refresh} /> : null}{lifecycle.data?.unavailable ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />One lifecycle queue is unavailable; available records remain visible.</div> : null}{lifecycle.data?.records.length ? <><p className="disclosure-context">Showing up to six promotion and six reasoning decisions. Lifecycle writes are not supported by the current service contract.</p><div className="technical-list">{lifecycle.data.records.map((row) => <div key={`${row.queue_kind}-${row.target_uri}-${row.release_id}`}><strong>{row.target_label}</strong><span>{row.evidence_summary}</span><code>{row.graph_uri}</code></div>)}</div><div className="disclosure-footer"><Link className="button secondary" to="/reviews?type=promotion">Open lifecycle queues</Link></div></> : lifecycle.data ? <p className="technical-empty">No graph lifecycle review items are active.</p> : <LoadingState label="Loading lifecycle queues" />}</Disclosure>
    </div> : null}
  </AppShell>
}

function CoverageRow({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) { return <div><div className="coverage-icon">{icon}</div><div><span>{label}</span><small>{detail}</small></div><strong>{value}</strong></div> }
