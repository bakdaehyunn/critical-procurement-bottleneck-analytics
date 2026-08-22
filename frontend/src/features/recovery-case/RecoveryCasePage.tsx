import { useRef, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Boxes,
  CheckCircle2,
  ChevronRight,
  CircleDot,
  Clock3,
  Cpu,
  Database,
  GitBranch,
  Network,
  RefreshCcw,
  ShieldCheck,
  Thermometer,
  UserRound,
  Wrench,
  Zap,
} from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  type InfrastructureDependency,
  type OntologyActionAffordance,
  type RequestDetail,
  type RequestSemanticContext,
} from './recoveryCaseModel'
import { submitOntologyActionRequest } from '../../ontologyActionApi'
import { AppShell } from '../../app/AppShell'
import { Disclosure, EmptyState, ErrorState, LoadingState, Metric, StatusBadge, type Tone } from '../../components/ui'
import {
  evidenceLabel,
  evidenceTone,
  formatDateTime,
  formatHours,
  priorityTone,
  readinessLabel,
  readinessTone,
  titleCase,
} from '../../utils/format'
import { actionAvailability, buildActionSubmission, type ActionInputValues } from './actionUtils'
import { useRecoveryCase } from './useRecoveryCase'
import { caseTabSearchParams, keyboardCaseTab, recoveryCaseTabs, resolveCaseTab, type CaseTab } from './recoveryCaseTabs'

export function RecoveryCasePage() {
  const { incidentId } = useParams()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = resolveCaseTab(searchParams.get('tab'))
  const { detail, semantic, selectedDependencies, loading, error, partialError, refresh } = useRecoveryCase(incidentId)
  const tabRefs = useRef<Partial<Record<CaseTab, HTMLButtonElement | null>>>({})

  const selectTab = (tab: CaseTab) => {
    setSearchParams(caseTabSearchParams(searchParams, tab))
  }

  const selectAndFocusTab = (tab: CaseTab) => {
    selectTab(tab)
    tabRefs.current[tab]?.focus()
  }

  return (
    <AppShell>
      <div className="case-page">
        <div className="case-breadcrumbs"><Link to="/"><ArrowLeft size={14} /> Recovery Queue</Link><ChevronRight size={14} /><span>{detail?.request.request_number ?? incidentId}</span></div>
        {loading && !detail ? <LoadingState label="Loading recovery case" /> : null}
        {error && !detail ? <ErrorState message={error} retry={refresh} /> : null}
        {detail ? (
          <>
            <CaseHeader detail={detail} onBack={() => navigate(-1)} onRefresh={refresh} loading={loading} />
            {partialError ? <div className="inline-notice warning" role="status"><AlertTriangle size={16} />{partialError}</div> : null}
            <nav className="case-tabs" role="tablist" aria-label="Recovery case workspaces">
              {recoveryCaseTabs.map((tab, index) => (
                <button
                  key={tab.id}
                  ref={(element) => { tabRefs.current[tab.id] = element }}
                  id={`case-tab-${tab.id}`}
                  type="button"
                  role="tab"
                  aria-selected={activeTab === tab.id}
                  aria-controls={`case-panel-${tab.id}`}
                  tabIndex={activeTab === tab.id ? 0 : -1}
                  className={activeTab === tab.id ? 'active' : ''}
                  onClick={() => selectTab(tab.id)}
                  onKeyDown={(event) => {
                    const nextTab = keyboardCaseTab(index, event.key)
                    if (!nextTab) return
                    event.preventDefault()
                    selectAndFocusTab(nextTab)
                  }}
                >{tab.label}</button>
              ))}
            </nav>
            <section id={`case-panel-${activeTab}`} className="case-workspace" role="tabpanel" aria-labelledby={`case-tab-${activeTab}`}>
              {activeTab === 'overview' ? <OverviewTab detail={detail} semantic={semantic} /> : null}
              {activeTab === 'recovery' ? <RecoveryTab detail={detail} onActionComplete={refresh} /> : null}
              {activeTab === 'impact' ? <ImpactTab detail={detail} /> : null}
              {activeTab === 'evidence' ? <EvidenceTab detail={detail} semantic={semantic} /> : null}
              {activeTab === 'dependencies' ? <DependenciesTab detail={detail} semantic={semantic} dependencies={selectedDependencies} /> : null}
            </section>
          </>
        ) : null}
      </div>
    </AppShell>
  )
}

