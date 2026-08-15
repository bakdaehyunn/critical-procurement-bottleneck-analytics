import { useCallback, useEffect, useRef, useState } from 'react'

export type AsyncResource<T> = {
  data: T | null
  loading: boolean
  error: string | null
  stale: boolean
  refreshedAt: Date | null
  refresh: () => void
}

export function useAsyncResource<T>(loader: () => Promise<T>): AsyncResource<T> {
  const [data, setData] = useState<T | null>(null)
  const dataRef = useRef<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stale, setStale] = useState(false)
  const [refreshedAt, setRefreshedAt] = useState<Date | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    void Promise.resolve().then(() => {
      if (!cancelled) {
        setLoading(true)
        setError(null)
      }
    })
    void loader()
      .then((nextData) => {
        if (cancelled) return
        dataRef.current = nextData
        setData(nextData)
        setStale(false)
        setRefreshedAt(new Date())
      })
      .catch((requestError: unknown) => {
        if (cancelled) return
        setError(requestError instanceof Error ? requestError.message : 'The requested operations data is unavailable.')
        setStale(Boolean(dataRef.current))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [loader, reloadKey])

  const refresh = useCallback(() => setReloadKey((value) => value + 1), [])
  return { data, loading, error, stale, refreshedAt, refresh }
}
