import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { AlertTriangle, ArrowRight, Bot, CheckCircle2, GitPullRequestArrow, RefreshCcw, Search, ShieldQuestion, Workflow, X } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  fetchActionReviewQueuePage,
  fetchAiProposalReviewQueuePage,
  fetchOntologyReviewQueuePage,
  fetchReviewAttentionSignals,
  submitAiProposalReview,
  submitOntologyActionTransition,
  type AiProposalItem,
  type FollowUpItem,
  type OntologyActionLifecycleState,
  type OntologyActionReviewQueueItem,
  type OntologyReviewQueueItem,
  type PagedResult,
} from '../../api'
import { AppShell } from '../../app/AppShell'
import { EmptyState, ErrorState, LoadingState, PageHeader, Pagination, StatusBadge, type Tone } from '../../components/ui'
import { useAsyncResource } from '../../hooks/useAsyncResource'
import { formatDateTime, relativeTime, titleCase } from '../../utils/format'
import { availableActionTransitions, isAiProposalActionable } from './reviewUtils'

type ReviewType = 'action' | 'ai' | 'promotion' | 'reasoning' | 'attention'
type ReviewRecord =
  | { kind: 'action'; item: OntologyActionReviewQueueItem }
  | { kind: 'ai'; item: AiProposalItem }
  | { kind: 'promotion' | 'reasoning'; item: OntologyReviewQueueItem }
  | { kind: 'attention'; item: FollowUpItem }
type ReviewPage = PagedResult<ReviewRecord> & { scope: ReviewType }

const categories: { id: ReviewType; label: string; icon: typeof GitPullRequestArrow }[] = [
  { id: 'action', label: 'Governed actions', icon: GitPullRequestArrow },
  { id: 'ai', label: 'AI proposals', icon: Bot },
  { id: 'promotion', label: 'Promotions', icon: CheckCircle2 },
  { id: 'reasoning', label: 'Reasoning', icon: Workflow },
  { id: 'attention', label: 'Case attention', icon: ShieldQuestion },
]

const PAGE_SIZE = 20
const emptyPageInfo = { page: 1, pageSize: PAGE_SIZE, pageCount: 1, totalRecords: 0 }

async function fetchCategory(type: ReviewType, page: number): Promise<ReviewPage> {
  if (type === 'action') {
    const result = await fetchActionReviewQueuePage(page, PAGE_SIZE)
    return { scope: type, records: result.records.map((item) => ({ kind: 'action' as const, item })), page_info: result.page_info }
  }
  if (type === 'ai') {
    const result = await fetchAiProposalReviewQueuePage(page, PAGE_SIZE)
    return { scope: type, records: result.records.map((item) => ({ kind: 'ai' as const, item })), page_info: result.page_info }
  }
  if (type === 'promotion' || type === 'reasoning') {
    const result = await fetchOntologyReviewQueuePage(type, page, PAGE_SIZE)
    return { scope: type, records: result.records.map((item) => ({ kind: type, item })), page_info: result.page_info }
  }
  const rows = (await fetchReviewAttentionSignals()).filter(isAttentionSignal)
  const start = (page - 1) * PAGE_SIZE
  return {
    scope: type,
    records: rows.slice(start, start + PAGE_SIZE).map((item) => ({ kind: 'attention' as const, item })),
    page_info: { page, pageSize: PAGE_SIZE, pageCount: Math.max(1, Math.ceil(rows.length / PAGE_SIZE)), totalRecords: rows.length },
  }
}

async function fetchCounts(): Promise<Record<ReviewType, number | null>> {
  const results = await Promise.allSettled([
    fetchActionReviewQueuePage(1, 1),
    fetchAiProposalReviewQueuePage(1, 1),
    fetchOntologyReviewQueuePage('promotion', 1, 1),
    fetchOntologyReviewQueuePage('reasoning', 1, 1),
    fetchReviewAttentionSignals(),
  ])
  const total = (result: PromiseSettledResult<PagedResult<unknown>>) => result.status === 'fulfilled' ? result.value.page_info.totalRecords : null
  return {
    action: total(results[0]), ai: total(results[1]), promotion: total(results[2]), reasoning: total(results[3]),
    attention: results[4].status === 'fulfilled' ? results[4].value.filter(isAttentionSignal).length : null,
  }
}

function isAttentionSignal(item: FollowUpItem) {
  return item.impact_confidence_status !== 'TRUSTED' || item.impact_trust_issue_count > 0 || item.current_stage.includes('VALIDATION')
}