function CaseHeader({ detail, onBack, onRefresh, loading }: { detail: RequestDetail; onBack: () => void; onRefresh: () => void; loading: boolean }) {
  const row = detail.request
  const owner = detail.work_orders[0]?.assigned_team ?? 'Unknown'
  return (
    <header className="case-header">
      <div className="case-header-main">
        <button type="button" className="icon-button quiet" onClick={onBack} aria-label="Go back"><ArrowLeft size={17} /></button>
        <div className="case-identity">
          <div><span className="eyebrow">Recovery case</span><h1>{row.request_number}</h1></div>
          <p>{row.request_title}</p>
          <div className="case-badges">
            <StatusBadge label={titleCase(row.priority_level)} tone={priorityTone(row.priority_level)} icon={false} />
            <StatusBadge label={readinessLabel(detail.restore_readiness.status)} tone={readinessTone(detail.restore_readiness.status)} />
            <StatusBadge label={evidenceLabel(detail.impact_confidence_status, detail.impact_trust_flags.length)} tone={evidenceTone(detail.impact_confidence_status, detail.impact_trust_flags.length)} />
          </div>
        </div>
        <button type="button" className="icon-button quiet" onClick={onRefresh} aria-label="Refresh recovery case"><RefreshCcw size={17} className={loading ? 'spin' : ''} /></button>
      </div>
      <dl className="case-header-facts">
        <div><dt>Current stage</dt><dd>{titleCase(row.current_stage)}</dd></div>
        <div><dt>Time blocked</dt><dd>{formatHours(row.hours_in_current_stage)}</dd></div>
        <div><dt>Owner</dt><dd>{owner}</dd></div>
        <div><dt>Exposure</dt><dd>{knownMetric(row.affected_gpu_count)} GPUs · {knownMetric(row.estimated_capacity_risk_kw, ' kW')}</dd></div>
      </dl>
      <div className="case-primary-action"><span>Recommended next action</span><strong>{row.recommended_action}</strong></div>
    </header>
  )
}

function OverviewTab({ detail, semantic }: { detail: RequestDetail; semantic: Partial<RequestSemanticContext> | null }) {
  const row = detail.request
  return (
    <div className="workspace-stack">
      <section className="decision-brief">
        <div className="decision-main"><span className="eyebrow">Operational brief</span><h2>{row.request_title}</h2><p>{detail.restore_readiness.summary ?? row.reason_summary}</p></div>
        <div className="decision-callout"><span>Decision now</span><strong>{row.recommended_action}</strong><small>Prioritized from the active blocker, exposure, and evidence state.</small></div>
      </section>
      <section className="case-metrics four">
        <Metric label="Blocker" value={titleCase(row.current_stage)} detail={`${formatHours(row.hours_in_current_stage)} in current stage`} tone="critical" icon={<Clock3 size={17} />} />
        <Metric label="Affected GPUs" value={knownMetric(row.affected_gpu_count)} detail={`${knownMetric(row.estimated_capacity_risk_kw, ' kW')} exposed`} tone={row.affected_gpu_count == null ? 'neutral' : 'warning'} icon={<Cpu size={17} />} />
        <Metric label="Redundancy" value={titleCase(row.redundancy_state)} detail={row.mitigation_status ? `Mitigation: ${titleCase(row.mitigation_status)}` : 'No mitigation state'} tone={row.redundancy_state?.includes('LOST') ? 'critical' : 'neutral'} icon={<ShieldCheck size={17} />} />
        <Metric label="Evidence" value={evidenceLabel(detail.impact_confidence_status, detail.impact_trust_flags.length)} detail={semantic?.incidentEvidence?.found ? 'Incident evidence linked' : 'Evidence link requires review'} tone={evidenceTone(detail.impact_confidence_status, detail.impact_trust_flags.length)} icon={<Database size={17} />} />
      </section>
      <section className="workspace-section">
        <div className="section-heading"><div><h2>Recovery progress</h2><p>The active stage and threshold breach are kept together for rapid shift handoff.</p></div></div>
        <RecoveryTimeline detail={detail} />
      </section>
      <section className="overview-grid">
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Current ownership</h2><p>Who can move the case forward</p></div></div><WorkOrderSummary detail={detail} /></div>
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Latest activity</h2><p>Most recent operational evidence</p></div></div><ActivityList detail={detail} limit={4} /></div>
      </section>
    </div>
  )
}

