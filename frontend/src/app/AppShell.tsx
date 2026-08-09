import type { ReactNode } from 'react'
import { Activity, ClipboardCheck, DatabaseZap, Menu, ServerCog, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useState } from 'react'

const navigation = [
  { to: '/', label: 'Recovery Queue', icon: Activity, end: true },
  { to: '/reviews', label: 'Review Inbox', icon: ClipboardCheck },
  { to: '/platform-status', label: 'Platform Status', icon: DatabaseZap },
]

export function AppShell({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)

  return (
    <div className="ops-app">
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <aside className={`app-sidebar ${open ? 'open' : ''}`} aria-label="Primary navigation">
        <div className="brand-lockup">
          <div className="brand-mark" aria-hidden="true"><ServerCog size={20} /></div>
          <div>
            <strong>Infrastructure Ops</strong>
            <span>Return-to-service console</span>
          </div>
          <button className="mobile-nav-close" type="button" onClick={() => setOpen(false)} aria-label="Close navigation">
            <X size={19} />
          </button>
        </div>
        <nav className="primary-nav">
          {navigation.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}>
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-context">
          <span className="context-dot" aria-hidden="true" />
          <div>
            <strong>Semantic operations</strong>
            <span>Read models connected</span>
          </div>
        </div>
      </aside>
      {open ? <button className="nav-scrim" type="button" onClick={() => setOpen(false)} aria-label="Close navigation" /> : null}
      <div className="app-stage">
        <header className="mobile-appbar">
          <button type="button" onClick={() => setOpen(true)} aria-label="Open navigation"><Menu size={20} /></button>
          <strong>Infrastructure Ops</strong>
        </header>
        <main id="main-content" className="main-content">{children}</main>
      </div>
    </div>
  )
}
