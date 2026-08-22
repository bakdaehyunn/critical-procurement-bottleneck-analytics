import { semanticQueryCatalog } from '../../semanticQueryCatalog'
import { postSemanticQuery } from '../../semanticQueryClient'
import {
  buildFollowUps,
  type SemanticFollowUpDetailRecord,
  type SemanticFollowUpQueueRecord,
  type SemanticTopologyDependencyRecord,
} from '../recovery-queue'
import {
  mapAiProposal,
  type SemanticActionReviewQueueRecord,
  type SemanticActionTransitionHistoryRecord,
  type SemanticAiProposalRecord,
} from '../review-inbox'

import type {
  RecoveryCaseActionResource,
  RecoveryCaseAiResource,
  RecoveryCaseEvidenceResource,
  RecoveryCaseImpactResource,
  RecoveryCasePlaybackResource,
  RecoveryCaseResource,
  RecoveryCaseResourceOutcome,
  RecoveryCaseSession,
  RecoveryCaseTimelineResource,
  RecoveryCaseTopologyResource,
} from './recoveryCaseModel'

import {
  buildRequestDetail,
  buildTopologyDependencies,
  mapDynamicPlayback,
  mapSemanticBlastRadius,
  mapSemanticDependencyImpact,
  mapSemanticIncidentEvidence,
  mapSemanticValidation,
  pickActionResource,
  pickEvidenceResource,
  pickTimelineResource,
  type SemanticActionAuditHistoryRecord,
  type SemanticActionAvailabilityRecord,
  type SemanticActionDispatchQueueRecord,
  type SemanticActionNotificationQueueRecord,
  type SemanticBlastRadiusRecord,
  type SemanticDependencyImpactRecord,
  type SemanticDynamicPlaybackRecord,
  type SemanticIncidentEvidenceRecord,
  type SemanticIncidentTimelineRecord,
  type SemanticValidationSummaryRecord,
} from './recoveryCaseMappers'