export function ReviewInboxPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedType = searchParams.get('type') as ReviewType | null
  const activeType = categories.some((category) => category.id === requestedType) ? requestedType! : 'action'
  const requestedPage = Math.max(1, Number.parseInt(searchParams.get('page') ?? '1', 10) || 1)
  const queue = useAsyncResource(useCallback(() => fetchCategory(activeType, requestedPage), [activeType, requestedPage]))
  const counts = useAsyncResource(useCallback(() => fetchCounts(), []))
  const query = (searchParams.get('q') ?? '').trim().toLowerCase()
  const currentPage = queue.data?.scope === activeType && queue.data.page_info.page === requestedPage ? queue.data : null
  const pageInfo = currentPage?.page_info ?? emptyPageInfo

  const updateParams = (updates: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(updates).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key))
    setSearchParams(next)
  }
  const visible = useMemo(() => {
    const rows = currentPage?.records ?? []
    return query ? rows.filter((record) => searchableText(record).toLowerCase().includes(query)) : rows
  }, [currentPage, query])
  const refresh = () => { queue.refresh(); counts.refresh() }
  const knownTotal = categories.reduce((sum, category) => sum + (counts.data?.[category.id] ?? 0), 0)

  return <AppShell>
    <PageHeader eyebrow="Specialist workspace" title="Review Inbox" description="Act on authoritative governed queues; case-derived signals are clearly separated from persisted reviews." actions={<div className="freshness-actions"><div className={`freshness ${queue.error || queue.stale ? 'warning' : ''}`}><span className="freshness-dot" /><div><strong>{counts.data ? `${knownTotal} known items` : 'Loading workload'}</strong><span>{relativeTime(queue.refreshedAt)}</span></div></div><button className="icon-button" type="button" onClick={refresh} aria-label="Refresh review queues"><RefreshCcw size={17} className={queue.loading ? 'spin' : ''} /></button></div>} />
    {queue.error && !currentPage ? <ErrorState message={queue.error} retry={refresh} /> : null}
    {queue.error && currentPage ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />Refresh failed. The last successful page remains visible.</div> : null}
    {counts.error ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />Some category totals are unavailable; unknown totals are shown as —.</div> : null}
    {queue.loading && !currentPage ? <LoadingState label="Loading the selected review queue" /> : null}
    <nav className="review-filters" aria-label="Review categories">{categories.map(({ id, label, icon: Icon }) => <button key={id} type="button" className={activeType === id ? 'active' : ''} aria-pressed={activeType === id} onClick={() => updateParams({ type: id === 'action' ? undefined : id, page: undefined, q: undefined })}><Icon size={16} />{label}<span>{counts.data?.[id] ?? '—'}</span></button>)}</nav>
    {currentPage ? <>
      <section className="review-summary" aria-label="Selected review workload"><div><span>Selected queue</span><strong>{categories.find((item) => item.id === activeType)?.label}</strong><small>One authoritative source</small></div><div><span>Total results</span><strong>{pageInfo.totalRecords}</strong><small>Server-reported result volume</small></div><div><span>Current page</span><strong>{pageInfo.page} / {pageInfo.pageCount}</strong><small>{queue.stale ? 'Stale snapshot' : 'Latest successful read'}</small></div><div><span>Queue behavior</span><strong>{activeType === 'attention' ? 'Navigate' : activeType === 'action' || activeType === 'ai' ? 'Act' : 'Read only'}</strong><small>{activeType === 'attention' ? 'Derived signal, not a review record' : 'Source-backed capability'}</small></div></section>
      <section className="review-toolbar" aria-label="Review search and result context"><label className="search-field"><Search size={17} /><span className="sr-only">Search current review page</span><input key={searchParams.get('q') ?? ''} type="search" placeholder="Search this result page" defaultValue={searchParams.get('q') ?? ''} onBlur={(event) => { if (event.currentTarget.value !== (searchParams.get('q') ?? '')) updateParams({ q: event.currentTarget.value || undefined }) }} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); updateParams({ q: event.currentTarget.value || undefined }) } }} />{searchParams.get('q') ? <button type="button" onClick={() => updateParams({ q: undefined })} aria-label="Clear review search"><X size={15} /></button> : null}</label><p>Search filters the loaded page. Paging and totals come from the semantic-service response.</p></section>
      <section className="review-list" aria-label={`${categories.find((item) => item.id === activeType)?.label} items`}>{visible.length ? visible.map((record) => <ReviewCard key={recordKey(record)} record={record} onComplete={refresh} />) : <EmptyState title="No matching items" description="This queue page has no records matching the current search." />}</section>
      <Pagination page={pageInfo.page} pageCount={pageInfo.pageCount} total={pageInfo.totalRecords} pageSize={pageInfo.pageSize} onChange={(page) => updateParams({ page: page === 1 ? undefined : String(page), q: undefined })} label="Review queue pages" />
    </> : null}
  </AppShell>
}

