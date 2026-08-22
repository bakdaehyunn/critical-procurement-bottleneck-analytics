import { describe, expect, it } from 'vitest'
import { mapTrustFinding } from './platformStatusRepository'

describe('platform status mapping', () => {
  it('does not invent status, severity, activity, timestamp, or failure volume', () => {
    const item = mapTrustFinding({
      trustFindingUri: 'urn:dcai:finding:1',
      summary: 'Evidence needs review',
      sourceFactUri: 'urn:dcai:fact:1',
    })

    expect(item.pipeline_run_id).toBeNull()
    expect(item.status).toBe('UNKNOWN')
    expect(item.severity).toBe('UNKNOWN')
    expect(item.failed_row_count).toBeNull()
    expect(item.created_at).toBeNull()
  })
})
