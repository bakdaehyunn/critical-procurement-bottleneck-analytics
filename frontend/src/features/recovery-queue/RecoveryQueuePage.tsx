import { useMemo, useState } from 'react'
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  ChevronDown,
  Clock3,
  Cpu,
  Filter,
  RefreshCcw,
  Search,
  ShieldAlert,
  SlidersHorizontal,
  Zap,
  X,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import type { FollowUpItem } from '../../api'
import { AppShell } from '../../app/AppShell'
import { ErrorState, LoadingState, Metric, PageHeader, StatusBadge } from '../../components/ui'
import { useDashboard } from '../../hooks/useDashboard'
import {
  dependencyOwner,
  evidenceLabel,
  evidenceTone,
  formatHours,
  priorityTone,
  readinessLabel,
  readinessTone,
  relativeTime,
  titleCase,
} from '../../utils/format'

type SortKey = 'priority' | 'blocked' | 'capacity' | 'gpus'

function activeFilterCount(params: URLSearchParams) {
  return ['q', 'zone', 'asset', 'priority', 'stage', 'evidence'].filter((key) => params.get(key)).length
}

function filteredRows(rows: FollowUpItem[], params: URLSearchParams) {
  const query = (params.get('q') ?? '').trim().toLowerCase()
  const zone = params.get('zone')
  const asset = params.get('asset')
  const priority = params.get('priority')
  const stage = params.get('stage')
  const evidence = params.get('evidence')
  const sort = (params.get('sort') ?? 'priority') as SortKey

  return rows
    .filter((row) => !query || [row.request_number, row.request_title, row.asset_name, row.zone_name, row.recommended_action]
      .some((value) => value.toLowerCase().includes(query)))
    .filter((row) => !zone || row.zone_id === zone)
    .filter((row) => !asset || row.asset_id === asset)
    .filter((row) => !priority || row.priority_level === priority)
    .filter((row) => !stage || row.current_stage === stage)
    .filter((row) => {
      if (!evidence) return true
      const needsReview = row.impact_confidence_status !== 'TRUSTED' || row.impact_trust_issue_count > 0
      return evidence === 'review' ? needsReview : !needsReview
    })
    .sort((left, right) => {
      if (sort === 'blocked') return right.hours_in_current_stage - left.hours_in_current_stage
      if (sort === 'capacity') return right.estimated_capacity_risk_kw - left.estimated_capacity_risk_kw
      if (sort === 'gpus') return right.affected_gpu_count - left.affected_gpu_count
      return left.priority_rank - right.priority_rank
    })
}

