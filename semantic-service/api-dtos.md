# Semantic Service API DTO Scaffold

This document describes request and response DTO boundaries for the Kotlin
semantic service. Phase 18 defined the response contract before HTTP runtime
existed. Post-Phase-20 now implements an internal-only private query endpoint
for approved fixture inspection and product read-model query IDs. Public
exposure, DTO generation, reasoning endpoints, and graph-write commands remain
out of scope.

## Phase 18 Response Contract Checkpoint

Future semantic query responses use typed result envelopes instead of raw
binding rows. The response contract is intentionally stable before any HTTP
runtime exists.

Shared response fields:

- `queryId`: approved query identifier from `queries/manifest.ttl`
- `resultType`: stable semantic result category
- `recordCount`: number of typed records in `records`
- `records`: typed records for the selected result category
- `provenance`: query id, graph scope, and result contract version

Supported Phase 18 response result types:

- `named-graph-inventory`
- `incident-summary`
- `provenance-source-records`
- `follow-up-queue`
- `dashboard-overview`
- `filter-metadata`
- `follow-up-detail`
- `impact-summary`
- `topology-dependencies`
- `trust-findings`
- `stage-bottlenecks`
- `asset-delay-summary`
- `zone-delay-summary`
- `spare-wait-summary`
- `validation-summary`
- `incident-evidence`
- `incident-timeline`
- `dependency-impact`
- `blast-radius`
- `ontology-review-queue`
- `action-availability`
- `action-audit-history`
- `action-notification-queue`
- `action-review-queue`
- `action-transition-history`
- `action-dispatch-queue`
- `dynamic-playback`
- `ai-proposal-review-queue`
- `ai-proposal-detail`

Versioning rules:

- OpenAPI scaffold version: `2026-06-phase18-response-contract-checkpoint`
- Query result provenance contract: `2026.06.phase17-result-envelope`
- Error envelope contract: `2026.06.phase18-error-envelope`
- Any breaking field rename, required-field change, result type removal, or
  record shape change must create a new contract version.
- Additive optional fields may keep the same response checkpoint only when
  existing required fields and result-type names remain stable.

## Query Execution

Endpoint shape: `POST /semantic/query/{queryId}`

Post-Phase-20 implementation status:

- implemented as an internal/private loopback endpoint
- allowed query IDs are limited to `fixtureNamedGraphInventory`,
  `fixtureIncidentSummary`, `fixtureProvenanceSourceRecords`, and
  product read-model query IDs currently approved in `queries/manifest.ttl`
- success responses are produced by `SemanticResponseSerializer`
- semantic errors use the Phase 18 error envelope
- request bodies must not contain raw SPARQL, arbitrary query text, SPARQL
  Update, or replacement query definitions
- request bodies may include string-valued `parameters` for approved lookup
  query IDs; unsupported parameter names are rejected before execution
- product workbench read-model query IDs are implemented only when backed by
  approved SPARQL, typed envelopes, shaper support, serializer support, and
  private endpoint tests

Request DTO:

- `queryId`: approved query identifier from `queries/manifest.ttl`
- `parameters`: string-valued query parameters
- `graphScopes`: allowed graph scopes such as canonical, provenance,
  reasoning, reasoning-audit, and ai-audit
- `timeoutMs`: optional timeout budget

Response DTO:

- `queryId`: executed query identifier
- `resultType`: one of the supported Phase 18 response result types
  including `action-dispatch-queue` and `dynamic-playback`
- `recordCount`: number of typed records
- `records`: typed records matching `resultType`
- `provenance`: `queryId`, `graphScope`, and `contractVersion`

Named graph inventory record:

- `graphUri`: named graph IRI
- `subjectCount`: subject count from the approved inspection query

Incident summary record:

- `graphUri`: named graph IRI
- `incidentUri`: incident resource IRI
- `incidentId`: incident identifier
- `assetUri`: affected asset resource IRI
- `stageUri`: workflow stage resource IRI
- `sourceRecordUri`: optional source record resource IRI

Provenance source record:

