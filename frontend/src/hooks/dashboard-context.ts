import { createContext } from 'react'
import type { DashboardData, FilterMetadata } from '../api'

export type DashboardContextValue = {
  data: DashboardData | null
  metadata: FilterMetadata | null
  loading: boolean
  error: string | null
  refreshedAt: Date | null
  refresh: () => Promise<void>
}

export const DashboardContext = createContext<DashboardContextValue | null>(null)
