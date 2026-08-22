import { useEffect, useMemo, useState } from 'react'
import type {
  InfrastructureDependency,
  RecoveryCaseResource,
  RequestDetail,
  RequestSemanticContext,
} from './recoveryCaseModel'
import { openRecoveryCase } from './recoveryCaseRepository'

function resourceLabel(kind: RecoveryCaseResource['kind']): string {
  return ({
    timeline: 'workflow timeline',
    evidence: 'evidence and trust',
    impact: 'impact reasoning',
    actions: 'governed actions',
    ai: 'AI governance',
    playback: 'dynamic playback',
    topology: 'dependency topology',
  } as const)[kind]
}

export function useRecoveryCase(incidentId: string | undefined) {
  const [detail, setDetail] = useState<RequestDetail | null>(null)
  const [semantic, setSemantic] = useState<Partial<RequestSemanticContext> | null>(null)
  const [dependencies, setDependencies] = useState<InfrastructureDependency[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [partialError, setPartialError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    const controller = new AbortController()

    async function loadCase() {
      if (!incidentId) {
        setLoading(false)
        setError('Recovery case identifier is missing.')
        return
      }
      setLoading(true)
      setError(null)
      setPartialError(null)
      try {
        const session = await openRecoveryCase(incidentId, controller.signal)
        if (!cancelled) {
          setDetail(session.core)
          setSemantic(null)
          setDependencies([])
        }
        const outcomes = await session.loadResources()
        if (!cancelled) {
          let mergedDetail = session.core
          let mergedSemantic: Partial<RequestSemanticContext> = {}
          let topology: InfrastructureDependency[] = []
          const failed: RecoveryCaseResource['kind'][] = []
          outcomes.forEach((outcome) => {
            if (outcome.status === 'rejected') {
              failed.push(outcome.kind)
              return
            }
            const resource = outcome.resource
            if ('detail' in resource) mergedDetail = { ...mergedDetail, ...resource.detail }
            if (resource.kind === 'evidence') {
              mergedSemantic = { ...mergedSemantic, incidentEvidence: resource.incidentEvidence }
            }
            if (resource.kind === 'impact') mergedSemantic = { ...mergedSemantic, ...resource.semantic }
            if (resource.kind === 'topology') topology = resource.dependencies
          })
          setDetail(mergedDetail)
          setSemantic(Object.keys(mergedSemantic).length ? mergedSemantic : null)
          setDependencies(topology)
          setPartialError(
            failed.length
              ? `${failed.map(resourceLabel).join(', ')} could not be refreshed. Core case data remains available.`
              : null,
          )
        }
      } catch (requestError) {
        if (!cancelled && !(requestError instanceof DOMException && requestError.name === 'AbortError')) {
          setError(requestError instanceof Error ? requestError.message : 'Recovery case is unavailable.')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void loadCase()
    return () => {
      cancelled = true
      controller.abort()
    }
  }, [incidentId, reloadKey])

  const selectedDependencies = useMemo(() => {
    if (!detail) return []
    return dependencies.filter(
      (item) => item.dependent_asset_id === detail.request.asset_id
        || item.dependency_asset_id === detail.request.asset_id,
    )
  }, [dependencies, detail])

  return {
    detail,
    semantic,
    selectedDependencies,
    loading,
    error,
    partialError,
    refresh: () => setReloadKey((value) => value + 1),
  }
}