function ReviewCard({ record, onComplete }: { record: ReviewRecord; onComplete: () => void }) {
  if (record.kind === 'action') return <ActionReviewCard item={record.item} onComplete={onComplete} />
  if (record.kind === 'ai') return <AiReviewCard item={record.item} onComplete={onComplete} />
  if (record.kind === 'attention') return <AttentionCard item={record.item} />
  return <LifecycleCard kind={record.kind} item={record.item} />
}

function ActionReviewCard({ item, onComplete }: { item: OntologyActionReviewQueueItem; onComplete: () => void }) {
  const transitions = availableActionTransitions(item.current_state)
  const [editing, setEditing] = useState(false), [actor, setActor] = useState(''), [reason, setReason] = useState('')
  const [toState, setToState] = useState<OntologyActionLifecycleState | ''>(transitions[0] ?? '')
  const [submitting, setSubmitting] = useState(false), [error, setError] = useState<string | null>(null)
  const submit = async () => {
    if (!toState || !actor.trim() || !reason.trim()) { setError('Reviewer, transition, and reason are required.'); return }
    setSubmitting(true); setError(null)
    try { await submitOntologyActionTransition({ target_execution_uri: item.execution_uri, to_state: toState, actor_id: actor.trim(), transition_reason: reason.trim() }); setEditing(false); onComplete() }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : 'Action transition failed.') }
    finally { setSubmitting(false) }
  }
  return <article className="review-card warning"><CardStatus label="Governed action" tone="warning" date={item.state_generated_at} /><div className="review-card-main"><h2>{titleCase(item.action_type_id)}</h2><p>{item.action_reason}</p><dl><div><dt>Current state</dt><dd>{titleCase(item.current_state)}</dd></div><div><dt>Incident</dt><dd>{item.incident_id}</dd></div><div><dt>Requested by</dt><dd>{item.actor_id}</dd></div><div><dt>Execution</dt><dd>{item.execution_id}</dd></div></dl>{editing ? <InlineDecisionForm error={error}><label><span>Next state</span><select value={toState} onChange={(event) => setToState(event.target.value as OntologyActionLifecycleState)}>{transitions.map((state) => <option key={state}>{state}</option>)}</select></label><label><span>Reviewer ID</span><input value={actor} onChange={(event) => setActor(event.target.value)} required /></label><label className="wide"><span>Transition reason</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} required /></label><FormActions submitting={submitting} onCancel={() => setEditing(false)} onSubmit={() => void submit()} /></InlineDecisionForm> : null}</div><div className="review-card-action"><button className="button secondary" type="button" disabled={!transitions.length} onClick={() => setEditing(true)}>{transitions.length ? 'Review transition' : 'Lifecycle complete'}</button></div></article>
}

function AiReviewCard({ item, onComplete }: { item: AiProposalItem; onComplete: () => void }) {
  const [editing, setEditing] = useState(false), [actor, setActor] = useState(''), [reason, setReason] = useState('')
  const [decision, setDecision] = useState<'APPROVE' | 'REJECT'>('APPROVE')
  const [submitting, setSubmitting] = useState(false), [error, setError] = useState<string | null>(null)
  const actionable = isAiProposalActionable(item.review_status)
  const submit = async () => {
    if (!actor.trim() || !reason.trim()) { setError('Reviewer and review reason are required.'); return }
    setSubmitting(true); setError(null)
    try { await submitAiProposalReview({ proposal_uri: item.proposal_uri, proposal_id: item.proposal_id, decision, actor_id: actor.trim(), review_reason: reason.trim() }); setEditing(false); onComplete() }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : 'AI proposal review failed.') }
    finally { setSubmitting(false) }
  }
  return <article className={`review-card ${item.risk_level === 'CRITICAL' ? 'critical' : 'warning'}`}><CardStatus label="AI proposal" tone={item.risk_level === 'CRITICAL' ? 'critical' : 'warning'} date={item.generated_at} /><div className="review-card-main"><h2>{item.summary}</h2><p>{item.rationale}</p><dl><div><dt>Review state</dt><dd>{titleCase(item.review_status)}</dd></div><div><dt>Incident</dt><dd>{item.incident_id}</dd></div><div><dt>Generated by</dt><dd>{item.actor_id || item.model_id}</dd></div><div><dt>Confidence</dt><dd>{Math.round(item.confidence_score * 100)}%</dd></div></dl>{editing ? <InlineDecisionForm error={error}><label><span>Decision</span><select value={decision} onChange={(event) => setDecision(event.target.value as 'APPROVE' | 'REJECT')}><option value="APPROVE">Approve</option><option value="REJECT">Reject</option></select></label><label><span>Reviewer ID</span><input value={actor} onChange={(event) => setActor(event.target.value)} required /></label><label className="wide"><span>Review reason</span><textarea value={reason} onChange={(event) => setReason(event.target.value)} required /></label><FormActions submitting={submitting} onCancel={() => setEditing(false)} onSubmit={() => void submit()} /></InlineDecisionForm> : null}</div><div className="review-card-action"><button className="button secondary" type="button" disabled={!actionable} onClick={() => setEditing(true)}>{actionable ? 'Review proposal' : titleCase(item.review_status)}</button></div></article>
}

