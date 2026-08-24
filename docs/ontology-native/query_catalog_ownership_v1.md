# Query Catalog Ownership

This is a current ownership supplement to `docs/04_api.md`. Runtime truth lives
in `QueryContractRegistry.kt`; the approved definitions live in
`queries/manifest.ttl`; and `QueryContractRegistryTest` checks that all
feature-owned IDs exactly match `frontend/src/semanticQueryCatalog.ts`.

## Feature owners

| Owner | Query IDs |
| --- | --- |
| Recovery Queue | `semanticFollowUpQueueList`, `semanticDashboardOverview`, `semanticFilterMetadata` |
| Recovery Case | `semanticFollowUpDetail`, `semanticTopologyDependencies`, `semanticValidationSummary`, `semanticIncidentEvidence`, `semanticIncidentTimeline`, `semanticDependencyImpactByAsset`, `semanticBlastRadiusByAsset`, `semanticAvailableActionsByFinding`, `semanticActionAuditHistoryByIncident`, `semanticActionNotificationQueueByIncident`, `semanticActionTransitionHistoryByIncident`, `semanticActionDispatchQueueByIncident`, `semanticDynamicEventTimelineByIncident`, `semanticDynamicStateChangesByIncident`, `semanticDynamicReasoningChangesByIncident`, `semanticDynamicActionLifecycleByIncident`, `semanticAiProposalDetailByIncident` |
| Review Inbox | `semanticPromotionReviewQueue`, `semanticReasoningReviewQueue`, `semanticActionReviewQueueByIncident`, `semanticAiProposalReviewQueue` |
| Platform Status | `semanticPlatformStatus`, `semanticTrustFindingList` |

Dynamic playback and AI proposal contracts operate on deterministic local
simulation/audit facts. Feature ownership does not make them live-stream or
external-AI capabilities.

## Backend-only owners

Internal inspection:

- `fixtureNamedGraphInventory`
- `fixtureIncidentSummary`
- `fixtureProvenanceSourceRecords`
- `semanticActionAuditHistoryByRelease`
- `semanticActionAuditHistoryByTarget`

Legacy read models retained for compatibility or backend inspection:

- `semanticImpactSummary`
- `semanticStageBottlenecks`
- `semanticAssetDelaySummary`
- `semanticZoneDelaySummary`
- `semanticSpareWaitSummary`

These IDs remain approved at the private service boundary but are not
frontend/product query contracts.

## Change rule

A new executable query requires a manifest entry, registry codec and owner,
result shaping/serialization tests, and—only for a feature owner—a frontend
catalog entry and consumer contract. Raw browser SPARQL and SPARQL Update remain
outside the boundary.
