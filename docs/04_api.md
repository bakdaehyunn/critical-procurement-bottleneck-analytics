# Semantic API

The implemented HTTP surface is loopback/private. It has no authentication,
authorization, TLS termination, or supported public binding.

## Implemented private routes

```text
POST /semantic/query/{queryId}
POST /semantic/internal/action-request
POST /semantic/internal/action-transition
POST /semantic/internal/ai-proposal-review
```

Runtime status is authoritative in
`semantic-service/openapi.semantic-service.yaml`: only paths marked
`x-runtime-status: implemented-private` are endpoints. Paths marked
`documented-only` are design contracts, not available routes.

The query endpoint accepts an approved query ID and string parameters. It
rejects raw SPARQL and SPARQL Update. Responses include `queryId`, `resultType`,
`recordCount`, `records`, and provenance metadata. Pageable contracts also
include stable-identity `pageInfo`.

## Feature-owned query IDs

The runtime owner in `QueryContractRegistry.kt` is the authority. The frontend
catalog is tested to match every non-legacy, non-inspection owner.

Recovery Queue:

- `semanticFollowUpQueueList`
- `semanticDashboardOverview`
- `semanticFilterMetadata`

Recovery Case:

- `semanticFollowUpDetail`
- `semanticTopologyDependencies`
- `semanticValidationSummary`
- `semanticIncidentEvidence`
- `semanticIncidentTimeline`
- `semanticDependencyImpactByAsset`
- `semanticBlastRadiusByAsset`
- `semanticAvailableActionsByFinding`
- `semanticActionAuditHistoryByIncident`
- `semanticActionNotificationQueueByIncident`
- `semanticActionTransitionHistoryByIncident`
- `semanticActionDispatchQueueByIncident`
- `semanticDynamicEventTimelineByIncident`
- `semanticDynamicStateChangesByIncident`
- `semanticDynamicReasoningChangesByIncident`
- `semanticDynamicActionLifecycleByIncident`
- `semanticAiProposalDetailByIncident`

Review Inbox:

- `semanticPromotionReviewQueue`
- `semanticReasoningReviewQueue`
- `semanticActionReviewQueueByIncident`
- `semanticAiProposalReviewQueue`

Platform Status:

- `semanticPlatformStatus`
- `semanticTrustFindingList`

Dynamic playback and AI proposal queries expose local simulated fixtures; they
are feature-owned contracts but not evidence of live streaming or a deployed
AI recommendation system.

## Internal and legacy query IDs

The approved manifest also contains backend-only contracts. They are not
frontend product queries:

- internal inspection: `fixtureNamedGraphInventory`,
  `fixtureIncidentSummary`, `fixtureProvenanceSourceRecords`,
  `semanticActionAuditHistoryByRelease`, and
  `semanticActionAuditHistoryByTarget`
- legacy read models: `semanticImpactSummary`, `semanticStageBottlenecks`,
  `semanticAssetDelaySummary`, `semanticZoneDelaySummary`, and
  `semanticSpareWaitSummary`

Approval means a query can be executed through the private controlled boundary;
it does not by itself make that query a product surface.

## Action availability and mutation boundary

`semanticAvailableActionsByFinding` returns one of:

- `AVAILABLE_FOR_LOCAL_AUDIT`: required source-backed target facts exist for a
  supported local audit action.
- `DISABLED`: the backend supplies the missing-target or runtime reason.

The frontend renders that status and cannot promote `DISABLED` to available.
The action request and transition routes write managed action-audit graph facts
only. They do not mutate canonical, reasoning, provenance, source, operations,
production, or external systems.

## Contract references

- `queries/manifest.ttl`: approved query definitions
- `semantic-service/src/main/kotlin/com/dcai/semanticservice/query/QueryContractRegistry.kt`:
  codecs and owners
- `frontend/src/semanticQueryCatalog.ts`: feature-owned frontend catalog
- `semantic-service/api-dtos.md`: response fields
- `semantic-service/openapi.semantic-service.yaml`: route and schema contract