- `graphUri`: named graph IRI
- `sourceRecordUri`: source record resource IRI
- `sourceRecordId`: source-system record identifier
- `sourceSystemUri`: source system resource IRI
- `payloadHash`: source payload hash
- `activityUri`: provenance import activity resource IRI

Follow-up queue record:

- `graphUri`: named graph IRI
- `incidentUri`: incident resource IRI
- `incidentId`: incident identifier
- `assetUri`: affected asset resource IRI
- `assetId`: asset identifier
- `zoneUri`: infrastructure zone resource IRI
- `zoneId`: zone identifier
- `stageUri`: current workflow stage resource IRI
- `stageLabel`: optional current stage label
- `sourceRecordUri`: source record resource IRI for row provenance
- `priorityRank`: optional graph-backed follow-up queue rank
- `requestTitle`: optional graph-backed follow-up title
- `currentStatus`: optional graph-backed operational status
- `hoursInCurrentStage`: optional current-stage duration in hours
- `neededByAt`: optional needed-by timestamp
- `priorityLevel`: optional priority level
- `businessImpact`: optional business impact summary
- priority score inputs: optional `assetCriticalityScore`, `downtimeScore`,
  `stageDelayScore`, `infrastructureZoneImpactScore`,
  `neededByUrgencyScore`, `repeatFailureScore`, `spareRiskScore`,
  `capacityRiskScore`, `redundancyRiskScore`, `thermalRiskScore`,
  `vendorEtaRiskScore`, `mitigationCreditScore`, and `totalPriorityScore`

Dashboard overview record:

- `graphUri`: named graph IRI
- `totalIncidents`: count of canonical infrastructure incidents
- `assetCount`: count of canonical infrastructure assets
- `zoneCount`: count of canonical infrastructure zones
- `impactObservationCount`: count of impact observations
- `capacityRiskKw`: summed capacity risk in kW
- `affectedGpuCount`: summed affected GPU count
- `dependencyEdgeCount`: count of dependency edges
- `trustFindingCount`: count of trust findings
- optional runtime totals: `avgDurationHours`, `totalDurationHours`,
  `totalDelayHours`, `mitigatedIncidentCount`, `affectedRackCount`,
  `thermalBreachMinutes`, `redundancyLostIncidentCount`, and
  `vendorEtaMissedCount`
- optional specialty counters: `repeatFailureAssetCount` and
  `engineerAssignmentDelayHours`

Filter metadata record:

- `graphUri`: named graph IRI
- `filterType`: filter group such as `zone`, `asset`, `assetType`, or `stage`
- `resourceUri`: resource IRI backing the option
- `id`: stable filter identifier
- `label`: optional display label
- `sourceRecordUri`: optional source record resource IRI

Follow-up detail record:

- includes the follow-up queue row fields
- `impactUri`: optional impact observation IRI
- `capacityRiskKw`: optional selected incident capacity risk
- `affectedGpuCount`: optional affected GPU count
- `followUpDecisionUri`: optional derived follow-up decision IRI
- `recommendedAction`: optional graph-backed recommended action
- `recoveryBlockerUri`: optional recovery blocker IRI
- `blockerSummary`: optional recovery blocker summary
- `trustFindingUri`: optional trust finding IRI
- `trustSummary`: optional trust finding summary
- specialty counters: optional `repeatFailureAssetCount` and
  `engineerAssignmentDelayHours`
- impact state fields: optional `redundancyState`, `affectedRackCount`,
  `estimatedGpuCapacityRiskPct`, `thermalBreachMinutes`,
  `powerRedundancyLost`, `coolingRedundancyLost`, `mitigationStatus`,
  `vendorEtaAt`, and `vendorStatus`

Impact summary record:

- `graphUri`: named graph IRI
- `impactObservationCount`: count of impact observations
- `incidentCount`: count of incidents with impact observations
- `capacityRiskKw`: summed capacity risk in kW
- `affectedGpuCount`: summed affected GPU count
- `trustFindingCount`: count of trust findings tied to impacts
- optional impact totals: `affectedRackCount`, `thermalBreachMinutes`,
  `redundancyLostIncidentCount`, `vendorEtaMissedCount`, and
  `mitigatedIncidentCount`

