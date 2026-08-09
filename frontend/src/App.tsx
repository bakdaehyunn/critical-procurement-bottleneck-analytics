import { Route, Routes } from 'react-router-dom'
import { AppShell } from './app/AppShell'
import { DashboardProvider } from './hooks/DashboardProvider'
import { PlatformStatusPage } from './features/platform-status/PlatformStatusPage'
import { RecoveryCasePage } from './features/recovery-case/RecoveryCasePage'
import { RecoveryQueuePage } from './features/recovery-queue/RecoveryQueuePage'
import { ReviewInboxPage } from './features/review-inbox/ReviewInboxPage'
import { EmptyState } from './components/ui'
import './App.css'

function NotFoundPage() {
  return (
    <AppShell>
      <EmptyState
        title="Workspace not found"
        description="The requested operations workspace is unavailable. Return to the recovery queue to continue."
        action={{ label: 'Return to recovery queue', href: '/' }}
      />
    </AppShell>
  )
}

export default function App() {
  return (
    <DashboardProvider>
      <Routes>
        <Route path="/" element={<RecoveryQueuePage />} />
        <Route path="/recovery-cases/:incidentId" element={<RecoveryCasePage />} />
        <Route path="/findings/:incidentId" element={<RecoveryCasePage />} />
        <Route path="/reviews" element={<ReviewInboxPage />} />
        <Route path="/platform-status" element={<PlatformStatusPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </DashboardProvider>
  )
}
