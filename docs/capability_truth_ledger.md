# Capability Truth Ledger

Status meanings:

- `IMPLEMENTED`: executable code exists and is covered by a test or one of the
  reproducible scenarios.
- `SIMULATED`: executable behavior uses deterministic local data rather than a
  real source or organization.
- `PARTIAL`: a bounded subset is implemented; the stated gap is material.
- `DESIGN_ONLY`: documented direction with no corresponding runtime capability.
- `NOT_IMPLEMENTED`: explicitly absent and must not be implied.

## Implemented

| Capability | Exact boundary | Executable evidence |
| --- | --- | --- |
| Recorded scenario generation | Seeded `demo`, `mvp`, and `stress` CSV exports | `RecordedSourceScenarioGenerator.kt`, `RecordedSourceScenarioGeneratorTest.kt` |
| Recorded-source loading and quarantine | One local CSV contract; supported invalid and duplicate rows are rejected | `RecordedSourceConnectorSimulation.kt`, its tests, scenario generator test |
| RDF mapping | Source DTOs map to source, canonical, and provenance models | `SourceExtractRdfMapper.kt`, `SourceExtractRdfMapperTest.kt` |
| Current-stage mapping | `IncidentSourceRecord.currentStageId` is copied to `dcai:hasCurrentStage` | `SourceExtractRdfMapper.mapIncident`, mapper tests |
| Graph promotion gates | Canonical/source/provenance models must pass validation before managed writes | `GraphPromotionService.kt`, `ProductionGraphValidationGate.kt`, promotion tests |
| Rollback under injected write failure | Managed multi-graph replacement restores snapshots in tested failure cases | `ManagedGraphWriteCoordinator.kt` and promotion/reasoning/action rollback tests |
| Deterministic reasoning | Restore readiness, blockers, trust, dependency impact, and blast radius | `ReasoningModelBuilder.kt`, `ReasoningModelBuilderTest.kt` |
| Direct dependency reasoning | A finding follows one explicit `DependencyEdge` and its path | `ReasoningModelBuilder.dependencyExposureFindings`, scenario 2 |
| Approved query boundary | Manifest query IDs are bound to codecs, owners, and optional paging policies | `QueryContractRegistry.kt`, `QueryContractRegistryTest.kt` |
| Private typed query service | Loopback endpoint executes approved IDs and serializes typed envelopes | `PrivateSemanticQueryEndpoint.kt`, endpoint and serializer tests |
| Server-owned stable paging | Selected queue/review/status read models count and page stable identities | `QueryPagingPolicy.kt`, paged executor/endpoint tests |
| Operator workspaces | Recovery Queue, Recovery Case, Review Inbox, and Platform Status | `frontend/src/features`, frontend tests and production build |
| Backend-authoritative action availability | Query returns `AVAILABLE_FOR_LOCAL_AUDIT` or `DISABLED`; UI cannot enable disabled actions | `semantic_available_actions_by_finding.select.rq`, `actionUtils.ts`, Kotlin/frontend tests |
| Local governed action audit | Three supported requests and controlled transitions write managed audit facts only | `OntologyActionAuditService.kt`, private endpoints, action tests |

Paths above are relative to `semantic-service/src/main/kotlin/com/dcai/semanticservice/`,
`semantic-service/src/test/kotlin/com/dcai/semanticservice/`, or the repository
root as appropriate.

## Simulated

| Capability | Exact boundary | Evidence |
| --- | --- | --- |
| Operational source families | Incident, workflow, work order, validation, telemetry/impact, asset, zone, and topology files share one recorded connector format | `fixtures/source-extracts/connector-contracts/recorded-source-system-v1.properties` |
| Three public incidents | Synthetic rows with stable IDs and timestamps | `docs/10_operational_case_study.md`, committed MVP CSV files |
| Volume fixtures | 48-scenario MVP and 600-scenario stress generation | generator profiles and tests; this is fixture scale, not production load proof |
| Dynamic playback | Local pre-authored playback scenario | `LocalDynamicPlaybackScenario.kt`; not a live event stream |
| AI proposals | Deterministic local proposal/audit fixtures and governed review path | governance package and fixtures; not a deployed AI recommendation service |

## Partial

| Capability | Implemented portion | Missing portion |
| --- | --- | --- |
| Source preservation | CSV artifacts remain in the repository; RDF records IDs and payload hashes | Complete raw payload bodies are not stored in RDF or an immutable source archive |
| Heterogeneous integration | One schema labels and exercises several source families | No source-specific DCIM, BMS, CMMS, telemetry, or incident connector exists |
| Queue prioritization | Read model consumes optional fixture rank/score/recommended-action facts and has stable paging | The current reasoner does not derive those facts; there is no validated operational ranking model |
| Workflow history | Source-provided current stage plus mapped event timeline | No event-sourced reconstruction of current state |
| Topology impact | Direct one-edge upstream exposure and direct reverse-edge blast-radius findings | No recursive traversal, path aggregation, cycle policy, or real topology discovery |
| Provenance | Source identity, record ID, payload hash, import/promotion/reasoning derivation | No external immutable evidence store, signatures, or end-to-end source attestation |
| Actions | Local validation, idempotency, audit facts, and lifecycle transitions | No authenticated operator identity, external dispatch, or operational execution |
| Verification | Unit/contract/in-memory integration tests plus compile/lint/build checks | No browser E2E suite, production environment test, or enforced coverage threshold |
| Demonstration | Documented local multi-command workflow | No one-command end-to-end demo or recorded walkthrough |

## Design only

- Source-specific production connector architecture.
- Recursive multi-hop dependency traversal and graph-versus-relational
  production benchmark.
- Production deployment, backup/recovery, observability, SLOs, and orchestration.
- Domain-expert ontology and action-policy approval.
- External action/writeback integration.

## Not implemented

- Real customer discovery or user research.
- Measured business ROI, incident reduction, or operator-time savings.
- Authentication, authorization, TLS termination, or public API exposure.
- Real infrastructure control or work-order mutation.
- Production readiness or production load testing.
- Runtime derivation of queue rank, priority score, or recommended action.
- Recursive/multi-hop dependency or blast-radius reasoning.
- Complete raw source payload storage in RDF.