Topology dependency record:

- `graphUri`: named graph IRI
- `dependencyEdgeUri`: dependency edge IRI
- `dependencyId`: dependency edge identifier
- `dependentAssetUri`: dependent/downstream asset IRI
- `dependentAssetId`: dependent/downstream asset identifier
- `dependencyAssetUri`: dependency/upstream asset IRI
- `dependencyAssetId`: dependency/upstream asset identifier
- `dependencyRole`: role of the dependency
- `impactScope`: optional impact scope
- `dependencyPathUri`: optional dependency path IRI
- `pathId`: optional dependency path identifier
- `sourceRecordUri`: source record resource IRI for row provenance

Trust finding record:

- `graphUri`: named graph IRI
- `trustFindingUri`: trust finding IRI
- `trustFindingId`: optional stable trust finding identifier
- `summary`: finding summary
- `sourceFactUri`: source fact IRI used by the finding
- `activityUri`: optional reasoning activity IRI
- `severity`: optional semantic severity
- `status`: optional confidence or validation status
- `createdAt`: optional finding creation timestamp

Stage bottleneck record:

- `graphUri`: named graph IRI
- `stageUri`: workflow stage IRI
- `stageLabel`: optional stage label
- `incidentCount`: incident count currently at the stage
- optional duration fields: `delayedCount`, `avgDurationHours`,
  `p90DurationHours`, and `totalDelayHours`
- `sourceRecordUri`: sampled source record IRI for provenance

Asset delay summary record:

- `graphUri`: named graph IRI
- `assetUri`: asset IRI
- `assetId`: asset identifier
- `zoneUri`: zone IRI
- `zoneId`: zone identifier
- `incidentCount`: incident count linked to the asset
- `impactObservationCount`: impact observation count linked to the asset
- `capacityRiskKw`: summed capacity risk in kW
- `affectedGpuCount`: summed affected GPU count
- optional delay fields: `delayedIncidentCount`, `totalDurationHours`,
  `avgDurationHours`, and `topFailureMode`
- `repeatFailureCount`: optional repeat-failure count for the asset
- `sourceRecordUri`: source record IRI for asset provenance

Zone delay summary record:

- `graphUri`: named graph IRI
- `zoneUri`: zone IRI
- `zoneId`: zone identifier
- `assetCount`: asset count in the zone
- `incidentCount`: incident count linked to zone assets
- `impactObservationCount`: impact observation count linked to zone assets
- `capacityRiskKw`: summed capacity risk in kW
- `affectedGpuCount`: summed affected GPU count
- optional delay fields: `delayedIncidentCount`, `criticalIncidentCount`,
  `totalDurationHours`, and `topBottleneckStage`
- `sourceRecordUri`: source record IRI for zone provenance

Spare wait summary record:

- `graphUri`: named graph IRI
- `stageUri`: workflow stage IRI
- `stageLabel`: optional stage label
- `incidentCount`: incidents in spare/vendor/waiting stages
- `recoveryBlockerCount`: recovery blocker count linked to those incidents
- optional wait fields: `totalWaitHours`, `avgWaitHours`, and `stockStatus`
- `sourceRecordUri`: sampled source record IRI for provenance

Validation summary record:

- `graphUri`: named graph IRI
- `sourceRecordCount`: source record count
- `incidentCount`: incident count
- `incidentWithProvenanceCount`: incidents carrying source provenance
- `assetCount`: asset count
- `assetWithProvenanceCount`: assets carrying source provenance

Incident evidence record:

- `graphUri`: named graph IRI
- `incidentUri`: incident IRI
- `incidentId`: incident identifier
- `stageUri`: current workflow stage IRI
- `stageLabel`: optional current workflow stage label
- `sourceRecordUri`: incident source record IRI
- `impactUri`: optional impact observation IRI
- `evidenceUri`: optional supporting evidence IRI
- `evidenceClassUri`: optional evidence class IRI
- `evidenceTimestamp`: optional evidence timestamp
- `confidenceState`: optional evidence confidence state
- telemetry fields: optional `metricName`, `metricValue`, `metricUnit`, and
  `telemetryStatus`