export async function openRecoveryCase(infrastructureRequestId: string, signal?: AbortSignal): Promise<RecoveryCaseSession> {
  const query = <T,>(queryId: Parameters<typeof postSemanticQuery<T>>[0], parameters: Record<string, string> = {}) => (
    postSemanticQuery<T>(queryId, parameters, { signal })
  )
  const [queueRecords, detailRecords] = await Promise.all([
    query<SemanticFollowUpQueueRecord>(semanticQueryCatalog.followUpQueueList),
    query<SemanticFollowUpDetailRecord>(semanticQueryCatalog.followUpDetail, { incidentIdParam: infrastructureRequestId }),
  ])
  const followUps = buildFollowUps(queueRecords, detailRecords)
  const request = followUps.find((row) => row.incident_id === infrastructureRequestId)
  if (!request) throw new Error(`Semantic finding not found: ${infrastructureRequestId}`)
  const detailRecord = detailRecords.find((record) => record.incidentId === infrastructureRequestId)
  const parameters = { incidentIdParam: infrastructureRequestId }

  const loadTimeline = async (): Promise<RecoveryCaseTimelineResource> => {
    const records = await query<SemanticIncidentTimelineRecord>(semanticQueryCatalog.incidentTimeline, parameters)
    const detail = buildRequestDetail(request, detailRecord, [], records)
    return { kind: 'timeline', detail: pickTimelineResource(detail) }
  }
  const loadEvidence = async (): Promise<RecoveryCaseEvidenceResource> => {
    const records = await query<SemanticIncidentEvidenceRecord>(semanticQueryCatalog.incidentEvidence, parameters)
    const detail = buildRequestDetail(request, detailRecord, records)
    return {
      kind: 'evidence',
      detail: pickEvidenceResource(detail),
      incidentEvidence: mapSemanticIncidentEvidence(infrastructureRequestId, records),
    }
  }
  const loadImpact = async (): Promise<RecoveryCaseImpactResource> => {
    const [validationRecords, dependencyRecords, blastRadiusRecords] = await Promise.all([
      query<SemanticValidationSummaryRecord>(semanticQueryCatalog.validationSummary),
      query<SemanticDependencyImpactRecord>(semanticQueryCatalog.dependencyImpactByAsset, { assetIdParam: request.asset_id }),
      query<SemanticBlastRadiusRecord>(semanticQueryCatalog.blastRadiusByAsset, { assetIdParam: request.asset_id }),
    ])
    return {
      kind: 'impact',
      semantic: {
        validation: mapSemanticValidation(validationRecords),
        dependencyImpact: mapSemanticDependencyImpact(request.asset_id, dependencyRecords),
        blastRadius: mapSemanticBlastRadius(request.asset_id, blastRadiusRecords),
      },
    }
  }
  const loadActions = async (): Promise<RecoveryCaseActionResource> => {
    const [availability, audit, notifications, reviews, transitions, dispatch] = await Promise.all([
      query<SemanticActionAvailabilityRecord>(semanticQueryCatalog.availableActionsByFinding, parameters),
      query<SemanticActionAuditHistoryRecord>(semanticQueryCatalog.actionAuditHistoryByIncident, parameters),
      query<SemanticActionNotificationQueueRecord>(semanticQueryCatalog.actionNotificationQueueByIncident, parameters),
      query<SemanticActionReviewQueueRecord>(semanticQueryCatalog.actionReviewQueueByIncident, parameters),
      query<SemanticActionTransitionHistoryRecord>(semanticQueryCatalog.actionTransitionHistoryByIncident, parameters),
      query<SemanticActionDispatchQueueRecord>(semanticQueryCatalog.actionDispatchQueueByIncident, parameters),
    ])
    const detail = buildRequestDetail(request, detailRecord, [], [], availability, audit, notifications, reviews, transitions, dispatch)
    return { kind: 'actions', detail: pickActionResource(detail) }
  }
  const loadAi = async (): Promise<RecoveryCaseAiResource> => {
    const records = await query<SemanticAiProposalRecord>(semanticQueryCatalog.aiProposalDetailByIncident, parameters)
    return { kind: 'ai', detail: { ai_proposals: records.map(mapAiProposal) } }
  }
  const loadPlayback = async (): Promise<RecoveryCasePlaybackResource> => {
    const [timeline, states, reasoning, actions] = await Promise.all([
      query<SemanticDynamicPlaybackRecord>(semanticQueryCatalog.dynamicEventTimelineByIncident, parameters),
      query<SemanticDynamicPlaybackRecord>(semanticQueryCatalog.dynamicStateChangesByIncident, parameters),
      query<SemanticDynamicPlaybackRecord>(semanticQueryCatalog.dynamicReasoningChangesByIncident, parameters),
      query<SemanticDynamicPlaybackRecord>(semanticQueryCatalog.dynamicActionLifecycleByIncident, parameters),
    ])
    return {
      kind: 'playback',
      detail: {
        dynamic_event_timeline: timeline.map(mapDynamicPlayback),
        dynamic_state_changes: states.map(mapDynamicPlayback),
        dynamic_reasoning_changes: reasoning.map(mapDynamicPlayback),
        dynamic_action_lifecycle: actions.map(mapDynamicPlayback),
      },
    }
  }
  const loadTopology = async (): Promise<RecoveryCaseTopologyResource> => {
    const records = await query<SemanticTopologyDependencyRecord>(semanticQueryCatalog.topologyDependencies)
    return { kind: 'topology', dependencies: buildTopologyDependencies(records, followUps) }
  }

  const loaders: { kind: RecoveryCaseResource['kind']; load: () => Promise<RecoveryCaseResource> }[] = [
    { kind: 'timeline', load: loadTimeline },
    { kind: 'evidence', load: loadEvidence },
    { kind: 'impact', load: loadImpact },
    { kind: 'actions', load: loadActions },
    { kind: 'ai', load: loadAi },
    { kind: 'playback', load: loadPlayback },
    { kind: 'topology', load: loadTopology },
  ]
  let resourceLoad: Promise<RecoveryCaseResourceOutcome[]> | undefined
  return {
    core: buildRequestDetail(request, detailRecord),
    loadResources: () => {
      resourceLoad ??= Promise.all(loaders.map(async ({ kind, load }): Promise<RecoveryCaseResourceOutcome> => {
        try {
          return { status: 'fulfilled', resource: await load() }
        } catch (reason) {
          return { status: 'rejected', kind, reason }
        }
      }))
      return resourceLoad
    },
  }
}
