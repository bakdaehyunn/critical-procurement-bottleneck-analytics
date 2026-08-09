import { type ReactNode, useCallback, useEffect, useState } from 'react'
import { fetchDashboardData, fetchFilterMetadata, type DashboardData, type FilterMetadata } from '../api'
import { DashboardContext } from './dashboard-context'

export function DashboardProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<DashboardData | null>(null)
  const [metadata, setMetadata] = useState<FilterMetadata | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshedAt, setRefreshedAt] = useState<Date | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [dashboard, filterMetadata] = await Promise.all([
        fetchDashboardData(),
        fetchFilterMetadata().catch(() => null),
      ])
      setData(dashboard)
      setMetadata(filterMetadata)
      setRefreshedAt(new Date())
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Operations data is currently unavailable.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function loadInitialData() {
      try {
        const [dashboard, filterMetadata] = await Promise.all([
          fetchDashboardData(),
          fetchFilterMetadata().catch(() => null),
        ])
        if (!cancelled) {
          setData(dashboard)
          setMetadata(filterMetadata)
          setRefreshedAt(new Date())
        }
      } catch (requestError) {
        if (!cancelled) setError(requestError instanceof Error ? requestError.message : 'Operations data is currently unavailable.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void loadInitialData()
    return () => { cancelled = true }
  }, [])

  return <DashboardContext.Provider value={{ data, metadata, loading, error, refreshedAt, refresh }}>{children}</DashboardContext.Provider>
}
