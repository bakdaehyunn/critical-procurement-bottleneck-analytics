import type { ReactNode } from 'react'
import { AlertTriangle, ArrowRight, CheckCircle2, CircleHelp, Info, LoaderCircle } from 'lucide-react'
import { Link } from 'react-router-dom'

export type Tone = 'critical' | 'warning' | 'success' | 'info' | 'neutral'

export function StatusBadge({ label, tone = 'neutral', icon = true }: { label: string; tone?: Tone; icon?: boolean }) {
  const Icon = tone === 'critical' ? AlertTriangle : tone === 'warning' ? CircleHelp : tone === 'success' ? CheckCircle2 : Info
  return <span className={`status-badge ${tone}`}>{icon ? <Icon size={13} /> : null}{label}</span>
}

export function PageHeader({ eyebrow, title, description, actions }: {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        {eyebrow ? <span className="eyebrow">{eyebrow}</span> : null}
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {actions ? <div className="page-actions">{actions}</div> : null}
    </header>
  )
}

export function SectionHeading({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="section-heading">
      <div><h2>{title}</h2>{description ? <p>{description}</p> : null}</div>
      {action}
    </div>
  )
}

export function Metric({ label, value, detail, tone = 'neutral', icon }: {
  label: string
  value: ReactNode
  detail?: string
  tone?: Tone
  icon?: ReactNode
}) {
  return (
    <div className={`metric ${tone}`}>
      {icon ? <div className="metric-icon">{icon}</div> : null}
      <div><span>{label}</span><strong>{value}</strong>{detail ? <small>{detail}</small> : null}</div>
    </div>
  )
}

export function EmptyState({ title, description, action }: {
  title: string
  description: string
  action?: { label: string; href: string }
}) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon"><Info size={20} /></div>
      <strong>{title}</strong>
      <p>{description}</p>
      {action ? <Link className="button secondary" to={action.href}>{action.label}<ArrowRight size={15} /></Link> : null}
    </div>
  )
}

export function LoadingState({ label = 'Loading operations data' }: { label?: string }) {
  return <div className="loading-state"><LoaderCircle size={20} className="spin" /><span>{label}</span></div>
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="error-state" role="alert">
      <AlertTriangle size={20} />
      <div><strong>Operations data unavailable</strong><p>{message}</p></div>
      {retry ? <button type="button" className="button secondary" onClick={retry}>Try again</button> : null}
    </div>
  )
}

export function Disclosure({ title, summary, children }: { title: string; summary?: string; children: ReactNode }) {
  return (
    <details className="disclosure">
      <summary><span><strong>{title}</strong>{summary ? <small>{summary}</small> : null}</span><span className="disclosure-plus" /></summary>
      <div className="disclosure-body">{children}</div>
    </details>
  )
}

export function Pagination({ page, pageCount, total, pageSize, onChange, label }: {
  page: number
  pageCount: number
  total: number
  pageSize: number
  onChange: (page: number) => void
  label: string
}) {
  if (!total) return null
  const start = (page - 1) * pageSize + 1
  const end = Math.min(page * pageSize, total)
  return (
    <nav className="pagination" aria-label={label}>
      <span>Showing {start}–{end} of {total}</span>
      <div>
        <button type="button" className="button secondary" disabled={page <= 1} onClick={() => onChange(page - 1)}>Previous</button>
        <strong>Page {page} of {pageCount}</strong>
        <button type="button" className="button secondary" disabled={page >= pageCount} onClick={() => onChange(page + 1)}>Next</button>
      </div>
    </nav>
  )
}