function RecoveryTimeline({ detail }: { detail: RequestDetail }) {
  return (
    <ol className="recovery-timeline" aria-label="Recovery stage timeline">
      {detail.stage_lead_times.map((stage, index) => {
        const active = !stage.exited_at || stage.stage === detail.request.current_stage
        const delayed = stage.delay_hours != null && stage.delay_hours > 0
        return (
          <li key={`${stage.stage}-${stage.entered_at}-${index}`} className={`${active ? 'active' : 'complete'} ${delayed ? 'delayed' : ''}`}>
            <div className="timeline-marker">{active ? <CircleDot size={17} /> : <CheckCircle2 size={16} />}</div>
            <div><span>{titleCase(stage.stage)}</span><strong>{formatHours(stage.duration_hours)}</strong><small>{delayed ? `${formatHours(stage.delay_hours)} over threshold` : stage.threshold_hours == null ? 'Threshold unknown' : `Threshold ${formatHours(stage.threshold_hours)}`}</small></div>
          </li>
        )
      })}
    </ol>
  )
}

function WorkOrderSummary({ detail }: { detail: RequestDetail }) {
  if (!detail.work_orders.length) return <EmptyState title="No work order linked" description="Ownership evidence has not been attached to this case." />
  return <div className="structured-list">{detail.work_orders.map((order) => <div key={order.work_order_id}><UserRound size={16} /><div><strong>{order.assigned_team}</strong><span>{titleCase(order.work_order_status)} · {order.assigned_engineer_id ?? 'Engineer unassigned'}</span>{order.required_spare_name ? <small>{order.required_spare_name} · {titleCase(order.stock_status)}</small> : null}</div></div>)}</div>
}

function ActivityList({ detail, limit }: { detail: RequestDetail; limit?: number }) {
  const events = limit ? detail.timeline.slice(0, limit) : detail.timeline
  if (!events.length) return <EmptyState title="No activity recorded" description="No operational timeline events are attached to this case." />
  return <div className="activity-list">{events.map((event) => <div key={`${event.event_id}-${event.occurred_at}`}><span className="activity-dot" /><div><strong>{event.message ?? titleCase(event.event_type)}</strong><span>{titleCase(event.stage)} · {formatDateTime(event.occurred_at)}</span><small>{titleCase(event.actor_type)}</small></div></div>)}</div>
}

function RecoveryTab({ detail, onActionComplete }: { detail: RequestDetail; onActionComplete: () => void }) {
  return (
    <div className="workspace-stack">
      <section className="workspace-section"><div className="section-heading"><div><h2>Recovery workflow</h2><p>Stage progress, threshold breaches, and the current intervention.</p></div></div><RecoveryTimeline detail={detail} /></section>
      <ActionPanel detail={detail} onActionComplete={onActionComplete} />
      <section className="overview-grid">
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Work orders & dependencies</h2><p>Execution ownership for this recovery</p></div></div><WorkOrderSummary detail={detail} /></div>
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Operational activity</h2><p>Event history for shift handoff</p></div></div><ActivityList detail={detail} /></div>
      </section>
      <Disclosure title="Governance and audit history" summary={`${detail.action_audit_history.length} audited requests · ${detail.action_transition_history.length} transitions`}>
        <AuditHistory detail={detail} />
      </Disclosure>
    </div>
  )
}

