import { describe, expect, it } from 'vitest'
import { buildFollowUps, type SemanticFollowUpQueueRecord } from './recoveryQueueRepository'

const coreRecord: SemanticFollowUpQueueRecord = {
  graphUri: 'urn:dcai:graph:reasoning:test',
  incidentUri: 'urn:dcai:incident:INC-1',
  incidentId: 'INC-1',
  assetUri: 'urn:dcai:asset:GPU-1',
  assetId: 'GPU-1',
  zoneUri: 'urn:dcai:zone:ZONE-A',
  zoneId: 'ZONE-A',
  stageUri: 'urn:dcai:stage:Validation',
  sourceRecordUri: 'urn:dcai:source-record:INC-1',
}

describe('recovery queue mapping', () => {
  it('keeps absent operational metrics unknown instead of fabricating zeroes', () => {
    const [item] = buildFollowUps([coreRecord], [])

    expect(item.hours_in_current_stage).toBeNull()
    expect(item.affected_gpu_count).toBeNull()
    expect(item.estimated_capacity_risk_kw).toBeNull()
    expect(item.total_priority_score).toBeNull()
    expect(item.current_status).toBe('UNKNOWN')
    expect(item.priority_level).toBe('UNKNOWN')
    expect(item.impact_confidence_status).toBe('UNKNOWN')
  })
})
