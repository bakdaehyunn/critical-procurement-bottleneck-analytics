import { afterEach, describe, expect, it, vi } from 'vitest'
import { openRecoveryCase } from './recoveryCaseRepository'

const INCIDENT_ID = 'INC-TEST-1'

describe('recovery case resource loading', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('loads core first and issues every optional query exactly once', async () => {
    const requests: { queryId: string; signal?: AbortSignal | null }[] = []
    vi.stubGlobal('fetch', semanticFetch(requests))

    const controller = new AbortController()
    const session = await openRecoveryCase(INCIDENT_ID, controller.signal)

    expect(requests.map(({ queryId }) => queryId)).toEqual([
      'semanticFollowUpQueueList',
      'semanticFollowUpDetail',
    ])
    expect(session.core.request.incident_id).toBe(INCIDENT_ID)
    expect(session.core.work_orders).toEqual([])
    expect(session.core.stage_lead_times).toEqual([])

    const outcomes = await session.loadResources()
    await session.loadResources()
    const queryIds = requests.map(({ queryId }) => queryId)
    expect(outcomes.every(({ status }) => status === 'fulfilled')).toBe(true)
    expect(queryIds).toHaveLength(19)
    expect(new Set(queryIds).size).toBe(19)
    expect(queryIds.filter((id) => id === 'semanticFollowUpQueueList')).toHaveLength(1)
    expect(queryIds.filter((id) => id === 'semanticFollowUpDetail')).toHaveLength(1)
    expect(queryIds.filter((id) => id === 'semanticIncidentEvidence')).toHaveLength(1)
    expect(requests.every(({ signal }) => signal === controller.signal)).toBe(true)
  })

  it('keeps core data available when an optional resource fails', async () => {
    const requests: { queryId: string; signal?: AbortSignal | null }[] = []
    vi.stubGlobal('fetch', semanticFetch(requests, new Set(['semanticIncidentEvidence'])))

    const session = await openRecoveryCase(INCIDENT_ID)
    const outcomes = await session.loadResources()

    expect(session.core.request.incident_id).toBe(INCIDENT_ID)
    expect(outcomes.filter(({ status }) => status === 'rejected')).toEqual([
      expect.objectContaining({ status: 'rejected', kind: 'evidence' }),
    ])
    expect(outcomes.filter(({ status }) => status === 'fulfilled')).toHaveLength(6)
  })

  it('passes cancellation through core and optional resource requests', async () => {
    const requests: { queryId: string; signal?: AbortSignal | null }[] = []
    vi.stubGlobal('fetch', semanticFetch(requests))
    const controller = new AbortController()

    const session = await openRecoveryCase(INCIDENT_ID, controller.signal)
    controller.abort()
    const outcomes = await session.loadResources()

    expect(outcomes.every(({ status }) => status === 'rejected')).toBe(true)
    expect(outcomes.map((outcome) => outcome.status === 'rejected' ? outcome.kind : null)).toEqual([
      'timeline', 'evidence', 'impact', 'actions', 'ai', 'playback', 'topology',
    ])
  })
})

function semanticFetch(
  requests: { queryId: string; signal?: AbortSignal | null }[],
  failures: Set<string> = new Set(),
) {
  return vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const queryId = String(input).split('/').pop()!
    requests.push({ queryId, signal: init?.signal })
    if (init?.signal?.aborted) throw new DOMException('The operation was aborted.', 'AbortError')
    if (failures.has(queryId)) return new Response('unavailable', { status: 503, statusText: 'Unavailable' })
    const records = recordsFor(queryId)
    return new Response(JSON.stringify({
      queryId,
      resultType: 'records',
      recordCount: records.length,
      records,
      provenance: { queryId, graphScope: 'test graph', contractVersion: 'v1' },
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function recordsFor(queryId: string): unknown[] {
  const base = {
    graphUri: 'urn:dcai:graph:canonical:test',
    incidentUri: 'urn:dcai:incident:test-1',
    incidentId: INCIDENT_ID,
    assetUri: 'urn:dcai:asset:test-1',
    assetId: 'ASSET-1',
    zoneUri: 'urn:dcai:zone:test-1',
    zoneId: 'ZONE-1',
    stageUri: 'urn:dcai:ontology:Validation',
    stageLabel: 'Validation',
    sourceRecordUri: 'urn:dcai:source-record:test-1',
    requestTitle: 'Test recovery case',
  }
  if (queryId === 'semanticFollowUpQueueList' || queryId === 'semanticFollowUpDetail') return [base]
  return []
}