function ActionPanel({ detail, onActionComplete }: { detail: RequestDetail; onActionComplete: () => void }) {
  const [submitting, setSubmitting] = useState<string | null>(null)
  const [editing, setEditing] = useState<OntologyActionAffordance | null>(null)
  const [values, setValues] = useState<ActionInputValues>({ actorId: '', actionReason: '' })
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const openAction = (action: OntologyActionAffordance) => {
    setError(null)
    setEditing(action)
    setValues({
      actorId: '',
      actionReason: '',
      assignedTeam: action.action_id === 'AssignEvidenceReview' ? 'OPS_VALIDATION' : undefined,
      reviewedStatus: action.action_id === 'RecordValidationReview' ? 'NEEDS_REVIEW' : undefined,
      reviewSummary: action.action_id === 'RecordValidationReview' ? '' : undefined,
    })
  }
  const requestAction = async (action: OntologyActionAffordance) => {
    const submission = buildActionSubmission(action, values)
    if (!submission) { setError('Complete all required action fields before submitting.'); return }
    setSubmitting(action.action_id)
    setNotice(null)
    setError(null)
    try {
      const result = await submitOntologyActionRequest(submission)
      setNotice(`${action.label} requested · ${result.requestId}`)
      setEditing(null)
      onActionComplete()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Action request failed.')
    } finally {
      setSubmitting(null)
    }
  }
  return (
    <section className="workspace-section action-workspace">
      <div className="section-heading"><div><h2>Available interventions</h2><p>Governed local actions preserve provenance and do not write back to source systems.</p></div><StatusBadge label="Audit only" tone="info" /></div>
      {notice ? <div className="inline-notice success" role="status"><CheckCircle2 size={16} />{notice}</div> : null}
      {error ? <div className="inline-notice critical" role="alert"><AlertTriangle size={16} />{error}</div> : null}
      {editing ? <div className="action-request-form" aria-label={`${editing.label} request form`}><div className="section-heading"><div><h3>{editing.label}</h3><p>Enter accountable operator inputs. Submission creates an audited local request only.</p></div><button className="button secondary" type="button" disabled={Boolean(submitting)} onClick={() => setEditing(null)}>Cancel</button></div><div className="decision-form-grid"><label><span>Actor ID</span><input value={values.actorId} onChange={(event) => setValues((current) => ({ ...current, actorId: event.target.value }))} required /></label>{editing.action_id === 'AssignEvidenceReview' ? <><label><span>Assigned team</span><input value={values.assignedTeam ?? ''} onChange={(event) => setValues((current) => ({ ...current, assignedTeam: event.target.value }))} required /></label><label><span>Assignee ID (optional)</span><input value={values.assigneeId ?? ''} onChange={(event) => setValues((current) => ({ ...current, assigneeId: event.target.value }))} /></label></> : null}{editing.action_id === 'RecordValidationReview' ? <label><span>Reviewed status</span><select value={values.reviewedStatus ?? ''} onChange={(event) => setValues((current) => ({ ...current, reviewedStatus: event.target.value }))}><option value="NEEDS_REVIEW">Needs review</option><option value="PASSED">Passed</option><option value="FAILED">Failed</option></select></label> : null}<label className="wide"><span>Action reason</span><textarea value={values.actionReason} onChange={(event) => setValues((current) => ({ ...current, actionReason: event.target.value }))} required /></label>{editing.action_id === 'RecordValidationReview' ? <label className="wide"><span>Review summary</span><textarea value={values.reviewSummary ?? ''} onChange={(event) => setValues((current) => ({ ...current, reviewSummary: event.target.value }))} required /></label> : null}<div className="decision-form-actions wide"><button className="button primary" type="button" disabled={Boolean(submitting)} onClick={() => void requestAction(editing)}>{submitting ? 'Requesting…' : 'Submit audited request'}</button></div></div></div> : null}
      {detail.ontology_actions.length ? <div className="action-list">{detail.ontology_actions.map((action) => {
        const availability = actionAvailability(action)
        return <article key={action.action_id}><div className="action-icon"><Wrench size={17} /></div><div className="action-copy"><strong>{action.label}</strong><p>{action.description}</p><small>{availability.reason}</small></div><button type="button" className="button secondary" disabled={!availability.available || Boolean(submitting)} onClick={() => openAction(action)}>Configure request<ArrowRight size={14} /></button></article>
      })}</div> : <EmptyState title="No governed intervention available" description="The current case has no action affordance from the approved semantic action catalog." />}
    </section>
  )
}