- telemetry alert fields: optional `telemetryAlertId`, `alertType`,
  `alertSeverity`, `alertTriggeredAt`, and `alertResolvedAt`
- validation fields: optional `validationId`, `validationStatus`,
  `validatorId`, `validationStartedAt`, `validationCompletedAt`, and
  `failureReason`
- work-order fields: optional `workOrderId`, `assignedTeam`,
  `assignedEngineerId`, `workOrderStatus`, `plannedStartAt`, `actualStartAt`,
  `actualCompletedAt`, `requiredSpareId`, `requiredSpareName`, and
  `stockStatus`
- `trustFindingUri`: optional trust finding IRI
- `trustSummary`: optional trust finding summary

Incident timeline record:

- `graphUri`: named graph IRI
- `incidentUri`: incident IRI
- `incidentId`: incident identifier
- `eventUri`: workflow event IRI
- `eventId`: optional source/system event identifier
- `stageUri`: workflow stage IRI
- `stageLabel`: optional workflow stage label
- `eventStatus`: optional event status
- `enteredAt`: optional stage entry timestamp
- `exitedAt`: optional stage exit timestamp
- `durationHours`: optional stage duration in hours
- `thresholdHours`: optional stage threshold in hours
- `delayHours`: optional duration above threshold
- `sourceRecordUri`: source record IRI for event provenance

Dependency impact record:

- `graphUri`: named graph IRI
- `assetUri`: asset IRI
- `assetId`: asset identifier
- `dependencyEdgeUri`: optional dependency edge IRI
- `dependencyId`: optional dependency edge identifier
- `dependencyAssetUri`: optional upstream dependency asset IRI
- `dependencyAssetId`: optional upstream dependency asset identifier
- `dependencyRole`: optional dependency role
- `impactScope`: optional dependency impact scope
- `findingUri`: optional dependency impact finding IRI
- `findingSummary`: optional dependency impact finding summary
- `sourceRecordUri`: optional dependency source record IRI

Blast radius record:

- `graphUri`: named graph IRI
- `assetUri`: asset IRI
- `assetId`: asset identifier
- `downstreamAssetUri`: optional downstream asset IRI
- `downstreamAssetId`: optional downstream asset identifier
- `incidentUri`: optional incident IRI
- `incidentId`: optional incident identifier
- `findingUri`: optional blast-radius finding IRI
- `findingSummary`: optional blast-radius finding summary

Action availability record:

- `graphUri`: canonical graph IRI used to derive the selected finding
- `incidentUri`: selected incident IRI
- `incidentId`: selected incident identifier
- `assetUri`: selected incident asset IRI
- `assetId`: selected incident asset identifier
- `sourceRecordUri`: selected incident source record IRI
- `actionId`: controlled ontology action identifier
- `actionLabel`: action label
- `actionDescription`: read-only action description
- `actionStatus`: disabled action status for browser display
- `uiPlacement`: selected finding UI placement such as `summary` or `trust`
- `detailKind`: row category for target objects, required parameters,
  preconditions, provenance requirements, or disabled reasons
- `detailRole`: semantic role for the detail row
- `detailLabel`: human-readable detail label
- `detailValue`: target object IRI or scalar detail value
- `detailSortOrder`: stable ordering within the action detail category

Ontology review queue record:

- `graphUri`: graph IRI where the review item was observed
- `queueId`: stable review queue item identifier
- `queueKind`: `promotion-batch`, `reasoning-refresh`, or
  `reasoning-approval`
