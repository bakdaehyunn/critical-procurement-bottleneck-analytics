# Query Catalog Ownership v1

This note documents the frontend and semantic-service ownership boundary for the
approved ontology-native query catalog. It does not change runtime loading:
`semantic-service` still executes only manifest entries with
`implementationStatus "phase16-approved"`, and the browser still sends approved
query IDs rather than raw SPARQL.

## Runtime Boundary

```text
React semantic workbench
  -> frontend semantic query catalog key
  -> POST /semantic/query/{queryId}
  -> semantic-service approved query catalog
  -> read-only SPARQL file
  -> typed query result envelope
  -> frontend read-model mapper
```

Internal action and AI review commands are separate private endpoints. They are
not part of the read-only query catalog and write only to managed audit graphs.

## Frontend Module Ownership

| Module | Responsibility |
| --- | --- |
| `frontend/src/semanticQueryCatalog.ts` | Stable frontend keys for approved semantic query IDs and query path construction. |
| `frontend/src/semanticRuntimeConfig.ts` | Local semantic-service base URL and controlled release/run IDs from Vite environment variables. |
| `frontend/src/semanticQueryClient.ts` | Approved query POST envelope handling for read-only semantic query IDs. |
| `frontend/src/ontologyActionApi.ts` | Private internal action request and transition submissions into the managed action-audit graph. |
| `frontend/src/aiGovernanceApi.ts` | Private internal AI proposal approve/reject submissions into managed ai-audit and optional action-audit graphs. |
| `frontend/src/api.ts` | Dashboard/detail read-model DTOs, semantic record DTOs, and graph-to-UI mappers. |

## Approved Query Ownership