function AuditHistory({ detail }: { detail: RequestDetail }) {
  const rows = detail.action_audit_history
  const transitions = detail.action_transition_history
  if (!rows.length && !transitions.length) return <EmptyState title="No governed history yet" description="Governed action requests and lifecycle transitions will appear here with actor, status, and provenance." />
  return <div className="workspace-stack">{rows.length ? <div className="audit-table-wrap"><table className="audit-table"><thead><tr><th>Action</th><th>Status</th><th>Actor</th><th>Requested</th><th>Validation</th></tr></thead><tbody>{rows.map((row) => <tr key={row.execution_uri}><td>{row.action_type_label ?? titleCase(row.action_type_id)}</td><td><StatusBadge label={titleCase(row.action_status)} tone={statusTone(row.action_status)} /></td><td>{row.actor_id}</td><td>{formatDateTime(row.requested_at)}</td><td>{titleCase(row.validation_status)}</td></tr>)}</tbody></table></div> : null}{transitions.length ? <div className="audit-table-wrap"><table className="audit-table"><thead><tr><th>Transition</th><th>Actor</th><th>Reason</th><th>Recorded</th></tr></thead><tbody>{transitions.map((row) => <tr key={row.transition_uri}><td>{row.from_state ? titleCase(row.from_state) : 'Initial'} → {titleCase(row.to_state)}</td><td>{row.actor_id}</td><td>{row.transition_reason}</td><td>{formatDateTime(row.generated_at)}</td></tr>)}</tbody></table></div> : null}</div>
}