- `reviewActionId`: governed action contract associated with the queue item
- `reviewActionLabel`: display label for the internal review action
- `reviewStatus`: read-only lifecycle state
- `targetUri`: primary graph, activity, or finding URI under review
- `targetType`: target ontology or graph object type
- `targetLabel`: release id, finding summary, or target label
- `releaseId`: source promotion release id or reasoning run id
- `sourceGraphUri`, `canonicalGraphUri`, `provenanceGraphUri`,
  `reasoningAuditGraphUri`, `reasoningGraphUri`: optional managed graph URIs
- `evidenceSummary`: graph-backed evidence summary for why the item appears
- `actionStatus`: always `DISABLED` in this read-only slice
- `disabledReason`: reason browser action execution is unavailable
- `incidentCount`, `assetCount`, `sourceRecordCount`, `activityCount`,
  `generatedFactCount`: graph-backed review counts
- `prioritySortOrder`: stable ordering for internal review queues

Action audit history record:

- `graphUri`: managed action-audit named graph IRI
- `actionAuditReleaseId`: release suffix for the action-audit graph
- `executionUri`: ontology action execution IRI
- `executionId`: execution identifier
- `requestUri`: ontology action request IRI
- `requestId`: request identifier
- `validationReportUri`: action validation report IRI
- `actionTypeUri`: controlled action type IRI
- `actionTypeId`: controlled action type identifier
- `actionTypeLabel`: optional action type label
- `idempotencyKey`: deterministic idempotency key
- `actorId`: internal actor identifier recorded by the audit runner
- `actionReason`: operator or process reason
- `actionStatus`: action audit status
- `requestedAt`: request timestamp
- `executedAt`: audit execution timestamp
- `targetObjectUri`: optional target object IRI
- `validationStatus`: action validation status
- `validationSummary`: optional validation summary
- `sourceRecordUri`: optional source record provenance IRI
- `assignedTeam`: optional assigned team
- `assigneeId`: optional assignee identifier
- `reviewedStatus`: optional review status
- `reviewSummary`: optional review summary
- `supportingEvidenceUri`: optional supporting evidence IRI

Action review queue record:

- `graphUri`: managed action-audit named graph IRI
- `actionAuditReleaseId`: release suffix for the action-audit graph
- `notificationUri`: local ontology action notification IRI
- `notificationId`: notification identifier
- `executionUri`: ontology action execution IRI under review
- `executionId`: execution identifier
- `requestUri`: ontology action request IRI
- `requestId`: request identifier
- `actionTypeUri`: controlled action type IRI
- `actionTypeId`: controlled action type identifier
- `actorId`: internal actor identifier
- `actionReason`: operator or process reason
- `currentState`: latest lifecycle state, one of `REQUESTED`, `VALIDATED`,
  `QUEUED`, `IN_REVIEW`, `APPROVED`, `REJECTED`, or `CLOSED`
- `stateGeneratedAt`: timestamp of the latest transition
- `incidentUri`: selected incident IRI
- `incidentId`: selected incident identifier
- `sourceRecordUri`: optional source record provenance IRI

Action transition history record:

- `graphUri`: managed action-audit named graph IRI
- `actionAuditReleaseId`: release suffix for the action-audit graph
- `transitionUri`: lifecycle transition activity IRI
- `transitionId`: transition identifier
- `executionUri`: ontology action execution IRI
- `executionId`: execution identifier
- `requestUri`: ontology action request IRI
- `requestId`: request identifier
- `actionTypeUri`: controlled action type IRI
- `actionTypeId`: controlled action type identifier
- `actorId`: actor that requested the lifecycle transition
- `transitionReason`: operator or process reason for the transition
- `fromState`: optional previous lifecycle state
- `toState`: new lifecycle state
- `generatedAt`: transition timestamp
- `incidentUri`: selected incident IRI
- `incidentId`: selected incident identifier

Action dispatch queue record:

- `graphUri`: managed action-audit named graph IRI
- `actionAuditReleaseId`: release suffix for the action-audit graph
- `dispatchUri`: simulated dispatch fact IRI
- `dispatchId`: dispatch identifier
- `dispatchChannel`: one of `NOC_QUEUE`, `WORK_ORDER_QUEUE`, or
  `VALIDATION_REVIEW_QUEUE`
