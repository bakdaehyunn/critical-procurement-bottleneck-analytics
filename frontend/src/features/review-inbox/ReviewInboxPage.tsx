import { useMemo } from 'react'
import { ArrowRight, Bot, CheckCircle2, ClipboardCheck, FileCheck2, GitPullRequestArrow, RefreshCcw, Search, ShieldQuestion, X } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { AppShell } from '../../app/AppShell'
import { EmptyState, ErrorState, LoadingState, PageHeader, Pagination, StatusBadge, type Tone } from '../../components/ui'
import { useDashboard } from '../../hooks/useDashboard'
import { formatDateTime, relativeTime, titleCase } from '../../utils/format'
import { uniqueOntologyReviewRows } from '../../utils/semanticRows'

type ReviewType = 'evidence' | 'validation' | 'action' | 'ai' | 'promotion'
type ReviewItem = {
  id: string
  type: ReviewType
  title: string
  description: string
  risk: string
  relatedObject: string
  evidence: string
  requestedBy: string
  requestedAt?: string | null
  href?: string
  action: string
  disabled?: boolean
  tone: Tone
}

const categories: { id: 'all' | ReviewType; label: string; icon: typeof ClipboardCheck }[] = [
  { id: 'all', label: 'All reviews', icon: ClipboardCheck },
  { id: 'evidence', label: 'Evidence', icon: ShieldQuestion },
  { id: 'validation', label: 'Validation', icon: FileCheck2 },
  { id: 'action', label: 'Governed actions', icon: GitPullRequestArrow },
  { id: 'ai', label: 'AI proposals', icon: Bot },
  { id: 'promotion', label: 'Promotions', icon: CheckCircle2 },
]

const PAGE_SIZE = 20