function ImpactTab({ detail }: { detail: RequestDetail }) {
  const impact = detail.impact_snapshot
  if (!impact) return <EmptyState title="No impact snapshot" description="Capacity and redundancy exposure has not been calculated for this case." />
  const powerState = impact.power_redundancy_lost == null ? 'Unknown' : impact.power_redundancy_lost ? 'Lost' : 'Available'
  const coolingState = impact.cooling_redundancy_lost == null ? 'Unknown' : impact.cooling_redundancy_lost ? 'Lost' : 'Available'
  return (
    <div className="workspace-stack">
      <section className="impact-hero"><div><span className="eyebrow">Operational exposure</span><h2>{knownMetric(impact.affected_gpu_count, ' GPUs')} and {knownMetric(impact.estimated_capacity_risk_kw, ' kW')} remain at risk</h2><p>{knownMetric(impact.affected_rack_count, ' racks')} are included in the latest impact snapshot.</p></div><StatusBadge label={titleCase(impact.redundancy_state)} tone={impact.redundancy_state.includes('LOST') ? 'critical' : impact.redundancy_state === 'Unknown' ? 'neutral' : 'success'} /></section>
      <section className="case-metrics four">
        <Metric label="GPU capacity" value={knownMetric(impact.estimated_gpu_capacity_risk_pct, '%', 1)} detail={`${knownMetric(impact.affected_gpu_count)} affected GPUs`} tone={impact.estimated_gpu_capacity_risk_pct == null ? 'neutral' : 'warning'} icon={<Cpu size={17} />} />
        <Metric label="Power redundancy" value={powerState} detail="Power path state" tone={impact.power_redundancy_lost == null ? 'neutral' : impact.power_redundancy_lost ? 'critical' : 'success'} icon={<Zap size={17} />} />
        <Metric label="Cooling redundancy" value={coolingState} detail="Cooling path state" tone={impact.cooling_redundancy_lost == null ? 'neutral' : impact.cooling_redundancy_lost ? 'critical' : 'success'} icon={<Activity size={17} />} />
        <Metric label="Thermal exposure" value={knownMetric(impact.thermal_breach_minutes, 'm')} detail="Breach duration" tone={impact.thermal_breach_minutes == null ? 'neutral' : impact.thermal_breach_minutes ? 'warning' : 'success'} icon={<Thermometer size={17} />} />
      </section>
      <section className="overview-grid">
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Recovery dependencies</h2><p>Vendor, spare, and mitigation state</p></div></div><div className="evidence-rows"><EvidenceRow label="Vendor state" value={titleCase(impact.vendor_status)} detail={impact.vendor_eta_at ? `ETA ${formatDateTime(impact.vendor_eta_at)}` : 'No ETA recorded'} tone={impact.vendor_status.includes('MISSED') ? 'critical' : 'neutral'} /><EvidenceRow label="Mitigation" value={titleCase(impact.mitigation_status)} detail="Latest operating state" tone={impact.mitigation_status.includes('DEGRADED') ? 'warning' : 'success'} /><EvidenceRow label="Snapshot source" value={titleCase(impact.source_system)} detail={formatDateTime(impact.snapshot_at)} /></div></div>
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Telemetry evidence</h2><p>Readings attached to the impact snapshot</p></div></div>{impact.telemetry_readings.length ? <div className="evidence-rows">{impact.telemetry_readings.map((reading) => <EvidenceRow key={reading.metric} label={titleCase(reading.metric)} value={`${reading.value} ${reading.unit}`} detail={titleCase(reading.status)} tone={reading.status.includes('CRITICAL') ? 'critical' : reading.status.includes('WARNING') ? 'warning' : 'neutral'} />)}</div> : <EmptyState title="No telemetry readings" description="The impact snapshot has no attached telemetry readings." />}</div>
      </section>
      <Disclosure title="Priority score inputs" summary="Explain why this case ranks where it does"><div className="score-grid">{[
        ['Downtime', detail.request.downtime_score], ['Stage delay', detail.request.stage_delay_score], ['Capacity risk', detail.request.capacity_risk_score], ['Redundancy risk', detail.request.redundancy_risk_score], ['Thermal risk', detail.request.thermal_risk_score], ['Vendor risk', detail.request.vendor_eta_risk_score],
      ].map(([label, value]) => <div key={String(label)}><span>{label}</span><strong>{knownMetric(value as number | null, '', 1)}</strong></div>)}</div></Disclosure>
    </div>
  )
}

function knownMetric(value: number | null, suffix = '', fractionDigits = 0): string {
  return value == null ? 'Unknown' : `${value.toFixed(fractionDigits)}${suffix}`
}