- `dispatchStatus`: simulated queue state such as `SIMULATED_QUEUED`
- `dispatchLifecycleState`: lifecycle state that produced the dispatch,
  currently `APPROVED`
- `dispatchSummary`: local review summary describing the simulated dispatch
- `executionUri`: ontology action execution IRI
- `executionId`: execution identifier
- `requestUri`: ontology action request IRI
- `requestId`: request identifier
- `actionTypeUri`: controlled action type IRI
- `actionTypeId`: controlled action type identifier
- `transitionUri`: approval transition activity IRI
- `transitionId`: approval transition identifier
- `actorId`: actor that approved the local action
- `generatedAt`: dispatch fact timestamp
- `incidentUri`: selected incident IRI
- `incidentId`: selected incident identifier
- `sourceRecordUri`: optional source record provenance IRI

AI proposal record:

- `graphUri`: managed ai-audit named graph IRI
- `aiAuditReleaseId`: release suffix for the ai-audit graph
- `proposalUri`: AI proposal resource IRI
- `proposalId`: deterministic proposal identifier
- `proposalType`: one of `REASONING_FINDING_SUGGESTION`,
  `ACTION_RECOMMENDATION`, or `EVIDENCE_SUMMARY`
- `proposalStatus`: local proposal lifecycle status, currently
  `PENDING_REVIEW`
- `reviewStatus`: human review status, currently `PENDING_HUMAN_REVIEW`
- `disabledReason`: reason approve/reject mutation is unavailable
- `summary`: proposal summary for review
- `rationale`: source-supported rationale text
- `confidenceScore`: local confidence policy score from `0.5` to `1.0`
- `riskLevel`: one of `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`
- `modelId`, `promptId`, and `promptHash`: deterministic model/prompt metadata
  placeholders; no external AI API is called
- `actorId`: local AI governance simulator actor
- `generatedAt`: proposal generation timestamp
- `batchUri` and `batchId`: proposal batch provenance
- `validationReportUri`, `validationStatus`, and `validationSummary`:
  validation gate output
- `incidentUri` and `incidentId`: selected incident target
- `targetObjectUri`: proposed target object
- `sourceRecordUri`: required source record provenance reference
- `supportingEvidenceUri`: required canonical or reasoning evidence reference

Dynamic playback record:

- `graphUri`: managed action-audit named graph IRI containing playback facts
- `actionAuditReleaseId`: release suffix for the action-audit graph
- `eventUri`: dynamic playback event IRI
- `eventId`: deterministic playback event identifier
- `scenarioId`: deterministic local playback scenario identifier
- `playbackBatchId`: replay batch identifier
- `playbackStep`: ordered replay step number
- `incidentUri`: selected incident IRI
- `incidentId`: selected incident identifier
- `eventKind`: controlled local event category such as telemetry impact,
  validation conflict, recovery blocker, or restore readiness change
- `sourceFamily`: source-system export family that produced the event
- `occurredAt`: replay event timestamp
- `summary`: human-readable semantic delta summary
- `sourceRecordUri`: promoted source record provenance IRI
- `beforeState` and `afterState`: canonical graph state labels before/after
  the replay step
- `beforeReasoningState` and `afterReasoningState`: reasoning state labels
  before/after refresh
- `beforeTrustState` and `afterTrustState`: trust state labels before/after
  the replay step
- `beforeBlastRadiusCount` and `afterBlastRadiusCount`: inferred exposure
  counts before/after refresh
- `actionLifecycleState`: local action lifecycle state associated with the
  replay step
- `canonicalGraphUri`, `provenanceGraphUri`, and `reasoningGraphUri`:
  optional graph references used by the playback fact

Internal ontology action lifecycle:

- `POST /semantic/internal/action-request` creates an audited local request,
  validation report, notification, and initial `REQUESTED -> VALIDATED ->
  QUEUED` transition chain in the managed action-audit graph.
- `POST /semantic/internal/action-transition` moves an existing action
  execution through controlled local states only. It is an internal/private
  boundary and does not mutate source, canonical, provenance, reasoning,
  operations, production, or external source-system state.
