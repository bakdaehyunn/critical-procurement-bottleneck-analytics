# API Package

Contains the post-Phase-20 private semantic query and internal ontology action
endpoint boundary.

Implemented boundary:

- `PrivateSemanticQueryEndpoint`
- `PrivateOntologyActionEndpoint`
- internal `POST /semantic/query/{queryId}` request handling
- internal `POST /semantic/internal/action-request` request handling for
  selected audit-only ontology actions
- internal `POST /semantic/internal/action-transition` request handling for
  controlled local action lifecycle transitions
- internal `POST /semantic/internal/ai-proposal-review` request handling for
  controlled AI proposal approve/reject review decisions
- loopback-only `PrivateSemanticQueryEndpointServer`
- separate endpoint composition, HTTP transport, and JSON payload writer
- success/error payloads through `SemanticResponseSerializer`
- optional string-valued `parameters`; approved paged queries additionally
  accept typed `page`/`pageSize` values that are removed before SPARQL binding
- fixed string-valued action request, transition, and AI proposal review DTO
  fields; graph URIs are derived from controlled release/run IDs server-side
- structured OpenAPI route parity is checked by the runtime contract validator;
  implemented action routes reference typed request and response schemas

Allowed query IDs are derived from `QueryContractRegistry`. The current
manifest-backed set is:

- `fixtureNamedGraphInventory`
- `fixtureIncidentSummary`
- `fixtureProvenanceSourceRecords`
- `semanticFollowUpQueueList`
- `semanticDashboardOverview`
- `semanticFilterMetadata`
- `semanticFollowUpDetail`
- `semanticImpactSummary`
- `semanticTopologyDependencies`
- `semanticTrustFindingList`
- `semanticStageBottlenecks`
- `semanticAssetDelaySummary`
- `semanticZoneDelaySummary`
- `semanticSpareWaitSummary`
- `semanticValidationSummary`
- `semanticIncidentEvidence`
- `semanticIncidentTimeline`
- `semanticDependencyImpactByAsset`
- `semanticBlastRadiusByAsset`
- `semanticPromotionReviewQueue`
- `semanticReasoningReviewQueue`
- `semanticAvailableActionsByFinding`
- `semanticActionAuditHistoryByRelease`
- `semanticActionAuditHistoryByIncident`
- `semanticActionAuditHistoryByTarget`
- `semanticActionNotificationQueueByIncident`
- `semanticActionReviewQueueByIncident`
- `semanticActionTransitionHistoryByIncident`
- `semanticActionDispatchQueueByIncident`
- `semanticDynamicEventTimelineByIncident`
- `semanticDynamicStateChangesByIncident`
- `semanticDynamicReasoningChangesByIncident`
- `semanticDynamicActionLifecycleByIncident`
- `semanticAiProposalReviewQueue`
- `semanticAiProposalDetailByIncident`

Non-goals:

- no public endpoints
- no raw SPARQL request body
- no SPARQL Update
- no canonical/reasoning/provenance/operations graph writes from action
  requests, action transitions, or AI proposals
- no external system writeback
- no authentication, external AI API calls, public AI approval endpoint, or AI
  writeback beyond managed ai-audit/action-audit review facts