function EvidenceTab({ detail, semantic }: { detail: RequestDetail; semantic: Partial<RequestSemanticContext> | null }) {
  const validationStatus = semantic?.validation?.status ?? 'UNKNOWN'
  const validationPassed = validationStatus === 'CONFORMS'
  const evidenceStatus = detail.impact_confidence_status
  const evidenceTrusted = evidenceStatus === 'TRUSTED' && detail.impact_trust_flags.length === 0
  const evidenceVerdict = detail.impact_trust_flags.length
    ? 'Review the evidence before relying on this recommendation'
    : evidenceTrusted
      ? 'Evidence is trusted for the latest analysis run'
      : 'No authoritative evidence verdict is available'
  const incidentEvidence = semantic?.incidentEvidence
  const incidentEvidenceLabel = incidentEvidence == null ? 'Unknown' : incidentEvidence.found ? 'Linked' : 'Missing'
  return (
    <div className="workspace-stack">
      <section className="evidence-verdict"><div className={`verdict-icon ${evidenceTrusted ? 'success' : 'warning'}`}>{evidenceTrusted ? <ShieldCheck size={22} /> : <AlertTriangle size={22} />}</div><div><span className="eyebrow">Evidence verdict</span><h2>{evidenceVerdict}</h2><p>{detail.restore_readiness.summary ?? 'No restore-readiness caveat was recorded.'}</p></div></section>
      <section className="case-metrics four">
        <Metric label="Impact confidence" value={titleCase(detail.impact_confidence_status)} detail={`${detail.impact_trust_flags.length} issue${detail.impact_trust_flags.length === 1 ? '' : 's'}`} tone={evidenceTone(detail.impact_confidence_status, detail.impact_trust_flags.length)} icon={<ShieldCheck size={17} />} />
        <Metric label="Source quality" value={detail.quality_flags.length ? 'Needs review' : evidenceTrusted ? 'Current' : 'Unknown'} detail={`${detail.quality_flags.length} source flag${detail.quality_flags.length === 1 ? '' : 's'}`} tone={detail.quality_flags.length ? 'warning' : evidenceTrusted ? 'success' : 'neutral'} icon={<Database size={17} />} />
        <Metric label="Graph validation" value={titleCase(validationStatus)} detail={validationStatus === 'UNKNOWN' ? 'No authoritative validation summary' : `${semantic?.validation?.issue_count ?? 0} validation issues`} tone={validationPassed ? 'success' : validationStatus === 'UNKNOWN' ? 'neutral' : 'warning'} icon={<CheckCircle2 size={17} />} />
        <Metric label="Incident evidence" value={incidentEvidenceLabel} detail="Incident-to-asset assertion" tone={incidentEvidence == null ? 'neutral' : incidentEvidence.found ? 'success' : 'critical'} icon={<GitBranch size={17} />} />
      </section>
      <section className="overview-grid">
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Evidence issues</h2><p>What needs verification before use</p></div></div>{detail.impact_trust_flags.length ? <div className="issue-list">{detail.impact_trust_flags.map((flag, index) => <article key={`${flag.issue_type}-${index}`}><AlertTriangle size={16} /><div><strong>{titleCase(flag.issue_type)}</strong><p>{flag.message}</p><small>{titleCase(flag.severity)}</small></div></article>)}</div> : <EmptyState title="No impact evidence issues" description="Impact evidence matches the latest analysis run." />}</div>
        <div className="workspace-section compact"><div className="section-heading"><div><h2>Validation records</h2><p>Operational validation evidence</p></div></div>{detail.validation_results.length ? <div className="evidence-rows">{detail.validation_results.map((item) => <EvidenceRow key={item.validation_id} label={item.validator_id ?? 'Unassigned validator'} value={titleCase(item.validation_status)} detail={item.failure_reason ?? formatDateTime(item.validation_completed_at)} tone={item.validation_status === 'PASSED' ? 'success' : 'warning'} />)}</div> : <EmptyState title="No validation records" description="No return-to-service validation record is linked." />}</div>
      </section>
      <Disclosure title="Technical semantic evidence" summary="SHACL issues, graph facts, and provenance for specialist review">
        <div className="technical-grid">
          <TechnicalGroup title="Graph validation" rows={(semantic?.validation?.issues ?? []).map((issue) => ({ title: issue.message, detail: `${issue.focus_node} · ${issue.result_path}`, meta: issue.severity }))} empty="No SHACL contract issues" />
          <TechnicalGroup title="Provenance chain" rows={detail.provenance_trace.map((item) => ({ title: item.label, detail: item.detail, meta: item.resource_uri }))} empty="No provenance chain available" />
          <TechnicalGroup title="Direct facts" rows={detail.ontology_evidence.direct_facts.map((fact) => ({ title: fact.label, detail: fact.value, meta: fact.resource_uri ?? fact.detail }))} empty="No direct ontology facts" />
          <TechnicalGroup title="Inferred facts" rows={detail.ontology_evidence.inferred_facts.map((fact) => ({ title: fact.label, detail: fact.value, meta: fact.resource_uri ?? fact.detail }))} empty="No inferred ontology facts" />
        </div>
      </Disclosure>
    </div>
  )
}

function TechnicalGroup({ title, rows, empty }: { title: string; rows: { title: string; detail: string; meta: string }[]; empty: string }) {
  return <section><h3>{title}</h3>{rows.length ? <div className="technical-list">{rows.map((row, index) => <div key={`${row.title}-${index}`}><strong>{row.title}</strong><span>{row.detail}</span><code>{row.meta}</code></div>)}</div> : <p className="technical-empty">{empty}</p>}</section>
}