| Query ID | Result type | Graph scope | Frontend consumer | Runtime status |
| --- | --- | --- | --- | --- |
| `fixtureNamedGraphInventory` | `named-graph-inventory` | Fixture source/canonical inventory | Fixture/diagnostic inspection | `phase16-approved` |
| `fixtureIncidentSummary` | `incident-summary` | Fixture or promoted canonical graph | Fixture/diagnostic inspection | `phase16-approved` |
| `fixtureProvenanceSourceRecords` | `provenance-source-records` | Fixture source/canonical/provenance | Fixture/diagnostic inspection | `phase16-approved` |
| `semanticDashboardOverview` | `dashboard-overview` | Canonical plus reasoning graph | Dashboard overview KPIs | `phase16-approved` |
| `semanticFollowUpQueueList` | `follow-up-queue` | Canonical graph | Dashboard finding list and detail joins | `phase16-approved` |
| `semanticFilterMetadata` | `filter-metadata` | Canonical graph | Dashboard filters | `phase16-approved` |
| `semanticFollowUpDetail` | `follow-up-detail` | Canonical plus reasoning graph | Selected finding detail | `phase16-approved` |
| `semanticImpactSummary` | `impact-summary` | Canonical plus reasoning graph | Dashboard impact summary | `phase16-approved` |
| `semanticTopologyDependencies` | `topology-dependencies` | Canonical graph | Dependency tab and topology summaries | `phase16-approved` |
| `semanticTrustFindingList` | `trust-findings` | Canonical or reasoning graph | Data-quality/trust review surfaces | `phase16-approved` |
| `semanticStageBottlenecks` | `stage-bottlenecks` | Canonical graph | Dashboard bottleneck metrics | `phase16-approved` |
| `semanticAssetDelaySummary` | `asset-delay-summary` | Canonical graph | Asset delay summary | `phase16-approved` |
| `semanticZoneDelaySummary` | `zone-delay-summary` | Canonical graph | Zone delay summary | `phase16-approved` |
| `semanticSpareWaitSummary` | `spare-wait-summary` | Canonical plus reasoning graph | Spare/vendor wait summary | `phase16-approved` |
| `semanticValidationSummary` | `validation-summary` | Canonical graph | Trust tab SHACL/validation evidence | `phase16-approved` |
| `semanticIncidentEvidence` | `incident-evidence` | Canonical plus reasoning graph | Evidence chain and selected finding context | `phase16-approved` |
| `semanticIncidentTimeline` | `incident-timeline` | Canonical graph | Selected finding timeline | `phase16-approved` |
| `semanticDependencyImpactByAsset` | `dependency-impact` | Canonical plus reasoning graph | Dependency exposure context by asset | `phase16-approved` |
| `semanticBlastRadiusByAsset` | `blast-radius` | Canonical plus reasoning graph | Blast-radius context by asset | `phase16-approved` |
| `semanticPromotionReviewQueue` | `ontology-review-queue` | Source/canonical/provenance lifecycle state | Internal review queue panel | `phase16-approved` |
| `semanticReasoningReviewQueue` | `ontology-review-queue` | Canonical, reasoning-audit, and reasoning graphs | Internal review queue panel | `phase16-approved` |
| `semanticAvailableActionsByFinding` | `action-availability` | Canonical plus reasoning graph | Governed action affordance cards | `phase16-approved` |
| `semanticActionAuditHistoryByRelease` | `action-audit-history` | Managed action-audit graph | Action audit diagnostics | `phase16-approved` |
| `semanticActionAuditHistoryByIncident` | `action-audit-history` | Managed action-audit plus canonical graph | Selected finding action panel | `phase16-approved` |
| `semanticActionAuditHistoryByTarget` | `action-audit-history` | Managed action-audit graph | Action audit diagnostics | `phase16-approved` |
| `semanticActionNotificationQueueByIncident` | `action-notification-queue` | Managed action-audit notification state | Selected finding action panel | `phase16-approved` |
| `semanticActionReviewQueueByIncident` | `action-review-queue` | Managed action-audit lifecycle state | Selected finding lifecycle controls | `phase16-approved` |
| `semanticActionTransitionHistoryByIncident` | `action-transition-history` | Managed action-audit lifecycle state | Selected finding transition history | `phase16-approved` |
| `semanticActionDispatchQueueByIncident` | `action-dispatch-queue` | Managed action-audit dispatch simulation state | Selected finding dispatch panel | `phase16-approved` |
| `semanticDynamicEventTimelineByIncident` | `dynamic-playback` | Managed action-audit dynamic playback state | Summary dynamic playback timeline | `phase16-approved` |
| `semanticDynamicStateChangesByIncident` | `dynamic-playback` | Managed action-audit dynamic playback state | Summary dynamic playback graph state | `phase16-approved` |
| `semanticDynamicReasoningChangesByIncident` | `dynamic-playback` | Managed action-audit dynamic playback state | Summary reasoning delta playback | `phase16-approved` |
| `semanticDynamicActionLifecycleByIncident` | `dynamic-playback` | Managed action-audit dynamic playback state | Summary action lifecycle playback | `phase16-approved` |
| `semanticAiProposalReviewQueue` | `ai-proposal-review-queue` | Managed ai-audit plus canonical/provenance graphs | AI proposal queue diagnostics | `phase16-approved` |
| `semanticAiProposalDetailByIncident` | `ai-proposal-detail` | Managed ai-audit plus canonical/provenance graphs | Selected finding AI proposal panel | `phase16-approved` |

## Reference-Only Catalog Entries

`sourceToCanonicalPromotion`, fixture validation references, provenance lineage
lookup, and the historical CONSTRUCT reasoning query entries remain in the
manifest as reference metadata. The active source promotion and reasoning
lifecycles are owned by Kotlin services and managed graph writers, not by
browser-supplied query execution.

## Guardrails

- Frontend code imports approved query IDs from `semanticQueryCatalog`; it does
  not construct arbitrary SPARQL.
- `semanticQueryClient` posts only `{ parameters }` envelopes to
  `/semantic/query/{queryId}`.
- Action and AI review modules call private internal endpoints and keep
  protected graph mutation out of the browser boundary.
- New query IDs must add manifest metadata, a semantic-service result envelope,
  frontend consumer mapping, and this ownership table before being considered
  part of the MVP catalog.