export function ReviewInboxPage() {
  const { data, loading, error, refreshedAt, refresh } = useDashboard()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedType = (searchParams.get('type') ?? 'all') as 'all' | ReviewType
  const activeType = categories.some((category) => category.id === requestedType) ? requestedType : 'all'
  const query = (searchParams.get('q') ?? '').trim().toLowerCase()
  const lifecycleRows = useMemo(() => uniqueOntologyReviewRows(data?.ontologyReviewQueue ?? []), [data])
  const items = useMemo<ReviewItem[]>(() => {
    if (!data) return []
    const evidenceItems: ReviewItem[] = data.followUps
      .filter((row) => row.impact_confidence_status !== 'TRUSTED' || row.impact_trust_issue_count > 0)
      .map((row) => ({
        id: `evidence-${row.incident_id}`,
        type: 'evidence',
        title: `Review evidence for ${row.request_number}`,
        description: row.restore_readiness_summary ?? row.reason_summary,
        risk: row.restore_readiness_status === 'NOT_READY' ? 'Unsafe or premature restoration decision' : 'Priority recommendation may rely on incomplete evidence',
        relatedObject: `${row.asset_name} · ${row.zone_name}`,
        evidence: `${row.impact_trust_issue_count} impact issue${row.impact_trust_issue_count === 1 ? '' : 's'} · ${titleCase(row.impact_confidence_status)}`,
        requestedBy: 'Semantic evidence monitor',
        href: `/recovery-cases/${row.incident_id}?tab=evidence`,
        action: 'Review evidence',
        tone: 'warning',
      }))
    const validationItems: ReviewItem[] = data.followUps
      .filter((row) => row.current_stage.includes('VALIDATION'))
      .map((row) => ({
        id: `validation-${row.incident_id}`,
        type: 'validation',
        title: `Complete return-to-service validation for ${row.request_number}`,
        description: row.recommended_action,
        risk: `${row.affected_gpu_count} GPUs remain unavailable until validation is resolved`,
        relatedObject: row.asset_name,
        evidence: `${row.hours_in_current_stage.toFixed(0)}h in validation`,
        requestedBy: 'Recovery workflow',
        href: `/recovery-cases/${row.incident_id}?tab=recovery`,
        action: 'Open validation',
        tone: row.hours_in_current_stage > 24 ? 'critical' : 'warning',
      }))
    const lifecycleItems: ReviewItem[] = lifecycleRows.map((row) => ({
      id: `${row.queue_id}|${row.target_uri}|${row.release_id}`,
      type: row.queue_kind.toLowerCase().includes('promotion') ? 'promotion' : row.queue_kind.toLowerCase().includes('ai') ? 'ai' : 'action',
      title: row.review_action_label,
      description: row.evidence_summary,
      risk: row.disabled_reason || 'Specialist review required before graph lifecycle progression',
      relatedObject: row.target_label,
      evidence: `${row.incident_count} incidents · ${row.source_record_count} source records · ${row.generated_fact_count} generated facts`,
      requestedBy: titleCase(row.queue_kind),
      action: row.action_status === 'DISABLED' ? 'Unavailable' : 'Open review',
      disabled: row.action_status === 'DISABLED',
      tone: row.review_status === 'APPROVED' ? 'success' : 'warning',
    }))
    return [...evidenceItems, ...validationItems, ...lifecycleItems]
  }, [data, lifecycleRows])
  const categoryItems = activeType === 'all' ? items : items.filter((item) => item.type === activeType)
  const visible = query
    ? categoryItems.filter((item) => [item.title, item.description, item.relatedObject, item.evidence].some((value) => value.toLowerCase().includes(query)))
    : categoryItems
  const requestedPage = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1)
  const pageCount = Math.max(1, Math.ceil(visible.length / PAGE_SIZE))
  const page = Math.min(requestedPage, pageCount)
  const pageItems = visible.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
  const critical = visible.filter((item) => item.tone === 'critical').length
  const duplicatesCollapsed = Math.max(0, (data?.ontologyReviewQueue.length ?? 0) - lifecycleRows.length)

  const updateParams = (updates: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(updates).forEach(([key, value]) => {
      if (value) next.set(key, value)
      else next.delete(key)
    })
    setSearchParams(next, { replace: true })
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Specialist workspace"
        title="Review Inbox"
        description="Resolve evidence, validation, and governed decisions without interrupting recovery triage."
        actions={<div className="freshness-actions"><div className="freshness"><span className="freshness-dot" /><div><strong>{items.length} open reviews</strong><span>{relativeTime(refreshedAt)}</span></div></div><button className="icon-button" type="button" onClick={() => void refresh()} aria-label="Refresh reviews"><RefreshCcw size={17} className={loading ? 'spin' : ''} /></button></div>}
      />
      {error && !data ? <ErrorState message={error} retry={() => void refresh()} /> : null}
      {loading && !data ? <LoadingState label="Loading review queues" /> : null}
      {data ? (
        <>
          <section className="review-summary" aria-label="Review workload summary">
            <div><span>Open decisions</span><strong>{visible.length}</strong><small>Current review scope</small></div>
            <div className={critical ? 'critical' : ''}><span>Critical</span><strong>{critical}</strong><small>Operational risk if delayed</small></div>
            <div><span>Evidence review</span><strong>{items.filter((item) => item.type === 'evidence').length}</strong><small>Recommendation confidence</small></div>
            <div><span>Governance review</span><strong>{items.filter((item) => ['action', 'ai', 'promotion'].includes(item.type)).length}</strong><small>Controlled lifecycle decisions</small></div>
          </section>
          <nav className="review-filters" aria-label="Review categories">
            {categories.map(({ id, label, icon: Icon }) => {
              const count = id === 'all' ? items.length : items.filter((item) => item.type === id).length
              return <button key={id} type="button" className={activeType === id ? 'active' : ''} aria-pressed={activeType === id} onClick={() => updateParams({ type: id === 'all' ? undefined : id, page: undefined })}><Icon size={16} />{label}<span>{count}</span></button>
            })}
          </nav>
          <section className="review-toolbar" aria-label="Review search and result context">
            <label className="search-field">
              <Search size={17} />
              <span className="sr-only">Search review decisions</span>
              <input type="search" placeholder="Search decision, asset, evidence, or risk" value={searchParams.get('q') ?? ''} onChange={(event) => updateParams({ q: event.target.value || undefined, page: undefined })} />
              {searchParams.get('q') ? <button type="button" onClick={() => updateParams({ q: undefined, page: undefined })} aria-label="Clear review search"><X size={15} /></button> : null}
            </label>
            <p>{duplicatesCollapsed ? `${duplicatesCollapsed} repeated graph observations collapsed into distinct target decisions.` : 'Showing distinct target decisions from the active semantic snapshot.'}</p>
          </section>
          <section className="review-list" aria-label={`${categories.find((item) => item.id === activeType)?.label} items`}>
            {pageItems.length ? pageItems.map((item) => <ReviewCard key={`${item.type}-${item.id}`} item={item} />) : <EmptyState title="No matching reviews" description="Adjust the category or search terms to return decisions to this inbox." />}
          </section>
          <Pagination page={page} pageCount={pageCount} total={visible.length} pageSize={PAGE_SIZE} onChange={(nextPage) => updateParams({ page: nextPage === 1 ? undefined : String(nextPage) })} label="Review inbox pages" />
        </>
      ) : null}
    </AppShell>
  )
}

function ReviewCard({ item }: { item: ReviewItem }) {
  return (
    <article className={`review-card ${item.tone}`}>
      <div className="review-card-status"><StatusBadge label={titleCase(item.type)} tone={item.tone} /><span>{item.requestedAt ? formatDateTime(item.requestedAt) : 'Current analysis run'}</span></div>
      <div className="review-card-main"><h2>{item.title}</h2><p>{item.description}</p><dl><div><dt>Operational risk</dt><dd>{item.risk}</dd></div><div><dt>Related object</dt><dd>{item.relatedObject}</dd></div><div><dt>Evidence completeness</dt><dd>{item.evidence}</dd></div><div><dt>Requested by</dt><dd>{item.requestedBy}</dd></div></dl></div>
      <div className="review-card-action">{item.href ? <Link className="button secondary" to={item.href}>{item.action}<ArrowRight size={14} /></Link> : <button className="button secondary" type="button" disabled={item.disabled}>{item.action}</button>}</div>
    </article>
  )
}