export function RecoveryQueuePage() {
  const { data, metadata, loading, error, refreshedAt, refresh } = useDashboard()
  const [searchParams, setSearchParams] = useSearchParams()
  const [showFilters, setShowFilters] = useState(false)
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(null)
  const rows = useMemo(() => filteredRows(data?.followUps ?? [], searchParams), [data, searchParams])
  const selected = rows.find((row) => row.incident_id === selectedIncidentId) ?? rows[0] ?? null
  const filterCount = activeFilterCount(searchParams)

  const setParam = (key: string, value?: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next, { replace: true })
  }

  const clearFilters = () => {
    const next = new URLSearchParams()
    if (searchParams.get('sort')) next.set('sort', searchParams.get('sort')!)
    setSearchParams(next, { replace: true })
  }

  const critical = rows.filter((row) => row.priority_level === 'CRITICAL').length
  const restoreBlocked = rows.filter((row) => row.restore_readiness_status === 'NOT_READY').length
  const evidenceReview = rows.filter((row) => row.impact_confidence_status !== 'TRUSTED' || row.impact_trust_issue_count > 0).length
  const redundancyLost = rows.filter((row) => row.redundancy_state?.includes('LOST')).length

  return (
    <AppShell>
      <PageHeader
        eyebrow="Live operations"
        title="Recovery Queue"
        description="Prioritized interventions for safely returning AI infrastructure to service."
        actions={(
          <div className="freshness-actions">
            <div className={`freshness ${error ? 'warning' : ''}`}>
              <span className="freshness-dot" />
              <div><strong>{error ? 'Connection issue' : 'Semantic snapshot current'}</strong><span>{relativeTime(refreshedAt)}</span></div>
            </div>
            <button className="icon-button" type="button" onClick={() => void refresh()} aria-label="Refresh operations data" title="Refresh operations data">
              <RefreshCcw size={17} className={loading ? 'spin' : ''} />
            </button>
          </div>
        )}
      />

      {error && !data ? <ErrorState message={error} retry={() => void refresh()} /> : null}
      {loading && !data ? <LoadingState label="Loading the recovery queue" /> : null}

      {data ? (
        <>
          <section className="signal-strip" aria-label="Critical recovery signals">
            <Metric label="Active cases" value={rows.length} detail={`${data.followUps.length} across all filters`} icon={<Clock3 size={17} />} />
            <Metric label="Restore blocked" value={restoreBlocked} detail="Not ready for service" tone={restoreBlocked ? 'critical' : 'success'} icon={<AlertTriangle size={17} />} />
            <Metric label="Critical priority" value={critical} detail="Immediate coordination" tone={critical ? 'critical' : 'success'} icon={<ShieldAlert size={17} />} />
            <Metric label="Capacity exposed" value={`${rows.reduce((sum, row) => sum + row.estimated_capacity_risk_kw, 0).toFixed(0)} kW`} detail={`${rows.reduce((sum, row) => sum + row.affected_gpu_count, 0)} affected GPUs`} tone="warning" icon={<Zap size={17} />} />
            <Metric label="Evidence review" value={evidenceReview} detail={`${redundancyLost} with redundancy loss`} tone={evidenceReview ? 'warning' : 'success'} icon={<CheckCircle2 size={17} />} />
          </section>

          <section className="queue-toolbar" aria-label="Recovery queue controls">
            <label className="search-field">
              <Search size={17} />
              <span className="sr-only">Search recovery cases</span>
              <input
                type="search"
                placeholder="Search incident, asset, zone, or action"
                value={searchParams.get('q') ?? ''}
                onChange={(event) => setParam('q', event.target.value)}
              />
              {searchParams.get('q') ? <button type="button" onClick={() => setParam('q')} aria-label="Clear search"><X size={15} /></button> : null}
            </label>
            <div className="toolbar-actions">
              <label className="compact-select">
                <span>Sort</span>
                <select value={searchParams.get('sort') ?? 'priority'} onChange={(event) => setParam('sort', event.target.value)}>
                  <option value="priority">Operational priority</option>
                  <option value="blocked">Longest blocked</option>
                  <option value="capacity">Capacity exposure</option>
                  <option value="gpus">Affected GPUs</option>
                </select>
                <ChevronDown size={15} aria-hidden="true" />
              </label>
              <button className={`button secondary ${showFilters ? 'active' : ''}`} type="button" onClick={() => setShowFilters((value) => !value)} aria-expanded={showFilters}>
                <SlidersHorizontal size={16} /> Filters {filterCount ? <span className="count-badge">{filterCount}</span> : null}
              </button>
            </div>
          </section>

          {showFilters ? (
            <section className="filter-panel" aria-label="Recovery queue filters">
              <Filter size={18} />
              <FilterSelect label="Zone" value={searchParams.get('zone') ?? ''} onChange={(value) => setParam('zone', value)} options={metadata?.infrastructure_zones.map((item) => ({ value: item.id, label: item.name })) ?? []} />
              <FilterSelect label="Asset" value={searchParams.get('asset') ?? ''} onChange={(value) => setParam('asset', value)} options={metadata?.assets.map((item) => ({ value: item.id, label: item.name })) ?? []} />
              <FilterSelect label="Priority" value={searchParams.get('priority') ?? ''} onChange={(value) => setParam('priority', value)} options={(metadata?.priority_levels ?? ['CRITICAL', 'HIGH', 'MEDIUM']).map((value) => ({ value, label: titleCase(value) }))} />
              <FilterSelect label="Recovery stage" value={searchParams.get('stage') ?? ''} onChange={(value) => setParam('stage', value)} options={(metadata?.stages ?? []).map((value) => ({ value, label: titleCase(value) }))} />
              <FilterSelect label="Evidence" value={searchParams.get('evidence') ?? ''} onChange={(value) => setParam('evidence', value)} options={[{ value: 'trusted', label: 'Trusted' }, { value: 'review', label: 'Needs review' }]} />
            </section>
          ) : null}

          {filterCount ? (
            <div className="applied-filters" aria-label="Applied filters">
              <span>{rows.length} matching case{rows.length === 1 ? '' : 's'}</span>
              {[...searchParams.entries()].filter(([key, value]) => key !== 'sort' && value).map(([key, value]) => (
                <button key={key} type="button" onClick={() => setParam(key)}>{titleCase(key)}: {titleCase(value)} <X size={13} /></button>
              ))}
              <button className="clear-filters" type="button" onClick={clearFilters}>Clear all</button>
            </div>
          ) : null}

          <div className="queue-layout">
            <section className="queue-surface" aria-labelledby="queue-heading">
              <div className="queue-surface-heading">
                <div><span className="eyebrow">Decision workspace</span><h2 id="queue-heading">Ranked recovery cases</h2></div>
                <span>{rows.length} active</span>
              </div>
              <RecoveryTable rows={rows} selectedId={selected?.incident_id ?? null} onSelect={setSelectedIncidentId} />
            </section>
            <SelectedCasePanel row={selected} />
          </div>
        </>
      ) : null}
    </AppShell>
  )
}

