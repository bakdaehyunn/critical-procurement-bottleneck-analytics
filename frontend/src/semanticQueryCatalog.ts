export const semanticQueryCatalog = {
  actionAuditHistoryByIncident: 'semanticActionAuditHistoryByIncident',
  actionDispatchQueueByIncident: 'semanticActionDispatchQueueByIncident',
  actionNotificationQueueByIncident: 'semanticActionNotificationQueueByIncident',
  actionReviewQueueByIncident: 'semanticActionReviewQueueByIncident',
  actionTransitionHistoryByIncident: 'semanticActionTransitionHistoryByIncident',
  aiProposalDetailByIncident: 'semanticAiProposalDetailByIncident',
  aiProposalReviewQueue: 'semanticAiProposalReviewQueue',
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
  platformStatus: 'semanticPlatformStatus',
  incidentEvidence: 'semanticIncidentEvidence',
  incidentTimeline: 'semanticIncidentTimeline',
  promotionReviewQueue: 'semanticPromotionReviewQueue',
  reasoningReviewQueue: 'semanticReasoningReviewQueue',
  topologyDependencies: 'semanticTopologyDependencies',
  trustFindingList: 'semanticTrustFindingList',
  validationSummary: 'semanticValidationSummary',
} as const

export type SemanticQueryId = typeof semanticQueryCatalog[keyof typeof semanticQueryCatalog]

export type SemanticQueryParameters = Record<string, string>

export const semanticQueryRequiredFields: Record<SemanticQueryId, readonly string[]> = {
  semanticActionAuditHistoryByIncident: ['executionId', 'requestId'],
  semanticActionDispatchQueueByIncident: ['dispatchId', 'executionId'],
  semanticActionNotificationQueueByIncident: ['notificationId', 'executionId'],
  semanticActionReviewQueueByIncident: ['executionId', 'currentState'],
  semanticActionTransitionHistoryByIncident: ['transitionId', 'toState'],
  semanticAiProposalDetailByIncident: ['proposalId', 'incidentId'],
  semanticAiProposalReviewQueue: ['proposalId', 'reviewStatus'],
  semanticAvailableActionsByFinding: ['actionId', 'detailKind'],
  semanticBlastRadiusByAsset: ['assetId'],
  semanticDashboardOverview: ['totalIncidents'],
  semanticDependencyImpactByAsset: ['assetId'],
  semanticDynamicActionLifecycleByIncident: ['eventId', 'incidentId'],
  semanticDynamicEventTimelineByIncident: ['eventId', 'incidentId'],
  semanticDynamicReasoningChangesByIncident: ['eventId', 'incidentId'],
  semanticDynamicStateChangesByIncident: ['eventId', 'incidentId'],
  semanticFilterMetadata: ['filterType', 'id'],
  semanticFollowUpDetail: ['incidentId', 'assetId'],
  semanticFollowUpQueueList: ['incidentId', 'assetId'],
  semanticIncidentEvidence: ['incidentId'],
  semanticIncidentTimeline: ['eventUri', 'incidentId'],
  semanticPlatformStatus: ['serviceBoundary', 'platformVerdict'],
  semanticPromotionReviewQueue: ['queueId', 'queueKind'],
  semanticReasoningReviewQueue: ['queueId', 'queueKind'],
  semanticTopologyDependencies: ['dependencyId', 'dependentAssetId'],
  semanticTrustFindingList: ['trustFindingUri', 'summary'],
  semanticValidationSummary: ['sourceRecordCount', 'incidentCount'],
}

export function semanticQueryPath(queryId: SemanticQueryId): string {
  return `/semantic/query/${queryId}`
}