function DependenciesTab({ detail, semantic, dependencies }: { detail: RequestDetail; semantic: Partial<RequestSemanticContext> | null; dependencies: InfrastructureDependency[] }) {
  const activePathIncidents = dependencies.reduce((sum, item) => sum + item.dependent_active_incident_count + item.dependency_active_incident_count, 0)
  return (
    <div className="workspace-stack">
      <section className="dependency-question"><div><span className="eyebrow">Dependency question</span><h2>Which power, cooling, and infrastructure paths amplify this blocker?</h2><p>Topology is shown only where it changes recovery priority or blast-radius understanding.</p></div><Network size={26} /></section>
      <section className="case-metrics four">
        <Metric label="Direct paths" value={dependencies.length} detail="Configured asset relationships" icon={<Network size={17} />} />
        <Metric label="Path incidents" value={activePathIncidents} detail="Active incidents on related paths" tone={activePathIncidents ? 'warning' : 'success'} icon={<AlertTriangle size={17} />} />
        <Metric label="Inferred downstream" value={semantic?.blastRadius ? semantic.blastRadius.inferred_downstream_assets.length : 'Unknown'} detail="Reasoning graph traversal" tone={semantic?.blastRadius ? 'warning' : 'neutral'} icon={<GitBranch size={17} />} />
        <Metric label="Capacity exposed" value={knownMetric(detail.request.estimated_capacity_risk_kw, ' kW')} detail={`${knownMetric(detail.request.affected_gpu_count)} GPUs`} tone={detail.request.estimated_capacity_risk_kw == null ? 'neutral' : 'warning'} icon={<Zap size={17} />} />
      </section>
      <section className="workspace-section"><div className="section-heading"><div><h2>Infrastructure dependency paths</h2><p>Direct relationships involving {detail.request.asset_name}.</p></div></div>{dependencies.length ? <div className="dependency-list">{dependencies.map((item, index) => <article key={`${item.dependency_id}-${index}`}><div className="dependency-type"><Boxes size={16} /><span>{titleCase(item.dependency_role)}</span></div><div className="dependency-flow"><div><small>Dependent</small><strong>{item.dependent_asset_name}</strong><span>{titleCase(item.dependent_status)}</span></div><ArrowRight size={18} /><div><small>Dependency</small><strong>{item.dependency_asset_name}</strong><span>{titleCase(item.dependency_status)}</span></div></div><StatusBadge label={titleCase(item.impact_scope)} tone={item.dependency_active_incident_count || item.dependent_active_incident_count ? 'warning' : 'neutral'} /></article>)}</div> : <EmptyState title="No direct dependency paths" description="No configured power, cooling, or telemetry path matches this asset." />}</section>
      <Disclosure title="Reasoning and blast-radius evidence" summary={semantic?.blastRadius ? `${semantic.blastRadius.affected_incident_count} affected incidents` : 'Affected incident count unknown'}><div className="technical-grid"><TechnicalGroup title="Dependency findings" rows={(semantic?.dependencyImpact?.reasoning_findings ?? []).map((item) => ({ title: item.summary, detail: item.source_record_uri ?? 'No source record', meta: item.finding_uri }))} empty="No dependency reasoning findings" /><TechnicalGroup title="Blast-radius findings" rows={(semantic?.blastRadius?.reasoning_findings ?? []).map((item) => ({ title: item.summary, detail: item.source_record_uri ?? 'No source record', meta: item.finding_uri }))} empty="No blast-radius reasoning findings" /></div></Disclosure>
    </div>
  )
}

function EvidenceRow({ label, value, detail, tone = 'neutral' }: { label: string; value: string; detail: string; tone?: Tone }) {
  return <div className={`evidence-row ${tone}`}><div><span>{label}</span><strong>{value}</strong></div><small>{detail}</small></div>
}

function statusTone(status: string): Tone {
  if (status === 'APPROVED' || status === 'CLOSED' || status === 'PASSED') return 'success'
  if (status === 'REJECTED' || status === 'FAILED') return 'critical'
  return 'warning'
}
