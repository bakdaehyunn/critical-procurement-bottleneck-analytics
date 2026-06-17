export const semanticQueryCatalog = {
  actionAuditHistoryByIncident: 'semanticActionAuditHistoryByIncident',
  actionDispatchQueueByIncident: 'semanticActionDispatchQueueByIncident',
  actionNotificationQueueByIncident: 'semanticActionNotificationQueueByIncident',
  actionReviewQueueByIncident: 'semanticActionReviewQueueByIncident',
  actionTransitionHistoryByIncident: 'semanticActionTransitionHistoryByIncident',
  aiProposalDetailByIncident: 'semanticAiProposalDetailByIncident',
  assetDelaySummary: 'semanticAssetDelaySummary',
  availableActionsByFinding: 'semanticAvailableActionsByFinding',
  blastRadiusByAsset: 'semanticBlastRadiusByAsset',
  dashboardOverview: 'semanticDashboardOverview',
  dependencyImpactByAsset: 'semanticDependencyImpactByAsset',
  dynamicActionLifecycleByIncident: 'semanticDynamicActionLifecycleByIncident',
  dynamicEventTimelineByIncident: 'semanticDynamicEventTimelineByIncident',
  dynamicReasoningChangesByIncident: 'semanticDynamicReasoningChangesByIncident',
  dynamicStateChangesByIncident: 'semanticDynamicStateChangesByIncident',
  filterMetadata: 'semanticFilterMetadata',
  followUpDetail: 'semanticFollowUpDetail',
  followUpQueueList: 'semanticFollowUpQueueList',
  impactSummary: 'semanticImpactSummary',
  incidentEvidence: 'semanticIncidentEvidence',
  incidentTimeline: 'semanticIncidentTimeline',
  promotionReviewQueue: 'semanticPromotionReviewQueue',
  reasoningReviewQueue: 'semanticReasoningReviewQueue',
  spareWaitSummary: 'semanticSpareWaitSummary',
  stageBottlenecks: 'semanticStageBottlenecks',
  topologyDependencies: 'semanticTopologyDependencies',
  trustFindingList: 'semanticTrustFindingList',
  validationSummary: 'semanticValidationSummary',
  zoneDelaySummary: 'semanticZoneDelaySummary',
} as const

export type SemanticQueryId = typeof semanticQueryCatalog[keyof typeof semanticQueryCatalog]

export type SemanticQueryParameters = Record<string, string>

export function semanticQueryPath(queryId: SemanticQueryId): string {
  return `/semantic/query/${queryId}`
}