function FilterSelect({ label, value, onChange, options }: {
  label: string
  value: string
  onChange: (value: string) => void
  options: { value: string; label: string }[]
}) {
  const uniqueOptions = [...new Map(options.map((option) => [option.value, option])).values()]
  return (
    <label className="filter-select">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">All</option>
        {uniqueOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
      <ChevronDown size={14} aria-hidden="true" />
    </label>
  )
}

function RecoveryTable({ rows, selectedId, onSelect }: { rows: FollowUpItem[]; selectedId: string | null; onSelect: (id: string) => void }) {
  if (!rows.length) {
    return <div className="queue-empty"><Search size={22} /><strong>No recovery cases match</strong><p>Remove one or more filters to return cases to the queue.</p></div>
  }
  return (
    <div className="recovery-table-wrap">
      <table className="recovery-table">
        <thead><tr><th>Priority</th><th>Recovery case</th><th>Current blocker</th><th>Exposure</th><th>Owner / dependency</th><th>Evidence</th><th><span className="sr-only">Open</span></th></tr></thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={row.incident_id}
              className={selectedId === row.incident_id ? 'selected' : ''}
              tabIndex={0}
              aria-selected={selectedId === row.incident_id}
              onClick={() => onSelect(row.incident_id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); onSelect(row.incident_id) }
              }}
            >
              <td data-label="Priority"><div className="priority-cell"><strong>#{row.priority_rank}</strong><StatusBadge label={titleCase(row.priority_level)} tone={priorityTone(row.priority_level)} icon={false} /></div></td>
              <td data-label="Recovery case"><div className="case-cell"><strong>{row.request_number}</strong><span>{row.asset_name}</span><small>{row.zone_name}</small></div></td>
              <td data-label="Current blocker"><div className="blocker-cell"><strong>{titleCase(row.current_stage)}</strong><span><Clock3 size={13} /> {formatHours(row.hours_in_current_stage)} in stage</span><small>{row.recommended_action}</small></div></td>
              <td data-label="Exposure"><div className="exposure-cell"><span><Cpu size={14} /> {row.affected_gpu_count} GPUs</span><span><Zap size={14} /> {row.estimated_capacity_risk_kw.toFixed(0)} kW</span>{row.redundancy_state?.includes('LOST') ? <small className="critical-text">Redundancy lost</small> : null}</div></td>
              <td data-label="Owner / dependency"><span className="owner-cell">{dependencyOwner(row)}</span></td>
              <td data-label="Evidence"><StatusBadge label={evidenceLabel(row.impact_confidence_status, row.impact_trust_issue_count)} tone={evidenceTone(row.impact_confidence_status, row.impact_trust_issue_count)} /></td>
              <td data-label="Open"><Link className="row-link" to={`/recovery-cases/${row.incident_id}`} onClick={(event) => event.stopPropagation()} aria-label={`Open recovery case ${row.request_number}`}><ArrowRight size={17} /></Link></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function SelectedCasePanel({ row }: { row: FollowUpItem | null }) {
  if (!row) return <aside className="case-preview empty"><strong>No case selected</strong><p>Select a recovery case to see the recommended intervention.</p></aside>
  return (
    <aside className="case-preview" aria-label={`Selected recovery case ${row.request_number}`}>
      <div className="case-preview-header">
        <div><span className="eyebrow">Selected case</span><h2>{row.request_number}</h2></div>
        <StatusBadge label={titleCase(row.priority_level)} tone={priorityTone(row.priority_level)} icon={false} />
      </div>
      <h3>{row.request_title}</h3>
      <div className="primary-action-callout"><span>Recommended next action</span><strong>{row.recommended_action}</strong></div>
      <dl className="preview-facts">
        <div><dt>Current blocker</dt><dd>{titleCase(row.current_stage)}</dd></div>
        <div><dt>Time blocked</dt><dd>{formatHours(row.hours_in_current_stage)}</dd></div>
        <div><dt>Owner / dependency</dt><dd>{dependencyOwner(row)}</dd></div>
        <div><dt>Operational exposure</dt><dd>{row.affected_gpu_count} GPUs · {row.estimated_capacity_risk_kw.toFixed(0)} kW</dd></div>
      </dl>
      <div className="preview-statuses">
        <StatusBadge label={readinessLabel(row.restore_readiness_status)} tone={readinessTone(row.restore_readiness_status)} />
        <StatusBadge label={evidenceLabel(row.impact_confidence_status, row.impact_trust_issue_count)} tone={evidenceTone(row.impact_confidence_status, row.impact_trust_issue_count)} />
      </div>
      <p className="preview-reason">{row.restore_readiness_summary ?? row.reason_summary}</p>
      <Link className="button primary full-width" to={`/recovery-cases/${row.incident_id}`}>Open recovery case <ArrowRight size={16} /></Link>
    </aside>
  )
}