function LifecycleCard({ kind, item }: { kind: 'promotion' | 'reasoning'; item: OntologyReviewQueueItem }) { return <article className="review-card warning"><CardStatus label={kind} tone="warning" /><div className="review-card-main"><h2>{item.review_action_label}</h2><p>{item.evidence_summary}</p><dl><div><dt>Status</dt><dd>{titleCase(item.review_status)}</dd></div><div><dt>Target</dt><dd>{item.target_label}</dd></div><div><dt>Release</dt><dd>{item.release_id}</dd></div><div><dt>Capability</dt><dd>{item.disabled_reason || 'No supported write operation'}</dd></div></dl></div><div className="review-card-action"><button className="button secondary" type="button" disabled>Read only</button></div></article> }
function AttentionCard({ item }: { item: FollowUpItem }) { const critical = item.restore_readiness_status === 'NOT_READY'; return <article className={`review-card ${critical ? 'critical' : 'warning'}`}><CardStatus label="Case signal" tone={critical ? 'critical' : 'warning'} /><div className="review-card-main"><h2>{item.request_number}: {item.request_title}</h2><p>{item.restore_readiness_summary ?? item.reason_summary}</p><dl><div><dt>Signal source</dt><dd>Derived current case state</dd></div><div><dt>Evidence issues</dt><dd>{item.impact_trust_issue_count}</dd></div><div><dt>Stage</dt><dd>{titleCase(item.current_stage)}</dd></div><div><dt>Asset</dt><dd>{item.asset_name}</dd></div></dl></div><div className="review-card-action"><Link className="button secondary" to={`/recovery-cases/${item.incident_id}?tab=evidence`}>Open case <ArrowRight size={14} /></Link></div></article> }
function CardStatus({ label, tone, date }: { label: string; tone: Tone; date?: string }) { return <div className="review-card-status"><StatusBadge label={titleCase(label)} tone={tone} /><span>{date ? formatDateTime(date) : 'Current source state'}</span></div> }
function InlineDecisionForm({ children, error }: { children: ReactNode; error: string | null }) { return <div className="decision-form">{error ? <div className="inline-notice critical" role="alert"><AlertTriangle size={15} />{error}</div> : null}<div className="decision-form-grid">{children}</div></div> }
function FormActions({ submitting, onCancel, onSubmit }: { submitting: boolean; onCancel: () => void; onSubmit: () => void }) { return <div className="decision-form-actions wide"><button className="button secondary" type="button" disabled={submitting} onClick={onCancel}>Cancel</button><button className="button primary" type="button" disabled={submitting} onClick={onSubmit}>{submitting ? 'Submitting…' : 'Submit decision'}</button></div> }
function recordKey(record: ReviewRecord) { if (record.kind === 'action') return record.item.execution_uri; if (record.kind === 'ai') return record.item.proposal_uri; if (record.kind === 'attention') return record.item.incident_id; return `${record.kind}-${record.item.target_uri}-${record.item.release_id}` }
function searchableText(record: ReviewRecord) { if (record.kind === 'action') return `${record.item.action_type_id} ${record.item.action_reason} ${record.item.actor_id} ${record.item.incident_id}`; if (record.kind === 'ai') return `${record.item.summary} ${record.item.rationale} ${record.item.actor_id} ${record.item.incident_id}`; if (record.kind === 'attention') return `${record.item.request_number} ${record.item.request_title} ${record.item.asset_name}`; return `${record.item.review_action_label} ${record.item.evidence_summary} ${record.item.target_label} ${record.item.release_id}` }
