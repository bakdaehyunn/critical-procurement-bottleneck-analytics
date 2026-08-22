import { afterEach, describe, expect, it, vi } from 'vitest'
import { postSemanticQuery, postSemanticQueryPage } from './semanticQueryClient'

describe('semantic query response validation', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('rejects envelopes for a different query before exposing records', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => response({ queryId: 'semanticFollowUpDetail' })))

    await expect(postSemanticQuery('semanticFollowUpQueueList')).rejects.toThrow('queryId does not match')
  })

  it('requires authoritative and internally consistent page metadata', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => response({
      pageInfo: { page: 1, pageSize: 20, pageCount: 1, totalRecords: 21 },
    })))

    await expect(postSemanticQueryPage('semanticTrustFindingList', 1, 20)).rejects.toThrow('pageCount is inconsistent')
  })

  it('rejects records that do not satisfy the query-specific runtime contract', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => response({
      queryId: 'semanticFollowUpQueueList',
      recordCount: 1,
      records: [{ incidentId: 'INC-1' }],
    })))

    await expect(postSemanticQuery('semanticFollowUpQueueList')).rejects.toThrow('missing required field assetId')
  })

  it('returns a validated page without reconstructing totals in the client', async () => {
    const pageInfo = { page: 2, pageSize: 20, pageCount: 3, totalRecords: 41 }
    vi.stubGlobal('fetch', vi.fn(async () => response({ pageInfo })))

    await expect(postSemanticQueryPage('semanticTrustFindingList', 2, 20)).resolves.toMatchObject({ pageInfo })
  })
})

function response(overrides: Record<string, unknown> = {}) {
  const queryId = overrides.queryId ?? 'semanticTrustFindingList'
  return new Response(JSON.stringify({
    queryId,
    resultType: 'records',
    recordCount: 0,
    records: [],
    provenance: { queryId, graphScope: 'test graph', contractVersion: 'v1' },
    ...overrides,
  }), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