- When a local action transition reaches `APPROVED`, the service creates
  simulated `NOC_QUEUE`, `WORK_ORDER_QUEUE`, and `VALIDATION_REVIEW_QUEUE`
  dispatch records in the managed action-audit graph. These are internal
  notification facts only, not external writeback.

Internal AI governance review lifecycle:

- `POST /semantic/internal/ai-proposal-review` records a human approve/reject
  decision for a managed AI proposal using string-only DTO fields.
- Review decisions write only managed ai-audit graph facts.
- Approved `ACTION_RECOMMENDATION` proposals create a governed ontology action
  request in the managed action-audit graph through the existing action
  validation and provenance gates.
- The endpoint reports `canonicalGraphMutation`, `reasoningGraphMutation`,
  `provenanceGraphMutation`, `sourceGraphMutation`, `operationsGraphMutation`,
  and `externalSystemMutation` as `false`.

Error DTO:

- `error.code`: stable machine-readable semantic service error code
- `error.message`: human-readable error text
- `error.detail`: optional diagnostic detail
- `error.queryId`: optional query id related to the failure
- `error.contractVersion`: error envelope contract version

Initial semantic query error codes:

- `unapproved-query-id`
- `unsupported-result-envelope`
- `missing-required-binding`
- `graph-unavailable`
- `contract-validation-failed`
- `internal-semantic-service-error`

Phase 19 internal serialization:

- `SemanticResponseSerializer` converts Phase 17 result envelopes into
  Phase 18-shaped in-memory response maps.
- It also converts approved semantic error codes into the Phase 18
  `SemanticErrorResponse` map shape.
- Post-Phase-20 wraps this serializer in a private loopback HTTP boundary for
  approved inspection and product read-model queries.

Phase 20 endpoint readiness:

- `endpoint-readiness.ttl` keeps the runtime internal-only for Phase 20.
- A later private endpoint scaffold must use `SemanticResponseSerializer`.
- A later endpoint must not return raw SPARQL bindings, accept arbitrary
  browser-supplied SPARQL, run SPARQL Update, or bypass the approved query
  manifest.

Post-Phase-20 private endpoint:

- the private scaffold now exists for the first approved-query slice
- it remains internal-only and loopback-bound
- the public endpoint gates in `endpoint-readiness.ttl` are still not accepted

## Reasoning Validation

Endpoint shape: `POST /semantic/reasoning/validate`

Request DTO:

- `candidateGraph`: expected to be `urn:dcai:graph:reasoning-audit`
- `shapeSet`: expected to reference `shapes/reasoning-output-validation.ttl`
- `candidateIds`: optional candidate resource identifiers

Response DTO:

- `conforms`: SHACL validation result
- `findings`: validation findings with severity, message, source shape, and
  focus node

## Provenance Lookup

Endpoint shape: `GET /semantic/provenance/{resourceId}`

Response DTO:

- `resourceId`: requested resource identifier or encoded IRI
- `lineage`: subject, predicate, object edges describing provenance

## Promotion Review

Endpoint shape: `POST /semantic/promotion/review`

Request DTO:

- `candidateGraph`: expected to be `urn:dcai:graph:reasoning-audit`
- `candidateIds`: candidate findings to review
- `reviewerId`: optional reviewer identity placeholder

Response DTO:

- `reviewStatus`: approved, rejected, or needs-review
- `promotionAllowed`: whether a future promotion step may proceed
- `reasons`: review explanations

## AI Governance Handoff

Endpoint shape: `POST /semantic/ai-governance/handoff`

Request DTO:

- `proposalId`: AI proposal identifier
- `proposedGraph`: expected to be an AI audit graph
- `riskClass`: low, medium, or high
- `requestedBy`: optional requester identity placeholder

Response DTO:

- `handoffId`: governance workflow identifier
- `governanceStatus`: queued, rejected, or needs-human-review
- `requiredGates`: required validation, provenance, or approval gates
